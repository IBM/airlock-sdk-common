package com.ibm.airlock.common.util;

/**
 * Holds constants values
 *
 * @author Denis Voloshin
 */
public class Constants {

    //Default file version
    public static final String DEFAULT_FILE_VERSION_v2_1 = "V2.1";
    public static final String DEFAULT_FILE_VERSION_v2_5 = "V2.5";
    public static final String DEFAULT_FILE_VERSION_v3_0 = "V3.0";

    //log Tag prefix
    public static final String LIB_LOG_TAG = "Airlock";

    //Airlock JSON fields
    public static final String JSON_FEATURE_FIELD_SEASON_ID = "seasonId";
    public static final String JSON_FEATURE_FIELD_PRODUCT_ID = "productId";
    public static final String JSON_FEATURE_FIELD_CACHED_S3PATH = "s3Path";
    public static final String JSON_FEATURE_FIELD_DIRECT_S3PATH = "devS3Path";
    public static final String JSON_FEATURE_FIELD_DEFAULT = "defaultIfAirlockSystemIsDown";
    public static final String JSON_FEATURE_FIELD_FEATURES = "features";
    public static final String JSON_FEATURE_FIELD_ROOT = "root";
    public static final String JSON_FEATURE_FIELD_STAGE = "stage";
    public static final String JSON_FEATURE_FIELD_NAME = "name";
    public static final String JSON_FEATURE_FIELD_UNIQUE_ID = "uniqueId";
    public static final String JSON_SEASON_FIELD_MIN_VER = "minVersion";


    public static final String JSON_FEATURE_FIELD_DEF_VAL = "defaultIfAirlockSystemIsDown";
    public static final String JSON_JS_FUNCTIONS_FIELD_NAME = "javascriptUtilities";
    public static final String JSON_DEFAULT_LANG_FIELD_NAME = "defaultLanguage";
    public static final String JSON_DEFAULT_CONFIG_FIELD_NAME = "defaultConfiguration";
    public static final String JSON_FEATURE_FIELD_NAMESPACE = "namespace";
    public static final String JSON_FEATURE_FIELD_ENABLED = "enabled";
    public static final String JSON_FIELD_INTERNAL_USER_GROUPS = "internalUserGroups";
    public static final String JSON_FEATURE_FIELD_TYPE = "type";
    public static final String JSON_FEATURE_FIELD_MIN_APP_VER = "minAppVersion";
    public static final String JSON_FEATURE_FIELD_RULE = "rule";
    public static final String JSON_FEATURE_FIELD_MIN_VERSION = "minVersion";
    public static final String JSON_FEATURE_FIELD_MAX_VERSION = "maxVersion";
    public static final String JSON_FEATURE_FIELD_VERSION_RANGE = "appVersions";
    public static final String JSON_SUPPORTED_LANGUAGES_FIELD = "supportedLanguages";
    public static final String JSON_FEATURE_FIELD_MAX_FEATURES_ON = "maxFeaturesOn";
    public static final String JSON_FEATURES_ATTRS = "featureAttributes";
    public static final String JSON_FEATURE_FIELD_NO_CACHED_RES = "noCachedResults";
    public static final String JSON_FEATURE_FIELD_CONFIGURATION_RULES = "configurationRules";
    public static final String JSON_DEFAULT_VERSION = "version";
    public static final String JSON_TRANSLATION_STRING = "strings";

    // Multi Server Default File
    public static final String JSON_DEFAULT_SERVER_FIELD_NAME = "defaultServer";
    public static final String JSON_SERVERS_ARRAY_FIELD_NAME = "servers";
    public static final String JSON_SERVER_NAME_FIELD_NAME = "displayName";
    public static final String JSON_SERVER_URL_FIELD_NAME = "url";
    public static final String JSON_SERVER_CDN_URL_FIELD_NAME = "cdnOverride";

    //Output schema
    public static final String FEATURE_ON = "featureON";

    //keep the flag for backward-compatibility
    public static final String ENABLE_FEATURE = "enableFeature";

