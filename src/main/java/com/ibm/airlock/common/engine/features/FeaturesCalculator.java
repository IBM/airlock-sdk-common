package com.ibm.airlock.common.engine.features;

import com.ibm.airlock.common.engine.*;
import com.ibm.airlock.common.engine.context.AirlockContextManager;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.util.AirlockVersionComparator;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.JsonUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.*;


/**
 * The Rule engine for airlock features
 *
 * @author Denis Voloshin
 */
public class FeaturesCalculator {

    @Nullable
    protected Map<String, Result> results;

    // convert JSON items to script objects
    public static ScriptInvoker createJsObjects(@Nullable AirlockContextManager airlockScriptScope, JSONObject context, String functions, JSONObject translations, List<String> profileGroups)
            throws JSONException, ScriptInitException {
        JSONObject items = new JSONObject();
        items.put(Constants.JS_CONTEXT_VAR_NAME, context);
        items.put(Constants.JS_GROUPS_VAR_NAME, new JSONArray(profileGroups));
        items.put(Constants.JS_TRANSLATIONS_VAR_NAME, translations);
        items.put(Constants.JSON_JS_FUNCTIONS_FIELD_NAME, functions);

        return new RhinoScriptInvoker(items, airlockScriptScope);
    }


    private static Fallback getFallback(JSONObject obj, Map<String, Fallback> fallback) {
        Fallback out;
        boolean avoidCache = isUncached(obj);

        if (!avoidCache) {
            out = fallback.get(getName(obj).toLowerCase());
            if (out != null) {
                return out;
            }
            // if requested cached fallback does not exist, use default fallback
        }

        boolean accept = obj.optBoolean(Constants.JSON_FEATURE_FIELD_DEF_VAL, false);
        JSONObject attributes = getDefaultConfiguration(obj);
        return new Fallback(accept,
                fallback.containsKey(getName(obj)) && fallback.get(getName(obj).toLowerCase()).premiumRuleOn,
                attributes);
    }


    private static JSONObject getDefaultConfiguration(JSONObject obj) {
        String config = obj.optString(Constants.JSON_DEFAULT_CONFIG_FIELD_NAME, "{}");
        if (config == null) {
            return new JSONObject();
        }

        try {
            return new JSONObject(config);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    @CheckForNull
    private static FeatureStage getStage(JSONObject obj) {
        String str = obj.optString(Constants.JSON_FEATURE_FIELD_STAGE);
        if (FeatureStage.DEVELOPMENT.toString().equals(str)) {
            return FeatureStage.DEVELOPMENT;
        }
        if (FeatureStage.PRODUCTION.toString().equals(str)) {
            return FeatureStage.PRODUCTION;
        }
        return null;
    }

    @SuppressWarnings("CastToConcreteClass")
    private static String extractTraceInfo(String prefix, Exception e) {
        try {
            if (e instanceof ConfigScriptExecutionException) {
                return String.format(prefix, ((ConfigScriptExecutionException) e).getConfig().get("name"));
            }
        } catch (Exception ex) {
            return String.format(prefix, "");
        }
        return String.format(prefix, "");
    }

    private static void disableFeatureFromConfiguration(Result result, String configFeatureName) {
        // 'featureON' is TRUE by default.
        // configuration can explicitly set 'featureON' to FALSE

        //check 'enableFeature' flag for the backward-compatibility
        String enableFeatureName = Constants.FEATURE_ON;
        if (result.attributes.has(Constants.ENABLE_FEATURE)) {
            enableFeatureName = Constants.ENABLE_FEATURE;
        }

        boolean enabled = result.attributes.optBoolean(enableFeatureName, true);
        if (!enabled) {
            result.setAccept(false);
            result.setTrace(result.getTrace() + '\n' + String.format(Result.FEATURE_TURNOFF, configFeatureName));
            result.attributes = new JSONObject();
        }
    }

    private static boolean isPremium(JSONObject json) {
        return json.optBoolean(Constants.JSON_FIELD_PREMIUM, false);
    }

    @CheckForNull
    private static String getPremiumRuleString(JSONObject json) {
        json = json.optJSONObject(Constants.JSON_FIELD_PREMIUM_RULE);
        return (json == null) ? null : json.optString(Constants.JSON_RULE_FIELD_RULE_STR);
    }

    @CheckForNull
    private static String getRuleString(JSONObject json) {
        json = json.optJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
        return (json == null) ? null : json.optString(Constants.JSON_RULE_FIELD_RULE_STR);
    }

    protected static String getName(JSONObject obj) // using namespace.name instead of id
    {
        String name = obj.optString(Constants.JSON_FIELD_NAME, "<unknown>");
        String namespace = getNamespace(obj);
        return namespace + '.' + name;
    }

    private static String getNamespace(JSONObject obj) {
        String namespace = obj.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE);
        if (!namespace.isEmpty()) {
            return namespace;
        }
        namespace = obj.optString(Constants.JSON_FIELD_EXPERIMENT_NAME);
        if (!namespace.isEmpty()) {
            return namespace;
        }

        return obj.has(Constants.JSON_FIELD_VARIANTS) ? Constants.JSON_FIELD_EXPERIMENTS : "<unknown>";
    }

    private static String getMinAppVersion(JSONObject obj) {
        return obj.has(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) ?
                obj.optString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER) : obj.optString(Constants.JSON_SEASON_FIELD_MIN_VER);
    }

    private static boolean checkPercentage(JSONObject obj, AdditionalData data) {

        if (data.featureRandomNumber == null) {
            return true;
        }
        double threshold = obj.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
        if (threshold <= 0) {
            return false;
        } else if (threshold >= 100.0) {
            return true;
        } else {
            int userFeatureRand = data.featureRandomNumber.optInt(getName(obj));
            if (userFeatureRand == 0) {
                return false;
            }
            return userFeatureRand <= threshold * 10000;
        }
    }

