package com.ibm.airlock.common.util;

import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.FeaturesList;
import com.ibm.airlock.common.engine.FeaturesCalculator;
import com.ibm.airlock.common.engine.ScriptExecutionException;
import com.ibm.airlock.common.engine.ScriptInvoker;
import com.ibm.airlock.common.log.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.CheckForNull;


/**
 * @author Rachel Levy
 */

public class BaseRawFeaturesJsonParser {

    private static final String TAG = "airlock.JsonRulesParser";

    public static class SingletonHolder {
        public static final BaseRawFeaturesJsonParser HOLDER_INSTANCE = new BaseRawFeaturesJsonParser();
    }

    public static BaseRawFeaturesJsonParser getInstance() {
        return SingletonHolder.HOLDER_INSTANCE;
    }


    public Feature getFeaturesTree(JSONObject root, Feature.Source source) throws JSONException, ScriptExecutionException {
        Feature feature = createEmptyFeature(getType(root));
        FeaturesList out = new FeaturesList();
        feature.setSource(source == Feature.Source.UNKNOWN ? getSource(root) : source);
        descend(root, feature, source, out);
        return feature;
    }


    public FeaturesList getFeatures(JSONObject root, Feature.Source source) throws JSONException, ScriptExecutionException {
        Feature feature = new Feature(FeaturesCalculator.FeatureType.ROOT.toString().toLowerCase(Locale.getDefault()), true,
                source == Feature.Source.UNKNOWN ? getSource(root) : source);
        FeaturesList out = new FeaturesList();
        descend(root, feature, source, out);
        return out;
    }


    protected Feature createEmptyFeature(Feature.Type type) {
        return new Feature();
    }


    protected void descend(JSONObject current, Feature feature, Feature.Source source, FeaturesList out) throws JSONException, ScriptExecutionException {
        JSONArray children = getChildren(current);
        if (!isMutex(current)) {
            feature.setName(getName(current));
        }

        // mutex could have an ordering rule and features with new order.
        // we have to merge analytics details to the parent.
        if (isMutex(current) && getReorderedChildren(current) != null && feature.isEnabledForAnalytics()) {
            feature.mergeAnalyticsOrderedFeatures(getName(current), getReorderedChildren(current));
            feature.mergeAnalyticsAppliedOrderRules(getAppliedOrderRules(current));
        }

        for (int i = 0; i < children.length(); ++i) {
            JSONObject childAsJson = children.getJSONObject(i);
            if (!isMutex(childAsJson)) {
                Feature childFeature = createEmptyFeature(getType(current));
                String childId = getName(childAsJson);
                childFeature.setName(childId);
                childFeature.setSource(source == Feature.Source.UNKNOWN ? getSource(current) : source);
                feature.addUpdateChild(childFeature);
                try {
                    if (source.equals(Feature.Source.DEFAULT)) {
                        childFeature.setOn(getDefaultIsOn(childAsJson));
                    } else {
                        populateFeatureChild(childFeature, source, childAsJson);
                    }
                } catch (JSONException e) {
                    if (Feature.Source.DEFAULT == source) {
                        Logger.log.e(TAG, String.format(AirlockMessages.LOG_CANT_PARSE_FORMATTED, childAsJson));
                        Logger.log.e(TAG, String.format(AirlockMessages.LOG_CANT_READ_FROM_DEFAULT_FORMATTED, Constants.JSON_FEATURE_FIELD_DEFAULT));
                        throw e;
                    }
                }
                childFeature.setParent(feature);
                descend(childAsJson, childFeature, source, out);
            } else {
                descend(childAsJson, feature, source, out);
            }
        }
        if (isMutex(current)) {
            return;
        }
        feature.setConfiguration(getConfiguration(current, source));

        if (feature.getName().length() > 0) {
            out.put(feature.getName(), feature);
        }
    }

    protected void populateFeatureChild(Feature childFeature, Feature.Source source, JSONObject childAsJson) throws ScriptExecutionException {
        childFeature.setOn(getIsOn(childAsJson));
        childFeature.setConfigurationStatuses(getConfigRuleStatuses(childAsJson));
        childFeature.setTraceInfo(getTrace(childAsJson));
        childFeature.setEnabledForAnalytics(getIsSendToAnalytics(childAsJson));
        childFeature.setAttributesForAnalytics(getConfigAttributesToAnalytics(childAsJson));
        childFeature.setAnalyticsAppliedRules(getAnalyticsAppliedRules(childAsJson));
        childFeature.setAnalyticsOrderedFeatures(getReorderedChildren(childAsJson));
        childFeature.setAnalyticsAppliedOrderRules(getAppliedOrderRules(childAsJson));

        childFeature.setPercentage(childAsJson.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100));
        childFeature.setWeight(childAsJson.optDouble(Constants.JSON_ORDERED_WEIGTH, 0));
        childFeature.setBranchStatus(Feature.BranchStatus.valueOf(
                childAsJson.optString(Constants.JSON_FIELD_BRANCH_STATUS, "NONE")));

