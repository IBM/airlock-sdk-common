package com.ibm.airlock.common.util;

import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.percentage.Percentile;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;


/**
 * Created by Denis Voloshin on 26/03/2017.
 */

public class RandomUtils {
    /**
     * Generates a new feature-random-map, based on a previous feature-random-map and the legacy random number
     *
     * @param json existing randoms to expand
     * @param legacyUserRandomNumber
     * @param inputFeatureRandoms
     * @throws JSONException exception on case there was a problem reading writing json
     */
    public static JSONObject calculateFeatureRandoms(JSONObject json, int legacyUserRandomNumber,
                                                     @Nullable JSONObject inputFeatureRandoms) throws JSONException {
        JSONObject outFeatureRandoms = new JSONObject();
        JSONObject root = json.getJSONObject(Constants.JSON_FEATURE_FIELD_ROOT);
        inspectFeaturesPercentage(root, inputFeatureRandoms == null ? new JSONObject() : inputFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
        JSONObject experiments = json.optJSONObject(Constants.JSON_FIELD_EXPERIMENTS);
        if (experiments != null) {
            JSONArray experimentsArray = experiments.optJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
            if (experimentsArray != null) {
                inspectExperimentsPercentage(experimentsArray, inputFeatureRandoms == null ? new JSONObject() : inputFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
            }
        }
        JSONObject entitlements = json.optJSONObject(Constants.JSON_FIELD_ENTITLEMENT_ROOT);
        if (entitlements != null) {
            JSONArray purchasesArray = entitlements.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
            if (purchasesArray != null && entitlements.length() > 0) {
                inspectEntitlementsPercentage(purchasesArray, inputFeatureRandoms == null ? new JSONObject() : inputFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
            }
        }
        return outFeatureRandoms;
    }

    public static JSONObject calculateStreamsRandoms(JSONObject streams,
                                                    JSONObject inputStreamsRandoms) throws JSONException {

        JSONArray streamsArray = streams.optJSONArray("streams");

        JSONObject outStreamsRandoms = new JSONObject();

        if (streamsArray != null) {
            for (int i = 0; i < streamsArray.length(); ++i) {
                JSONObject stream = (JSONObject) streamsArray.get(i);
                String name = stream.optString(Constants.JSON_FEATURE_FIELD_NAME, "<unknown>");
                //noinspection DynamicRegexReplaceableByCompiledPattern
                name = name.replaceAll("[. ]", "_");
                if (inputStreamsRandoms.has(name)) {
                    int previousRandom = inputStreamsRandoms.optInt(name);
                    outStreamsRandoms.put(name, previousRandom);
                } else {
                    outStreamsRandoms.put(name, anyRandom());
                }
            }
        }
        return outStreamsRandoms;
    }


    private static void inspectFeaturesPercentage(JSONObject features,
                                                  JSONObject inFeatureRandoms, JSONObject outFeatureRandoms, int legacyUserRandomNumber) throws JSONException {
        Feature.Type type = getNodeType(features);
        if (type == Feature.Type.FEATURE || type == Feature.Type.CONFIGURATION_RULE) {
            String name = getId(features);
            if (inFeatureRandoms.has(name)) {
                int previousRandom = inFeatureRandoms.optInt(name);
                outFeatureRandoms.put(name, previousRandom);
            } else {
                double threshold = features.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
                String b64 = features.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP, "").trim();
                // 0 or 100 percentages will not use the bitmap
                if (threshold <= 0 || threshold >= 100 || b64.isEmpty()) {
                    // allocate random [1 to 1 million inclusive]
                    outFeatureRandoms.put(name, anyRandom());
                } else // legacy percentage, fake a random to capture previous behavior
                {
                    boolean isOn;
                    double bitmapPercentage;
                    try {
                        Percentile percentile = new Percentile(b64);
                        isOn = percentile.isAccepted(legacyUserRandomNumber);
                        bitmapPercentage = percentile.getPercentage();
                    } catch (Exception e) {
                        //throw new JSONException("invalid rollout bitmap for item " + name); // throw this?
                        isOn = false;
                        bitmapPercentage = threshold;
                    }

                    // sometimes the bitmap percentage is different than the giver percentage.
                    // there may be a valid reason (inheritance of bitmaps), or the console may
                    // have changed the bitmap after upgrading to 2.5. The second case is more problematic,
                    // so we prefer to accommodate it by ignoring the bitmap in some cases.

                    if (isOn && threshold < bitmapPercentage) // the percentage has decreased for a tested user
                    {
                        outFeatureRandoms.put(name, anyRandom());
                    } else if (!isOn && threshold > bitmapPercentage) // the percentage has increased for an untested user
                    {
                        outFeatureRandoms.put(name, anyRandom());
                    } else {
                        outFeatureRandoms.put(name, constrainedRandom(threshold, isOn));
                    }
                }
            }
        }

        JSONArray array = features.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                inspectFeaturesPercentage(array.getJSONObject(i), inFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
            }
        }
        array = features.optJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                inspectFeaturesPercentage(array.getJSONObject(i), inFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
            }
        }
    }

