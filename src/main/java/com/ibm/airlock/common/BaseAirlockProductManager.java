package com.ibm.airlock.common;

import com.ibm.airlock.common.cache.CacheManager;
import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.PercentageManager;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.cache.RuntimeLoader;
import com.ibm.airlock.common.data.Entitlement;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.FeaturesList;
import com.ibm.airlock.common.data.PurchaseOption;
import com.ibm.airlock.common.data.Servers;
import com.ibm.airlock.common.engine.AirlockContextManager;
import com.ibm.airlock.common.engine.StateFullContext;
import com.ibm.airlock.common.inapp.PurchasesManager;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.AirlockDAO;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.notifications.NotificationsManager;
import com.ibm.airlock.common.streams.AirlockStreamResultsTracker;
import com.ibm.airlock.common.streams.StreamsManager;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.LocaleProvider;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.ibm.airlock.common.util.BaseRawFeaturesJsonParser.getFieldValueFromJsonObject;


/**
 * Created by Denis Voloshin on 05/11/2017.
 */

public abstract class BaseAirlockProductManager implements AirlockProductManager {

    private final String TAG = this.getClass().getSimpleName();
    protected boolean init = false;
    private boolean isDoubleLengthStrings = false;
    private boolean allowExperimentEvaluation = false;
    protected final CacheManager cacheManager;
    protected StreamsManager streamsManager;
    protected PurchasesManager purchasesManager;
    protected ConnectionManager connectionManager;
    protected NotificationsManager notificationsManager;
    @Nullable
    protected String appVersion;

    protected BaseAirlockProductManager() {
        this.cacheManager = new CacheManager();
    }

    public BaseAirlockProductManager(String appVersion) {
        this();
        this.appVersion = appVersion;
    }

