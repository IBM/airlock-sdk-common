package com.ibm.airlock.common.test.golds_machine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.util.Constants;

//import com.google.common.collect.MapDifference;
//import com.google.common.collect.Maps;
//import com.weather.airlock.sdk.utils.AndroidTestUtils;


/**
 * Created by iditb on 25/09/17.
 */

public class GoldToOutputComparator {

    public GoldToOutputComparator() {}


    private void getFlatFeatures(JSONObject obj, JSONArray arr) throws JSONException {
        JSONArray childFeatures = obj.optJSONArray("features");
        if (obj.optString("type").isEmpty() || (obj.optString("type").equals("FEATURE") || obj.optString("type").equals("ROOT"))) {
            arr.put(obj);
        }

        if (childFeatures != null) {
            for (int i = 0; i < childFeatures.length(); i++) {
                JSONObject feature = childFeatures.getJSONObject(i);
                getFlatFeatures(feature, arr);
            }
        }
    }

    private String checkFeatureValues(Feature feature, JSONObject goldValues) throws JSONException {
        StringBuilder checkResultMsg = new StringBuilder();
        if (feature.isOn() != goldValues.getBoolean("isON")) {
            checkResultMsg.append("Unexpected value for feature " + feature.getName() + ": isON value was- " + feature.isOn() + ", expected- " + goldValues
                    .getBoolean("isON") + ".\n");
        }
        if (feature.getConfiguration() != null) {
			String goldConfig ;
            try{
                goldConfig = goldValues.getString("featureAttributes");
            }catch(Exception e){
                goldConfig = goldValues.optString("defaultConfiguration");
            }
                if (goldConfig==null) goldConfig = "{}";
                else if (goldConfig.equals("null")) goldConfig = "{}";
            try{
                JSONObject goldConfigJson = new JSONObject(goldConfig);
            }catch (Exception e){
                e.printStackTrace();
                Assert.fail("Cannot create JSONObject. Feature: "+feature.getName()+". String: "+goldConfig);
            }
            String configCheckResult = checkJSONValues(feature.getConfiguration(), new JSONObject(goldValues.getString("featureAttributes")));
            if (!configCheckResult.isEmpty()) {
                checkResultMsg.append("Unexpected configuration value for feature " + feature.getName() + ": " + configCheckResult + "\n");
            }
        }

        if (!feature.getTraceInfo().equals("")) {
            if (goldValues.optString("resultTrace") == null || goldValues.optString("resultTrace").equals("")) {
                checkResultMsg.append("Unexpected value for feature " + feature.getName() + ": 1 traceInfo was- " + feature.getTraceInfo() + ", expected- " + goldValues
                        .getString("resultTrace") + ".\n");
            }

            //if trace starts with Configurations: requires exact match else could be error and required 25% similarity
            if (!feature.getTraceInfo().startsWith("Configurations:")) {
                if (!goldValues.optString("resultTrace").equals(feature.getTraceInfo())) {
                    checkResultMsg.append("Unexpected value for feature " + feature.getName() + ": 2 traceInfo was- " + feature.getTraceInfo() + ", expected- " + goldValues
                            .getString("resultTrace") + ".\n");
                }
            }
        }
        return checkResultMsg.toString();
    }

    private String checkJSONValues(JSONObject configuration, JSONObject goldValues) {
        StringBuilder checkJSONMsg = new StringBuilder();
        Iterator<String> listIterator = goldValues.keys();
        while (listIterator.hasNext()) {
            String attributesName = listIterator.next();
            try {
                if (configuration.get(attributesName) instanceof JSONObject) {
                    JSONObject goldJSON;
                    try {
                        goldJSON = goldValues.getJSONObject(attributesName);
                    } catch (JSONException e) {
                        goldJSON = new JSONObject(goldValues.getString(attributesName));
                    }
                    String msg = checkJSONValues(configuration.getJSONObject(attributesName), goldJSON);
                    if (!msg.isEmpty()) {
                        checkJSONMsg.append("Unexpected " + attributesName + "  values: [" + msg + "]");
                    }
                } else if (!areEqual(configuration.get(attributesName), goldValues.get(attributesName))) {
                    checkJSONMsg.append(attributesName + " value was - " + configuration.get(attributesName) + ", expected- " + goldValues.getString
                            (attributesName) + ". ");
                }
            } catch (JSONException e) {
                checkJSONMsg.append("no value for " + attributesName + ", even though expected. ");
            }
        }
        return checkJSONMsg.toString();
    }

