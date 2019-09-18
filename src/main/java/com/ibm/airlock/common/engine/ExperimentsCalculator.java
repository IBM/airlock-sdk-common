package com.ibm.airlock.common.engine;

import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.engine.entitlements.EntitlementsBranchMerger;
import com.ibm.airlock.common.engine.entitlements.EntitlementsCalculator;
import com.ibm.airlock.common.engine.features.FeaturesBranchMerger;
import com.ibm.airlock.common.engine.features.FeaturesCalculator;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.*;

import static com.ibm.airlock.common.util.Constants.*;


/**
 * The Airlock rule engine for airlock experiments and features
 *
 * @author Denis Voloshin
 */
public class ExperimentsCalculator extends FeaturesCalculator {
    private static final String DEV_MODE = "DevelopmentMode";
    private static final String MASTER_DEFAULT = "default";

    public CalculationResults calculate(InfraAirlockService infraAirlockService, @Nullable JSONObject runtimeFeatures,
                                        @Nullable JSONObject context, String functions, JSONObject translations,
                                        @Nullable List<String> userGroups, @Nullable Map<String, Fallback> fallback,
                                        @Nullable String productVersion, @Nullable JSONObject featureRandomNumber,
                                        @Nullable Collection<String> purchasedIds,
                                        boolean isAllowExperimentEvaluation)
            throws JSONException, FeaturesBranchMerger.MergeException, ScriptInitException {

        final PersistenceHandler ph = infraAirlockService.getPersistenceHandler();

        if (context == null) {
            context = new JSONObject("{}");
        }

        if (userGroups == null) {
            userGroups = new LinkedList<>();
        }

        if (productVersion == null) {
            productVersion = "";
        }

        if (fallback == null) {
            fallback = new Hashtable<>();
        }

        ScriptInvoker invoker = createJsObjects(infraAirlockService.getAirlockContextManager(), context, functions, translations, userGroups);
        AdditionalData additionalData = new AdditionalData(userGroups, productVersion, featureRandomNumber, purchasedIds, null);

        // get selected branch if was selected (dev mode)....
        List<BranchInfo> branchInfo = getPreSelectedBranch(ph);

        //no pre-selected branch
        if (branchInfo == null) {
            JSONObject previousExperimentInfo = new JSONObject(ph.read(Constants.SP_EXPERIMENT_INFO, "{}"));
            String previousVariantId = previousExperimentInfo.optString(Constants.JSON_FIELD_VARIANT);
            long previousDateJoined = previousExperimentInfo.optLong(JSON_FIELD_VARIANT_DATE_JOINED);
            if (!isAllowExperimentEvaluation) {
                String previousBranchName = ph.getLastBranchName();
                String previousExperimentId = previousExperimentInfo.optString(Constants.JSON_FIELD_EXPERIMENT);
                if (!previousExperimentId.isEmpty()) {
                    branchInfo = new ArrayList<>();
                    BranchInfo info = new BranchInfo(previousExperimentId, previousBranchName, previousVariantId);
                    info.dateJoinedVariant = previousDateJoined;
                    branchInfo.add(info);
                }
            } else {
                branchInfo = findExperiments(ph, runtimeFeatures, invoker, additionalData, fallback);
                if (!branchInfo.isEmpty()) {
                    if (!previousVariantId.isEmpty() && branchInfo.get(0).variant != null && branchInfo.get(0).variant.equals(previousVariantId)) {
                        branchInfo.get(0).dateJoinedVariant = previousDateJoined;
                    } else {
                        branchInfo.get(0).dateJoinedVariant = (new Date()).getTime();
                    }
                }
            }
        }

        if (branchInfo != null && !branchInfo.isEmpty()) {
            final String currentBranchName = branchInfo.get(0).branch;
            String previousBranchName = ph.getLastBranchName();
            if (!previousBranchName.equals(currentBranchName)) {
                infraAirlockService.resetFeaturesToDefault();
                infraAirlockService.resetEntitlementsToDefault();
                new Thread() {
                    @Override
                    public void run() {
                        ph.write(Constants.SP_BRANCH_NAME, currentBranchName == null ? "" : currentBranchName);
                    }
                }.start();
            }
        } else {
            branchInfo = new ArrayList<>();
            new Thread() {
                @Override
                public void run() {
                    ph.write(Constants.SP_BRANCH_NAME, "");
                }
            }.start();
        }


        long start = System.currentTimeMillis();
        JSONObject runtimeTree = applyBranchesToMainRuntimeTree(ph, runtimeFeatures, branchInfo);
        JSONObject rootFeatures = getRoot(runtimeTree);
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().report(AirlockEnginePerformanceMetric.BRANCH_MERGING, start);

        EntitlementsCalculator entitlementsCalculator = new EntitlementsCalculator();

        final JSONObject entitlements = entitlementsCalculator.calculate(infraAirlockService, runtimeTree,
                context, functions, translations, userGroups, fallback,
                productVersion, featureRandomNumber, null, null);


        additionalData.setPurchaseToProductId(entitlementsCalculator.getPurchaseToProductIdsMap(entitlements));

        start = System.currentTimeMillis();
        Map<String, Result> featuresResults = new TreeMap<>();
        doFeatureGroup(rootFeatures, getChildrenName(), invoker, additionalData, -1, fallback, featuresResults);
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().report(AirlockEnginePerformanceMetric.CALCULATION_FEATURES, start);
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().reportValue(AirlockEnginePerformanceMetric.EVALS_COUNTER, invoker.getEvalsCounter());

        // handle Notifications
        infraAirlockService.getNotificationService().calculateAndSaveNotifications(invoker);
        invoker.exit();

        CalculationResults calculationResults = new CalculationResults();
        calculationResults.setFeatures(embedResults(runtimeTree, getChildrenName(), featuresResults, additionalData));
        calculationResults.setEntitlements(entitlements);
        return calculationResults;
    }


