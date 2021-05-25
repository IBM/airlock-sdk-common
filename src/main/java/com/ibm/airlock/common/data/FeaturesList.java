package com.ibm.airlock.common.data;

import com.ibm.airlock.common.engine.FeaturesCalculator;
import com.ibm.airlock.common.engine.Result;
import com.ibm.airlock.common.engine.ScriptExecutionException;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.BaseRawFeaturesJsonParser;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;


/**
 * Object represents a list of features.
 *
 * @author Rachel Levy
 */

public class FeaturesList<T extends Feature> {

    protected final String TAG = "Airlock.FeaturesList";
    protected Map<String, T> features;
    protected List<String> latestSyncChangedFeatures;

    /**
     * Construct a new FeatureList object
     */
    public FeaturesList() {
        features = new ConcurrentHashMap<>();
    }

    public FeaturesList(String json, Feature.Source source) {
        try {
            if (json == null || json.length() == 0) {
                features = new Hashtable<>();
            }
            JSONObject root = new JSONObject(json);
            JSONObject featuresArray = root.optJSONObject(getRooName());
            if (featuresArray == null) {
                featuresArray = new JSONObject();
            }
            FeaturesList res = BaseRawFeaturesJsonParser.getInstance().getFeatures(featuresArray, source);
            features = res.features;
        } catch (JSONException | ScriptExecutionException e) {
            Logger.log.e(TAG, e.getMessage(), e);
            features = new Hashtable<>();
        }
    }

    protected String getRooName(){
        return  Constants.JSON_FEATURE_FIELD_ROOT;
    }

    public Collection<T> getMutableChildrenList(){
        return features.values();
    }

    /**
     * Add feature to the feature list
     *
     * @param key     The key for the list (usually namespace.name)
     * @param feature The feature to be added
     */
    public synchronized void put(@Nullable String key, @Nullable T feature) {
        if (key == null || key.trim().length() == 0 || feature == null) {
            return;
        }
        features.put(key.toLowerCase(Locale.getDefault()).trim(), feature);
    }

    /**
     * Copies all of the mappings from the specified list to this list.
     *
     * @param values the map to be added
     */
    public synchronized void putAll(FeaturesList<T> values) {
        for (Map.Entry<String, T> value : values.getFeatures().entrySet()) {
            String key = value.getKey();
            T feature = (T)value.getValue().clone();
            features.put(key, feature);
        }
    }

    /**
     * Returns the feature associated with the given key
     *
     * @param key the feature key
     * @return The feature associated with the given key
     */
    @CheckForNull
    public T getFeature(@Nullable String key) {
        if (key == null) {
            return null;
        }
        T feature = features.get(key.toLowerCase(Locale.getDefault()));
        if (feature == null) {
            feature = (T)new Feature(key, false, Feature.Source.MISSING);
        }
        return feature;
    }

    /**
     * Returns a cloned list of the ROOT children features.
     * This method is safe to traverse through its returned List, since it clones all the child features.
     *
     * @return A cloned list of the ROOT children.
     */
    public synchronized List<Feature> getRootFeatures() {
        List<Feature> rootFeatures;
        Feature root = features.get(FeaturesCalculator.FeatureType.ROOT.toString().toLowerCase(Locale.getDefault()));
        if (root == null) {
            rootFeatures = new ArrayList<>();
        } else {
            rootFeatures = root.clone().getChildren();
        }
        return rootFeatures;
    }

    /**
     * Merge the given list with the current list.
     *
     * @param newValues The list to be merged with
     */
    public void merge(FeaturesList newValues, boolean addCachedFeatures) {
        merge(newValues, addCachedFeatures, false);
    }

    public void merge(FeaturesList newValues) {
        merge(newValues, true, false);
    }

    /**
     * Merge the given list with the current list.
     *
     * @param newValues The list to be merged with
     */
    private synchronized void merge(FeaturesList newValues, boolean addCachedFeatures,  boolean storeChangesList) {
        List<String> changedFeatures = null;
        if (storeChangesList) {//this is for future use - returning ti user the changes features...
            changedFeatures = new ArrayList<>();
        }
        if (newValues.size() == 0) {
            return;
        }
        Map<String, T> newFeaturesMap = new HashMap<>(newValues.getFeatures());
        Map<String, T> oldFeaturesMap = new HashMap<>(features);
        for (Map.Entry<String, T> currentFromNew : newFeaturesMap.entrySet()) {
            String currentKeyFromNew = currentFromNew.getKey();
            // if the old feature map contains the new key - remove it from the old feature map
            if (oldFeaturesMap.containsKey(currentKeyFromNew)) {
                if (storeChangesList) {
                    Feature orig = oldFeaturesMap.get(currentKeyFromNew);
                    Feature current = currentFromNew.getValue();
                    if (current.getTraceInfo().equals(Result.FEATURE_SKIPPED)){
                        continue;
                    }
                    boolean featureChanged = false;
                    if (orig.isOn() != current.isOn()) {
                        featureChanged = true;
                    }
                    if (orig.isOn() && !featureChanged) {
                        JSONObject currentConfig = current.getConfiguration();
                        JSONObject origConfig = orig.getConfiguration();
                        if (currentConfig != null && origConfig != null &&
                                !origConfig.toString().equals(currentConfig.toString())) {
                            featureChanged = true;
                        }
                    }
                    if (featureChanged) {
                        changedFeatures.add(currentKeyFromNew);
                    }
                }
                oldFeaturesMap.remove(currentKeyFromNew);
            }
        }
        // put in the new tree all the features that wasn't in the new one
        // the features in this list can't have children (????).
        if (oldFeaturesMap.size() > 0 && addCachedFeatures) {
            for (Map.Entry<String, T> currentEntry : oldFeaturesMap.entrySet()) {
                String currentKey = currentEntry.getKey();
                T currentFeatureClone = (T)currentEntry.getValue().clone();
                //parent
                Feature currentParent = currentFeatureClone.getParent(); // where to put the new feature
                if (currentParent == null) {
                    // add to the new list without parent
                    newFeaturesMap.put(currentKey, currentFeatureClone);
                    if (storeChangesList) {
                        changedFeatures.add(currentKey);
                    }
                } else {
                    String parentKey = currentParent.getName().toLowerCase(Locale.getDefault());
                    // if a feature parent still in the new tree add it to the parent
                    if (newFeaturesMap.containsKey(parentKey)) {
                        // add the feature under the same parent in the new tree
                        Feature newParent = newFeaturesMap.get(parentKey);
                        newParent.addUpdateChild(currentFeatureClone);
                        currentFeatureClone.setParent(newParent);
                    }
                    newFeaturesMap.put(currentKey, currentFeatureClone);
                }

                //find all parent children which already processed and add parent reference to them
                List<Feature> children = currentFeatureClone.getChildren();
                int childrenSize = children.size();
                for (Feature child : children) {
                    // the child has new parent - remove it from the children list
                    if (newFeaturesMap.containsKey(child.getName().toLowerCase(Locale.getDefault()))) {
                        newFeaturesMap.get(child.getName().toLowerCase(Locale.getDefault())).setParent(currentFeatureClone);
                    }
                }

                if (storeChangesList) {
                    changedFeatures.add(currentKey);
                }
            }
        }


        features = newFeaturesMap;
        this.latestSyncChangedFeatures = changedFeatures;
    }

