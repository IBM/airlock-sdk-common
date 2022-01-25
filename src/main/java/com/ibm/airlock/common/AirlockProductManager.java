package com.ibm.airlock.common;

import com.ibm.airlock.common.cache.CacheManager;
import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.PercentageManager;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.cache.RuntimeLoader;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.FeaturesList;
import com.ibm.airlock.common.data.Entitlement;
import com.ibm.airlock.common.engine.AirlockContextManager;
import com.ibm.airlock.common.engine.StateFullContext;
import com.ibm.airlock.common.inapp.PurchasesManager;
import com.ibm.airlock.common.net.AirlockDAO;
import com.ibm.airlock.common.notifications.NotificationsManager;
import com.ibm.airlock.common.streams.StreamsManager;
import com.ibm.airlock.common.util.LocaleProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;


/**
 * Created by Denis Voloshin on 05/11/2017.
 */

public interface AirlockProductManager {

    CacheManager getDebuggableCache();

    StreamsManager getStreamsManager();

    PurchasesManager getPurchasesManager();

    NotificationsManager getNotificationsManager();

    FeaturesList getSyncFeatureList();

    String readAsStringByKey(String key, String defaultValue);

    AirlockContextManager getAirlockContextManager();

    PersistenceHandler getPersistenceHandler();

    String getLastBranchName();


    /**
     * @return whether AirlockManager.getString methods should return the same string as a doubled value
     */
    boolean isDoubleLengthStrings();

    /**
     * Sets the whether SDK should be configured to return AirlockManager.getString as as a doubled value
     *
     * @param doubleLengthStrings
     */
    void setDoubleLengthStrings(boolean doubleLengthStrings);

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current airlock context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    void initSDK(Context appContext, int defaultFileId, String productVersion, Object...additionalParams) throws AirlockInvalidFileException, IOException;

    void initSDK(Context appContext, RuntimeLoader runtimeLoader, String encryptionKey) throws AirlockInvalidFileException, IOException;

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current airlock context.
     * @param defaultFile    Defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    void initSDK(Context appContext, String defaultFile, String productVersion) throws AirlockInvalidFileException, IOException;


    /**
     * Returns the airlock session id the airlock instance was initialized with
     * or null if {@link #initSDK(Context, int, String) initSDK} method hasn't called yet.
     */
    @CheckForNull
    String getSeasonId();

    /**
     * Returns the airlock version from the airlock instance was initialized with
     * or null if {@link #initSDK(Context, int, String) initSDK} method hasn't called yet.
     */
    @CheckForNull
    String getAirlockVersion();

    /**
     * Returns the airlock product id the airlock instance was initialized with or null
     * if {@link #initSDK(Context, int, String) initSDK} method hasn't called yet.
     */
    @CheckForNull
    String getProductId();

    /**
     * Returns the airlock ExperimentList
     */
    Map<String, String> getExperimentInfo();

    /**
     * Returns the data provider mode that the SDK is using.
     *
     * @return The data provider mode that the SDK is using.
     */
    AirlockDAO.DataProviderType getDataProviderType();

    /**
     * Sets the data provider mode that is configured for the SDK instance. Airlock must be initialized;
     * otherwise, the method has no effect.
     *
     * @param type The DataProviderType to be used.
     */
    void setDataProviderType(AirlockDAO.DataProviderType type);

    /**
     * Asynchronously downloads the current list of features from the server.
     *
     * @param callback Callback to be called when the function returns.
     * @throws AirlockNotInitializedException if the Airlock SDK has not been initialized.
     */
    void pullFeatures(final AirlockCallback callback) throws AirlockNotInitializedException;

    /**
     * Gets the raw list of features and their rules from server.
     */
    @CheckForNull
    JSONObject getFeaturesConfigurationFromServer();


    /**
     * Calculates the status of the features according to the pullFeatures results and return the Features as a tree.
     *
     * @param context     the airlock context provided by caller
     * @param purchasedProductIds the list of purchased product an user bought so far.
     * @throws AirlockNotInitializedException if the Airlock SDK has not been initialized
     * @throws JSONException                  if the pullFeature results, the userProfile or the deviceProfile is not in the correct JSON format.
     */
    void calculateFeatures(@Nullable JSONObject context, Collection<String> purchasedProductIds) throws AirlockNotInitializedException, JSONException;