    // unique URL_PRODUCTS_STRING_FORMAT result parsing strings
    public static final String JSON_FEATURE_FIELD_PRODUCTS = "products";
    public static final String JSON_FEATURE_FIELD_PRODUCT_UNIQUE_ID = "uniqueId";
    public static final String JSON_FEATURE_FIELD_PRODUCT_NAME = "name";
    public static final String JSON_FEATURE_FIELD_SEASONS = "seasons";
    public static final String JSON_AIRLOCK_BUILD_IN_FIELDS = "airlock";
    public static final String JSON_USER_ID = "userId";
    public static final String JSON_AIRLOCK_USER_ID = "airlockUserId";


    public static final String JSON_PRODUCT_SEASON_UNIQUE_ID = "uniqueId";

    public static final String JSON_FEATURE_FIELD_PERCENTAGE = "rolloutPercentage";
    public static final String JSON_FEATURE_FIELD_PERCENTAGE_BITMAP = "rolloutPercentageBitmap";

    //rule JSON fields
    public static final String JSON_RULE_FIELD_RULE_STR = "ruleString";
    public static final String JSON_FIELD_ROOT = "root";
    public static final String SCRIPT_EXECUTION_TIMEOUT_EXCEPTION = "Feature rule execution exceeded the maximum time of %d milliseconds";
    public static final String SCRIPT_CONTEXT_INIT_EXCEPTION = "Javascript engine initialization failed.";

    //feature toString fields
    public static final String JSON_FEATURE_FULL_NAME = "fullName";
    public static final String JSON_FEATURE_SOURCE = "source";
    public static final String JSON_FEATURE_PARENT_NAME = "parent";
    public static final String JSON_FEATURE_CHILDREN = "children";
    public static final String JSON_FEATURE_IS_ON = "isON";
    public static final String JSON_FEATURE_TRACE = "traceInfo";
    public static final String JSON_FEATURE_CONFIGURATION = "configuration";
    public static final String JSON_FIELD_SEND_TO_ANALYTICS = "sendToAnalytics";
    public static final String JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS = "configAttributesForAnalytics";
    public static final String JSON_FEATURE_CONFIG_ANALYTICS_APPLIED_RULES = "analyticsAppliedRules";
    public static final String JSON_FEATURE_CONFIGURATES_STATUSES = "configurationStatuses";


    //analytics
    public static final String JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS = "inputFieldsForAnalytics";
    public static final String JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS = "featuresAndConfigurationsForAnalytics";
    public static final String JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS = "featuresAttributesForAnalytics";
    public static final String JSON_FIELD_ANALYTICS = "analytics";
    public static final String JSON_FIELD_ATTRIBUTES = "attributes";


    // premium
    public static final String JSON_FIELD_PURCHASE_ANDROID_STORE_TYPE = "Google Play Store";
    public static final String JSON_FEATURE_FIELD_ENTITLEMENTS = "entitlements";
    public static final String JSON_FEATURE_FIELD_ENTITLEMENT = "entitlement";
    public static final String JSON_FIELD_ENTITLEMENT_ROOT = "entitlementsRoot";
    public static final String JSON_FIELD_PURCHASE_OPTIONS = "purchaseOptions";
    public static final String JSON_FIELD_STORE_PRODUCTS = "storeProductIds";
    public static final String JSON_FIELD_STORE_TYPE = "storeType";
    public static final String JSON_FIELD_STORE_PRODUCT_ID = "productId";
    public static final String JSON_FIELD_PREMIUM = "premium";
    public static final String JSON_FIELD_PREMIUM_RULE = "premiumRule";
    public static final String JSON_FIELD_IS_PREMIUM = "isPremium";
    public static final String JSON_FIELD_PURCHASED = "purchased";
    public static final String JSON_FIELD_BRANCH_PURCHASE_OPTION_ITEMS = "branchPurchaseOptionsItems";
    public static final String JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS = "branchEntitlementItems";
    public static final String JSON_FIELD_INCLUDED_ENTITLEMENTS = "includedEntitlements";
    public static final String JSON_FIELD_PREMIUM_RULE_LAST_RESULT = "premiumRuleResult";
    public static final String PURCHASED_IDS_FOR_DEBUG = "purchasedProductIdsForDebug";



