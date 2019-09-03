package com.ibm.airlock.common.percentage;


import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.streams.AirlockStream;
import com.ibm.airlock.common.services.StreamsService;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.ibm.airlock.common.util.Constants.JSON_FEATURE_FIELD_ENTITLEMENTS;
import static com.ibm.airlock.common.util.Constants.JSON_FIELD_EXPERIMENTS;
import static com.ibm.airlock.common.util.Constants.JSON_FIELD_FEATURES;
import static com.ibm.airlock.common.util.Constants.JSON_FIELD_NOTIFICATIONS;
import static com.ibm.airlock.common.util.Constants.JSON_FIELD_STREAMS;

public class PercentageManager {

    private static final String TAG = "PercentageManager";
    private final Hashtable<String, Map<String, Double>> percentageMap = new Hashtable<>();
    private final PersistenceHandler persistenceHandler;
    private final StreamsService streamsService;

    public enum Sections {
        FEATURES(JSON_FIELD_FEATURES),
        EXPERIMENTS(JSON_FIELD_EXPERIMENTS),
        ENTITLEMENTS(JSON_FEATURE_FIELD_ENTITLEMENTS),
        STREAMS(JSON_FIELD_STREAMS),
        NOTIFICATIONS(JSON_FIELD_NOTIFICATIONS);

        public final String name;