        //set premium stuff.
        childFeature.setPremium(childAsJson.optBoolean(Constants.JSON_FIELD_IS_PREMIUM, false));
        childFeature.setPurchased(childAsJson.optBoolean(Constants.JSON_FIELD_PURCHASED, false));
        childFeature.setPremiumRuleResult(ScriptInvoker.Result.valueOf(
                childAsJson.getString(Constants.JSON_FIELD_PREMIUM_RULE_LAST_RESULT)));
    }


    protected Feature.Type getType(JSONObject feature) {
        String typeAsString = feature.optString(Constants.JSON_FEATURE_FIELD_TYPE, FeaturesCalculator.FeatureType.FEATURE.toString());
        if (typeAsString == null) {
            return Feature.Type.FEATURE;
        }
        try {
            return Feature.Type.valueOf(typeAsString);
        } catch (Exception e) {
            return Feature.Type.FEATURE;
        }
    }

    protected JSONArray getChildren(JSONObject obj) {
        JSONArray out = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
        if (out == null) {
            out = new JSONArray();
        }
        return out;
    }

    protected boolean isMutex(JSONObject obj) {
        String str = obj.optString(Constants.JSON_FEATURE_FIELD_TYPE, FeaturesCalculator.FeatureType.FEATURE.toString());
        FeaturesCalculator.FeatureType type = FeaturesCalculator.FeatureType.valueOf(str);
        return (type == FeaturesCalculator.FeatureType.MUTUAL_EXCLUSION_GROUP);
    }

    private JSONObject getConfiguration(JSONObject obj, Feature.Source source) {
        String attributes;
        if (source.equals(Feature.Source.DEFAULT)) {
            attributes = obj.optString(Constants.JSON_DEFAULT_CONFIG_FIELD_NAME);
        } else {
            attributes = obj.optString(Constants.JSON_FEATURES_ATTRS);
        }
        try {
            return (attributes.equals("null") || attributes.length() == 0) ? new JSONObject() : new JSONObject(attributes);
        } catch (JSONException e) {
            Logger.log.e(TAG, AirlockMessages.LOG_CANT_PARSE_ATTRIBUTE, e);
            return new JSONObject();
        }
    }

    protected Feature.Source getSource(JSONObject obj) {
        String source = "";
        if (obj != null) {
            source = obj.optString(Constants.JSON_FEATURE_SOURCE);
        }
        if (source.equals("")) {
            return Feature.Source.UNKNOWN;
        }
        return Feature.Source.valueOf(source);
    }

    // if type = root name == root
    private String getName(JSONObject obj) {
        if (obj.optString(Constants.JSON_FEATURE_FIELD_TYPE).equals(FeaturesCalculator.FeatureType.ROOT.toString())) {
            return FeaturesCalculator.FeatureType.ROOT.toString();
        }
        String fullName = obj.optString(Constants.JSON_FEATURE_FULL_NAME);
        if (!fullName.equals("")) {
            return fullName;
        }
        String name = obj.optString(Constants.JSON_FEATURE_FIELD_NAME);
        // MX group name
        if (name.startsWith("mx.")) {
            return name;
        }
        String nameSpace = obj.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE);
        return nameSpace.length() > 0 || name.length() > 0 ? nameSpace + "." + name : "";
    }

    public static boolean getDefaultIsOn(JSONObject obj) throws JSONException {
        return obj.getBoolean(Constants.JSON_FEATURE_FIELD_DEFAULT);
    }

    public static JSONArray getConfigRuleStatuses(JSONObject obj) throws JSONException {
        return obj.optJSONArray(Constants.JSON_FEATURE_CONFIGURATES_STATUSES);
    }

    public static JSONArray getAppliedOrderRules(JSONObject obj) throws JSONException {
        return obj.optJSONArray(Constants.JSON_APPLIED_REORDERED_RULE_NAMES);
    }

    public static JSONArray getReorderedChildren(JSONObject obj) throws JSONException {
        return obj.optJSONArray(Constants.JSON_FIELD_REORDERED_CHILDREN);
    }

    public static boolean getIsOn(JSONObject obj) throws JSONException {
        return obj.getBoolean(Constants.JSON_FEATURE_IS_ON);
    }

    public static boolean getIsSendToAnalytics(JSONObject obj) {
        return obj.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false);
    }

    public static JSONArray getConfigAttributesToAnalytics(JSONObject obj) {
        return obj.optJSONArray(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS);
    }

    public static List<String> getAnalyticsAppliedRules(JSONObject obj) {
        Object appliedRules = obj.opt(Constants.JSON_FEATURE_CONFIG_ANALYTICS_APPLIED_RULES);
        if (appliedRules != null && !(appliedRules instanceof JSONArray)) {
            return null;
        } else {
            List<String> listdata = new ArrayList<String>();
            JSONArray jArray = (JSONArray) appliedRules;
            if (jArray != null) {
                for (int i = 0; i < jArray.length(); i++) {
                    listdata.add(jArray.getString(i));
                }
            }
            return listdata;
        }
    }

    private static String getTrace(JSONObject obj) {
        return obj.optString(Constants.JSON_FEATURE_TRACE);
    }

    @CheckForNull
    public static Object getFieldValueFromJsonObject(JSONObject object, String[] fieldName, boolean returnAsStringValue) {
        try {
            int currentIndex = 0;
            String nextObj = fieldName[currentIndex];
            if (nextObj.equalsIgnoreCase("context") && fieldName.length > 1) {
                currentIndex++;
                nextObj = fieldName[currentIndex];
            }
            int arrayAtIndex = -1;
            if (nextObj.endsWith("]")) {
                String[] arrayIndex = nextObj.split("\\[");
                nextObj = arrayIndex[0];
                arrayAtIndex = Integer.valueOf(arrayIndex[1].substring(0, arrayIndex[1].length() - 1));
            }
            Object value = object.opt(nextObj);
            if (value != null && arrayAtIndex > -1 && value instanceof JSONArray) {
                value = ((JSONArray) value).optJSONArray(arrayAtIndex);
            }

            if (value == null) {
                return null;
            } else if (currentIndex == (fieldName.length - 1) || !(value instanceof JSONObject)) {
                if (returnAsStringValue){
                    return value.toString();
                }else{
                    return value;
                }
            } else {
                return getFieldValueFromJsonObject((JSONObject) value, Arrays.copyOfRange(fieldName, currentIndex + 1, fieldName.length), returnAsStringValue);
            }
        } catch (Throwable th) {
            return null;
        }
    }
}