    // Shared Preferences
    public static final String SP_SERVER_FEATURE_LIST = "airlock.sdk.features";
    public static final String SP_SYNCED_FEATURES_LIST = "airlock.synced.features";
    public static final String SP_PRE_SYNCED_FEATURES_LIST = "airlock.presynced.features";
    public static final String SP_LAST_SYNC_TIME = "airlock.lastSyncTime";
    public static final String SP_SYNCED_ENTITLEMENTS_LIST = "airlock.synced.entitlements";
    public static final String SP_PRE_SYNCED_ENTITLEMENTS_LIST = "airlock.presynced.entitlements";
    public static final String SP_LAST_CALCULATE_TIME = "airlock.lastCalculateTime";
    public static final String SP_LAST_FEATURES_PULL_TIME = "airlock.lastFeaturesConfigPullRequestTime";
    public static final String SP_USER_RANDOM_NUMBER = "airlock.userRandomNumber";
    public static final String SP_AIRLOCK_USER_UNIQUE_ID = "airlock.user.unique.id";
    public static final String SP_SEASON_ID = "airlock.seasonId";
    public static final String SP_DEFAULT_PRODUCT_ID = "airlock.default_productId";
    public static final String SP_UPDATED_DEFAULT_FILE = "airlock.defaultFile";
    public static final String SP_DEFAULT_LANGUAGE = "airlock.default.language";
    public static final String SP_SUPPORTED_LANGUAGES = "airlock.supported.languages";
    public static final String SP_PRODUCT_VERSION = "airlock.productVersion";
    public static final String SP_AIRLOCK_VERSION = "airlock.version";
    public static final String SP_DEFAULT_FILE = "airlock.defaultFile";
    public static final String SP_RAW_RULES = "airlock.raw_rules";
    public static final String SP_CURRENT_CONTEXT = "airlock.current.context";
    public static final String SP_RANDOMS = "airlock.randoms";
    public static final String JSON_STREAM_NAME = "name";

    //Notifications
    public static final String SP_PENDING_NOTIFICATIONS   = "airlock.notifications.pending";
    public static final String SP_NOTIFICATIONS =           "airlock.notifications";
    public static final String SP_FIRED_NOTIFICATIONS   = "airlock.notifications.fired";
    public static final String SP_NOTIFICATIONS_HISTORY   = "airlock.notifications.history";


    public static final String SP_STREAMS_PROCESS_SUSPENDED = "airlock.feature.streams.process.suspended";
    public static final String SP_FEATURE_USAGE_STREAMS = "airlock.feature.usage.streams";
    public static final String SP_FEATURE_UTILS_STREAMS = "airlock.feature.utils.streams";
    public static final String SP_STREAM_NAMES = "airlock.feature.stream.names";
    public static final String SP_RAW_TRANSLATIONS = "airlock.raw_translations";
    public static final String SP_RAW_JS_FUNCTIONS = "airlock.raw_js_functions";
    public static final String SP_DIRECT_S3PATH = "airlock.direct.s3Path";
    public static final String SP_CACHED_S3PATH = "airlock.cached.s3Path";
    public static final String SP_DEFAULT_SERVER_NAME = "airlock.default_server_name";
    public static final String SP_USER_GROUPS = "airlock.userGroups";
    public static final String SP_DEVELOP_BRANCH = "airlock.develop.branch";
    public static final String SP_DEVELOP_BRANCH_NAME = "airlock.develop.branch.name";
    public static final String SP_BRANCH_NAME = "airlock.branch.name";
    public static final String SP_EXPERIMENT_INFO = "airlock.experiment.info";
    public static final String SP_DEVELOP_BRANCH_ID = "airlock.develop.branch.id";
    public static final String SP_LAST_DEVICE_LANGUAGE = "airlock.lastLanguage";
    public static final String SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME = "airlock.lastJSUtilsDownloadTime";
    public static final String SP_LAST_TRANS_FULL_DOWNLOAD_TIME = "airlock.lastTranslationDownloadTime";
    public static final String SP_LAST_TIME_PRODUCT_CHANGED = "airlock.lastTimeProductChanged";
    public static final String SP_LAST_FEATURES_FULL_DOWNLOAD_TIME = "airlock.lastFeaturesConfigDownloadTime";
    public static final String SP_LAST_STREAMS_FULL_DOWNLOAD_TIME = "airlock.lastStreamsDownloadTime";
    public static final String SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME = "airlock.lastStreamUtilsDownloadTime";
    public static final String SP_LAST_NOTIFICATIONS_FULL_DOWNLOAD_TIME = "airlock.lastNotificationsDownloadTime";
    public static final String SP_CURRENT_LOCALE = "airlock.current.locale";
    public static final String SP_PRODUCT_KEY = "airlock.product.key";
    public static final String SP_FEATURES_PERCENAGE_MAP = "airlock.feature.percentage.map";