        Sections(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

    public PercentageManager(InfraAirlockService infraAirlockService) {
        this.persistenceHandler = infraAirlockService.getPersistenceHandler();
        this.streamsService = infraAirlockService.getStreamsService();
        this.percentageMap.put(Sections.FEATURES.name, new HashMap<String, Double>());
        this.percentageMap.put(Sections.EXPERIMENTS.name, new HashMap<String, Double>());
        this.percentageMap.put(Sections.ENTITLEMENTS.name, new HashMap<String, Double>());
        this.percentageMap.put(Sections.STREAMS.name, new HashMap<String, Double>());
        this.percentageMap.put(Sections.NOTIFICATIONS.name, new HashMap<String, Double>());
        try {
            if (persistenceHandler.readJSON(Constants.SP_FEATURES_PERCENAGE_MAP).toString().equals("{}")) {
                if (persistenceHandler.readJSON(Constants.SP_RAW_RULES).optJSONObject(Constants.JSON_FIELD_ROOT) != null) {
                    generateFeaturesPercentageMap(persistenceHandler.readJSON(Constants.SP_RAW_RULES).getJSONObject(Constants.JSON_FIELD_ROOT));
                }
                generateExperimentsPercentageMap(new JSONObject(persistenceHandler.read(Constants.JSON_FIELD_DEVICE_EXPERIMENTS_LIST, "{}")));

                if(persistenceHandler.readJSON(Constants.SP_RAW_RULES).has(Constants.JSON_FIELD_ENTITLEMENT_ROOT)){
                    generateEntitlementsPercentageMap(persistenceHandler.readJSON(Constants.SP_RAW_RULES).
                            getJSONObject(Constants.JSON_FIELD_ENTITLEMENT_ROOT));
                }

                generateStreamsPercentageMap(this.streamsService.getStreams());
                persistenceHandler.write(Constants.SP_FEATURES_PERCENAGE_MAP, new JSONObject(percentageMap).toString());

            }
        } catch (final JSONException e) {
            Logger.log.e(TAG, e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public void forcedReInit() throws JSONException {
        if (persistenceHandler.readJSON(Constants.SP_RAW_RULES).optJSONObject(Constants.JSON_FIELD_ROOT) != null) {
            percentageMap.get(Sections.FEATURES.name).clear();
            generateFeaturesPercentageMap(persistenceHandler.readJSON(Constants.SP_RAW_RULES).getJSONObject(Constants.JSON_FIELD_ROOT));
        }
        generateExperimentsPercentageMap(new JSONObject(persistenceHandler.read(Constants.JSON_FIELD_DEVICE_EXPERIMENTS_LIST, "{}")));

        if(persistenceHandler.readJSON(Constants.SP_RAW_RULES).has(Constants.JSON_FIELD_ENTITLEMENT_ROOT)){
            generateEntitlementsPercentageMap(persistenceHandler.readJSON(Constants.SP_RAW_RULES).
                    getJSONObject(Constants.JSON_FIELD_ENTITLEMENT_ROOT));

        }
        generateStreamsPercentageMap(this.streamsService.getStreams());
        persistenceHandler.write(Constants.SP_FEATURES_PERCENAGE_MAP, new JSONObject(percentageMap).toString());
    }

    @SuppressWarnings("unused")
    public void reInit() throws JSONException {
        if (persistenceHandler.readJSON(Constants.SP_FEATURES_PERCENAGE_MAP).toString().equals("{}")) {
            if (persistenceHandler.readJSON(Constants.SP_RAW_RULES).optJSONObject(Constants.JSON_FIELD_ROOT) != null) {
                percentageMap.get(Sections.FEATURES.name).clear();
                generateFeaturesPercentageMap(persistenceHandler.readJSON(Constants.SP_RAW_RULES).getJSONObject(Constants.JSON_FIELD_ROOT));
            }

            generateExperimentsPercentageMap(new JSONObject(persistenceHandler.read(Constants.JSON_FIELD_DEVICE_EXPERIMENTS_LIST, "{}")));

            if(persistenceHandler.readJSON(Constants.SP_RAW_RULES).has(Constants.JSON_FIELD_ENTITLEMENT_ROOT)){
                generateEntitlementsPercentageMap(persistenceHandler.readJSON(Constants.SP_RAW_RULES).
                        getJSONObject(Constants.JSON_FIELD_ENTITLEMENT_ROOT));
            }
            generateStreamsPercentageMap(this.streamsService.getStreams());
            persistenceHandler.write(Constants.SP_FEATURES_PERCENAGE_MAP, new JSONObject(percentageMap).toString());
        }
    }

    private void generateFeaturesPercentageMap(JSONObject feature) throws JSONException {
        if (feature.optString(Constants.JSON_FEATURE_FIELD_TYPE).equals(Feature.Type.FEATURE.toString())) {
            percentageMap.get(Sections.FEATURES.name).put(feature.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + '.' + feature.optString(Constants.JSON_FEATURE_FIELD_NAME), feature.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE));
            JSONArray configurationRules = feature.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
            for (int i = 0; i < configurationRules.length(); i++) {
                generateConfigsPercentageMap(configurationRules.getJSONObject(i));
            }
        }
        JSONArray subFeatures = feature.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
        for (int i = 0; i < subFeatures.length(); i++) {
            generateFeaturesPercentageMap(subFeatures.getJSONObject(i));
        }
    }

    private void generateStreamsPercentageMap(List<AirlockStream> streams) throws JSONException {
        if (streams != null) {
            Map<String, Double> streamsMap = new HashMap<>();
            for (AirlockStream stream : streams) {
                streamsMap.put(stream.getName(), (double) stream.getRolloutPercentage());
            }
            percentageMap.put(Sections.STREAMS.name, streamsMap);
        }
    }


    private void generateConfigsPercentageMap(JSONObject obj) throws JSONException {
        if (obj.getString(Constants.JSON_FEATURE_FIELD_TYPE).equals(Feature.Type.CONFIGURATION_RULE.toString())) {
            percentageMap.get(Sections.FEATURES.name).put(obj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + '.' + obj.getString(Constants.JSON_FEATURE_FIELD_NAME), obj.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE));
        } else if (obj.getString(Constants.JSON_FEATURE_FIELD_TYPE).equals(Feature.Type.CONFIG_MUTUAL_EXCLUSION_GROUP.toString())) {
            JSONArray configurationRules = obj.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
            for (int i = 0; i < configurationRules.length(); i++) {
                generateConfigsPercentageMap(configurationRules.getJSONObject(i));
            }
        }
    }

    private void generateExperimentsPercentageMap(JSONObject root) throws JSONException {
        JSONArray experimentsArray = root.optJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
        if (experimentsArray != null) {
            for (int i = 0; i < experimentsArray.length(); i++) {
                JSONObject experiment = experimentsArray.getJSONObject(i);
                percentageMap.get(Sections.EXPERIMENTS.name).put(Constants.JSON_FIELD_EXPERIMENTS + '.' + experiment.getString(Constants.JSON_FEATURE_FIELD_NAME), experiment.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE));
                JSONArray variantsArray = experiment.getJSONArray(Constants.JSON_FIELD_VARIANTS);
                for (int j = 0; j < variantsArray.length(); j++) {
                    JSONObject variant = variantsArray.getJSONObject(j);
                    percentageMap.get(Sections.EXPERIMENTS.name).put(experiment.get(Constants.JSON_FEATURE_FIELD_NAME) + "." + variant.getString(Constants.JSON_FEATURE_FIELD_NAME), variant.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE));
                }
            }
        }
    }

