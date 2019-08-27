package com.ibm.airlock.common.model;

import com.ibm.airlock.common.engine.ScriptInvoker;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.BaseRawFeaturesJsonParser;
import com.ibm.airlock.common.util.Constants;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;


/**
 * Object represents a feature in the feature map.
 *
 * @author Denis Voloshin
 */
public class Feature implements Serializable {

    private static final String TAG = "Airlock.Feature";

    String name = "";

    boolean isOn;

    private boolean enabledForAnalytics;
    @Nullable
    private JSONArray attributesForAnalytics;
    @Nullable
    private JSONArray configurationStatuses;
    @Nullable
    private List<String> analyticsAppliedRules;
    @Nullable
    private JSONArray analyticsOrderedFeatures;
    @Nullable
    private JSONArray analyticsAppliedOrderedRules;

    private Source source;

    private String traceInfo = "";

    private JSONObject configuration = new JSONObject();
    //    @Nullable private String parentKey = "";
    private Feature parent;

    List<Feature> children = new CopyOnWriteArrayList<>();

    private double percentage = 100;

    private BranchStatus branchStatus = BranchStatus.NONE;

    private double weight = 0;


    // premium
    /**
     * holds the feature purchased product id as it defined in the store
     */
    @Nullable
    private String storeProductId;

    /**
     * holds if a feature is a premium
     */
    private boolean isPremium;

    /**
     * holds if a feature is purchased or not, is relevant only for premium feature
     */
    private boolean isPurchased;


    /**
     * holds if a feature  premium rule last calc result
     */
    @Nullable
    private ScriptInvoker.Result premiumRuleResult;


    public Feature() {
    }

    /**
     * Constructs a new feature object.
     *
     * @param name   The unique name of the feature.
     * @param isOn   True if the feature is on.
     * @param source the feature source; can be default, server, or missing
     **/

    public Feature(String name, boolean isOn, Source source) {
        this.name = name;
        this.isOn = isOn;
        this.source = source;
    }

    /**
     * Parses the given string and constructs a new feature object.
     * Also supports the previous format: (name;isOn;source)
     *
     * @param featureInfo String in the format: this.toString
     **/
    public Feature(String featureInfo) {
        this(new JSONObject(featureInfo));
    }

    /**
     * Parses the given string and constructs a new feature object.
     * Also supports the previous format: (name;isOn;source)
     *
     * @param info JSON holds the feature values
     **/
    Feature(JSONObject info) {
        name = info.optString(Constants.JSON_FEATURE_FULL_NAME);
        source = Source.valueOf(info.optString(Constants.JSON_FEATURE_SOURCE, Source.SERVER.name()));
        JSONObject tmpConfig = info.optJSONObject(Constants.JSON_FEATURE_CONFIGURATION);
        configuration = tmpConfig == null ? new JSONObject() : tmpConfig;

        if (info.has(Constants.JSON_FEATURE_IS_ON)) {
            isOn = info.getBoolean(Constants.JSON_FEATURE_IS_ON);
        } else {
            isOn = info.optBoolean(Constants.JSON_FEATURE_FIELD_DEFAULT, false);
        }

        percentage = info.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
        traceInfo = info.optString(Constants.JSON_FEATURE_TRACE);
        premiumRuleResult = ScriptInvoker.Result.valueOf(info.optString(Constants.JSON_FIELD_PREMIUM_RULE_LAST_RESULT,"FALSE"));
    }