    private static void saveCalculatedExperimentsList(PersistenceHandler ph, JSONArray experiments, HashMap<Pair<String, String>, Result> calculationResults) {
        try {
            JSONArray calculatedExperimentsArray = new JSONArray();
            for (int i = 0; i < experiments.length(); i++) {
                JSONObject experiment = experiments.getJSONObject(i);
                JSONObject calculatedExperiment = new JSONObject();
                String experimentName = experiment.getString(Constants.JSON_FEATURE_FIELD_NAME);
                calculatedExperiment.put(Constants.JSON_FEATURE_FIELD_NAME, experimentName);
                calculatedExperiment.put(Constants.JSON_FEATURE_FIELD_VERSION_RANGE, experiment.getString(JSON_FEATURE_FIELD_MIN_VERSION) + (experiment.get(JSON_FEATURE_FIELD_MAX_VERSION).equals(JSONObject.NULL) ? (" to " + experiment.get(JSON_FEATURE_FIELD_MAX_VERSION)) : " and up"));
                calculatedExperiment.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE, experiment.optDouble(JSON_FEATURE_FIELD_PERCENTAGE, 100));
                Result res = calculationResults.get(Pair.create(experimentName, ""));
                if (res != null) {
                    calculatedExperiment.put(Constants.JSON_FEATURE_IS_ON, String.valueOf(res.isAccept()));
                    calculatedExperiment.put(Constants.JSON_FEATURE_TRACE, res.getTrace());
                } else {
                    calculatedExperiment.put(Constants.JSON_FEATURE_IS_ON, String.valueOf(false));
                    calculatedExperiment.put(Constants.JSON_FEATURE_TRACE, Result.RULE_SKIPPED);
                }
                JSONArray allExperimentVariants = experiment.getJSONArray(Constants.JSON_FIELD_VARIANTS);
                JSONArray calculatedVariantsArray = new JSONArray();
                for (int j = 0; j < allExperimentVariants.length(); j++) {
                    JSONObject variant = allExperimentVariants.getJSONObject(j);
                    JSONObject calculatedVariant = new JSONObject();
                    String variantName = variant.getString(Constants.JSON_FEATURE_FIELD_NAME);
                    calculatedVariant.put(Constants.JSON_FEATURE_FIELD_NAME, variantName);
                    calculatedVariant.put(Constants.JSON_FIELD_BRANCH_NAME, variant.getString(Constants.JSON_FIELD_BRANCH_NAME));
                    calculatedVariant.put(Constants.JSON_FIELD_EXPERIMENT_NAME, variant.getString(Constants.JSON_FIELD_EXPERIMENT_NAME));
                    calculatedVariant.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE, variant.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100));
                    calculatedVariantsArray.put(calculatedVariant);
                    Result res2 = calculationResults.get(Pair.create(experimentName, variantName));
                    if (res == null || !res.isAccept()) {
                        calculatedVariant.put(Constants.JSON_FEATURE_IS_ON, String.valueOf(false));
                        calculatedVariant.put(Constants.JSON_FEATURE_TRACE, Result.RULE_PARENT_FAILED);
                    } else {
                        if (res2 != null) {
                            calculatedVariant.put(Constants.JSON_FEATURE_IS_ON, String.valueOf(res2.isAccept()));
                            calculatedVariant.put(Constants.JSON_FEATURE_TRACE, res2.getTrace());
                        } else {
                            calculatedVariant.put(Constants.JSON_FEATURE_IS_ON, String.valueOf(false));
                            calculatedVariant.put(Constants.JSON_FEATURE_TRACE, Result.RULE_SKIPPED);
                        }
                    }
                }
                calculatedExperiment.put(Constants.JSON_FIELD_VARIANTS, calculatedVariantsArray);
                calculatedExperimentsArray.put(calculatedExperiment);
            }
            ph.write(Constants.JSON_FIELD_DEVICE_EXPERIMENTS_LIST, (new JSONObject().put(Constants.JSON_FIELD_EXPERIMENTS, calculatedExperimentsArray).toString()));
        } catch (JSONException ignored) {

        }
    }

    @CheckForNull
    @Nullable
    private static List<BranchInfo> getPreSelectedBranch(PersistenceHandler ph) {
        List<BranchInfo> branchesList = null;
        String branchName = ph.getDevelopBranchName();
        if (!branchName.isEmpty()) {
            branchesList = new ArrayList<>();
            branchesList.add(new BranchInfo(DEV_MODE, branchName, branchName));
        }
        return branchesList;
    }


    private static JSONObject applyBranchesToMainRuntimeTree(PersistenceHandler ph, @Nullable JSONObject masterAndBranches,@Nullable List<BranchInfo> branchInfo) throws JSONException,
            FeaturesBranchMerger.MergeException {
        if (masterAndBranches == null) {
            return new JSONObject();
        }

        FeaturesBranchMerger featuresBranchMerger = new FeaturesBranchMerger();
        EntitlementsBranchMerger entitlementsBranchMerger = new EntitlementsBranchMerger();

        if (branchInfo == null || branchInfo.isEmpty()) {
            // clone just the top layer, return the rest as-is
            JSONObject shallowClone = (JSONObject) featuresBranchMerger.cloneJson(masterAndBranches, false);
            shallowClone.put(Constants.JSON_FIELD_EXPERIMENT, "<none>");
            shallowClone.put(Constants.JSON_FIELD_VARIANT, MASTER_DEFAULT);
            return shallowClone;
        }

        JSONObject master = masterAndBranches.getJSONObject(Constants.JSON_FIELD_ROOT);
        JSONObject clonedMaster = (JSONObject) featuresBranchMerger.cloneJson(master, true);
        Map<String, JSONObject> featuresNamesMap = featuresBranchMerger.getItemMap(clonedMaster, false);

        JSONObject clonedInAppEntitlementsMaster = null;
        Map<String, JSONObject> inAppPurchaseNamesMap = null;
        JSONObject inAppPurchasesMaster = masterAndBranches.optJSONObject(Constants.JSON_FIELD_ENTITLEMENT_ROOT);
        if (inAppPurchasesMaster != null) {
            clonedInAppEntitlementsMaster = (JSONObject) entitlementsBranchMerger.cloneJson(inAppPurchasesMaster, true);
            inAppPurchaseNamesMap = entitlementsBranchMerger.getItemMap(clonedInAppEntitlementsMaster, false);
        }


        JSONArray experimentNames = new JSONArray();

        // the master's analytics are already merged into the experiments, just get the model from the experiments
        JSONObject barInfo = new JSONObject();
        TreeSet<String> inputAnalytics = new TreeSet<>();
        TreeSet<String> nameAnalytics = new TreeSet<>();
        Map<String, Set<String>> attrAnalytics = new HashMap<>();

        boolean isDevMode = false;
        if (!branchInfo.isEmpty()) {
            for (BranchInfo info : branchInfo) {
                barInfo.put(Constants.JSON_FIELD_EXPERIMENT, info.experiment);
                barInfo.put(Constants.JSON_FIELD_VARIANT, info.variant);
                barInfo.put(Constants.JSON_FIELD_VARIANT_DATE_JOINED, info.dateJoinedVariant);

                if (info.experiment != null && info.experiment.equals(DEV_MODE)) {
                    isDevMode = true;
                    experimentNames.put(DEV_MODE);
                } else {
                    experimentNames.put(info.experiment + '/' + info.variant);
                }

                JSONObject branch;

                if (info.branch != null) {
                    String devBranchAsString = ph.getDevelopBranch();
                    try {
                        if (isDevMode && !devBranchAsString.isEmpty()) {
                            branch = new JSONObject(devBranchAsString);
                        } else {
                            isDevMode = false;
                            branch = findBranch(masterAndBranches, info.branch);
                        }
                        clonedMaster = featuresBranchMerger.merge(clonedMaster, featuresNamesMap, branch);
                        clonedInAppEntitlementsMaster = entitlementsBranchMerger.merge(clonedInAppEntitlementsMaster, inAppPurchaseNamesMap, branch);
                    } catch (JSONException ex) {
                        // Do nothing just use the root branch
                    }
                }
                if (isDevMode) {
                    break;
                }
                // the experiment model is used even running with the default master
                if (info.experiment != null && !info.experiment.isEmpty()) {
                    JSONObject experiment = findExperiment(masterAndBranches, info.experiment);
                    addAnalytics(experiment, Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, inputAnalytics);
                    addAnalytics(experiment, Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS, nameAnalytics);
                    addAttrAnalytics(experiment, attrAnalytics);
                }
                // otherwise, use the master as-is
            }
        }

        JSONObject out = new JSONObject();
        out.put(Constants.JSON_FIELD_ROOT, clonedMaster);
        out.put(Constants.JSON_FIELD_ENTITLEMENT_ROOT, clonedInAppEntitlementsMaster);
        out.put(Constants.SP_EXPERIMENT_INFO, barInfo); // for BAR reports
        out.put(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, new JSONArray(inputAnalytics));

        copyAnalytics(featuresNamesMap, nameAnalytics);
        copyAnalytics(featuresNamesMap, attrAnalytics);
        String branchName = Constants.JSON_FIELD_ROOT;
        if (!branchInfo.isEmpty()) {
            branchName = branchInfo.get(0).branch;
        }
        out.put(JSON_FIELD_BRANCH_NAME, branchName);//when branch is changed - cached feature list should not be used on merge.

        return out;
    }

    private static JSONObject findBranch(JSONObject json, String branchName) throws JSONException {
        JSONArray branches = json.optJSONArray(Constants.JSON_FIELD_BRANCHES);
        if (branches != null) {
            for (int i = 0; i < branches.length(); ++i) {
                JSONObject branch = branches.getJSONObject(i);
                String name = branch.getString(Constants.JSON_FIELD_NAME);
                if (name.equals(branchName)) {
                    return branch;
                }
            }
        }
        throw new JSONException("missing branch name " + branchName);
    }

    private static JSONObject findExperiment(JSONObject masterAndBranches, String experimentName) throws JSONException {
        JSONObject experiment;
        JSONObject experimentRoot = masterAndBranches.getJSONObject(Constants.JSON_FIELD_EXPERIMENTS);
        JSONArray experiments = experimentRoot.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
        for (int i = 0; i < experiments.length(); ++i) {
            experiment = experiments.getJSONObject(i);
            String name = experiment.getString(Constants.JSON_FIELD_NAME);
            if (name.equals(experimentName)) {
                return experiment;
            }
        }
        throw new JSONException("missing experiment name " + experimentName);
    }

    // merge analytics names from all experiments
    private static void addAnalytics(JSONObject experiment, String key, TreeSet<String> analytics) {
        JSONObject json = experiment.optJSONObject(Constants.JSON_FIELD_ANALYTICS);
        if (json != null) {
            JSONArray array = json.optJSONArray(key);
            if (array != null) {
                for (int i = 0; i < array.length(); ++i) {
                    analytics.add(array.optString(i));
                }
            }
        }
    }

    // merge analytics values from all experiments. using Map<featureName, Set<flatAttrValue>>
    private static void addAttrAnalytics(JSONObject experiment, Map<String, Set<String>> attrAnalytics) {
        JSONObject json = experiment.optJSONObject(Constants.JSON_FIELD_ANALYTICS);
        if (json == null) {
            return;
        }
        JSONArray array = json.optJSONArray(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS);
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.length(); ++i) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }

            String name = obj.optString(Constants.JSON_FIELD_NAME);
            JSONArray attributes = obj.optJSONArray(Constants.JSON_FIELD_ATTRIBUTES);

            Set<String> items = attrAnalytics.get(name);
            if (items == null) {
                items = new TreeSet<>();
                attrAnalytics.put(name, items);
            }

            if (attributes == null) {
                continue;
            }
            for (int j = 0; j < attributes.length(); ++j) {
                String item = attributes.optString(j);
                if (!item.isEmpty()) {
                    items.add(attributes.optString(j));
                }
            }
        }
    }

    // copy analytics names into runtime
    private static void copyAnalytics(Map<String, JSONObject> nameMap, TreeSet<String> nameAnalytics) throws JSONException {
        for (String name : nameAnalytics) {
            JSONObject obj = nameMap.get(name);
            if (obj != null) {
                obj.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, true);
            }
        }
    }

    // copy analytics attributes into runtime
    private static void copyAnalytics(Map<String, JSONObject> nameMap, Map<String, Set<String>> attrAnalytics) throws JSONException {
        for (Map.Entry<String, Set<String>> ent : attrAnalytics.entrySet()) {
            String name = ent.getKey();
            Set<String> data = ent.getValue();

            JSONObject obj = nameMap.get(name);
            if (obj != null) {
                obj.put(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS, new JSONArray(data));
            }
        }
    }

    private List<BranchInfo> findExperiments(PersistenceHandler ph, @Nullable JSONObject parent, ScriptInvoker invoker, AdditionalData additionalData, Map<String,
            Fallback> fallback) {
        ArrayList<BranchInfo> out = new ArrayList<>();

        if (parent == null) {
            return out;
        }
        HashMap<Pair<String, String>, Result> resultSet = new HashMap<>();

        JSONObject experimentRoot = parent.optJSONObject(Constants.JSON_FIELD_EXPERIMENTS);
        if (experimentRoot == null) {
            return out;
        }
        JSONArray experiments = experimentRoot.optJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
        if (experiments == null) {
            return out;
        }
        int maxExperiments = experimentRoot.optInt(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON);

        for (int i = 0; i < experiments.length(); ++i) {
            JSONObject experiment = experiments.optJSONObject(i);
            if (experiment == null) {
                continue;
            }
            Result result = doFeature(experiment, invoker, additionalData, fallback);
            String experimentName = experiment.optString(Constants.JSON_FEATURE_FIELD_NAME);
            resultSet.put(new Pair<>(experimentName, ""), result);
            if (!result.accept) {
                continue;
            }

            if (experimentName.isEmpty()) {
                continue;
            }
            String variantName = MASTER_DEFAULT; // default when all variants fail
            String branchName = "";  // default when all variants fail

            additionalData.checkMinVersion = false; // variants do not check minAppVersion

            JSONArray variants = experiment.optJSONArray(Constants.JSON_FIELD_VARIANTS);
            if (variants == null) {
                continue;
            }
            for (int j = 0; j < variants.length(); ++j) {
                JSONObject variant = variants.optJSONObject(j);
                if (variant == null) {
                    continue;
                }
                Result result2 = doFeature(variant, invoker, additionalData, fallback);
                variantName = variant.optString(Constants.JSON_FIELD_NAME, MASTER_DEFAULT);
                resultSet.put(new Pair<>(experimentName, variantName), result2);
                if (result2.accept) {
                    branchName = variant.optString(JSON_FIELD_BRANCH_NAME);
                    break;
                }
            }
            additionalData.checkMinVersion = true;
            if (variantName.isEmpty() || branchName.isEmpty()) {
                continue;
            }

            // If the experiment rule succeeded but all variant rules failed,
            // (or if a successful variant wants to use the master)
            // put in a null branch, meaning the experiment should use the master as-is
            if (variantName.equals(MASTER_DEFAULT)) {
                branchName = null;
            }

            out.add(new BranchInfo(experimentName, branchName, variantName));
            if (out.size() >= maxExperiments) {
                break;
            }
        }
        saveCalculatedExperimentsList(ph, experiments, resultSet);
        return out;
    }


    public static class CalculationResults {

        JSONObject features;
        JSONObject entitlements;

        CalculationResults() {
            features = new JSONObject();
            entitlements = new JSONObject();
        }

        public CalculationResults(JSONObject features, JSONObject entitlements) {
            this.features = features;
            this.entitlements = entitlements;
        }

        public JSONObject getFeatures() {
            return features;
        }

        void setFeatures(JSONObject features) {
            this.features = features;
        }

        public JSONObject getEntitlements() {
            return entitlements;
        }

        void setEntitlements(JSONObject entitlements) {
            this.entitlements = entitlements;
        }
    }

    static class BranchInfo {
        final String experiment;
        @Nullable
        final String branch;
        final String variant;
        long dateJoinedVariant;

        BranchInfo(String experiment, @Nullable String branch, String variant) {
            this.experiment = experiment;
            this.branch = branch;
            this.variant = variant; // null branch means "use the master as-is"
        }

        public String toString() {
            return "Experiment: " + experiment + ", variant: " + variant + ", branch: " + branch;
        }
    }
}
