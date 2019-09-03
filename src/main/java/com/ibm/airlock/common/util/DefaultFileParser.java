package com.ibm.airlock.common.util;

import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.model.FeaturesList;
import com.ibm.airlock.common.engine.ScriptExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Denis Voloshin
 */

public class DefaultFileParser {

    private static final Logger log = Logger.getLogger(DefaultFileParser.class.getName());

    public static JSONObject getEntitlementDefaultsAsList(String defaultFileContent) throws AirlockInvalidFileException {
        JSONObject defaultsJson;
        try {
            defaultsJson = new JSONObject(defaultFileContent);
        } catch (JSONException e) {
            log.log(Level.SEVERE, AirlockMessages.LOG_DEFAULT_NOT_IN_VALID_JSON_FORMAT, e);
            throw new AirlockInvalidFileException(e.getMessage());
        }
        if (defaultsJson.has(Constants.JSON_FIELD_ENTITLEMENT_ROOT)
                && defaultsJson.getJSONObject(Constants.JSON_FIELD_ENTITLEMENT_ROOT).has(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)) {
            JSONObject entitlementDefaults = new JSONObject();
            return defaultsJson.getJSONObject(Constants.JSON_FIELD_ENTITLEMENT_ROOT);
        }
        return new JSONObject();
    }

    private static void flattenEntitlementDefault(JSONObject entitlementDefault, JSONArray entitlementDefaultsAsList) {
        JSONArray entitlementDefaultsArray =
                entitlementDefault.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);