    /**
     * Returns a new feature that is a clone of this one.
     *
     * @return New feature that is a clone of this one.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Feature clone() {
        Feature result = getNew();
        if (this.parent != null) {
            result.parent = new Feature(this.parent.getName(), this.parent.isOn(), this.parent.getSource());
        } else {
            result.parent = null;
        }
        if (this.children != null) {
            result.children = new ArrayList<>(this.children.size());
            for (int i = 0; i < this.children.size(); i++) {
                result.children.add(this.children.get(i).clone());
            }
        }
        result.traceInfo = this.traceInfo;
        if (this.configuration != null) {
            try {
                result.configuration = new JSONObject(this.configuration.toString());
            } catch (JSONException e) {
                //Do nothing - just leave it empty
            }
        }

        if (this.analyticsAppliedRules != null) {
            result.analyticsAppliedRules = new ArrayList<>(this.analyticsAppliedRules);
        }

        if (this.analyticsOrderedFeatures != null) {
            result.analyticsOrderedFeatures = new JSONArray(this.analyticsOrderedFeatures.toString());
        }
        if (this.analyticsAppliedOrderedRules != null) {
            result.analyticsAppliedOrderedRules = new JSONArray(this.analyticsAppliedOrderedRules.toString());
        }

        if (this.configurationStatuses != null) {
            try {
                result.configurationStatuses = new JSONArray(this.configurationStatuses.toString());
            } catch (JSONException e) {
                //Do nothing - just leave it empty
            }
        }

        if (this.attributesForAnalytics != null) {
            try {
                result.attributesForAnalytics = new JSONArray(this.attributesForAnalytics.toString());
            } catch (JSONException e) {
                result.attributesForAnalytics = null;
            }
        }
        result.enabledForAnalytics = this.enabledForAnalytics;
        result.percentage = this.percentage;
        result.weight = this.weight;
        result.branchStatus = this.branchStatus;


        // premium
        result.storeProductId = this.storeProductId;
        result.isPremium = this.isPremium;
        result.isPurchased = this.isPurchased;
        result.premiumRuleResult = this.premiumRuleResult;

        return result;
    }

    @Nullable
    public ScriptInvoker.Result getPremiumRuleResult() {
        return premiumRuleResult;
    }

    public void setPremiumRuleResult(ScriptInvoker.Result premiumRuleResult) {
        this.premiumRuleResult = premiumRuleResult;
    }

    Feature getNew() {
        return new Feature(getName(), isOn(), getSource());
    }

    /**
     * Returns the full name of the feature (namespace.name)
     *
     * @return The full name of the feature.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the full name of this feature (namespace.name)
     *
     * @param name The new name for this feature.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns true if the feature is premium ON and purchased or not premium and ON
     *
     * @return True if the feature is premium ON and purchased or not premium and ON
     */
    public boolean isOn() {
        return isPremium ? (isOn && isPurchased) : isOn;
    }

    /**
     * Sets the feature to be on or off
     *
     * @param on Boolean that represents the new status.
     */
    public void setOn(boolean on) {
        isOn = on;
    }

    /**
     * Returns the feature source.
     *
     * @return default - if the feature value was taken from the defaults file server -  if the feature flag is from the server missing - if the application
     * requested a feature that wasn't available in the features map cached -  if the SDK got this feature from the server before, but didn't receive it with
     * the last pull action, or if the application just started and this feature was loaded from the cache.
     */
    public Source getSource() {
        return source;
    }

    /**
     * Sets the source of this feature.
     *
     * @param source The new feature source.
     */
    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * Returns the feature configuration in JSON format.
     *
     * @return The feature configuration in JSON format if the feature is off - returns null.
     */
    @CheckForNull
    @Nullable
    public JSONObject getConfiguration() {
        if (!isOn) {
            return null;
        } else {
            return (configuration == null ? new JSONObject() : configuration);
        }
    }

    /**
     * Sets a new configuration for this feature.
     *
     * @param configuration The new configuration to be set.
     */
    public void setConfiguration(JSONObject configuration) {
        if (configuration != null) {
            this.configuration = configuration;
        }
    }

    /**
     * Returns feature status information in string format that explains why a feature is off, for example:
     * "Rule returned false", "Parent feature is off"
     * "Feature is off because a previous feature in its mutual exclusion group is on"
     *
     * @return feature Status information in string format that explains why a feature is off.
     */
    public String getTraceInfo() {
        return traceInfo;
    }

    /**
     * Sets a string that explains why this feature is off.
     *
     * @param data The trace info string.
     */
    public void setTraceInfo(String data) {
        traceInfo = data;
    }