    /**
     * Calculates the status of the features according to the pullFeatures results.
     * No feature status changes are exposed until the syncFeatures method is called.
     *
     * @param userProfile    the user profile
     * @param airlockContext the airlock runtime context
     * @throws AirlockNotInitializedException if the Airlock SDK has not been initialized
     * @throws JSONException                  if the pullFeature results, the userProfile or the deviceProfile is not in the correct JSON format.
     */
    void calculateFeatures(@Nullable JSONObject userProfile, @Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException;

    /**
     * Asynchronously returns the list of user groups defined on the Airlock server.
     *
     * @param callback Callback to be called when the function returns.
     */

    void getServerUserGroups(final AirlockCallback callback);

    void getProductBranches(final AirlockCallback callback);

    String getStreamsSummary();

    void persistStreams();

    JSONArray addStreamsEvent(JSONArray events, boolean processImmediately);

    JSONArray addStreamsEvent(JSONObject event);

    /**
     * Returns a list of user groups selected for the device.
     *
     * @return a list of user groups selected for the device.
     * @throws JSONException if the provided value breaks the JSON serialization process.
     */
    List<String> getDeviceUserGroups();

    /**
     * Specifies a list of user groups selected for the device.
     *
     * @param userGroups List of the selected user groups.
     * @throws JSONException if the provided value breaks the JSON serialization process.
     */
    void setDeviceUserGroups(@Nullable List<String> userGroups);

    /**
     * Synchronizes the latest refreshFeatures results with the current feature set.
     * Updates LastSyncTime.
     *
     * @throws AirlockNotInitializedException if the SDK has not been initialized.
     */
    void syncFeatures() throws AirlockNotInitializedException;


    /**
     * Returns the Entitlement object by its name.
     * If the Entitlement doesn't exist in the purchases set, getEntitlement returns a new Entitlement object
     * with the given name, isOn=false, and source=missing.
     *
     * @param entitlementName Entitlement name in the format namespace.name.
     * @return Returns the Entitlement object.
     */
    Entitlement getEntitlement(String entitlementName);



    /**
     * Returns the purchased entitlements list by product ids.
     * if a product id is associated with more than one entitlement
     * all entitlements will be returned if an entitlement includes another entitlements (a bundle)
     * all entitlements are considered as purchased
     *
     * @param productIds the list of product ids an Entitlement is associated with
     * @return Returns the list of Entitlement object.
     */

    Collection<Entitlement> getPurchasedEntitlements(Collection<String> productIds);


    /**
     * Returns the feature object by its name.
     * If the feature doesn't exist in the feature set, getFeature returns a new Feature object
     * with the given name, isOn=false, and source=missing.
     *
     * @param featureName Feature name in the format namespace.name.
     * @return Returns the feature object.
     */

    Feature getFeature(String featureName);

    /**
     * Returns a cloned list of the ROOT children features.
     * This method is safe to traverse through its returned List, since it clones all the child features.
     *
     * @return A cloned list of the ROOT children.
     */
    List<Feature> getRootFeatures();

    /**
     * Returns a cloned list of the entitlements.
     * This method is safe to traverse through its returned List
     *
     * @return A cloned list of the entitlements
     */
    Collection<Entitlement> getEntitlements();

    /**
     * Check if the device locale has been changed.
     * If it changed, clear all available runtime data and return the
     * application to the default state.
     */
    void resetLocale();

    /**
     * Method nullifies the last pull timestamp to initial value.
     */
    void resetLastPullTime();

    /**
     * Returns the date and time of the last calculate.
     *
     * @return the date and time of the last calculate.
     */
    Date getLastCalculateTime();

    /**
     * Returns the date and time when calculate results were synchronized with the current feature set.
     *
     * @return the date of the last sync time.
     */
    Date getLastSyncTime();

    /**
     * Returns the date and time of the last successfully completed pull request from the server.
     *
     * @return the date and time
     */
    Date getLastPullTime();

    @CheckForNull
    JSONArray getContextFieldsForAnalytics();

    String getDefaultFile();

    @CheckForNull
    JSONObject getContextFieldsValuesForAnalytics(JSONObject contextObject, boolean returnValueAsString);

    /**
     * Returns a translated UI string based on the current device locale. When the key (string ID) is not found, returns null.
     *
     * @param key  represents a string value
     * @param args a list of string arguments if it contains placeholders
     * @return translated UI string based on the current device locale
     */
    @CheckForNull
    String getString(String key, String... args);

    String getDevelopBranchName();

    String getAppVersion();

    boolean isAllowExperimentEvaluation();

    void setAllowExperimentEvaluation(boolean allowExperimentEvaluation);

    boolean isCurrentServerDefault();

    void reset(Context context, boolean simulateUninstall);

    void reset(Context context);

    void reInitSDK(Context appContext, int defaultFileId, String productVersion) throws AirlockInvalidFileException, IOException;

    void updateProductContext(String context);

    void updateProductContext(String context, boolean clearPreviousContext);

    void removeProductContextField(String fieldPath);

    void setSharedContext(StateFullContext stateFullContext);

    void setLocaleProvider(LocaleProvider localeProvider);

    Locale getLocale();

    Map<String, String> getAllStrings();

    void setDeviceInItemPercentageRange(PercentageManager.Sections section, String featureName, boolean inRange);

    boolean isDeviceInItemPercentageRange(PercentageManager.Sections section, String featureName);

    String getAirlockUserUniqueId() throws AirlockNotInitializedException;

    void addPurchasedProductsId(String productId);

    void removePurchasedProductId(String productId);

    void clearPurchasedProductId(String productId);

    Collection<String> getPurchasedProductIds();

    String resetAirlockId();
}

