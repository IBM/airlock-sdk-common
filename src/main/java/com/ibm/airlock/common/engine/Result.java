package com.ibm.airlock.common.engine;

import com.ibm.airlock.common.model.Feature;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;


/**
 * The class represents rule engine result, the class has to members {@link Result#accept} whether the rule was accepted or not {@link Result#trace} trace
 * information holds addition details.
 *
 * @author Denis Voloshin
 */
public class Result {

    /**
     * Constants represent different trace results
     */
    public static final String RULE_OK = "";
    public static final String RULE_FAIL = "Rule returned false";
    public static final String FEATURE_IS_PREMIUM_NOT_PURCHASED = "Feature is premium and not purchased";
    public static final String FEATURE_IS_PREMIUM_PURCHASED = "Feature is premium and purchased";
    public static final String FEATURE_PREMIUM_RULE_OFF_NO_PURCHASED = "Feature premium rule is OFF and not purchased";
    public static final String FEATURE_PREMIUM_RULE_OFF_PURCHASED = "Feature premium rule is OFF and purchased";
    public static final String RULE_ERROR_FALLBACK = "Rule error, result obtained from fallback";
    public static final String RULE_CONFIG_FALLBACK = "Rule was successful, but configuration [%1$s] failed and was taken from fallback";
    public static final String RULE_DISABLED = "Rule disabled";
    public static final String FEATURE_SKIPPED = "Feature skipped";
    public static final String RULE_MUTEX = "Mutex"; // internal use only
    public static final String RULE_VERSIONED = "Product version is outside of feature version range";
    public static final String RULE_USER_GROUP = "Feature is in development and the device is not associated with any of the feature's internal user groups";
    public static final String RULE_PARENT_FAILED = "Parent feature is off";
    public static final String RULE_SKIPPED = "Feature is off because another feature in its mutual exclusion group is on";
    public static final String RULE_ORDERING_FAILURE = "ReOrdering rule failed due to [%1$s] errors.The default order will be used.";
    public static final String RULE_PERCENTAGE = "Feature is turned off due to roll-out percentage";
    public static final String RULE_AND_CONFIG_ERROR = "Rule and configuration [%1$s] errors; both obtained from fallback";
    public static final String FEATURE_TURNOFF = "Feature was on, but was turned off by configuration rule (%1$s)";
    public static final String RULE_CONFIG_TURNOFF = "Rule was successful, but configuration [%1$s] failed and the feature's fallback is off";
    public static final String CANCEL_RULE_EVAL = "Cancellation rule evaluated as true and the Notification was UnScheduled";

    /**
     * whether the rule was accepted or not
     */

    public boolean accept;
    /**
     * holds configuration attrs
     */
    public JSONObject attributes;
    /**
     * holds configuration rule names and is it's on/off after calculation
     */
    public JSONArray configRuleStatuses;
    /**
     * holds configuration attrs for analytics
     */
    public JSONArray configAttributesForAnalytics;
    /**
     * whether the rule should be sent to analytics
     */

    private boolean sendToAnalytics;
    /**
     * trace information holds addition details.
     */
    private String trace;

    /**
     * feature percentage value
     */
    protected double percentage;

    /**
     * feature order  weight
     */
    protected double orderingWeight;

    /**
     * feature order  weight
     */
    private String branchStatus = Feature.BranchStatus.NONE.name();
    /**
     * holds the applied rules that are enabled for analytics
     */
    @Nullable
    protected ArrayList<String> analyticsAppliedRules;


    @Nullable
    private String storeProductId;

    /**
     * holds the feature type base on Feature.Type enum
     */
    public Feature.Type type;

    /**
     * holds if a feature is a premium
     */
    public boolean isPremium;

    /**
     * holds if a feature is purchased or not, is relevant only for premium feature
     */
    public boolean isPurchased;

    /**
     * holds if a feature premium rule last result
     */
    private ScriptInvoker.Result premiumRuleResult;


    public Result(boolean accept, String trace) {
        this.accept = accept;
        this.trace = trace;
        this.attributes = new JSONObject();
        this.configRuleStatuses = new JSONArray();
        this.analyticsAppliedRules = new ArrayList<>();
        this.percentage = 100;
        this.orderingWeight = 0;
        this.type = Feature.Type.FEATURE;
        this.isPremium = false;
        this.isPurchased = false;
        this.premiumRuleResult = ScriptInvoker.Result.FALSE;
    }

    public ScriptInvoker.Result getPremiumRuleResult() {
        return premiumRuleResult;
    }

    public void setPremiumRuleResult(ScriptInvoker.Result premiumRuleResult) {
        this.premiumRuleResult = premiumRuleResult;
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


    @CheckForNull
    public String getStoreProductId() {
        return storeProductId;
    }

    public List<String> getPurchasesOptions() {
        return Collections.emptyList();
    }

    public void setStoreProductId(@Nullable String storeProductId) {
        this.storeProductId = storeProductId;
    }

    public Feature.Type getType() {
        return type;
    }

    public void setType(Feature.Type type) {
        this.type = type;
    }

    public boolean isAccept() {
        return accept;
    }

    public void setAccept(boolean accept) {
        this.accept = accept;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    public void setConfigRuleStatuses(JSONArray configRuleStatuses) {
        this.configRuleStatuses = configRuleStatuses;
    }

    public void setConfigAttributesForAnalytics(JSONArray configAttributesForAnalytics) {
        this.configAttributesForAnalytics = configAttributesForAnalytics;
    }

    public double getPercentage() {
        return percentage;
    }

    public double getOrderingWeight() {
        return orderingWeight;
    }

    public String getBranchStatus() {
        return branchStatus;
    }

    public void setBranchStatus(String branchStatus) {
        this.branchStatus = branchStatus;
    }

    public void setOrderingWeight(double orderingWeight) {
        this.orderingWeight = orderingWeight;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String toString() {
        return (accept ? "Status: ON" : "Status: OFF") + ", Attributes: " + attributes + ",Trace: " + trace;
    }

    public boolean isSendToAnalytics() {
        return sendToAnalytics;
    }

    public void setSendToAnalytics(boolean sendToAnalytics) {
        this.sendToAnalytics = sendToAnalytics;
    }

    @Nullable
    public ArrayList<String> getAnalyticsAppliedRules() {
        return analyticsAppliedRules;
    }

    public void setAnalyticsAppliedRules(ArrayList<String> analyticsAppliedRules) {
        this.analyticsAppliedRules = analyticsAppliedRules;
    }
}