    public boolean isEnabledForAnalytics() {
        return enabledForAnalytics;
    }

    public void setEnabledForAnalytics(boolean enabledForAnalytics) {
        this.enabledForAnalytics = enabledForAnalytics;
    }

    @Nullable
    public JSONArray getAttributesForAnalytics() {
        return attributesForAnalytics;
    }

    public void setAttributesForAnalytics(JSONArray attributesForAnalytics) {
        this.attributesForAnalytics = attributesForAnalytics;
    }

    /**
     * Returns the parent feature in the hierarchy.
     *
     * @return The parent feature in the hierarchy.
     */
    @Nullable
    public Feature getParent() {
        return parent;
    }

    /**
     * Set the unique name for this feature parent in the format namespace.name
     *
     * @param newParent This feature parent.
     */
    public void setParent(@Nullable Feature newParent) {
        parent = newParent;
    }

    /**
     * The feature's applied rules.
     *
     * @return A list of the feature's applied rules
     */
    @Nullable
    @CheckForNull
    public List<String> getAnalyticsAppliedRules() {
        if (analyticsAppliedRules == null) {
            return null;
        } else {
            return new ArrayList<>(analyticsAppliedRules);
        }
    }

    /**
     * The feature's order based on order rule.
     *
     * @return A list of the feature's applied rules
     */
    @Nullable
    @CheckForNull
    public JSONArray getAnalyticsAppliedOrderRules() {
        if (analyticsAppliedOrderedRules == null) {
            return null;
        } else {
            return new JSONArray(this.analyticsAppliedOrderedRules.toString());
        }
    }

    /**
     * The feature's order based on order rule.
     *
     * @return A list of the feature's applied rules
     */
    @Nullable
    @CheckForNull
    public JSONArray getAnalyticsOrderedFeatures() {
        if (analyticsOrderedFeatures == null || !isEnabledForAnalytics()) {
            return null;
        } else {
            return new JSONArray(this.analyticsOrderedFeatures.toString());
        }
    }

    @Nullable
    public String getStoreProductId() {
        return storeProductId;
    }