    @CheckForNull
    public String getAppVersion() {
        return this.appVersion;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public StreamsManager getStreamsManager() {
        return streamsManager;
    }

    public NotificationsManager getNotificationsManager() {
        return notificationsManager;
    }


    /**
     * @return whether AirlockManager.getString methods should return the same string as a doubled value
     */
    public boolean isDoubleLengthStrings() {
        return isDoubleLengthStrings;
    }

    /**
     * Sets the whether SDK should be configured to return AirlockManager.getString as as a doubled value
     *
     * @param doubleLengthStrings
     */
    public void setDoubleLengthStrings(boolean doubleLengthStrings) {
        isDoubleLengthStrings = doubleLengthStrings;
    }

    @Override
    public void initSDK(Context appContext, int defaultFileId, String productVersion) throws AirlockInvalidFileException, IOException {

    }

    @Override
    public void initSDK(Context appContext, String defaultFile, String productVersion) throws AirlockInvalidFileException, IOException {

    }

    @Override
    public void initSDK(Context appContext, RuntimeLoader runtimeLoader, String encryptionKey) throws AirlockInvalidFileException, IOException {

    }

    /**
     * Returns the airlock session id the airlock instance was initialized with
     * or null if {@link #initSDK(Context, int, String) initSDK} method hasn't called yet.
     */
    @CheckForNull
    public String getSeasonId() {
        return !init ? null : cacheManager.getSeasionId();
    }

    /**
     * Returns the airlock version from the airlock instance was initialized with
     * or null if {@link #initSDK(Context, int, String) initSDK} method hasn't called yet.
     */
    @CheckForNull
    public String getAirlockVersion() {
        return !init ? null : cacheManager.getAirlockVersion();
    }


    /**
     * Returns the airlock product id the airlock instance was initialized with or null
     * if {@link #initSDK(Context, int, String) initSDK} method hasn't called yet.
     */
    @CheckForNull
    public String getProductId() {
        return !init ? null : cacheManager.getProductId();
    }

    /**
     * Returns the airlock ExperimentInfo
     */
    @CheckForNull
    public Map<String, String> getExperimentInfo() {
        return !init ? null : cacheManager.getExperimentInfo();
    }


    /**
     * Returns the data provider mode that the SDK is using.
     *
     * @return The data provider mode that the SDK is using.
     */
    public AirlockDAO.DataProviderType getDataProviderType() {
        if (connectionManager != null) {
            return connectionManager.getDataProviderType();
        }
        return AirlockDAO.DataProviderType.CACHED_MODE;
    }

    /**
     * Sets the data provider mode that is configured for the SDK instance. Airlock must be initialized;
     * otherwise, the method has no effect.
     *
     * @param type The DataProviderType to be used.
     */
    public void setDataProviderType(AirlockDAO.DataProviderType type) {
        if (connectionManager != null) {
            connectionManager.setDataProviderType(type);
        }
    }

    /**
     * Asynchronously downloads the current list of features from the server.
     *
     * @param callback Callback to be called when the function returns.
     * @throws AirlockNotInitializedException if the Airlock SDK has not been initialized.
     */
    public void pullFeatures(final AirlockCallback callback) throws AirlockNotInitializedException {
        if (!init) {
            throw new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
        }
        cacheManager.pullFeatures(callback);
    }

    /**
     * Gets the raw list of features and their rules from server.
     */
    @CheckForNull
    public JSONObject getFeaturesConfigurationFromServer() {
        return cacheManager.getRawFeaturesConfiguration();
    }

    public void calculateFeatures(@Nullable JSONObject context, Collection<String> purchasedProducts) throws AirlockNotInitializedException, JSONException {
        if (!init) {
            throw new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
        }
        // if we are on the debug mode. check if there is
        // purchase option which a set as purchased for debugging purposes
        if (getDeviceUserGroups().size() > 0) {
            purchasedProducts.addAll(getPurchasedProductIdsForDebug());
        }
        cacheManager.calculateFeatures(null, context, purchasedProducts);
    }


    /**
     * Calculates the status of the features according to the pullFeatures results.
     * No feature status changes are exposed until the syncFeatures method is called.
     *
     * @param userProfile    the user profile
     * @param airlockContext the airlock context, consists of  airlock runtime context.
     * @throws AirlockNotInitializedException if the Airlock SDK has not been initialized
     * @throws JSONException                  if the pullFeature results, the userProfile or the airlock runtime context is not in the correct JSON format.
     */
    public void calculateFeatures(@Nullable JSONObject userProfile, @Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException {
        if (!init) {
            throw new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
        }
        cacheManager.calculateFeatures(userProfile, airlockContext);
    }

    /**
     * Asynchronously returns the list of user groups defined on the Airlock server.
     *
     * @param callback Callback to be called when the function returns.
     */

    public void getServerUserGroups(final AirlockCallback callback) {

        if (!init) {
            callback.onFailure(new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED));
            return;
        }

        AirlockDAO.pullUserGroups(cacheManager, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.log.e(TAG, String.format(AirlockMessages.ERROR_FETCH_USER_GROUP_FORMATTED, call.request().url().toString()));
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call request, Response response) throws IOException {
                String userGroupsResponse = response.body() == null ? "[]" : response.body().string();
                try {
                    JSONObject userGroupsJson = new JSONObject(userGroupsResponse);
                    if (userGroupsJson.has("internalUserGroups")) {
                        callback.onSuccess(userGroupsJson.getJSONArray("internalUserGroups").toString());
                    }
                } catch (Exception e) {
                    callback.onSuccess("[]");
                } finally {
                    response.body().close();
                }
            }
        });
    }


    /**
     * Asynchronously returns the list of user groups defined on the Airlock server.
     *
     * @param callback Callback to be called when the function returns.
     */

    public void getProductBranches(final AirlockCallback callback) {

        if (!init) {
            callback.onFailure(new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED));
            return;
        }

        AirlockDAO.pullBranches(cacheManager, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.log.e(TAG, String.format(AirlockMessages.ERROR_FETCH_BRANCHES_FORMATTED, call.request().url().toString()));
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call request, Response response) throws IOException {
                String userGroupsResponse = response.body() == null ? "[]" : response.body().string();
                try {
                    JSONObject userGroupsJson = new JSONObject(userGroupsResponse);
                    if (userGroupsJson.has("branches")) {
                        callback.onSuccess(userGroupsJson.getJSONArray("branches").toString());
                    }
                } catch (Exception e) {
                    callback.onSuccess("[]");
                } finally {
                    response.body().close();
                }
            }
        });
    }


    /**
     * @return the current streams result state.
     */
    public String getStreamsSummary() {
        String result = "{}";
        if (!streamsManager.isEnabled()) {
            return result;
        } else {
            result = streamsManager.getStreamsResults();
        }
        return result;
    }


    /**
     * stores the current streams state.
     */
    public void persistStreams() {
        streamsManager.persist();
    }


    /**
     * adds an event to the streams processing
     *
     * @param events
     * @param processImmediately
     */
    public JSONArray addStreamsEvent(JSONArray events, boolean processImmediately) {
        try {
            return streamsManager.calculateAndSaveStreams(events, processImmediately);
        } catch (JSONException e) {
            //Do nothing
            return null;
        }
    }

    /**
     * adds an event to the streams processing
     *
     * @param events
     * @param processImmediately
     */
    public JSONArray addStreamsEvent(JSONArray events, boolean processImmediately, @Nullable JSONArray fieldToSendToAnalytics, @Nullable AirlockStreamResultsTracker airlockStreamResultsTracker) {
        try {
            return streamsManager.calculateAndSaveStreams(events, processImmediately, null, fieldToSendToAnalytics, airlockStreamResultsTracker);
        } catch (JSONException e) {
            //Do nothing
            return null;
        }
    }

    /**
     * adds an event to the streams processing
     *
     * @param event
     */
    public JSONArray addStreamsEvent(JSONObject event) {
        JSONArray events = new JSONArray();
        JSONArray errors = null;

        events.put(event);
        try {
            errors = streamsManager.calculateAndSaveStreams(events, true);
        } catch (JSONException e) {
            //Do nothing
        }
        return errors;
    }

    /**
     * Returns a list of user groups selected for the device.
     *
     * @return a list of user groups selected for the device.
     * @throws JSONException if the provided value breaks the JSON serialization process.
     */
    public List<String> getDeviceUserGroups() {
        return cacheManager.getPersistenceHandler().getDeviceUserGroups();
    }

    /**
     * Specifies a list of user groups selected for the device.
     *
     * @param userGroups List of the selected user groups.
     * @throws JSONException if the provided value breaks the JSON serialization process.
     */
    public void setDeviceUserGroups(@Nullable List<String> userGroups) {
        if (userGroups == null || userGroups.size() == 0) {
            cacheManager.clearPreSyncRuntimeData();
        }
        if (userGroups == null) {
            return;
        }
        cacheManager.getPersistenceHandler().storeDeviceUserGroups(userGroups, streamsManager);
    }

    /**
     * Synchronizes the latest refreshFeatures results with the current feature set.
     * Updates LastSyncTime.
     *
     * @throws AirlockNotInitializedException if the SDK has not been initialized.
     */
    public void syncFeatures() throws AirlockNotInitializedException {
        if (!init) {
            throw new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
        }
        cacheManager.syncFeatures();
    }

    /**
     * Returns the feature object by its name.
     * If the feature doesn't exist in the feature set, getFeature returns a new Feature object
     * with the given name, isOn=false, and source=missing.
     *
     * @param featureName Feature name in the format namespace.name.
     * @return Returns the feature object.
     */
    public Feature getFeature(String featureName) {
        if (!init || cacheManager.getFeature(featureName) == null) {
            return new Feature(featureName, false, Feature.Source.MISSING);
        }
        return cacheManager.getFeature(featureName);
    }


    /**
     * Returns a cloned list of the ROOT children features.
     * This method is safe to traverse through its returned List, since it clones all the child features.
     *
     * @return A cloned list of the ROOT children.
     */
    public List<Feature> getRootFeatures() {
        if (!init) {
            return new ArrayList<>();
        }
        return cacheManager.getSyncFeatureList().getRootFeatures();
    }

    /**
     * Returns a cloned list of the entitlements.
     * This method is safe to traverse through its returned List.
     *
     * @return A cloned list of the entitlements.
     */
    public Map<String, Entitlement> getEntitlementsTree() {
        Map<String, Entitlement> result = new Hashtable();
        if (!init) {
            return result;
        }
        JSONObject rootEntitlements = cacheManager.getSyncedEntitlements();
        if (rootEntitlements != null && rootEntitlements.length() > 0) {
            JSONArray entitlements = rootEntitlements.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
            if (entitlements != null && entitlements.length() > 0) {
                for (int i = 0; i < entitlements.length(); i++) {
                    JSONObject entitlementObj = entitlements.getJSONObject(i);
                    Entitlement entitlement = new Entitlement(entitlementObj);
                    result.put(entitlement.getName(), entitlement);
                }
            }
        }
        return result;
    }


    @Override
    public Collection<Entitlement> getPurchasedEntitlements(Collection<String> productIds) {
        Collection<Entitlement> entitlements = getEntitlements();
        List<Entitlement> purchasedEntitlements = new ArrayList<>();
        for (Entitlement entitlement : entitlements) {
            isEntitlementPurchased(entitlement, productIds, purchasedEntitlements);
        }
        return purchasedEntitlements;
    }

    private void isEntitlementPurchased(Entitlement entitlement, Collection<String> productIds,
                                        Collection<Entitlement> purchasedEntitlements) {
        if (entitlement.getEntitlementChildren().size() > 0) {
            for (Entitlement child : entitlement.getEntitlementChildren()) {
                isEntitlementPurchased(child, productIds, purchasedEntitlements);
            }
        }
        for (PurchaseOption purchaseOption : entitlement.getPurchaseOptions()) {
            if (productIds.contains(purchaseOption.getProductId())) {
                purchasedEntitlements.add(entitlement);
                // add included entitlements
                for (String entitlementName : entitlement.getIncludedEntitlements()) {
                    purchasedEntitlements.add(getEntitlement(entitlementName));
                }
            }
        }
    }

    /**
     * Returns a cloned list of the entitlements.
     * This method is safe to traverse through its returned List.
     *
     * @return A cloned list of the entitlements.
     */
    public Collection<Entitlement> getEntitlements() {
        List<Entitlement> result = Collections.emptyList();
        if (!init) {
            return result;
        }
        JSONObject rootEntitlements = cacheManager.getSyncedEntitlements();
        if (rootEntitlements != null && rootEntitlements.length() > 0) {
            JSONArray entitlements = rootEntitlements.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
            if (entitlements != null && entitlements.length() > 0) {
                result = new ArrayList<>();
                for (int i = 0; i < entitlements.length(); i++) {
                    JSONObject entitlementObj = entitlements.getJSONObject(i);
                    result.add(new Entitlement(entitlementObj));
                }
            }
        }
        return result;
    }

    /**
     * Check if the device locale has been changed.
     * If it changed, clear all available runtime data and return the
     * application to the default state.
     */
    public void resetLocale() {
        cacheManager.resetLocale();
    }


    /**
     * Method nullifies the last pull timestamp to initial value.
     */
    public void resetLastPullTime() {
        PersistenceHandler sp = cacheManager.getPersistenceHandler();
        sp.write(Constants.SP_LAST_TRANS_FULL_DOWNLOAD_TIME, "");
        sp.write(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");
        sp.write(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED);
        sp.write(Constants.SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, "");
        sp.write(Constants.SP_LAST_TIME_PRODUCT_CHANGED, "");
    }


    /**
     * Returns the date and time of the last calculate.
     *
     * @return the date and time of the last calculate.
     */
    public Date getLastCalculateTime() {
        long lastTime = cacheManager.getPersistenceHandler().read(Constants.SP_LAST_CALCULATE_TIME, Constants.TIME_NOT_DEFINED);
        return new Date(lastTime);
    }

    /**
     * Returns the date and time when calculate results were synchronized with the current feature set.
     *
     * @return the date of the last sync time.
     */
    public Date getLastSyncTime() {
        long lastTime = cacheManager.getPersistenceHandler().read(Constants.SP_LAST_SYNC_TIME, Constants.TIME_NOT_DEFINED);
        return new Date(lastTime);
    }

    /**
     * Returns the date and time of the last successfully completed pull request from the server.
     *
     * @return the date and time
     */
    public Date getLastPullTime() {
        long lastTime = cacheManager.getPersistenceHandler().read(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED);
        return new Date(lastTime);
    }


    /**
     * @return default file the product was init with
     */
    public String getDefaultFile() {
        return this.cacheManager.getPersistenceHandler().read(Constants.SP_DEFAULT_FILE, "{}");
    }


    @CheckForNull
    public JSONArray getContextFieldsForAnalytics() {
        return cacheManager.getContextFieldsForAnalytics();
    }

    @CheckForNull
    public JSONObject getContextFieldsValuesForAnalytics(JSONObject contextObject, boolean doSetValueAsString) {
        JSONObject calculatedFeatures = new JSONObject();
        JSONArray contextFields = cacheManager.getContextFieldsForAnalytics();

        int contextFieldsLength = 0;
        //noinspection ConstantConditions
        if (contextFields != null) {
            contextFieldsLength = contextFields.length();
        }
        for (int i = 0; i < contextFieldsLength; i++) {
            String contextFieldName = null;
            try {
                contextFieldName = contextFields.get(i).toString();
            } catch (JSONException e) {
                Logger.log.e(TAG, e.getMessage());
            }
            if (contextFieldName != null) {
                Object contextFieldValue = getFieldValueFromJsonObject(contextObject, contextFieldName.split("\\."), doSetValueAsString);
                if (contextFieldValue != null) {
                    calculatedFeatures.put(contextFieldName, contextFieldValue);
                }
            }
        }
        return calculatedFeatures;
    }

    /**
     * Returns a translated UI string based on the current device locale. When the key (string ID) is not found, returns null.
     *
     * @param key  represents a string value
     * @param args a list of string arguments if it contains placeholders
     * @return translated UI string based on the current device locale
     */
    @CheckForNull
    public String getString(String key, String... args) {
        JSONObject translations = cacheManager.getPersistenceHandler().readJSON(Constants.SP_RAW_TRANSLATIONS);
        if (translations == null || translations.optJSONObject("strings") == null) {
            return null;
        }
        JSONObject translationsTable = translations.optJSONObject("strings");
        String translatedValue = translationsTable.optString(key);
        if (translatedValue == null) {
            return null;
        }

        for (int i = 0; i < args.length; i++) {
            translatedValue = translatedValue.replace("[[[" + (i + 1) + "]]]", args[i]);
        }

        return (this.isDoubleLengthStrings) ? translatedValue + " " + translatedValue : translatedValue;
    }

    public String getDevelopBranchName() {
        return cacheManager.getPersistenceHandler().getDevelopBranchName();
    }

    public boolean isAllowExperimentEvaluation() {
        return allowExperimentEvaluation;
    }

    public void setAllowExperimentEvaluation(boolean allowExperimentEvaluation) {
        this.allowExperimentEvaluation = allowExperimentEvaluation;
    }

    public boolean isCurrentServerDefault() {
        boolean isCurrent = true;
        if (cacheManager.getServers() != null) {

            Servers.Server current = cacheManager.getServers().getCurrentServer();
            Servers.Server defaultServer = cacheManager.getServers().getDefaultServer();
            if (current != null && current.getUrl() != null && defaultServer != null && defaultServer.getUrl() != null) {
                isCurrent = current.getUrl().equals(defaultServer.getUrl());
            }
        }
        return isCurrent;
    }

    @Override
    public abstract void reset(Context context, boolean simulateUninstall);

    @Override
    public void reset(Context context) {
        if (cacheManager.getServers() != null) {
            cacheManager.getServers().nullifyServerList();
        }
        this.reset(context, true);
    }


    @Override
    public Entitlement getEntitlement(@Nullable String entitlementName) {
        Entitlement entitlement = getEntitlementsTree().get(entitlementName == null ? "" : entitlementName);
        return entitlement == null ? new Entitlement() : entitlement;
    }


    @Override
    public void reInitSDK(Context appContext, int defaultFileId, String productVersion) throws AirlockInvalidFileException, IOException {
        reset(appContext, false);
        initSDK(appContext, defaultFileId, productVersion);
    }


    public void updateContext(String context, boolean clearPreviousContext) {
        if (cacheManager.getAirlockContextManager().getCurrentContext() != null) {
            cacheManager.getAirlockContextManager().getCurrentContext().update(new JSONObject(context), clearPreviousContext);
        }


        new Thread() {
            public void run() {
                cacheManager.getPersistenceHandler().write(Constants.SP_CURRENT_CONTEXT,
                        cacheManager.getAirlockContextManager().getCurrentContext().toString());
            }
        }.start();
    }

    public void removeContextField(String fieldPath) {
        if (cacheManager.getAirlockContextManager().getCurrentContext() != null) {
            cacheManager.getAirlockContextManager().getCurrentContext().removeContextField(fieldPath);
        }
    }

    public void setSharedContext(StateFullContext sharedContext) {
        cacheManager.setAirlockSharedContext(sharedContext);
    }

    public void setLocaleProvider(LocaleProvider localeProvider) {
        cacheManager.setLocaleProvider(localeProvider);
        cacheManager.getPersistenceHandler().write(Constants.SP_CURRENT_LOCALE, localeProvider.getLocale().toString());
    }

    public Locale getLocale() {
        return cacheManager.getLocale();
    }

    public Map<String, String> getAllStrings() {
        Map<String, String> allStrings = new Hashtable<>();
        JSONObject translations = cacheManager.getPersistenceHandler().readJSON(Constants.SP_RAW_TRANSLATIONS);
        if (translations == null || translations.optJSONObject("strings") == null) {
            return allStrings;
        }
        JSONObject translationsTable = translations.optJSONObject("strings");
        if (translationsTable == null) {
            return allStrings;
        }

        Iterator<String> keys = translationsTable.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = translationsTable.optString(key);
            if (value != null) {
                allStrings.put(key, value);
            }
        }
        return allStrings;
    }

    public void setDeviceInItemPercentageRange(PercentageManager.Sections section, String featureName, boolean inRange) {
        if (this.cacheManager.getPercentageManager() != null) {
            this.cacheManager.getPercentageManager().reInit();
            this.cacheManager.getPercentageManager().setDeviceInItemPercentageRange(section, featureName, inRange);
        }
    }

    public boolean isDeviceInItemPercentageRange(PercentageManager.Sections section, String featureName) {
        if (this.cacheManager.getPercentageManager() != null) {
            this.cacheManager.getPercentageManager().reInit();
            return this.cacheManager.getPercentageManager().isDeviceInItemPercentageRange(section, featureName);
        }
        return false;
    }

    public FeaturesList getSyncFeatureList() {
        return cacheManager.getSyncFeatureList();
    }

    public PersistenceHandler getPersistenceHandler() {
        return cacheManager.getPersistenceHandler();
    }

    public AirlockContextManager getAirlockContextManager() {
        return cacheManager.getAirlockContextManager();
    }

    public String getLastBranchName() {
        return cacheManager.getPersistenceHandler().getLastBranchName();
    }

    public String readAsStringByKey(String key, String defaultValue) {
        return cacheManager.getPersistenceHandler().read(key, defaultValue);
    }

    public String getAirlockUserUniqueId() throws AirlockNotInitializedException {
        if (cacheManager.getAirlockUserUniqueId() == null) {
            throw new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
        }
        return cacheManager.getAirlockUserUniqueId();
    }

    @Override
    public PurchasesManager getPurchasesManager() {
        return this.purchasesManager;
    }


    @TestOnly
    public void addPurchasedProductsId(String productId) {
        JSONArray productIdsArray = new JSONArray(cacheManager.getPersistenceHandler().
                read(Constants.PURCHASED_IDS_FOR_DEBUG, "[]"));

        if (productIdsArray.length() == 0) {
            productIdsArray.put(productId);
        } else {
            boolean found = false;
            for (int i = 0; i < productIdsArray.length(); i++) {
                if (productIdsArray.get(i).equals(productId)) {
                    found = true;
                }
            }
            if (!found) {
                productIdsArray.put(productId);
            }
        }
        cacheManager.getPersistenceHandler().write(Constants.PURCHASED_IDS_FOR_DEBUG, productIdsArray.toString());
    }

    @TestOnly
    public void removePurchasedProductId(String productId) {
        JSONArray productIdsArray = new JSONArray(cacheManager.getPersistenceHandler().
                read(Constants.PURCHASED_IDS_FOR_DEBUG, "[]"));
        JSONArray newProductIdsArray = new JSONArray();
        for (int i = 0; i < productIdsArray.length(); i++) {
            if (!productIdsArray.get(i).equals(productId)) {
                newProductIdsArray.put(productIdsArray.get(i));
            }
        }
        cacheManager.getPersistenceHandler().write(Constants.PURCHASED_IDS_FOR_DEBUG, newProductIdsArray.toString());
    }

    @TestOnly
    public void clearPurchasedProductId(String productId) {
        cacheManager.getPersistenceHandler().write(Constants.PURCHASED_IDS_FOR_DEBUG, new JSONArray().toString());
    }


    public Collection<String> getPurchasedProductIds() {
        JSONArray productIdsArray = new JSONArray(cacheManager.getPersistenceHandler().
                read(Constants.PURCHASED_IDS_FOR_DEBUG, "[]"));

        List<String> productIdsList = new ArrayList<>(productIdsArray.length());
        for (int i = 0; i < productIdsArray.length(); i++) {
            productIdsList.add(productIdsArray.getString(i));
        }
        return productIdsList;
    }

    @TestOnly
    public Collection<String> getPurchasedProductIdsForDebug() {
        return getPurchasedProductIds();
    }

    public String resetAirlockId(){
        String newUUID = UUID.randomUUID().toString();
        cacheManager.getPersistenceHandler().write(Constants.SP_AIRLOCK_USER_UNIQUE_ID, newUUID);
        return newUUID;
    }
}