    /**
     * Returns a Map object with the features.
     *
     * @return A Map object with the features.
     */
    public Map<String, T> getFeatures() {
        return features;
    }

    /**
     * Returns the number of keys in this list.
     *
     * @return the number of keys in this list.
     */
    public int size() {
        if (features == null) {
            return 0;
        }
        return features.size();
    }

    /**
     * Returns whether this FeatureList contains the specified key.
     *
     * @param key the key to search for.
     * @return true if this FeatureList contains the specified key, otherwise false.
     */
    public boolean containsKey(@Nullable String key) {
        return key != null && features.containsKey(key.toLowerCase(Locale.getDefault()));
    }

    /**
     * Remove all elements from this list
     */
    public void clear() {
        features.clear();
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Constants.JSON_FEATURE_FIELD_FEATURES, new JSONArray(features.values().toString()));
            return jsonObject.toString();
        } catch (JSONException e) {
            Logger.log.e(TAG, AirlockMessages.LOG_TO_STRING_FAILED, e);
        }
        return "";
    }

    public synchronized JSONObject toJsonObject() {
        JSONObject result = new JSONObject();
        Feature root = getFeature("ROOT");

        JSONObject toReturn = new JSONObject();
        try {
            result.put(Constants.JSON_FEATURE_IS_ON, root.isOn());
            result.put(Constants.JSON_FEATURES_ATTRS, root.getConfiguration());
            result.put(Constants.JSON_FEATURE_SOURCE, root.getSource());
            result.put(Constants.JSON_FEATURE_FIELD_FEATURES, parseChildren(root, new JSONArray()));
            result.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, root.isEnabledForAnalytics());
            result.put(Constants.JSON_FEATURE_TRACE, root.getTraceInfo());
            List<String> analyticsAppliedRules = root.getAnalyticsAppliedRules();
            if (analyticsAppliedRules != null) {
                result.put(Constants.JSON_FEATURE_CONFIG_ANALYTICS_APPLIED_RULES, new JSONArray(root.getAnalyticsAppliedRules()));
            }

            // ordering stuff
            JSONArray analyticsFeaturesOrder = root.getAnalyticsOrderedFeatures();
            if (analyticsFeaturesOrder != null) {
                result.put(Constants.JSON_FIELD_REORDERED_CHILDREN, new JSONArray(analyticsFeaturesOrder));
            }

            JSONArray analyticsAppliedOrderRule = root.getAnalyticsAppliedOrderRules();
            if (analyticsAppliedOrderRule != null) {
                result.put(Constants.JSON_APPLIED_REORDERED_RULE_NAMES, new JSONArray(analyticsAppliedOrderRule));
            }


            JSONArray attributesForAnalytics = root.getAttributesForAnalytics();
            if (attributesForAnalytics != null) {
                result.put(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS, attributesForAnalytics);
            }
            toReturn.put("root", result);
        } catch (JSONException e) {
            Logger.log.e(TAG, e.getMessage(), e);
        }
        Logger.log.d(TAG, "ToJsonObject = " + toReturn.toString());
        return toReturn;
    }

    private JSONArray parseChildren(Feature root, JSONArray childrenArray) {
        List<Feature> children = root.getChildren();
        if (children == null || children.size() == 0) {
            return childrenArray;
        }
        List<Feature> features = new ArrayList<>(children);
        for (Feature child : features) {
            JSONObject childJson = child.toJsonObject();
            try {
                childJson.put(Constants.JSON_FEATURE_FIELD_FEATURES, parseChildren(child, childrenArray));
                childrenArray.put(child.toJsonObject());
            } catch (JSONException e) {
                Logger.log.e(TAG, "", e);
            }
        }
        return childrenArray;
    }

    @TestOnly
    public String printableToString() {
        StringBuilder sb = new StringBuilder();
        for (Feature feature : features.values()) {
            sb.append(feature.printableToString());
            sb.append("---------------\n");
        }
        return sb.toString();
    }

    public List<String> getLatestSyncChangedFeatures() {
        return latestSyncChangedFeatures;
    }
}