    public void setStoreProductId(@Nullable String storeProductId) {
        this.storeProductId = storeProductId;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public BranchStatus getBranchStatus() {
        return branchStatus;
    }

    public void setBranchStatus(BranchStatus branchStatus) {
        this.branchStatus = branchStatus;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setAnalyticsAppliedOrderRules(@Nullable JSONArray analyticsAppliedOrderedRules) {
        this.analyticsAppliedOrderedRules = analyticsAppliedOrderedRules;
    }

    public void setAnalyticsOrderedFeatures(@Nullable JSONArray analyticsOrderedFeatures) {
        this.analyticsOrderedFeatures = analyticsOrderedFeatures;
    }


    public void setAnalyticsAppliedRules(@Nullable List<String> analyticsAppliedRules) {
        this.analyticsAppliedRules = analyticsAppliedRules;
    }

    @CheckForNull
    public JSONArray getConfigurationStatuses() {
        return configurationStatuses;
    }

    public void setConfigurationStatuses(@Nullable JSONArray configurationStatuses) {
        this.configurationStatuses = configurationStatuses;
    }

    public List<Feature> getChildren() {
        return children;
    }

    /**
     * Add a unique child name to the list of children. The name is in the format namespace.name
     *
     * @param child The child to be added.
     */
    public void addUpdateChild(@Nullable Feature child) {
        if (child == null) {
            return;
        }
        if (children == null) {
            children = new LinkedList<>();
        }
        int found = getChildIndexByName(child.getName());

        if (found != -1) {
            children.set(found, child);
        } else {
            children.add(child);
        }
    }


    private int getChildIndexByName(String name) {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getName().toLowerCase(Locale.getDefault()).equals(name.toLowerCase(Locale.getDefault()))) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Removes a child from the list of children.
     *
     * @param child The unique name of the child in the format namespace.name
     */
    public void removeChild(@Nullable Feature child) {
        if (children == null || child == null) {
            return;
        }
        for (Feature current : children) {
            if (current.getName().equals(child.getName())) {
                children.remove(current);
            }
            return;
        }
    }

    public JSONObject toJsonObject() {
        JSONObject childJson = new JSONObject();
        try {
            childJson.put(Constants.JSON_FEATURE_FULL_NAME, getName());
            childJson.put(Constants.JSON_FEATURE_IS_ON, isOn());
            childJson.put(Constants.JSON_FEATURE_SOURCE, getSource());
            childJson.put(Constants.JSON_FEATURE_FIELD_TYPE, Type.FEATURE);
            childJson.put(Constants.JSON_FEATURE_TRACE, getTraceInfo());

            if (getConfiguration() != null) {
                childJson.put(Constants.JSON_FEATURES_ATTRS, getConfiguration());
            }
            childJson.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, isEnabledForAnalytics());
            if (analyticsAppliedRules != null) {
                childJson.put(Constants.JSON_FEATURE_CONFIG_ANALYTICS_APPLIED_RULES, new JSONArray(analyticsAppliedRules));
            }

            if (configurationStatuses != null) {
                childJson.put(Constants.JSON_FEATURE_CONFIGURATES_STATUSES, configurationStatuses);
            }

            if (attributesForAnalytics != null) {
                childJson.put(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS, attributesForAnalytics);
            }
            childJson.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE, percentage);
            childJson.put(Constants.JSON_ORDERED_WEIGTH, weight);
            childJson.put(Constants.JSON_FIELD_BRANCH_STATUS, branchStatus);

            if (children != null && !children.isEmpty()) {
                JSONArray childrenArray = new JSONArray();
                if (children != null && !children.isEmpty()) {
                    for (Feature child : children) {
                        childrenArray.put(child.toJsonObject());
                    }
                }
                childJson.put(Constants.JSON_FEATURE_FIELD_FEATURES, childrenArray);
            }

            // premium stuff
            childJson.put(Constants.JSON_FIELD_IS_PREMIUM, isPremium);
            childJson.put(Constants.JSON_FIELD_PURCHASED, isPurchased);
            childJson.put(Constants.JSON_FIELD_STORE_PRODUCT_ID, storeProductId);
            childJson.put(Constants.JSON_FIELD_PREMIUM_RULE_LAST_RESULT, premiumRuleResult);

        } catch (JSONException e) {
            Logger.log.e(TAG, "", e);
        }
        return childJson;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt(Constants.JSON_FEATURE_FULL_NAME, name);
            jsonObject.putOpt(Constants.JSON_FEATURE_SOURCE, source);
            jsonObject.putOpt(Constants.JSON_FEATURE_CONFIGURATION, configuration);
            jsonObject.putOpt(Constants.JSON_FEATURE_IS_ON, isOn);
            jsonObject.putOpt(Constants.JSON_FIELD_PURCHASED, isPurchased);
            jsonObject.putOpt(Constants.JSON_FIELD_IS_PREMIUM, isPremium);
        } catch (JSONException e) {
            Logger.log.e(TAG, AirlockMessages.ERROR_FAILED_ON_TOSTRING_FUNCTION, e);
        }
        return jsonObject.toString();
    }

    @TestOnly
    public String printableToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.JSON_FEATURE_FULL_NAME + " = ").append(name).append('\n');
        sb.append(Constants.JSON_FEATURE_IS_ON + " = ").append(isOn).append('\n');
        sb.append(Constants.JSON_FEATURE_SOURCE + " = ").append(source).append('\n');
        sb.append(Constants.JSON_FEATURE_CONFIGURATION + " = ").append(configuration).append('\n');
        sb.append(Constants.JSON_FEATURE_CONFIGURATES_STATUSES + " = ").append(configurationStatuses).append('\n');
        sb.append(Constants.JSON_FEATURE_FIELD_PERCENTAGE + " = ").append(percentage).append('\n');
        sb.append(Constants.JSON_ORDERED_WEIGTH + " = ").append(weight).append('\n');
        sb.append(Constants.JSON_FIELD_BRANCH_STATUS + " = ").append(branchStatus).append('\n');
        sb.append(Constants.JSON_FIELD_PURCHASED + " = ").append(isPurchased).append('\n');
        sb.append(Constants.JSON_FIELD_IS_PREMIUM + " = ").append(isPremium).append('\n');
        sb.append(Constants.JSON_FIELD_STORE_PRODUCT_ID + " = ").append(storeProductId).append('\n');


        if (parent != null) {
            sb.append(Constants.JSON_FEATURE_PARENT_NAME + " = ").append(parent.getName()).append('\n');
        }
        if (children != null && !children.isEmpty()) {
            sb.append(Constants.JSON_FEATURE_CHILDREN + " = ");
            for (Feature child : children) {
                sb.append(child.getName()).append(',');
            }
            sb.append('\n');
        }
        sb.append(!traceInfo.isEmpty() ? Constants.JSON_FEATURE_TRACE + " = " + traceInfo + '\n' : "");
        return sb.toString();
    }

    public Map<String, Object> getConfigurationJsonObjectsMap() throws JSONException {

        JSONArray enabledConfigs = getAttributesForAnalytics();
        JSONObject configObject = getConfiguration();
        Map<String, Object> returnedObjects = new HashMap<>();

        if (configObject == null) {
            return returnedObjects;
        }

        int enabledConfigLength = 0;
        if (enabledConfigs != null) {
            enabledConfigLength = enabledConfigs.length();
        }
        for (int i = 0; i < enabledConfigLength; i++) {
            String key = enabledConfigs.get(i).toString();
            String value = BaseRawFeaturesJsonParser.getFieldValueFromJsonObject(configObject, key.split("\\."));
            if (value != null) {
                returnedObjects.put(key, value);
            }
        }
        return returnedObjects;
    }

    public void mergeAnalyticsOrderedFeatures(String childId, @Nullable JSONArray reorderedChildren) {
        if (this.analyticsOrderedFeatures != null && reorderedChildren != null && reorderedChildren.length() > 0) {
            JSONArray mergedArray = new JSONArray();
            for (int i = 0; i < analyticsOrderedFeatures.length(); i++) {
                if (!this.analyticsOrderedFeatures.getString(i).equals(childId)) {
                    mergedArray.put(this.analyticsOrderedFeatures.getString(i));
                } else {
                    for (int j = 0; j < reorderedChildren.length(); j++) {
                        mergedArray.put(reorderedChildren.getString(j));
                    }
                }
            }
            this.analyticsOrderedFeatures = mergedArray;
        }
    }

    public void mergeAnalyticsAppliedOrderRules(JSONArray appliedOrderRules) {
        TreeSet<String> set = new TreeSet();
        for (int i = 0; i < appliedOrderRules.length(); i++) {
            set.add(appliedOrderRules.getString(i));
        }
        if (this.analyticsAppliedOrderedRules != null) {
            for (int i = 0; i < this.analyticsAppliedOrderedRules.length(); i++) {
                set.add(this.analyticsAppliedOrderedRules.getString(i));
            }
        }
        this.analyticsAppliedOrderedRules = new JSONArray(set.toArray());
    }

    public enum Type {
        FEATURE,
        CONFIGURATION_RULE,
        MUTUAL_EXCLUSION_GROUP,
        ORDERING_RULE_MUTUAL_EXCLUSION_GROUP,
        ORDERING_RULE,
        CONFIG_MUTUAL_EXCLUSION_GROUP,
        ENTITLEMENT_MUTUAL_EXCLUSION_GROUP,
        PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP,
        ENTITLEMENT,
        PURCHASE_OPTIONS,
        ROOT
    }

    public enum BranchStatus {
        CHECKED_OUT,
        NEW,
        NONE, //taken from master
        TEMPORARY // mark temporary items
    }

    public enum Source {
        SERVER,
        DEFAULT,
        MISSING,
        CACHE,
        UNKNOWN
    }
}