    // select Server field
    public static final String SP_CURRENT_SERVER = "airlock.CurrentServer";
    public static final String SP_CURRENT_PRODUCT_ID = "airlock.CurrentProductId";

    // Javascript variables names
    public static final String JS_TRANSLATIONS_VAR_NAME = "translations";
    public static final String JS_GROUPS_VAR_NAME = "groups";
    public static final String JS_CONTEXT_VAR_NAME = "context";
    public static final String JS_PROFILE_VAR_NAME = "profile";

    //runtime constants
    public static final long TIME_NOT_DEFINED = 0;
    public static final int INVALID_FILE_ID = -1;
    public static final String EMPTY_JSON_OBJ = "{}";
    public static final String JSON_FIELD_BRANCH_STATUS = "branchStatus";
    public static final String JSON_FIELD_BRANCH_FEATURE_PARENT_NAME = "branchFeatureParentName";
    public static final String JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS = "branchConfigurationRuleItems";
    public static final String JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS = "branchOrderingRuleItems";
    public static final String JSON_FIELD_BRANCH_FEATURES_ITEMS = "branchFeaturesItems";
    public static final String JSON_FIELD_BRANCHES = "branches";
    public static final String MASTER_BRANCH_NAME = "MASTER";

    //General fields - used for randoms map and more
    public static final String JSON_FIELD_EXPERIMENTS = "experiments";
    public static final String JSON_FIELD_STREAMS = "streams";
    public static final String JSON_FIELD_FEATURES = "features";
    public static final String JSON_FIELD_NOTIFICATIONS = "notifications";

    //ExperimentsCalculator
    public static final String JSON_FIELD_VARIANTS = "variants";
    public static final String JSON_FIELD_BRANCH_NAME = "branchName";
    public static final String JSON_FIELD_MAX_EXPERIMENTS_ON = "maxExperimentsOn";
    public static final String JSON_FIELD_EXPERIMENT = "experiment";
    public static final String JSON_FIELD_VARIANT = "variant";
    public static final String JSON_FIELD_ORDERED_RULES_NAMES = "feature_%s_order_rules_appliedRules";
    public static final String JSON_FIELD_ORDERED_FEATURES = "feature_%s_children_order";
    public static final String JSON_FIELD_EXPERIMENT_NAME = "experimentName";
    public static final String JSON_FIELD_VARIANT_DATE_JOINED = "dateJoinedVariant";
    public static final String JSON_FIELD_DEVICE_EXPERIMENTS_LIST = "deviceExperimentsList";
    public static final String JSON_FIELD_NAME = "name";
    public static final String JSON_FIELD_UNIQUE_ID = "uniqueId";
    public static final String JSON_ORDERED_WEIGTH = "weight";
    //Debug Screen
    public static final String DEVICE_CONTEXT = "deviceContext";
    public static final String DEFAULT_FILE_ID = "defaultFileId";
    public static final String PRODUCT_VERSION = "productVersion";
    public static final String PURCHASED_PRODUCT_IDS = "purchasedProductIds";


    //Branches
    public enum BranchStatus {
        CHECKED_OUT,
        NEW,
        NONE, //taken from master
        TEMPORARY // mark temporary items
    }

    // Stream
    public static final String STREAM_PREFIX = "stream_";

    // user group
    public static final String USER_GROUP_CLICK_BOARD_PREFIX       = "userGroupPX:";


    // reordering
    public static final String JSON_APPLIED_REORDERED_RULE_NAMES = "appliedOrderedRuleNames";
    public static final String JSON_FIELD_REORDERED_CHILDREN = "reorderedChildren";
    public static final String JSON_FEATURE_FIELD_BASED_RULES_ORDERING = "orderingRules";

    //android stuff
    public static final String SP_NAME = "airlock";
}
