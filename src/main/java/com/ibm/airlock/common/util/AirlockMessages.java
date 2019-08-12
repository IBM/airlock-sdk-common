package com.ibm.airlock.common.util;

/**
 * Holds airlock messages (Log, Exception message etc)
 *
 * @author Denis Voloshin
 */

public class AirlockMessages {

    // Error messages
    public static final String ERROR_RESPONSE_BODY_IS_EMPTY = "The response body is empty.";
    public static final String ERROR_RESPONSE_CODE_ERROR_FORMATTED = "The response returned with an error - responseCode = %s";
    public static String ERROR_NAME_NAMESPACE_EMPTY = "Name and Namespace cannot be empty.";
    public static final String ERROR_MISSING_OR_EMPTY_VALUE_FORMATTED = "%s is missing or empty.";
    public static String ERROR_VALUE_MUST_BE_BOOLEAN_FORMATTED = "%s must be a Boolean value.";
    public static final String ERROR_SDK_NOT_INITIALIZED = "Call the InitSdk method before any other calls.";
    public static String ERROR_NO_VALID_SERVER_IN_DEFAULT = "Can't find valid server in default file";
    public static final String ERROR_SERVER_TIMEOUT_PULL_FEATURES = "A server connection timeout on pulling airlock feature configuration request";
    public static String ERROR_INVALID_FILE_ID = "Invalid file id";
    public static final String ERROR_DEFAULT_EMPTY_OR_NULL = "parseDefaultFile: file is empty or null";

    // Log messages

    //InfraAirlockService
    public static final String LOG_CALCULATE_MISSING_PULL_RESULT = "CalculateFeatures: last pull results are empty, function returns without calculating";
    public static String LOG_FAILED_TO_PARSE_GROUPS_FROM_CLIP_BOARD = "Failed to parse user groups from the clip board";
    public static final String LOG_PULL_TRANSLATION_RESPONSE_CODE_FORMATTED = " response code pull translation= %s";
    public static final String LOG_PULL_FEATURES_RESPONSE_CODE_FORMATTED = " response code pull features= %s";

    public static String LOG_FAILED_TO_PULL_FEATURE_LIST = "Failed to pull feature list";
    public static final String LOG_FAILED_TO_PULL_SERVER_LIST = "Failed to pull Server List ";
    public static String LOG_FAILED_TO_PULL_FEATURE_USAGE_RULES = "Failed to pull Feature Usage Rules";
    public static final String LOG_FAILED_TO_PULL_DEFAULT_FILE = "Failed to pull default file";
    public static final String LOG_FAILED_TO_PULL_SEASON_ID = "Failed to fetch Updated SeasonId ";
    public static final String LOG_FAILED_TO_PULL_PRODUCTS = "Failed to get product list";

    public static final String LOG_PRODUCT_DOESNT_SUPPORT_MULTI_SERVERS = "This airlock product can't be selected because the server doesn't support the multiple servers feature";

    public static final String LOG_SDK_GET_PROD_CONFIG = "The SDK retrieves the production configuration";
    public static final String LOG_SDK_GET_DEV_CONFIG = "The SDK retrieves the development configuration";

    //DefaultFileParser
    public static final String LOG_DEFAULTS_FILE_EMPTY = "Defaults file is empty.";
    public static final String LOG_PARSE_ALL_DEFAULT_FILE = "Parse the entire defaults file";
    public static final String LOG_PARSE_DEFAULTS_ONLY_FEATURES = "Parse only features from the defaults file";
    public static final String LOG_NO_FEATURES_IN_DEFAULT_FILE = "No features in the defaults file";
    public static final String LOG_EXCEPTION_ON_PARSE_FEATURES = "Exception on parse features";
    public static final String LOG_DEFAULT_NOT_IN_VALID_JSON_FORMAT = "Failed - the defaults file is not in a valid JSON format";

    // FeatureList
    public static final String LOG_TO_STRING_FAILED = "toString failed";
    public static String LOG_PARENT_DOESNT_EXIST_IN_NEW_LIST_FORMATTED = "Parent %s does not exist in the new features list";

    // RemoteConfigurationAsyncFetcher
    public static final String LOG_ERROR_WHEN_ACCESSING_URL_FORMATTED = "Error occurred when accessing, url: %s";
    public static final String LOG_CONTENT_HASNT_CHANGED_FORMATTED = "The content has not changed, url: %s";

    //BaseRawFeaturesJsonParser
    public static final String LOG_CANT_PARSE_FORMATTED = "Can't parse %s";
    public static final String LOG_CANT_PARSE_ATTRIBUTE = "Can't parse attribute/configuration";
    public static final String LOG_CANT_READ_FROM_DEFAULT_FORMATTED = "Can't read %s from the defaults file";

    //AirlockManager:
    public static final String ERROR_FETCH_USER_GROUP_FORMATTED = "Error when fetching user groups, url: %s";
    public static final String ERROR_FETCH_BRANCHES_FORMATTED = "Error when fetching product branches, url: %s";

    //AirlockManager:
    public static final String ERROR_SP_NOT_INIT_CANT_CLEAR = "SharedPreferences was not initialized - can't clear SharedPreference records.";

    //Feature
    public static final String ERROR_FAILED_ON_TOSTRING_FUNCTION = "Error on feature toString function - failed to create a JSON object from the feature.";
}