    private static void setRandomValue(JSONObject jsonItem, String name, JSONObject inFeatureRandoms, JSONObject outFeatureRandoms, int legacyUserRandomNumber) throws JSONException {
        if (inFeatureRandoms.has(name)) {
            int previousRandom = inFeatureRandoms.optInt(name);
            outFeatureRandoms.put(name, previousRandom);
        } else {
            double threshold = jsonItem.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
            String b64 = jsonItem.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP, "").trim();
            // 0 or 100 percentages will not use the bitmap
            if (threshold <= 0 || threshold >= 100 || b64.isEmpty()) {
                // allocate random [1 to 1 million inclusive]
                outFeatureRandoms.put(name, anyRandom());
            } else // legacy percentage, fake a random to capture previous behavior
            {
                boolean isOn;
                double bitmapPercentage;
                try {
                    Percentile percentile = new Percentile(b64);
                    isOn = percentile.isAccepted(legacyUserRandomNumber);
                    bitmapPercentage = percentile.getPercentage();
                } catch (Exception e) {
                    //throw new JSONException("invalid rollout bitmap for item " + name); // throw this?
                    isOn = false;
                    bitmapPercentage = threshold;
                }

                // sometimes the bitmap percentage is different than the giver percentage.
                // there may be a valid reason (inheritance of bitmaps), or the console may
                // have changed the bitmap after upgrading to 2.5. The second case is more problematic,
                // so we prefer to accommodate it by ignoring the bitmap in some cases.

                if (isOn && threshold < bitmapPercentage) // the percentage has decreased for a tested user
                {
                    outFeatureRandoms.put(name, anyRandom());
                } else if (!isOn && threshold > bitmapPercentage) // the percentage has increased for an untested user
                {
                    outFeatureRandoms.put(name, anyRandom());
                } else {
                    outFeatureRandoms.put(name, constrainedRandom(threshold, isOn));
                }
            }
        }
    }

    private static void inspectEntitlementsPercentage(JSONArray entitlements, JSONObject inFeatureRandoms,
                                                      JSONObject outFeatureRandoms, int legacyUserRandomNumber) throws JSONException {
        for (int i = 0; i < entitlements.length(); i++) {
            inspectEntitlementPercentage(entitlements.optJSONObject(i), inFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
        }
    }

    private static void inspectEntitlementPercentage(JSONObject entitlement, JSONObject inFeatureRandoms,
                                                     JSONObject outFeatureRandoms, int legacyUserRandomNumber) throws JSONException {

        if (entitlement.has(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)) {
            JSONArray entitlements = entitlement.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
            for (int i = 0; i < entitlements.length(); i++) {
                inspectEntitlementPercentage(entitlements.optJSONObject(i), inFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
            }
        }

        String name = entitlement.optString(Constants.JSON_FEATURE_FIELD_NAME);
        String namespace = entitlement.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE);
        setRandomValue(entitlement, namespace + '.' + name, inFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
        //handle purchase options
        JSONArray purchaseOptions = entitlement.optJSONArray("purchaseOptions");
        if (purchaseOptions != null) {
            for (int j = 0; j < purchaseOptions.length(); j++) {
                JSONObject optionItem = purchaseOptions.optJSONObject(j);
                String optionName = namespace + '.' + optionItem.optString("name");
                setRandomValue(optionItem, optionName, inFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
            }
        }
    }


    private static void inspectExperimentsPercentage(JSONArray experiments,
                                                     JSONObject inFeatureRandoms, JSONObject outFeatureRandoms, int legacyUserRandomNumber) throws JSONException {
        for (int i = 0; i < experiments.length(); i++) {
            JSONObject experimentItem = experiments.optJSONObject(i);
            String name = experimentItem.optString("name");

            setRandomValue(experimentItem, "experiments." + name, inFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
            //handle variants
            JSONArray variants = experimentItem.optJSONArray("variants");
            if (variants != null) {
                for (int j = 0; j < variants.length(); j++) {
                    JSONObject variantItem = variants.optJSONObject(j);
                    String variantName = name + '.' + variantItem.optString("name");
                    setRandomValue(variantItem, variantName, inFeatureRandoms, outFeatureRandoms, legacyUserRandomNumber);
                }
            }
        }
    }

    private static int anyRandom() // allocate any random [1 to 1 million inclusive]
    {
        return new Random().nextInt(1000000) + 1;
    }

    private static int constrainedRandom(double threshold, boolean isOn) // allocate a random that will simulate the legacy threshold
    {
        int splitPoint = (int) Math.floor(threshold * 10000); // 1 through 999999 inclusive
        if (splitPoint < 1) {
            splitPoint = 1;
        } else if (splitPoint > 999999) {
            splitPoint = 999999; // just in case
        }

        if (isOn) // select a user random number smaller than the split point
        {
            return new Random().nextInt(splitPoint) + 1;
        } else// select a user random number bigger than the split point

        {
            return new Random().nextInt(1000000 - splitPoint) + splitPoint + 1;
        }
    }

    private static String getId(JSONObject obj) {
        String name = obj.optString(Constants.JSON_FEATURE_FIELD_NAME, "<unknown>"); // using name instead of id. using + for mut
        String namespace = obj.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE, "<unknown>");
        return namespace + '.' + name;
    }

    private static Feature.Type resolveFeatureType(String type, Feature.Type defaultValue) {

        if (type.isEmpty()) {
            return defaultValue;
        }

        try {
            return Feature.Type.valueOf(type.trim());
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

    private static Feature.Type getNodeType(JSONObject obj) {
        String str = obj.optString(Constants.JSON_FEATURE_FIELD_TYPE, Feature.Type.FEATURE.toString());
        return resolveFeatureType(str, Feature.Type.FEATURE); // empty on error
    }

    public static JSONObject calculateNotificationsRandoms(JSONArray notificationsArray,
                                                           JSONObject inputNotificationsRandoms) throws JSONException {

        JSONObject outNotificationsRandoms = new JSONObject();

        for (int i = 0; i < notificationsArray.length(); ++i) {
            JSONObject stream = (JSONObject) notificationsArray.get(i);
            String name = stream.optString(Constants.JSON_FEATURE_FIELD_NAME, "<unknown>");
            //noinspection DynamicRegexReplaceableByCompiledPattern
            name = name.replaceAll("[. ]", "_");
            if (inputNotificationsRandoms.has(name)) {
                int previousRandom = inputNotificationsRandoms.optInt(name);
                outNotificationsRandoms.put(name, previousRandom);
            } else {
                outNotificationsRandoms.put(name, anyRandom());
            }
        }

        return outNotificationsRandoms;
    }

}