        if (entitlementDefaultsArray != null && entitlementDefaultsArray.length() > 0) {
            for (int i = 0; i < entitlementDefaultsArray.length(); i++) {
                flattenEntitlementDefault(entitlementDefaultsArray.optJSONObject(i), entitlementDefaultsAsList);
            }
            entitlementDefaultsAsList.put(entitlementDefault);
        } else {
            entitlementDefaultsAsList.put(entitlementDefault);
        }

    }

    public static FeaturesList parseDefaultFile(PersistenceHandler sp, String defaultFileContent, boolean featuresOnly, boolean initialAppRun) throws
            AirlockInvalidFileException {
        JSONObject defaultJsonObject;
        try {
            defaultJsonObject = new JSONObject(defaultFileContent);
        } catch (JSONException e) {
            log.log(Level.SEVERE, AirlockMessages.LOG_DEFAULT_NOT_IN_VALID_JSON_FORMAT, e);
            throw new AirlockInvalidFileException(e.getMessage());
        }
        if (defaultJsonObject.length() == 0) {
            log.log(Level.WARNING, AirlockMessages.LOG_DEFAULTS_FILE_EMPTY);
            return new FeaturesList();
        }
        //TODO: open this comment when all products will have default version V2.1
        String fileVersion = defaultJsonObject.optString(Constants.JSON_DEFAULT_VERSION);
        if (!Constants.DEFAULT_FILE_VERSION_v3_0.equals(fileVersion) && !Constants.DEFAULT_FILE_VERSION_v2_1.equals(fileVersion) && !Constants
                .DEFAULT_FILE_VERSION_v2_5.equals(fileVersion)) {
            String msg = "Default file version doesn't supported, should be [" + Constants.DEFAULT_FILE_VERSION_v2_1 + '-' + Constants
                    .DEFAULT_FILE_VERSION_v3_0 + "]. Found " + fileVersion;
            log.log(Level.WARNING, msg);
            throw new AirlockInvalidFileException(msg);
        }

        if (initialAppRun) {
            sp.write(Constants.SP_AIRLOCK_VERSION, fileVersion);
        }

        if (!featuresOnly) {
            log.log(Level.INFO, AirlockMessages.LOG_PARSE_ALL_DEFAULT_FILE);
            setConfigurationProperty(sp, Constants.JSON_FEATURE_FIELD_SEASON_ID, Constants.SP_SEASON_ID, defaultJsonObject, true);
            setConfigurationProperty(sp, Constants.JSON_FEATURE_FIELD_PRODUCT_ID, Constants.SP_DEFAULT_PRODUCT_ID, defaultJsonObject, true, initialAppRun);
            setConfigurationProperty(sp, Constants.JSON_FEATURE_FIELD_CACHED_S3PATH, Constants.SP_CACHED_S3PATH, defaultJsonObject, true, initialAppRun);
            setConfigurationProperty(sp, Constants.JSON_FEATURE_FIELD_DIRECT_S3PATH, Constants.SP_DIRECT_S3PATH, defaultJsonObject, false, initialAppRun);
            setConfigurationProperty(sp, Constants.JSON_DEFAULT_LANG_FIELD_NAME, Constants.SP_DEFAULT_LANGUAGE, defaultJsonObject, false);
            setConfigurationProperty(sp, Constants.JSON_SUPPORTED_LANGUAGES_FIELD, Constants.SP_SUPPORTED_LANGUAGES, defaultJsonObject, false);
            if (initialAppRun) {
                setConfigurationProperty(sp, Constants.JSON_FEATURE_FIELD_PRODUCT_ID, Constants.SP_CURRENT_PRODUCT_ID, defaultJsonObject, true, initialAppRun);
            }
        } else {
            log.log(Level.WARNING, AirlockMessages.LOG_PARSE_DEFAULTS_ONLY_FEATURES);
        }
        JSONObject featuresArray = defaultJsonObject.optJSONObject(Constants.JSON_FEATURE_FIELD_ROOT);
        FeaturesList result = new FeaturesList();
        if (featuresArray == null) {
            log.log(Level.WARNING, AirlockMessages.LOG_NO_FEATURES_IN_DEFAULT_FILE);
            return result;
        }
        try {
            result = BaseRawFeaturesJsonParser.getInstance().getFeatures(featuresArray, Feature.Source.DEFAULT);
        } catch (JSONException | ScriptExecutionException e) {
            log.log(Level.SEVERE, AirlockMessages.LOG_EXCEPTION_ON_PARSE_FEATURES, e);
            throw new AirlockInvalidFileException(e.getMessage());
        }
        return result;
    }

    public static FeaturesList cloneDefaultFile(String defaultFileContent) {
        JSONObject defaultJsonObject;
        try {
            defaultJsonObject = new JSONObject(defaultFileContent);
        } catch (JSONException e) {
            log.log(Level.SEVERE, AirlockMessages.LOG_DEFAULT_NOT_IN_VALID_JSON_FORMAT, e);
            return new FeaturesList();
        }
        if (defaultJsonObject.length() == 0) {
            log.log(Level.WARNING, AirlockMessages.LOG_DEFAULTS_FILE_EMPTY);
            return new FeaturesList();
        }
        JSONObject featuresArray = defaultJsonObject.optJSONObject(Constants.JSON_FEATURE_FIELD_ROOT);
        FeaturesList result = new FeaturesList();
        if (featuresArray == null) {
            log.log(Level.INFO, AirlockMessages.LOG_NO_FEATURES_IN_DEFAULT_FILE);
            return result;
        }
        try {
            result = BaseRawFeaturesJsonParser.getInstance().getFeatures(featuresArray, Feature.Source.DEFAULT);
        } catch (JSONException | ScriptExecutionException e) {
            log.log(Level.SEVERE, AirlockMessages.LOG_EXCEPTION_ON_PARSE_FEATURES, e);
        }
        return result;
    }

    //read config value from the default value and copies into the  SharedPreference
    //returns the value in the SharedPreference
    //throws AirlockInvalidFileException if @mandatory = true and the name doesn't exist in the JsonObject

    private static void setConfigurationProperty(PersistenceHandler sp, String propertyName, String sharePreferenceName, JSONObject defaultConfig,
                                                   boolean mandatory) throws AirlockInvalidFileException {
        setConfigurationProperty(sp, propertyName, sharePreferenceName, defaultConfig, mandatory, true);
    }

    private static void setConfigurationProperty(PersistenceHandler sp, String propertyName, String sharePreferenceName, JSONObject defaultConfig,
                                                   boolean mandatory, boolean overwriteIfExist) throws AirlockInvalidFileException {
        // not overwrite and exist - return the value from preferences.
        if (!overwriteIfExist) {
            String spValue = sp.read(sharePreferenceName, "");
            if (!spValue.isEmpty()) {
                return;
            }
        }
        String value = defaultConfig.optString(propertyName);
        if (!value.isEmpty()) {
            sp.write(sharePreferenceName, value);
        } else if (mandatory) {
            throw new AirlockInvalidFileException(String.format(AirlockMessages.ERROR_MISSING_OR_EMPTY_VALUE_FORMATTED, propertyName));
        }
        return;
    }
}