    private static TreeSet<String> getUserGroups(JSONObject obj) {
        JSONArray array;
        try {
            array = obj.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
        } catch (JSONException e) {
            array = new JSONArray();
        }

        TreeSet<String> out = new TreeSet<>();
        for (int i = 0; i < array.length(); ++i) {
            String str = array.optString(i);
            if (str != null && !str.isEmpty()) {
                out.add(str);
            }
        }
        return out;
    }

    private static boolean hasReOrderingRule(JSONObject obj) {
        JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_BASED_RULES_ORDERING);
        return array != null && array.length() > 0;
    }

    private static String getMutexId(JSONObject obj) // using mx.uniqueId
    {
        String name = obj.optString(Constants.JSON_FEATURE_FIELD_PRODUCT_UNIQUE_ID, "<unknown>");
        return "mx." + name;
    }

    @CheckForNull
    @Nullable
    private static JSONObject evaluateConfigurationScript(ScriptInvoker invoker,
                                                          @Nullable String trigger, String attributesString) throws ScriptExecutionException {

        if (trigger == null) {
            return null; // not triggered
        }

        ScriptInvoker.Output out = invoker.evaluate(trigger);
        if (out.result == ScriptInvoker.Result.FALSE) {
            return null; // not triggered
        }

        if (out.result == ScriptInvoker.Result.ERROR) {
            throw new ScriptExecutionException("Error evaluating configuration trigger [" + trigger + "] : " + out.error);
        }

        try {
            return invoker.evaluateConfiguration(attributesString);
        } catch (Exception e) {
            throw new ScriptExecutionException("Error evaluating configuration values [" + attributesString + "] : " + e.toString());
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static boolean isUncached(JSONObject json) {
        return json.optBoolean(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES, false);
    }

    /**
     * method calculates feature rules and returns maps with feature-name feature-enable/disable
     *
     * @param features            features list
     * @param context             device context
     * @param functions           js functions are used in while execution
     * @param translations        js translations object json representation
     * @param userGroups          access group
     * @param fallback            fallback results
     * @param productVersion      product number
     * @param featureRandomNumber random numbers is used to calculate a feature percentage is based on random number generated once and stored in the device as
     *                            int [1,1.000.000] as the app downloads the feature runtime configuration.
     * @return The result od calculating the features
     * @throws JSONException On case of initialization exception - this exception will be thrown
     */
    public JSONObject calculate(InfraAirlockService infraAirlockService, @Nullable JSONObject features,
                                @Nullable JSONObject context,
                                String functions,
                                JSONObject translations,
                                @Nullable List<String> userGroups,
                                @Nullable Map<String, Fallback> fallback,
                                @Nullable String productVersion,
                                @Nullable JSONObject featureRandomNumber
    )
            throws
            JSONException,
            ScriptInitException {
        return calculate(infraAirlockService, features, context, functions, translations, userGroups, fallback, productVersion, featureRandomNumber, null, null);
    }


    /**
     * method calculates feature rules and returns maps with feature-name feature-enable/disable
     *
     * @param features            features list
     * @param context             device context
     * @param functions           js functions are used in while execution
     * @param translations        js translations object json representation
     * @param userGroups          access group
     * @param fallback            fallback results
     * @param productVersion      product number
     * @param featureRandomNumber random numbers is used to calculate a feature percentage is based on random number generated once and stored in the device as
     *                            int [1,1.000.000] as the app downloads the feature runtime configuration.
     * @param purchaseIds         list of purchased id
     * @param purchaseToProductId map for purchase name to in-app/subscription product id
     * @return The result od calculating the features
     * @throws JSONException On case of initialization exception - this exception will be thrown
     */
    public JSONObject calculate(@Nullable InfraAirlockService infraAirlockService, @Nullable JSONObject features,
                                @Nullable JSONObject context,
                                String functions,
                                @Nullable JSONObject translations,
                                @Nullable List<String> userGroups,
                                @Nullable Map<String, Fallback> fallback,
                                @Nullable String productVersion,
                                @Nullable JSONObject featureRandomNumber,
                                @Nullable Collection<String> purchaseIds,
                                @Nullable Map<String, List<String>> purchaseToProductId)
            throws
            JSONException,
            ScriptInitException {
        if (features == null || getRoot(features).length() == 0) {
            return new JSONObject();
        }

        if (context == null) {
            context = new JSONObject("{}");
        }

        if (userGroups == null) {
            userGroups = new LinkedList<>();
        }

        if (productVersion == null) {
            productVersion = "";
        }

        if (translations == null) {
            translations = new JSONObject("{}");
        }

        if (fallback == null) {
            fallback = new Hashtable<>();
        }
        ScriptInvoker invoker = createJsObjects(infraAirlockService == null ? null : infraAirlockService.getAirlockContextManager(),
                context, functions, translations, userGroups);
        AdditionalData additionalData = new AdditionalData(userGroups,
                productVersion,
                featureRandomNumber,
                purchaseIds,
                purchaseToProductId);
        results = new TreeMap<>();
        doFeatureGroup(getRoot(features), getChildrenName(), invoker, additionalData, -1, fallback, results);
        invoker.exit();
        return embedResults(features, getChildrenName(), results, additionalData);
    }

    protected String getRootName() {
        return Constants.JSON_FIELD_ROOT;
    }

    protected String getChildrenName() {
        return Constants.JSON_FEATURE_FIELD_FEATURES;
    }

    protected JSONObject getRoot(@Nullable JSONObject obj) {
        if (obj == null) {
            return new JSONObject();
        }
        return obj.optJSONObject(Constants.JSON_FIELD_ROOT);
    }


    protected int doFeatureGroup(
            JSONObject parent,
            String childName,
            ScriptInvoker invoker,
            AdditionalData additionalData,
            int mutexConstraint, Map<String,
            Fallback> fallback,
            Map<String, Result> out) throws JSONException {

        if (parent.length() == 0) {
            return 0;
        }

        JSONArray array = getChildren(parent, childName);
        if (array == null || array.length() == 0) {
            return 0;
        }

        int groupMutexCount = mutexCount(parent);
        boolean groupIsMutex = (groupMutexCount > 0);
        boolean hasReOrderingRules = hasReOrderingRule(parent);
        boolean hasDirectOrWithinMXGroupReOrderingRule = hasDirectOrWithinMXOrderingRule(parent);

        // holds feature calculated results for the pruning loop
        TreeMap<String, Result> featureCalculatedResults = null;
        JSONArray reorderTrace = null;

        if (hasReOrderingRules || hasDirectOrWithinMXGroupReOrderingRule) {
            reorderTrace = new JSONArray();
            additionalData.reorderedFeatures.put(extendedName(parent), reorderTrace);
        }

        if (hasReOrderingRules) {

            //noinspection unchecked
            featureCalculatedResults = new TreeMap();

            for (int i = 0; i < array.length(); ++i) {
                JSONObject child = array.getJSONObject(i);
                String featureId = mutexCount(child) > 0 ? getMutexId(child) : getName(child);
                Result result = doFeature(child, invoker, additionalData, fallback);
                featureCalculatedResults.put(featureId, result);
            }


            try {
                applyRuleBasedOrder(parent, invoker, additionalData);
            } catch (ConfigScriptExecutionException e) {
                out.put(getName(parent), new Result(false, String.format(Result.RULE_ORDERING_FAILURE, e.getMessage())));
            }
        }

        if (groupIsMutex) {
            // reconcile with parent's constraint. (use the child if parent constraint is not set, or is more lenient)
            if (mutexConstraint < 0 || mutexConstraint > groupMutexCount) {
                mutexConstraint = groupMutexCount;
            }
            // else keep the parent's constraint (may be zero if parent is exhausted)
        } else {
            mutexConstraint = -1; // not set
        }

        // reassign children array if the order has been changed
        if (hasReOrderingRules) {
            array = getChildren(parent, childName);
        }

        int successes = 0;
        int arrayLength = 0;
        if (array != null){
            arrayLength = array.length();
        }
        for (int i = 0; i < arrayLength; ++i) {
            JSONObject child = array.getJSONObject(i);
            Result result;

            String featureName = extendedName(child);

            if (groupIsMutex && mutexConstraint <= 0) {
                result = new Result(false, Result.RULE_SKIPPED);
            } else if (hasReOrderingRules) {
                result = featureCalculatedResults.get(featureName);
            } else {
                result = doFeature(child, invoker, additionalData, fallback);
            }

            // set percentage
            result.setPercentage(child.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0));

            //set ordering weight
            result.setOrderingWeight(child.optDouble(Constants.JSON_ORDERED_WEIGTH, 0));

            // send reordered feature  for analytics
            if (hasReOrderingRules || hasDirectOrWithinMXGroupReOrderingRule) {
                reorderTrace.put(featureName);
            }

            if (!result.isAccept()) {
                propagateFail(child, childName, result, out); // pass false to all children
            } else {
                boolean childIsMutex = mutexCount(child) > 0;
                if (!childIsMutex) // child is not a mutex, count it as success
                {
                    if (shouldBeInMutexGroup(result)) {
                        ++successes;
                    }
                    out.put(getName(child), result);

                    if (groupIsMutex) {
                        --mutexConstraint; // must be done here before descending into subtree
                    }
                }

                int sub_mutex_success = doFeatureGroup(child, childName, invoker, additionalData, mutexConstraint, fallback, out); // success: descend & check children
                if (childIsMutex) {
                    successes += sub_mutex_success;
                    if (groupIsMutex) {
                        mutexConstraint -= sub_mutex_success;
                    }
                }
            }
        }

        // mutex nodes notify caller about number of successes (they are added to parent mutex success count)
        // non-mutex nodes notify 1 or zero.
        if (!groupIsMutex && successes > 1) {
            successes = 1;
        }
        return successes;
    }


    private boolean shouldBeInMutexGroup(Result result) {
        if (result.isPremium) {
            return result.accept && result.isPurchased;
        } else {
            return result.accept;
        }
    }

    protected String extendedName(JSONObject obj) {
        return (mutexCount(obj) > 0) ? getMutexId(obj) : getName(obj);
    }

    protected Result doFeature(JSONObject feature, ScriptInvoker invoker, AdditionalData additionalData, Map<String, Fallback> fallback) {
        if (mutexCount(feature) > 0) {
            return new Result(true, Result.RULE_MUTEX); // not added to output, but marked as successful to process its children
        }

        Result res = checkPreconditions(feature, additionalData);
        res.setType(getNodeType(feature));
        res.setSendToAnalytics(feature.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false));
        if (!res.isAccept()) {
            return res; // precondition failure
        }

        // a missing rule returns true
        String rule = getRuleString(feature);
        if (rule == null) {
            res = new Result(true, Result.RULE_OK);
            res.setType(getNodeType(feature));
            res.setSendToAnalytics(feature.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false));
            return res;
        }


        ScriptInvoker.Output ruleResult = invoker.evaluate(rule);

        // the premium rule has a default of TRUE
        ScriptInvoker.Output premiumRuleResult = new ScriptInvoker.Output(ScriptInvoker.Result.TRUE);
        // if the is-premium flag is off, there is no need to eval a rule
        if (isPremium(feature)) {
            premiumRuleResult = invoker.evaluate(getPremiumRuleString(feature));
            //do nothing
            if (premiumRuleResult.result == ScriptInvoker.Result.ERROR) {
                premiumRuleResult = new ScriptInvoker.Output(getFallback(feature, fallback).premiumRuleOn ?
                        ScriptInvoker.Result.TRUE : ScriptInvoker.Result.FALSE);
            }
        }


        Result featureResult;
        Fallback featureFallback = getFallback(feature, fallback);
        switch (ruleResult.result) {
            case ERROR: {
                featureResult = new Result(featureFallback.accept, Result.RULE_ERROR_FALLBACK + ". Error trace: " + ruleResult.error);

                if (featureFallback.accept) // try to calculate configuration anyhow since rule is on by default
                {
                    try {
                        ConfigResult cr = calculateAttributes(feature, invoker, additionalData);
                        featureResult.attributes = cr.attributes;
                        featureResult.setConfigRuleStatuses(cr.configRulesStatuses);
                        featureResult.setAnalyticsAppliedRules(cr.appliedRulesAnalyticsEnabled);
                        featureResult.setTrace(featureResult.getTrace() + "\nConfigurations: " + cr.appliedRules.toString());
                        featureResult.setConfigAttributesForAnalytics(feature.optJSONArray(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS));
                        featureResult.setSendToAnalytics(feature.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false));
                        featureResult.setType(getNodeType(feature));
                        disableFeatureFromConfiguration(featureResult, cr.disablingFeatureConfigName); // some configurations will turn off the feature explicitly

                        if (isPremium(feature)) {
                            applyPremiumConstraints(feature, featureResult, premiumRuleResult, additionalData);
                        }

                        return featureResult;

                    } catch (Exception e) {
                        featureResult = new Result(featureFallback.accept, extractTraceInfo(Result.RULE_AND_CONFIG_ERROR, e) + ". Error trace: " + ruleResult.error);
                        featureResult.attributes = featureFallback.attributes;

                        return featureResult;
                    }
                }

            }

            case TRUE: {
                featureResult = new Result(true, Result.RULE_OK);
                try {
                    ConfigResult cr = calculateAttributes(feature, invoker, additionalData);
                    featureResult.attributes = cr.attributes;
                    featureResult.setConfigRuleStatuses(cr.configRulesStatuses);
                    featureResult.setAnalyticsAppliedRules(cr.appliedRulesAnalyticsEnabled);
                    featureResult.setTrace("Configurations: " + cr.appliedRules.toString());
                    featureResult.setConfigAttributesForAnalytics(feature.optJSONArray(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS));
                    featureResult.setSendToAnalytics(feature.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false));
                    featureResult.setType(getNodeType(feature));
                    disableFeatureFromConfiguration(featureResult, cr.disablingFeatureConfigName); // some configurations will turn off the feature explicitly

                    if (isPremium(feature)) {
                        applyPremiumConstraints(feature, featureResult, premiumRuleResult, additionalData);
                    }

                    return featureResult;

                } catch (JSONException | ConfigScriptExecutionException e) {
                    if (!featureFallback.accept) {
                        return new Result(false, extractTraceInfo(Result.RULE_CONFIG_TURNOFF, e) + ". Error trace: " + e.getMessage());
                    } else {
                        featureResult.attributes = featureFallback.attributes;
                        featureResult.setTrace(extractTraceInfo(Result.RULE_CONFIG_FALLBACK, e) + ". Error trace: " + e.getMessage());

                        return featureResult;
                    }
                }
            }
            case FALSE:
            default:
                featureResult = new Result(false, Result.RULE_FAIL);
                featureResult.setType(getNodeType(feature));
                featureResult.setSendToAnalytics(feature.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false));


                return featureResult;
        }

    }

    @CheckForNull
    private String getPremiumPurchaseName(JSONObject feature) {
        return feature.optString(Constants.JSON_FEATURE_FIELD_ENTITLEMENT);
    }


    private boolean isPurchased(AdditionalData additionalData, JSONObject feature, Result result) {
        String premiumPurchaseName = getPremiumPurchaseName(feature);

        if (premiumPurchaseName == null) {
            return false;
        }

        List<String> productIdList = additionalData.getPurchaseToProductId().get(premiumPurchaseName);

        if (productIdList == null || productIdList.isEmpty()) {
            return false;
        }

        for (String productId : productIdList) {
            if (additionalData.getPurchasedProductIds().contains(productId)) {
                result.setStoreProductId(productId);
                return true;
            }
        }
        return false;
    }

    private void applyPremiumConstraints(JSONObject feature,
                                         Result featureResult,
                                         ScriptInvoker.Output premiumRuleResult,
                                         AdditionalData additionalData) {

        featureResult.setPremiumRuleResult(premiumRuleResult.result);

        //  is-premium flag is on, and the premium rule evaluates to false, and the regular rule evaluates to true (and all the rest)
        if (premiumRuleResult.result == ScriptInvoker.Result.TRUE) {
            featureResult.setPremium(true);
            //
            if (!isPurchased(additionalData, feature, featureResult)) {
                featureResult.setPurchased(false);
                featureResult.setTrace(Result.FEATURE_IS_PREMIUM_NOT_PURCHASED);
            } else {
                featureResult.setPurchased(true);
                featureResult.setTrace(Result.FEATURE_IS_PREMIUM_PURCHASED);
            }
        } else {
            featureResult.setPremium(false);
            if (isPurchased(additionalData, feature, featureResult)) {
                featureResult.setPurchased(true);
                featureResult.setTrace(Result.FEATURE_PREMIUM_RULE_OFF_PURCHASED);
            } else {
                featureResult.setPurchased(false);
                featureResult.setTrace(Result.FEATURE_PREMIUM_RULE_OFF_NO_PURCHASED);
            }
        }
    }

    private void applyRuleBasedOrder(JSONObject feature, ScriptInvoker invoker, AdditionalData additionalData) throws JSONException, ConfigScriptExecutionException {
        JSONArray array = getChildren(feature, Constants.JSON_FEATURE_FIELD_FEATURES);
        if (array == null || array.length() == 0) {
            return;
        }
        ConfigResult cr = calculateRuleBasedChildrenOrder(feature, invoker, additionalData);
        additionalData.appliedOrderRulesForAnalytics.put(extendedName(feature), cr.appliedRulesAnalyticsEnabled);
        feature.put(Constants.JSON_FEATURE_FIELD_FEATURES, reorderChildren(array, cr.attributes));
    }

    private JSONArray reorderChildren(JSONArray array, final JSONObject weights) {
        JSONArray sortedJsonArray = new JSONArray();

        List<JSONObject> jsonValues = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            jsonValues.add(array.optJSONObject(i));
        }

        Collections.sort(jsonValues, new Comparator<JSONObject>() {

            @Override
            public int compare(JSONObject child_a, JSONObject child_b) {
                double weight_a = 0;
                double weight_b = 0;

                if (getNodeType(child_a) == Feature.Type.MUTUAL_EXCLUSION_GROUP && child_a.has(Constants.JSON_FIELD_UNIQUE_ID)) {
                    try {
                        weight_a = Double.parseDouble(weights.optString("mx" + '.' +
                                child_a.optString(Constants.JSON_FIELD_UNIQUE_ID)));
                        child_a.put(Constants.JSON_ORDERED_WEIGTH, weight_a);
                    } catch (NumberFormatException e) {
                        // do nothing the value remains 0
                    }
                } else {
                    if (child_a.has(Constants.JSON_FEATURE_FIELD_NAMESPACE) && child_a.has(Constants.JSON_FIELD_NAME)) {
                        try {
                            weight_a = Double.parseDouble(weights.optString(child_a.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + '.' +
                                    child_a.optString(Constants.JSON_FIELD_NAME)));
                            child_a.put(Constants.JSON_ORDERED_WEIGTH, weight_a);
                        } catch (NumberFormatException e) {
                            // do nothing the value remains 0
                        }
                    }
                }
                if (getNodeType(child_b) == Feature.Type.MUTUAL_EXCLUSION_GROUP && child_b.has(Constants.JSON_FIELD_UNIQUE_ID)) {
                    try {
                        weight_b = Double.parseDouble(weights.optString("mx" + '.' +
                                child_b.optString(Constants.JSON_FIELD_UNIQUE_ID)));
                        child_b.put(Constants.JSON_ORDERED_WEIGTH, weight_b);
                    } catch (NumberFormatException e) {
                        // do nothing the value remains 0
                    }
                } else {
                    if (child_b.has(Constants.JSON_FEATURE_FIELD_NAMESPACE) && child_b.has(Constants.JSON_FIELD_NAME)) {
                        try {
                            weight_b = Double.parseDouble(weights.optString(child_b.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + '.' +
                                    child_b.optString(Constants.JSON_FIELD_NAME)));
                            child_b.put(Constants.JSON_ORDERED_WEIGTH, weight_b);
                        } catch (NumberFormatException e) {
                            // do nothing the value remains 0
                        }
                    }
                }
                return Double.compare(weight_b, weight_a);
            }
        });

        for (int i = 0; i < array.length(); i++) {
            sortedJsonArray.put(jsonValues.get(i));
        }

        return sortedJsonArray;
    }

    private Result checkPreconditions(JSONObject obj, AdditionalData additionalData) {
        if (!isEnabled(obj)) {
            return new Result(false, Result.RULE_DISABLED);
        }

        // percentage check moved here again, in front of evaluation
        if (!checkPercentage(obj, additionalData)) {
            return new Result(false, Result.RULE_PERCENTAGE);
        }

        // the minAppVersion of the feature should be <= to the current productVersion
        // minAppVersion is now mandatory, so fail if missing
        String minAppVersion = getMinAppVersion(obj);
        AirlockVersionComparator comparator = new AirlockVersionComparator();
        if (comparator.compare(minAppVersion, additionalData.productVersion) > 0) {
            return new Result(false, Result.RULE_VERSIONED);
        }

        // for development users, check that the feature belongs to the right user group
        if (getStage(obj) == FeatureStage.DEVELOPMENT) {
            TreeSet<String> featureGroups = getUserGroups(obj);
            featureGroups.retainAll(additionalData.profileGroups);
            if (featureGroups.isEmpty()) {
                return new Result(false, Result.RULE_USER_GROUP);
            }
        }

        return new Result(true, Result.RULE_OK); // all preconditions are met
    }

    // propagate a fail down to all children
    private void propagateFail(JSONObject feature, String childName, Result result, Map<String, Result> out) throws JSONException {
        String id = getName(feature);

        if (mutexCount(feature) == 0) {
            out.put(id, result);
        }

        JSONArray children = getChildren(feature, childName);
        if (children == null || children.length() == 0) {
            return;
        }

        for (int i = 0; i < children.length(); ++i) {
            Result parentFail = new Result(false, Result.RULE_PARENT_FAILED);
            JSONObject child = children.getJSONObject(i);
            parentFail.setType(getNodeType(child));
            parentFail.setSendToAnalytics(child.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false));
            propagateFail(children.getJSONObject(i), childName, parentFail, out);
        }
    }

    protected int mutexCount(JSONObject obj) {
        Feature.Type type = getNodeType(obj);
        switch (type) {
            case MUTUAL_EXCLUSION_GROUP:
            case CONFIG_MUTUAL_EXCLUSION_GROUP:
            case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
                return obj.optInt(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON, 1);

            default:
                return 0;
        }
    }

    @CheckForNull
    protected JSONArray getChildren(JSONObject obj, String childName) {
        return obj.optJSONArray(childName);
    }

    private boolean isEnabled(JSONObject obj) {
        return obj.optBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
    }

    private ConfigResult calculateRuleBasedChildrenOrder(JSONObject feature, ScriptInvoker invoker, AdditionalData additionalData) throws
            ConfigScriptExecutionException, JSONException {
        // start with defaults
        ConfigResult out = new ConfigResult();
        out.attributes = new JSONObject();
        out.appliedRules = new ArrayList<>();
        out.configRulesStatuses = new JSONArray();
        out.appliedRules = new ArrayList<>();
        out.appliedRulesAnalyticsEnabled = new ArrayList<>();

        // override defaults
        processCalculableChildren(feature, invoker, additionalData, out, -1, Constants.JSON_FEATURE_FIELD_BASED_RULES_ORDERING);
        return out;
    }

    private ConfigResult calculateAttributes(JSONObject feature, ScriptInvoker invoker, AdditionalData additionalData) throws ConfigScriptExecutionException, JSONException {
        // start with defaults
        ConfigResult out = new ConfigResult();
        out.attributes = getDefaultConfiguration(feature);
        out.appliedRules = new ArrayList<>();
        out.configRulesStatuses = new JSONArray();
        out.appliedRules.add("defaultConfiguration");
        out.appliedRulesAnalyticsEnabled = new ArrayList<>();

        // override defaults
        processCalculableChildren(feature, invoker, additionalData, out, -1, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
        return out;
    }

    private boolean hasDirectOrWithinMXOrderingRule(JSONObject parent) {
        boolean hasDirectOrder = hasReOrderingRule(parent);
        boolean hasWithinMXOrderingRule = false;

        JSONArray array = getChildren(parent, Constants.JSON_FEATURE_FIELD_FEATURES);

        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                JSONObject child = array.getJSONObject(i);
                if (mutexCount(child) > 0 && hasReOrderingRule(child)) {
                    hasWithinMXOrderingRule = true;
                }
            }
        }

        return hasDirectOrder || hasWithinMXOrderingRule;
    }

    private int processCalculableChildren(JSONObject obj,
                                          ScriptInvoker invoker,
                                          AdditionalData additionalData,
                                          ConfigResult out,
                                          int mxCounter,
                                          String configName) throws
            ConfigScriptExecutionException,
            JSONException {

        JSONArray array = obj.optJSONArray(configName);
        if (array == null || array.length() == 0) {
            return 0;
        }

        int groupMutexCount = mutexCount(obj);
        boolean groupIsMutex = (groupMutexCount > 0);

        if (groupIsMutex) {
            // reconcile with parent's constraint. (use the child if parent constraint is not set, or is more lenient)
            if (mxCounter < 0 || mxCounter > groupMutexCount) {
                mxCounter = groupMutexCount;
            }
            // else keep the parent's constraint (may be zero if parent is exhausted)
        } else {
            mxCounter = -1; // not set
        }

        int successes = 0;
        for (int i = 0; i < array.length(); ++i) {
            if (groupIsMutex && mxCounter <= 0) {
                break;
            }

            JSONObject child = array.getJSONObject(i);
            Feature.Type childType = getNodeType(child);

            boolean childIsMutex = true;
            if (childType == Feature.Type.CONFIGURATION_RULE || childType == Feature.Type.ORDERING_RULE) {
                childIsMutex = false;
                int childSuccess = processCalculableChild(child, invoker, additionalData, out); // 0 or 1
                if (childSuccess == 0) {
                    continue; // do not descend into children, skip to next brother
                }

                successes += 1;
                if (groupIsMutex) {
                    mxCounter -= 1;
                }
            }

            // descend into children if node is a mutex or a successful configuration
            int sub_mutex_success = processCalculableChildren(child, invoker, additionalData, out, mxCounter, configName);
            if (childIsMutex) {
                successes += sub_mutex_success;
                if (groupIsMutex) {
                    mxCounter -= sub_mutex_success;
                }
            }
        }

        if (!groupIsMutex && successes > 1) // none-mutex nodes return 1 or 0 successes
        {
            successes = 1;
        }
        return successes;
    }

    private int processCalculableChild(JSONObject obj, ScriptInvoker invoker, AdditionalData additionalData, ConfigResult out) throws
            ConfigScriptExecutionException, JSONException {
        String ruleId = getName(obj);
        Result res = checkPreconditions(obj, additionalData);
        if (!res.isAccept()) {
            out.configRulesStatuses.put(new JSONObject("{'name':'" + ruleId + "','isON':false}"));
            return 0; // precondition not met
        }

        String attributesString = obj.optString(Constants.JSON_FEATURE_CONFIGURATION, "");
        String trigger = getRuleString(obj);

        JSONObject attributes;
        try {
            attributes = evaluateConfigurationScript(invoker, trigger, attributesString);
        } catch (ScriptExecutionException e) {
            ConfigScriptExecutionException ex = new ConfigScriptExecutionException(e.getMessage());
            ex.setConfig(obj);
            out.configRulesStatuses.put(new JSONObject("{'name':'" + ruleId + "','isON':false}"));
            throw ex;
        }

        if (attributes == null) {
            out.configRulesStatuses.put(new JSONObject("{'name':'" + ruleId + "','isON':false}"));
            return 0; // configuration rule not met
        }

        if (attributes.has(Constants.FEATURE_ON) && !attributes.optBoolean(Constants.FEATURE_ON)) {
            out.disablingFeatureConfigName = obj.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + '.' + obj.optString(Constants.JSON_FEATURE_FIELD_NAME);
        }

        JsonUtils.mergeJson(out.attributes, attributes);
        out.appliedRules.add(ruleId);
        if (obj.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false)) {
            out.appliedRulesAnalyticsEnabled.add(ruleId);
        }

        out.configRulesStatuses.put(new JSONObject("{'name':'" + ruleId + "','isON':true}"));

        return 1;
    }

    private Feature.Type resolveFeatureType(String type, Feature.Type defaultValue) {

        try {
            return Feature.Type.valueOf(type.trim());
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

    protected Feature.Type getNodeType(JSONObject obj) {
        String str = obj.optString(Constants.JSON_FEATURE_FIELD_TYPE, Feature.Type.FEATURE.toString());
        return resolveFeatureType(str, Feature.Type.FEATURE);
    }

    protected JSONObject embedResults(JSONObject features, String childName, Map<String, Result> resultMap, AdditionalData additionalData) throws JSONException {
        JSONObject root = getRoot(features);
        JSONArray childResults = embedChildren(root, childName, resultMap, additionalData);
        JSONObject out = new JSONObject();
        out.put(Constants.JSON_FEATURE_FIELD_NAME, getRootName());
        out.put(getChildrenName(), childResults);
        out.put(Constants.JSON_FEATURE_FIELD_TYPE, Feature.Type.ROOT.toString());
        Object experimentList = features.opt(Constants.SP_EXPERIMENT_INFO);
        if (experimentList != null) {
            out.put(Constants.SP_EXPERIMENT_INFO, experimentList);
        }
        out.put(Constants.JSON_FIELD_BRANCH_NAME, features.optString(Constants.JSON_FIELD_BRANCH_NAME, getRootName()));
        return out;
    }

    protected JSONArray embedChildren(JSONObject feature, String childName, Map<String, Result> resultMap, AdditionalData additionalData) throws JSONException {
        JSONArray children = getChildren(feature, childName);
        JSONArray out = new JSONArray();
        if (children != null) {
            for (int i = 0; i < children.length(); ++i) {
                JSONObject node = embedOneChild(children.getJSONObject(i), childName, resultMap, additionalData);
                out.put(node);
            }
        }
        return out;
    }

    private String getChildrenElementNameByType(Feature.Type type) {
        if (type == Feature.Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP ||
                type == Feature.Type.ENTITLEMENT) {
            return Constants.JSON_FEATURE_FIELD_ENTITLEMENTS;
        } else if (type == Feature.Type.FEATURE) {
            return Constants.JSON_FEATURE_FIELD_FEATURES;
        } else if (type == Feature.Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP
                || type == Feature.Type.PURCHASE_OPTIONS) {
            return Constants.JSON_FIELD_PURCHASE_OPTIONS;
        }
        return Constants.JSON_FEATURE_FIELD_FEATURES;
    }

    @CheckForNull
    protected JSONObject embedOneChild(JSONObject feature, String childName, Map<String, Result> resultMap, AdditionalData additionalData) throws JSONException {
        String type = feature.optString(Constants.JSON_FEATURE_FIELD_TYPE, Feature.Type.FEATURE.toString());
        JSONArray children = embedChildren(feature, childName, resultMap, additionalData);

        JSONObject embedChild = new JSONObject();
        embedChild.put(Constants.JSON_FEATURE_FIELD_TYPE, type);
        embedChild.put(getChildrenElementNameByType(resolveFeatureType(type, Feature.Type.FEATURE)), children);
        JSONArray childFeatures = feature.optJSONArray(getChildrenName());
        if (childFeatures != null) {
            for (int i = 0; i < childFeatures.length(); i++) {
                JSONObject childFeature = childFeatures.getJSONObject(i);
                Feature.Type featureType = resolveFeatureType(childFeature.optString(Constants.JSON_FEATURE_FIELD_TYPE), Feature.Type.FEATURE);
                if (featureType == Feature.Type.MUTUAL_EXCLUSION_GROUP) {
                    List<String> appliedOrderRulesForAnalytics = additionalData.appliedOrderRulesForAnalytics.get("mx." + childFeature.optString("uniqueId"));

                    if (appliedOrderRulesForAnalytics != null) {
                        embedChild.put(Constants.JSON_APPLIED_REORDERED_RULE_NAMES, new JSONArray(appliedOrderRulesForAnalytics)); // appears on both features and mx nodes

                        JSONArray reordered = additionalData.reorderedFeatures.get("mx." + childFeature.optString("uniqueId"));
                        if (reordered != null) {
                            embedChild.put(Constants.JSON_FIELD_REORDERED_CHILDREN, reordered); // appears on both features and mx nodes
                        }
                    }
                }
            }
        }

        List<String> appliedOrderRulesForAnalytics = additionalData.appliedOrderRulesForAnalytics.get(extendedName(feature));

        if (appliedOrderRulesForAnalytics != null) {
            embedChild.put(Constants.JSON_APPLIED_REORDERED_RULE_NAMES, new JSONArray(appliedOrderRulesForAnalytics)); // appears on both features and mx nodes

            JSONArray reordered = additionalData.reorderedFeatures.get(extendedName(feature));
            if (reordered != null) {
                embedChild.put(Constants.JSON_FIELD_REORDERED_CHILDREN, reordered); // appears on both features and mx nodes
            }
        }

        int mutCount = mutexCount(feature);

        if (mutCount > 0) {
            embedChild.put(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON, mutCount);
            embedChild.put(Constants.JSON_FEATURE_FIELD_NAME, getMutexId(feature)); // mx.uniqueId
        } else {
            embedOneChildAttributes(feature, embedChild, resultMap);
        }
        return embedChild;
    }

    protected void embedOneChildAttributes(JSONObject feature, JSONObject embedChild, Map<String, Result> resultMap) {
        String id = getName(feature);
        Result result = resultMap.get(id);
        if (result == null) {
            return;
        }

        int loc = id.indexOf('.');
        if (loc == -1) {
            return;
        }
        String namespace = id.substring(0, loc);
        String name = id.substring(loc + 1);

        embedChild.put(Constants.JSON_FEATURE_FIELD_NAME, name);
        embedChild.put(Constants.JSON_FEATURE_FIELD_NAMESPACE, namespace);
        embedChild.put(Constants.JSON_FEATURE_IS_ON, result.isAccept());
        embedChild.put(Constants.JSON_FEATURE_TRACE, result.getTrace());
        embedChild.put(Constants.JSON_FEATURES_ATTRS, result.attributes.toString());
        embedChild.put(Constants.JSON_ORDERED_WEIGTH, result.getOrderingWeight());
        embedChild.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE, result.getPercentage());
        embedChild.put(Constants.JSON_FIELD_BRANCH_STATUS, result.getBranchStatus());
        embedChild.put(Constants.JSON_FIELD_PURCHASED, result.isPurchased());
        embedChild.put(Constants.JSON_FIELD_IS_PREMIUM, result.isPremium());
        embedChild.put(Constants.JSON_FIELD_STORE_PRODUCT_ID, result.getStoreProductId());
        embedChild.put(Constants.JSON_FIELD_PREMIUM_RULE_LAST_RESULT, result.getPremiumRuleResult());
        if (result.configRuleStatuses != null) {
            embedChild.put(Constants.JSON_FEATURE_CONFIGURATES_STATUSES, result.configRuleStatuses);
        }
        if (result.configAttributesForAnalytics != null) {
            embedChild.put(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS, result.configAttributesForAnalytics);
        }
        ArrayList<String> analyticsAppliedRules = result.getAnalyticsAppliedRules();
        if (analyticsAppliedRules != null && !analyticsAppliedRules.isEmpty()) {
            embedChild.put(Constants.JSON_FEATURE_CONFIG_ANALYTICS_APPLIED_RULES, new JSONArray(analyticsAppliedRules));
        }
        embedChild.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, result.isSendToAnalytics());
    }

    public enum FeatureStage {
        DEVELOPMENT,
        PRODUCTION
    }


    public enum FeatureType {
        FEATURE,
        MUTUAL_EXCLUSION_GROUP,
        ROOT
    }

    static class ConfigResult {
        JSONObject attributes;
        String disablingFeatureConfigName;
        ArrayList<String> appliedRules;
        JSONArray configRulesStatuses;
        ArrayList<String> appliedRulesAnalyticsEnabled;
    }

    public static class Fallback {
        final boolean accept;
        final boolean premiumRuleOn;
        final JSONObject attributes;

        public Fallback() {
            this.accept = false;
            this.premiumRuleOn = false;
            this.attributes = new JSONObject("{}");
        }

        public Fallback(boolean accept, JSONObject attributes) {
            this.accept = accept;
            this.premiumRuleOn = false;
            this.attributes = attributes;
        }

        public Fallback(boolean accept, boolean premiumRuleOn, JSONObject attributes) {
            this.accept = accept;
            this.premiumRuleOn = premiumRuleOn;
            this.attributes = attributes;
        }
    }

    public static class AdditionalData {
        final TreeSet<String> profileGroups;
        final String productVersion;
        @Nullable
        final JSONObject featureRandomNumber;
        final TreeMap<String, List<String>> appliedOrderRulesForAnalytics;
        final TreeMap<String, JSONArray> reorderedFeatures;
        final Hashtable<String, String> purchasedProductIds;
        public boolean checkMinVersion;
        private Map<String, List<String>> purchaseToProductId;

        public AdditionalData(List<String> profileGroups,
                              String productVersion,
                              @Nullable JSONObject featureRandomNumber,
                              @Nullable Collection<String> purchasedIdsList,
                              @Nullable Map<String, List<String>> purchaseToProductId) {
            this.profileGroups = new TreeSet<>(profileGroups);
            this.productVersion = productVersion;
            this.featureRandomNumber = featureRandomNumber;
            this.checkMinVersion = true;
            //noinspection unchecked
            this.reorderedFeatures = new TreeMap();
            this.appliedOrderRulesForAnalytics = new TreeMap<>();

            if (purchasedIdsList != null) {
                this.purchasedProductIds = new Hashtable<>();
                for (String purchaseId : purchasedIdsList) {
                    purchasedProductIds.put(purchaseId, purchaseId);
                }
            } else {
                purchasedProductIds = new Hashtable<>();
            }

            if (purchaseToProductId != null) {
                this.purchaseToProductId = new Hashtable<>(purchaseToProductId);

            } else {
                this.purchaseToProductId = new Hashtable<>();
            }
        }

        Map<String, List<String>> getPurchaseToProductId() {
            return purchaseToProductId;
        }

        Hashtable<String, String> getPurchasedProductIds() {
            return purchasedProductIds;
        }

        public void setPurchaseToProductId(Map<String, List<String>> purchaseToProductId) {
            this.purchaseToProductId = purchaseToProductId;
        }
    }
}