    private boolean areEqual(Object ob1, Object ob2) throws JSONException {
        Object obj1Converted = convertJsonElement(ob1);
        Object obj2Converted = convertJsonElement(ob2);
        return obj1Converted.equals(obj2Converted);
    }

    private Object convertJsonElement(Object elem) throws JSONException {
        if (elem instanceof JSONObject) {
            JSONObject obj = (JSONObject) elem;
            Iterator<String> keys = obj.keys();
            Map<String, Object> jsonMap = new HashMap<>();
            while (keys.hasNext()) {
                String key = keys.next();
                jsonMap.put(key, convertJsonElement(obj.get(key)));
            }
            return jsonMap;
        } else if (elem instanceof JSONArray) {
            JSONArray arr = (JSONArray) elem;
            Set<Object> jsonSet = new HashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                jsonSet.add(convertJsonElement(arr.get(i)));
            }
            return jsonSet;
        } else {
            return elem;
        }
    }

    private String checkAnalyticsFeatureValues(Feature feature, JSONObject goldValues) throws JSONException {
        StringBuilder checkResultMsg = new StringBuilder();
        if (feature == null) {
            checkResultMsg.append("Unexpected null feature value value.\n");
            return  checkResultMsg.toString();
        }
        //check isEnabledForAnalytics
        if (feature.isEnabledForAnalytics() != goldValues.optBoolean("featureIsReported", false)) {
            checkResultMsg.append("Unexpected value for feature " + feature.getName() + ": featureIsReported value was- " + feature.isEnabledForAnalytics() + ", expected- " + goldValues
                    .getBoolean("featureIsReported") + ".\n");
        }
        //check getAnalyticsAppliedRules
        if (feature.isEnabledForAnalytics() && !compareAnalyticsAppliedRules(feature.getAnalyticsAppliedRules(), goldValues.optJSONObject("reportedConfigurationNames"))) {
            checkResultMsg.append("Unexpected value for feature " + feature.getName() + ": appliedConfigRules value was- " + feature.getAnalyticsAppliedRules() + ", expected- " + goldValues
                    .optJSONObject("reportedConfigurationValues") + ".\n");
        }
        //check getAttributesForAnalytics
        Map<String, Object> configurationObjects = feature.getConfigurationJsonObjectsMap();
        if (feature.isEnabledForAnalytics() && !compareAnalyticsAttributes(configurationObjects, goldValues.opt("featureAttributes"))) {
            checkResultMsg.append("Unexpected value for feature " + feature.getName() + ": featureAttributes value was- " + configurationObjects + ", expected- " + goldValues
                    .opt("featureAttributes") + ".\n");
        }
        return checkResultMsg.toString();
    }

    private boolean compareAnalyticsAppliedRules(List<String> serverAppliedRules, JSONObject goldAppliedRules) {
        boolean isEqual = false;
        if (goldAppliedRules == null || goldAppliedRules.toString().equals("{}")) {
            if (serverAppliedRules == null || serverAppliedRules.size() == 0) {
                isEqual = true;
            } else {
                isEqual = false;
            }
        } else {
            List<String> goldAppliedList = new ArrayList<>();
            Iterator itr = goldAppliedRules.keys();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                boolean value = false;
                if (!key.equals("defaultConfiguration")) {
                    try {
                        value = (boolean) goldAppliedRules.get(key);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (value) {
                        goldAppliedList.add(key);
                    }
                }
            }
            if (goldAppliedList.size() == 0) {
                if (serverAppliedRules == null || serverAppliedRules.size() == 0) {
                    isEqual = true;
                } else {
                    isEqual = false;
                }
            } else {
                isEqual = goldAppliedList.equals(serverAppliedRules);
            }
        }

        return isEqual;
    }

    private boolean compareAnalyticsAttributes(Map<String, Object> configurationObjects, Object goldAppliedAttributes) {
        boolean isEqual = false;
        if (goldAppliedAttributes == null || goldAppliedAttributes.toString().equals("{}")) {
            if (configurationObjects.size() == 0) {
                isEqual = true;
            } else {
                isEqual = false;
            }
        } else {
            Map<String, Object> goldConfigurationObjects = new HashMap<>();

            if (configurationObjects.size() == 0) {
                return false;
            }

            if (goldAppliedAttributes instanceof String) {
                try {
                    JSONObject goldAttributesObj = new JSONObject(goldAppliedAttributes.toString());
                    Iterator itr = new JSONObject(goldAppliedAttributes.toString()).keys();
                    while (itr.hasNext()) {
                        String key = (String) itr.next();
                        Object value = goldAttributesObj.get(key);
                        goldConfigurationObjects.put(key, value);
                    }
                    //MapDifference difference = Maps.difference(configurationObjects, goldConfigurationObjects);
                    /*if (difference.areEqual()) {
                        isEqual = true;
                    } else {
                        isEqual = false;
                    }*/
                } catch (JSONException e) {
                    isEqual = false;
                }
            }
        }
        return isEqual;
    }

    public void compare(String productName, String goldName, JSONObject goldContent, JSONObject analyticsGoldContent, Map<String, Feature> output, String testName) throws IOException, JSONException {
        JSONArray features;
        JSONArray analyticsFeatures;
       // String productName = dataPath.substring(dataPath.lastIndexOf("/") + 1);
        StringBuilder failedMsg = new StringBuilder();

        features = new JSONArray();
        getFlatFeatures(goldContent.getJSONObject("root"), features);
        analyticsFeatures = new JSONArray();
        if (analyticsGoldContent != null) {
                getFlatFeatures(analyticsGoldContent.getJSONObject("features"), analyticsFeatures);
            }

        int featuresCounter = output.size();
        if (features.length() == featuresCounter) {
            boolean first = true;
            for (int i = 0; i < features.length(); i++) {
                JSONObject goldFeature = features.getJSONObject(i);
                if (goldFeature.optString(Constants.JSON_FEATURE_FIELD_TYPE).equals("FEATURE")) {
                    String featureName = goldFeature.getString("name");
                    Feature cachedFeature = output.get(featureName);
					if (cachedFeature==null)
                            cachedFeature = output.get(featureName.toLowerCase().trim());
                    String resultMsg = checkFeatureValues(cachedFeature, goldFeature);
                    if (!resultMsg.equals("")) {
                        if (first) {
                            first = false;
                            failedMsg.append(productName + " " + testName + " FAILURE: \nGold File Name: " + goldName + "\n" + resultMsg);
                        }
                        failedMsg.append(resultMsg);
                    }
                }
            }
            if (!first) {
                failedMsg.append("\n");
            }
        } else {
            failedMsg.append(productName + " " + testName + " FAILURE: \nGold File Name: " + goldName + "\n" + " : Number of features mismatch.\nGot from server: " + featuresCounter + "\nIn gold file: " + features.length());
        }
        if (analyticsFeatures.length() > 0) {
            boolean first = true;
            for (int i = 0; i < analyticsFeatures.length(); i++) {
                JSONObject goldFeature = analyticsFeatures.getJSONObject(i);
                Iterator<String> json_keys = goldFeature.keys();
                while (json_keys.hasNext()) {
                    String featureName = json_keys.next();
                    JSONObject entry = (JSONObject) goldFeature.get(featureName);
                    Feature cachedFeature = output.get(featureName);
                    String resultMsg = checkAnalyticsFeatureValues(cachedFeature, entry);
                    if (!resultMsg.equals("")) {
                        if (first) {
                            first = false;
                            failedMsg.append(productName + " " + testName + " ANALYTICS FAILURE: \n");
                        }
                        failedMsg.append(resultMsg);
                    }
                }
            }
            if (!first) {
                failedMsg.append("\n");
            }
        }
    }
}