    private void generateEntitlementsPercentageMap(JSONObject root) throws JSONException {
        JSONArray entitlementsArray = root.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
        if (entitlementsArray != null) {
            for (int i = 0; i < entitlementsArray.length(); i++) {
                generateEntitlementPercentageMap( entitlementsArray.getJSONObject(i));
            }
        }
    }

    private void generateEntitlementPercentageMap(JSONObject entitlement) throws JSONException {
        if (entitlement.has(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)) {
            for (int i = 0; i < entitlement.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS).length(); i++) {
                generateEntitlementPercentageMap(entitlement.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS).getJSONObject(i));
            }
        }
        percentageMap.get(Sections.ENTITLEMENTS.name).put(entitlement.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + '.' +
                entitlement.getString(Constants.JSON_FEATURE_FIELD_NAME), entitlement.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE));
        JSONArray optionsArray = entitlement.getJSONArray(Constants.JSON_FIELD_PURCHASE_OPTIONS);
        for (int j = 0; j < optionsArray.length(); j++) {
            JSONObject purchaseOption = optionsArray.getJSONObject(j);
            percentageMap.get(Sections.ENTITLEMENTS.name).put(entitlement.get(Constants.JSON_FEATURE_FIELD_NAMESPACE) + "." +
                    purchaseOption.getString(Constants.JSON_FEATURE_FIELD_NAME), purchaseOption.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE));
        }
    }

    @SuppressWarnings("unused")
    public void setDeviceInItemPercentageRange(Sections section, String name, boolean inRange) throws
            JSONException {

        if (percentageMap.isEmpty()) {
            return;
        }

        JSONObject randomMap = persistenceHandler.getRandomMap();
        Double percentage = percentageMap.get(section.name).get(name);
        if (percentage == null) {
            percentage = (double) 0;
        }

        //do nothing is the given percentage is 0 or 100 since we can't put anything in the given range
        if (percentage == 0 || percentage == 100) {
            return;
        }

        int splitPoint = (int) Math.floor(percentage * 10000);
        int rand;

        if (inRange) {
            // select a user random number smaller than the split point
            rand = new Random().nextInt(splitPoint) + 1;
        } else {
            // select a user random number bigger than the split point
            rand = new Random().nextInt(1000000 - splitPoint) + splitPoint + 1;
        }

        if (section.name.equals(Sections.EXPERIMENTS.name) || section.name.equals(Sections.ENTITLEMENTS.name)) {
            //experiments and features reside on same key on map
            section = Sections.FEATURES;
        }

        randomMap.getJSONObject(section.name).put(name, rand);
        persistenceHandler.write(Constants.SP_RANDOMS, randomMap.toString());
    }

    @SuppressWarnings("unused")
    public boolean isDeviceInItemPercentageRange(Sections section, String name) throws
            JSONException {

        if (percentageMap.isEmpty()) {
            return false;
        }

        Double percentage = percentageMap.get(section.name).get(name);
        if (percentage == null) {
            percentage = (double) 0;
        }
        int threshold = (int) Math.floor(percentage * 10000);
        if (threshold == 1000000) {
            return true;
        } else if (threshold == 0) {
            return false;
        }

        int featureRandom = 0;

        switch (section) {
            case STREAMS:
                featureRandom = persistenceHandler.getStreamsRandomMap() == null ? 0 : persistenceHandler.getStreamsRandomMap().optInt(name);
                break;
            case FEATURES:
            case EXPERIMENTS:
            case ENTITLEMENTS:
                //The feature and experiments reside on same key on map.
                featureRandom = persistenceHandler.getFeaturesRandomMap() == null ? 0 : persistenceHandler.getFeaturesRandomMap().optInt(name);
                break;
            case NOTIFICATIONS:
                featureRandom = persistenceHandler.getNotificationsRandomMap() == null ? 0 : persistenceHandler.getNotificationsRandomMap().optInt(name);
                break;
        }
        return threshold >= featureRandom;
    }

    @SuppressWarnings("unused")
    public Double getPercentage(@Nullable PercentageManager.Sections section, String name) {
        String sectionName;
        if (section == null || this.percentageMap.get(section.name().toLowerCase()) == null) {
            return (double) 0;
        } else {
            sectionName = section.name();
        }
        return percentageMap.get(sectionName.toLowerCase()).get(name);
    }

    @SuppressWarnings("unused")
    public boolean isEmpty() {
        return percentageMap.get(Sections.FEATURES.name()) == null;
    }
}
