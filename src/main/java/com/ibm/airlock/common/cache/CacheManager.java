package com.ibm.airlock.common.cache;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockException;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockMismatchSeasonException;
import com.ibm.airlock.common.data.CalculateErrorItem;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.FeaturesList;
import com.ibm.airlock.common.data.Servers;
import com.ibm.airlock.common.engine.AirlockContextManager;
import com.ibm.airlock.common.engine.AirlockEnginePerformanceMetric;
import com.ibm.airlock.common.engine.ExperimentsCalculator;
import com.ibm.airlock.common.engine.FeaturesBranchMerger;
import com.ibm.airlock.common.engine.FeaturesCalculator;
import com.ibm.airlock.common.engine.Result;
import com.ibm.airlock.common.engine.ScriptExecutionException;
import com.ibm.airlock.common.engine.ScriptInitException;
import com.ibm.airlock.common.engine.ScriptInvoker;
import com.ibm.airlock.common.engine.StateFullContext;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.AirlockDAO;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.notifications.NotificationsManager;
import com.ibm.airlock.common.streams.StreamsManager;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.AirlockVersionComparator;
import com.ibm.airlock.common.util.BaseRawFeaturesJsonParser;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.DefaultFileParser;
import com.ibm.airlock.common.util.LocaleProvider;
import com.ibm.airlock.common.util.RandomUtils;
import com.sangupta.murmur.Murmur2;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.ibm.airlock.common.util.Constants.JSON_AIRLYTICS_SHARD;

/**
 * This class will generate the feature map and manage it.
 *
 * @author Rachel Levy
 */

public class CacheManager {

    private final static String TAG = "airlock.CacheManager";
    private final static String TAG_PERFORMANCE = "airlock.CacheMgr.perf";
    private static final String RAW_RULES_TEMP = "airlock.raw_rules.temp";
    private static final String RAW_RULES_TEMP_JSON_OBJ = "airlock.raw_rules.temp.json.obj";
    private static final String RAW_STREAMS_TEMP_JSON_OBJ = "airlock.raw_streams.temp.json.obj";
    private static final String RAW_NOTIFICATIONS_TEMP_JSON_OBJ = "airlock.raw_notifications.temp.json.obj";

    private static final String RAW_TRANSLATIONS_TEMP = "airlock.raw_translations.temp";
    private static final String JSON_JS_FUNCTIONS_FIELD_NAME_TEMP = "airlock.raw_js.utils.temp";

    public static final String STREAMS_TEMP = "airlock.streams.temp";
    public static final String SYNC_FEATURE_MAP = "sync.features.map";
    public static final String SYNC_ENTITLEMENTS_JSON = "sync.entitlements.map";
    public static final String PRE_SYNC_ENTITLEMENTS_JSON = "presync.entitlements.map";
    public static final String PRE_SYNC_FEATURE_MAP = "presync.features.map";
    public static final long FEATURE_MAP_TIME_TO_LIVE = 10 * 60 * 1000; // 10 minutes
    public static final String STREAMS_JS_UTILS_TEMP = "airlock.streams.js.utils.temp";

    private static final String NOTIFICATIONS_TEMP = "airlock.notifications.temp";

    private static final String SP_LAST_TRANS_FULL_DOWNLOAD_TIME_TEMP = "airlock.translation.timestamp.temp";
    private static final String SP_LAST_FEATURES_FULL_DOWNLOAD_TIME_TEMP = "airlock.config.timestamp.temp";
    private static final String SP_LAST_JS_UTILS_DOWNLOAD_TIME_TEMP = "airlock.js.utils.timestamp.temp";
    private static final String SP_LAST_STREAMS_FULL_DOWNLOAD_TIME_TEMP = "airlock.streams.timestamp.temp";
    private static final String SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME_TEMP = "airlock.streams.js.utils.timestamp.temp";
    private static final String SP_LAST_NOTIFICATIONS_FULL_DOWNLOAD_TIME_TEMP = "airlock.notifications.timestamp.temp";

    //Map that we be updated on refresh and merged to featureMap ONLY after sync
    protected final InMemoryCache preSyncServerFeatureList = new InMemoryCache();

    protected final InMemoryCache syncFeatureList = new InMemoryCache();

    protected final InMemoryCache syncedEntitlementsJSON = new InMemoryCache();

    protected final InMemoryCache preSyncedEntitlementsJSON = new InMemoryCache();

    private final Hashtable<String, String> tempResultsHolder = new Hashtable<>();

    private final Hashtable<String, JSONObject> tempJSONResultsHolder = new Hashtable<>();

    private final Map<String, CalculateErrorItem> lastJSCalculateErrors = new HashMap<>();

    private Servers serversList;

    private static long featuresMapTimeToLive = FEATURE_MAP_TIME_TO_LIVE;

    private String productVersion;

    protected PersistenceHandler persistenceHandler;

    private StreamsManager streamsManager;

    private NotificationsManager notificationsManager;

    private boolean sharedPreferenceHandlerInitialized;
    @Nullable
    private ConnectionManager connectionManager;
    @Nullable
    protected PercentageManager percentageManager;

    private LocaleProvider localeProvider = new LocaleProvider() {
        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }
    };
    @Nullable
    private AirlockContextManager airlockContextManager;
    @Nullable
    private StateFullContext airlockSharedContext;

    // we use this cache for the fast access to the strings table from stateless calculation
    private InMemoryCache translatedStringsCache;
    private String defaultFile;
    private Context context;

    public PersistenceHandler getPersistenceHandler() {
        return persistenceHandler;
    }

    public static void setFeaturesMapTimeToLive(long timeToLive) {
        featuresMapTimeToLive = timeToLive;
    }

    public static long getFeaturesMapTimeToLive() {
        return featuresMapTimeToLive;
    }

    public void setPersistenceHandler(PersistenceHandler ph) {
        this.persistenceHandler = ph;
    }

    private void printPerformanceLog(String message, long observePoint) {
        Logger.log.d(TAG_PERFORMANCE, message + " : " + (System.currentTimeMillis() - observePoint) + " "
                + "ms");
    }

    public Locale getLocale() {
        return localeProvider.getLocale();
    }

    public void setLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    public String getProductVersion() {
        return productVersion;
    }

    @Deprecated
    public Servers getServers() {
        return serversList;
    }

    public CacheManager() {
        this.syncFeatureList.put(SYNC_FEATURE_MAP, new FeaturesList(), getFeaturesMapTimeToLive());
        this.preSyncServerFeatureList.put(PRE_SYNC_FEATURE_MAP, new FeaturesList(), getFeaturesMapTimeToLive());
        this.syncedEntitlementsJSON.put(SYNC_ENTITLEMENTS_JSON, new FeaturesList(), getFeaturesMapTimeToLive());
        this.preSyncedEntitlementsJSON.put(SYNC_ENTITLEMENTS_JSON, new FeaturesList(), getFeaturesMapTimeToLive());
    }

    @SuppressWarnings("NewApi")
    public void init(String productName, Context context, String defaultFile,
                     String version, PersistenceHandler sp, final StreamsManager streamsManager,
                     NotificationsManager notificationsManager,
                     ConnectionManager connectionManager) throws AirlockInvalidFileException {
        long startInit = System.currentTimeMillis();
        this.persistenceHandler = sp;
        this.defaultFile = defaultFile;
        this.context = context;
        this.airlockContextManager = new AirlockContextManager(productName);
        this.translatedStringsCache = new InMemoryCache();
        productVersion = version;
        this.connectionManager = connectionManager;
        this.notificationsManager = notificationsManager;
        this.streamsManager = streamsManager;
        printPerformanceLog("CacheManager.init time:", startInit);
        Future<Void> future = (Future<Void>) sp.getUploadStatus();
        if (future != null && !future.isDone()) {
            try {
                onSpInitCompletion(future.get());
                return;
            } catch (Exception ignore) {
            }
        } else {
            onSpInitCompletion(null);
        }
    }

    private <Void> Void onSpInitCompletion(Void unused) throws AirlockInvalidFileException {
        this.airlockContextManager.getCurrentContext().
                update(this.persistenceHandler.readJSON(Constants.SP_CURRENT_CONTEXT));
        streamsManager.updateStreams();
        sharedPreferenceHandlerInitialized = this.persistenceHandler.isInitialized();
        resetLocale();
        if (defaultFile != null) {
            this.persistenceHandler.write(Constants.SP_DEFAULT_FILE, defaultFile);
        }
        String previousVersion = "";
        if (sharedPreferenceHandlerInitialized) {
            previousVersion = this.persistenceHandler.read(Constants.SP_PRODUCT_VERSION, "");
        }
        setUserRandomNumber();
        setAirlockUserUniqueId();
        // new version, need to parse ALL the default file, and reset persistenceHandler cached relevant to refresh.
        AirlockVersionComparator comparator = new AirlockVersionComparator();

        if (previousVersion.equalsIgnoreCase("") || !comparator.equals(productVersion, previousVersion)) {
            //reset the product's id on the app upgrade
            this.persistenceHandler.write(Constants.SP_DEFAULT_PRODUCT_ID, "");
            this.persistenceHandler.write(Constants.SP_CURRENT_PRODUCT_ID, "");

            resetSPOnNewSeasonId();
            if (defaultFile != null) {
                parseDefaultFile(defaultFile, false);
            }
            this.persistenceHandler.write(Constants.SP_PRODUCT_VERSION, productVersion);
        } else {
            // look for  default that we downloaded previously.
            String updatedDefaultFile = this.persistenceHandler.read(Constants.SP_UPDATED_DEFAULT_FILE, "");
            // we didn't download default file, use the one we received as parameter
            if (updatedDefaultFile.equals("")) {
                updatedDefaultFile = defaultFile;
            }
            parseDefaultFile(updatedDefaultFile, true);
            FeaturesList cachedFeatures = new FeaturesList();
            cachedFeatures.putAll(this.persistenceHandler.getCachedFeatureMap());
            getSyncedFeaturedList().merge(cachedFeatures);

            // set the current cached entitlement to override the default if it exists
            JSONObject syncedEntitlements = this.persistenceHandler.readJSON(Constants.SP_SYNCED_ENTITLEMENTS_LIST);
            if (!syncedEntitlements.toString().equals(new JSONObject().toString())) {
                putSyncedEntitlements(syncedEntitlements);
            }
        }
        // if Current product id == null - put the default product id as current.
        if (sharedPreferenceHandlerInitialized && this.persistenceHandler.read(Constants.SP_CURRENT_PRODUCT_ID, "").equals("")) {
            this.persistenceHandler.write(Constants.SP_CURRENT_PRODUCT_ID, this.persistenceHandler.read(Constants.SP_DEFAULT_PRODUCT_ID, ""));
        }

        //read only if there is shared preferenceHandler is initialized
        if (sharedPreferenceHandlerInitialized) {
            readUserGroupsFromDevice(this.persistenceHandler, this.context);
        }

        this.serversList = new Servers(this.persistenceHandler);

        if (sharedPreferenceHandlerInitialized) {
            // If not new app - could have dev mode branch set- download the branch json file. (to reflect updates of branch)
            String branchId = this.persistenceHandler.getDevelopBranchId();
            if (!branchId.isEmpty()) {
                setBranch(branchId);
            }
        }

        this.notificationsManager = notificationsManager;
        this.streamsManager = streamsManager;
        this.percentageManager = new PercentageManager(this);
        sharedPreferenceHandlerInitialized = true;
        return unused;
    }

    @CheckForNull
    public PercentageManager getPercentageManager() {
        return percentageManager;
    }

    public StreamsManager getStreamsManager() {
        return streamsManager;
    }

    @CheckForNull
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(@Nullable ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Returns the airlock session id the from the user preferences
     */
    public String getSeasionId() {
        return persistenceHandler.read(Constants.SP_SEASON_ID, "");
    }


    /**
     * Returns the airlock selected product id the from the user preferences
     */
    @CheckForNull
    public String getProductId() {
        return persistenceHandler.read(Constants.SP_CURRENT_PRODUCT_ID, "");
    }

    /**
     * Returns the airlock experiment inof for analytics
     */
    public Map<String, String> getExperimentInfo() {
        HashMap<String, String> experimentInfo = new HashMap<>();

        String experimentInfOString = persistenceHandler.read(Constants.SP_EXPERIMENT_INFO, "{}");
        JSONObject previousExperimentInfo = null;
        try {
            previousExperimentInfo = new JSONObject(experimentInfOString);
        } catch (JSONException e) {
            previousExperimentInfo = new JSONObject();
        }

        String previousVariantId = previousExperimentInfo.optString(Constants.JSON_FIELD_VARIANT);
        String previousExperimentId = previousExperimentInfo.optString(Constants.JSON_FIELD_EXPERIMENT);
        String previousVariantDateJoined = previousExperimentInfo.optString(Constants.JSON_FIELD_VARIANT_DATE_JOINED);

        experimentInfo.put(Constants.JSON_FIELD_VARIANT, previousVariantId);
        experimentInfo.put(Constants.JSON_FIELD_EXPERIMENT, previousExperimentId);
        experimentInfo.put(Constants.JSON_FIELD_VARIANT_DATE_JOINED, previousVariantDateJoined);
        return experimentInfo;
    }


    /**
     * Returns the airlock version from the user preferences
     */
    public String getAirlockVersion() {
        return persistenceHandler.read(Constants.SP_AIRLOCK_VERSION, "");
    }


    /**
     * Reads the stored user groups list from the device.
     *
     * @param ph
     * @param context
     */
    public void readUserGroupsFromDevice(PersistenceHandler ph, Context context) {
        // stub
    }

    public String getAirlockUserUniqueId() {
        return persistenceHandler.read(Constants.SP_AIRLOCK_USER_UNIQUE_ID, null);
    }


    private void setAirlockUserUniqueId() {
        if (persistenceHandler.read(Constants.SP_AIRLOCK_USER_UNIQUE_ID, null) == null) {
            persistenceHandler.write(Constants.SP_AIRLOCK_USER_UNIQUE_ID, UUID.randomUUID().toString());
        }
    }


    private void setUserRandomNumber() {
        if (persistenceHandler.read(Constants.SP_USER_RANDOM_NUMBER, -1) == -1) {
            Random randomGenerator = new Random();
            persistenceHandler.write(Constants.SP_USER_RANDOM_NUMBER, randomGenerator.nextInt(100));
        }
    }


    private JSONObject getPreSyncedEntitlementsJSON() {
        synchronized (this.preSyncedEntitlementsJSON) {
            JSONObject preSyncedEntitlementsJSON = (JSONObject) this.preSyncedEntitlementsJSON.get(PRE_SYNC_ENTITLEMENTS_JSON);
            if (preSyncedEntitlementsJSON == null) {
                preSyncedEntitlementsJSON = persistenceHandler.readJSON(Constants.SP_PRE_SYNCED_ENTITLEMENTS_LIST);
                this.syncedEntitlementsJSON.put(PRE_SYNC_ENTITLEMENTS_JSON, preSyncedEntitlementsJSON, getFeaturesMapTimeToLive());
            }
            return preSyncedEntitlementsJSON;
        }
    }


    private JSONObject getSyncedEntitlementsJSON() {
        synchronized (this.syncedEntitlementsJSON) {
            JSONObject syncedEntitlementsJSON = (JSONObject) this.syncedEntitlementsJSON.get(SYNC_ENTITLEMENTS_JSON);
            if (syncedEntitlementsJSON == null) {
                syncedEntitlementsJSON = persistenceHandler.readJSON(Constants.SP_SYNCED_ENTITLEMENTS_LIST);
                this.syncedEntitlementsJSON.put(SYNC_ENTITLEMENTS_JSON, syncedEntitlementsJSON, getFeaturesMapTimeToLive());
            }
            return syncedEntitlementsJSON;
        }
    }

    private FeaturesList<Feature> getPreSyncedFeaturedList() {
        synchronized (this.preSyncServerFeatureList) {
            FeaturesList featuresList = (FeaturesList) this.preSyncServerFeatureList.get(PRE_SYNC_FEATURE_MAP);
            if (featuresList == null) {
                featuresList = persistenceHandler.getCachedPreSyncedFeaturesMap();
                this.preSyncServerFeatureList.put(PRE_SYNC_FEATURE_MAP, featuresList, getFeaturesMapTimeToLive());
            }
            return featuresList;
        }
    }

    private FeaturesList<Feature> getSyncedFeaturedList() {
        synchronized (this.syncFeatureList) {
            FeaturesList featuresList = (FeaturesList) this.syncFeatureList.get(SYNC_FEATURE_MAP);
            if (featuresList == null) {
                featuresList = persistenceHandler.getCachedSyncedFeaturesMap();
                this.syncFeatureList.put(SYNC_FEATURE_MAP, featuresList, getFeaturesMapTimeToLive());
            }
            return featuresList;
        }
    }

    @CheckForNull
    public Feature getFeature(String key) {
        return getSyncedFeaturedList().getFeature(key);
    }

    public void syncFeatures() {
        if (getPreSyncedFeaturedList() != null && getPreSyncedFeaturedList().size() > 0) {
            //set all server features to cache, so if it'll not return from server, it'll be source = cache
            updateFeatureListSource(getSyncedFeaturedList(), Feature.Source.SERVER, Feature.Source.CACHE);
            getSyncedFeaturedList().merge(getPreSyncedFeaturedList(), (getDeviceUserGroups() != null && getDeviceUserGroups().size() > 0));
            persistenceHandler.setSyncedFeaturesMap(getSyncFeatureList());
            persistenceHandler.write(Constants.SP_LAST_SYNC_TIME, System.currentTimeMillis());
        }

        if (getPreSyncedEntitlementsJSON() != null) {
            putSyncedEntitlements(getPreSyncedEntitlementsJSON());
            persistenceHandler.write(Constants.SP_SYNCED_ENTITLEMENTS_LIST, getPreSyncedEntitlementsJSON());
            persistenceHandler.write(Constants.SP_LAST_SYNC_TIME, System.currentTimeMillis());
        }
    }

    /**
     * Returns all selected user groups for this device
     *
     * @return array of strings
     */
    private List<String> getDeviceUserGroups() {
        return persistenceHandler.getDeviceUserGroups();
    }


    private void doPullJSUtils(final AirlockCallback callback) {
        try {
            final long doPullJSUtilsStart = System.currentTimeMillis();
            AirlockDAO.pullJSUtils(this, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    printPerformanceLog("Total time doPullJSUtils :", doPullJSUtilsStart);
                    final long doPullJSUtilsProcess = System.currentTimeMillis();
                    //the resource hasn't been updated since the last fetching, do nothing.
                    if (response.code() == 304) {
                        response.body().close();
                        callback.onSuccess("");
                        printPerformanceLog("Total time process doPullJSUtils:", doPullJSUtilsProcess);
                        return;
                    }
                    if (response.code() != 200) {
                        response.body().close();
                        callback.onFailure(new AirlockException(String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code())));
                        return;
                    }

                    String responseBody = response.body().string();
                    response.body().close();
                    if (responseBody.equals("")) {
                        callback.onFailure(new AirlockException(AirlockMessages.ERROR_RESPONSE_BODY_IS_EMPTY));
                        return;
                    }

                    //update last time translation json was successfully downloaded.
                    if (response.header("Last-Modified") != null) {
                        tempResultsHolder.put(SP_LAST_JS_UTILS_DOWNLOAD_TIME_TEMP, response.header("Last-Modified"));
                    }

                    tempResultsHolder.put(JSON_JS_FUNCTIONS_FIELD_NAME_TEMP, responseBody);
                    callback.onSuccess("");
                    printPerformanceLog("Total time process doPullJSUtils:", doPullJSUtilsProcess);
                }
            });
        } catch (RuntimeException e) {
            callback.onFailure(e);
        }
    }


    @CheckForNull
    private String pullTranslationSynchronously(String locale) {
        Set<String> supportedLangSet = getSupportedLanguages();


        //check locale formant has to be xx_XX if not return null
        if (locale.split("_").length < 2) {
            return null;
        }

        String language = null;
        String country = "";


        //looking for whether system locale is in the supported list by language_country
        if (supportedLangSet.contains(language + "_" + country)) {
            language = locale.split("_")[0];
            country = locale.split("_")[1];
        }

        //looking for whether system language is in the supported list by language
        if (supportedLangSet.contains(localeProvider.getLocale().getLanguage())) {
            language = locale.split("_")[0];
        }

        //language is not in supported list
        if (language == null) {
            return null;
        }

        return AirlockDAO.pullTranslationsTable(this, language, country);
    }


    private void doPullTranslation(final AirlockCallback callback) {
        try {
            final long doPullTranslationStart = System.currentTimeMillis();
            String language = getDefaultLanguage();
            String country = "";
            Set<String> supportedLangSet = getSupportedLanguages();
            String localeProviderLanguage = localeProvider.getLocale().getLanguage();

            //looking for whether system locale is in the supported list (language + country)
            if (supportedLangSet.contains(localeProvider.getLocale().getLanguage() + "_" + localeProvider.getLocale().getCountry())) {
                language = localeProvider.getLocale().getLanguage();
                country = localeProvider.getLocale().getCountry();
            }

            // bug ANDFLAG-2967 looking for whether system language is in the supported list base only on language
            // if language is not found use the first appearance of language code and country code
            // if not found as well use en
            if (supportedLangSet.contains(localeProviderLanguage)) {
                language = localeProviderLanguage;
            } else if (supportedLangSet.contains(localeProviderLanguage + "_" + getSupportedCountryByLanguage(localeProviderLanguage))) {
                language = localeProviderLanguage;
                country = getSupportedCountryByLanguage(language);
            } else {
                language = "en";
            }


            AirlockDAO.pullTranslationsTable(this, language, country.equals("") ? "" : "_" + country, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    printPerformanceLog("Total time doPullTranslation:", doPullTranslationStart);
                    final long doPullTranslationProcess = System.currentTimeMillis();
                    //the resource hasn't been updated since the last fetching, do nothing.
                    Logger.log.d(TAG, String.format(AirlockMessages.LOG_PULL_TRANSLATION_RESPONSE_CODE_FORMATTED, response.code()));
                    if (response.code() == 304) {
                        response.body().close();
                        callback.onSuccess("");
                        printPerformanceLog("Total time  process doPullTranslation:", doPullTranslationProcess);
                        return;
                    }
                    if (response.code() != 200) {
                        response.body().close();
                        callback.onFailure(new AirlockException(String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code())));
                        return;
                    }

                    String responseBody = response.body().string();
                    response.body().close();
                    if (responseBody.equals("")) {
                        callback.onFailure(new AirlockException(AirlockMessages.ERROR_RESPONSE_BODY_IS_EMPTY));
                        return;
                    }

                    if (response.header("Last-Modified") != null) {
                        tempResultsHolder.put(SP_LAST_TRANS_FULL_DOWNLOAD_TIME_TEMP, response.header("Last-Modified"));
                    }

                    tempResultsHolder.put(RAW_TRANSLATIONS_TEMP, responseBody);
                    callback.onSuccess("");
                    printPerformanceLog("Total time  process doPullTranslation:", doPullTranslationProcess);
                }
            });
        } catch (RuntimeException e) {
            callback.onFailure(e);
        }
    }

    public void pullStreams(final AirlockCallback callback) {
        AirlockDAO.pullStreams(this, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 304) {
                    response.body().close();
                    callback.onSuccess("");
                    response.close();
                    return;
                }
                if (response.code() != 200) {
                    response.body().close();
                    String msg = String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code());
                    Logger.log.e(TAG, msg);
                    AirlockException exception = new AirlockException(String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code()), response.code());
                    callback.onFailure(exception);
                    response.close();
                    return;
                }

                String responseBody = response.body().string();
                response.body().close();
                JSONObject jsonResult = null;
                try {
                    jsonResult = new JSONObject(responseBody);
                } catch (JSONException e) {
                    //do nothing
                }

                tempJSONResultsHolder.put(RAW_STREAMS_TEMP_JSON_OBJ, (jsonResult == null ? new JSONObject() : jsonResult));

                //update last time translation json was successfully downloaded.
                if (response.header("Last-Modified") != null) {
                    tempResultsHolder.put(SP_LAST_STREAMS_FULL_DOWNLOAD_TIME_TEMP, response.header("Last-Modified"));
                }
                tempResultsHolder.put(STREAMS_TEMP, responseBody);

                callback.onSuccess("");
            }
        });
    }

    public void pullFeatureUsageUtils(final AirlockCallback callback) {
        AirlockDAO.pullFeatureUsageUIUtils(this, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 304) {
                    callback.onSuccess("");
                    return;
                }
                if (response.code() != 200) {
                    response.body().close();
                    String msg = String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code());
                    Logger.log.e(TAG, msg);
                    callback.onFailure(new AirlockException(String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code()), response.code()));
                    return;
                }

                String responseBody = response.body().string();
                response.body().close();

                //update last time translation json was successfully downloaded.
                if (response.header("Last-Modified") != null) {
                    tempResultsHolder.put(SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME_TEMP, response.header("Last-Modified"));
                }
                tempResultsHolder.put(STREAMS_JS_UTILS_TEMP, responseBody);

//                PersistenceHandler.getInstance().write(Constants.SP_FEATURE_UTILS_STREAMS,responseBody);
                callback.onSuccess("");
            }
        });
    }

    public void pullNotifications(final AirlockCallback callback) {
        AirlockDAO.pullNotifications(this, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 304) {
                    callback.onSuccess("");
                    return;
                }
                if (response.code() != 200) {
                    response.body().close();
                    String msg = String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code());
                    Logger.log.e(TAG, msg);
                    AirlockException exception = new AirlockException(String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code()), response.code());
                    callback.onFailure(exception);
                    return;
                }

                String responseBody = response.body().string();
                response.body().close();
                JSONObject jsonResult = null;
                try {
                    jsonResult = new JSONObject(responseBody);
                } catch (JSONException e) {
                    //do nothing
                }

                tempJSONResultsHolder.put(RAW_NOTIFICATIONS_TEMP_JSON_OBJ, (jsonResult == null ? new JSONObject() : jsonResult));

                //update last time translation json was successfully downloaded.
                if (response.header("Last-Modified") != null) {
                    tempResultsHolder.put(SP_LAST_NOTIFICATIONS_FULL_DOWNLOAD_TIME_TEMP, response.header("Last-Modified"));
                }
                tempResultsHolder.put(NOTIFICATIONS_TEMP, responseBody);

                callback.onSuccess("");
            }
        });
    }


    public void pullServerList(final AirlockCallback airlockCallback) {
        AirlockDAO.pullServerList(this, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.log.e(TAG, AirlockMessages.LOG_FAILED_TO_PULL_SERVER_LIST, e);
                airlockCallback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() != 200) {
                    response.body().close();
                    String msg = String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code());
                    Logger.log.e(TAG, msg);
                    airlockCallback.onFailure(new AirlockException(msg));
                    return;
                }

                String responseBody = response.body().string();
                response.body().close();
                if (responseBody.equals("")) {
                    Logger.log.e(TAG, AirlockMessages.ERROR_RESPONSE_BODY_IS_EMPTY);
                    airlockCallback.onFailure(new AirlockException(AirlockMessages.ERROR_RESPONSE_BODY_IS_EMPTY));
                    return;
                }
                try {
                    serversList.updateServersList(new JSONObject(responseBody));
                } catch (JSONException e) {
                    Logger.log.e(TAG, AirlockMessages.LOG_FAILED_TO_PULL_SERVER_LIST + e.getMessage());
                    airlockCallback.onFailure(e);
                    return;
                }
                airlockCallback.onSuccess("");
            }
        });
    }

    public void pullDefaultFile(final Servers.Server server, final Servers.Product product, final AirlockCallback airlockCallback) {
        AirlockDAO.pullDefaultFile(this, server, product, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.log.e(TAG, AirlockMessages.LOG_FAILED_TO_PULL_DEFAULT_FILE, e);
                airlockCallback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() != 200) {
                    response.body().close();
                    String msg = String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code());
                    Logger.log.e(TAG, msg);
                    airlockCallback.onFailure(new AirlockException(msg));
                    return;
                }

                final String responseBody = response.body().string();
                response.body().close();
                if (responseBody.equals("")) {
                    Logger.log.e(TAG, AirlockMessages.ERROR_RESPONSE_BODY_IS_EMPTY);
                    airlockCallback.onFailure(new AirlockException(AirlockMessages.ERROR_RESPONSE_BODY_IS_EMPTY));
                    return;
                }

                JSONObject defaultJsonObject;
                try {
                    defaultJsonObject = new JSONObject(responseBody);
                    String newDefaultVersion = defaultJsonObject.optString(Constants.JSON_DEFAULT_VERSION);
                    String serverUrl = defaultJsonObject.optString(Constants.JSON_FEATURE_FIELD_CACHED_S3PATH);
                    if (!newDefaultVersion.equals(Constants.DEFAULT_FILE_VERSION_v2_5) && !newDefaultVersion.equals(Constants.DEFAULT_FILE_VERSION_v3_0)) {
                        String error = "The airlock product couldn't be selected the default file version [" + newDefaultVersion + "] is not compatible with the "
                                + "app "
                                + "airlock default "
                                + "versions [" + Constants.DEFAULT_FILE_VERSION_v2_5 + "," +
                                Constants.DEFAULT_FILE_VERSION_v3_0 + "]";

                        Logger.log.e(TAG, error);
                        airlockCallback.onFailure(new Exception(error));
                        return;
                    }

                    serverUrl = !serverUrl.endsWith("/") ? serverUrl + "/" : serverUrl;

                    AirlockDAO.checkIfProductChanged(CacheManager.this, serverUrl, product.getProductId(), product.getSeasonId(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            String error = AirlockMessages.LOG_PRODUCT_DOESNT_SUPPORT_MULTI_SERVERS;
                            Logger.log.e(TAG, error);
                            airlockCallback.onFailure(new Exception(error));
                            return;
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try {
                                parseDefaultFile(responseBody, true);
                                response.body().close();
                            } catch (AirlockInvalidFileException e) {
                                response.body().close();
                                Logger.log.e(TAG, AirlockMessages.LOG_FAILED_TO_PULL_DEFAULT_FILE + e.getMessage());
                                airlockCallback.onFailure(e);
                                return;
                            }

                            // save default, product and server to SP
                            saveSelectedProduct(server, product, responseBody);
                            airlockCallback.onSuccess("");
                        }
                    });
                } catch (JSONException e) {
                    Logger.log.e(TAG, AirlockMessages.LOG_FAILED_TO_PULL_DEFAULT_FILE + e.getMessage());
                    airlockCallback.onFailure(e);
                    return;
                }
            }
        });
    }

    private void saveSelectedProduct(Servers.Server server, Servers.Product product, String responseBody) {
        serversList.setCurrentServer(server);
        persistenceHandler.write(Constants.SP_CURRENT_PRODUCT_ID, product.getProductId());
        persistenceHandler.write(Constants.SP_SEASON_ID, product.getSeasonId());
        persistenceHandler.write(Constants.SP_UPDATED_DEFAULT_FILE, responseBody);
    }

    public void setBranch(String branchId) {
        AirlockDAO.pullBranchById(this, branchId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do nothing - stay with old copy
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //read the response to the string
                if (response.body() == null || response.body().toString().isEmpty() || !response.isSuccessful()) {

                    if (response.body() != null) {
                        response.body().close();
                    }

                    // Do nothing - stay with old copy
                    return;
                }

                //parse server response,the response has to be in json format
                try {
                    final JSONObject branchesFullResponse = new JSONObject(response.body().string());
                    response.body().close();
                    persistenceHandler.setDevelopBranch(branchesFullResponse.toString());
                } catch (JSONException e) {
                    // Do nothing - stay with old copy
                }
            }
        });
    }

    private void persistTempResult(String persistAttrName, @Nullable String value) {
        if (tempResultsHolder.get(value) != null) {
            persistenceHandler.write(persistAttrName, tempResultsHolder.get(value));
        }
    }

    //executes pull features and translation json on parallel and wait for their completion
    private void pullFeaturesAndTranslationTable(final AirlockCallback callback) {

        final boolean isNotificationsEnabled = notificationsManager.isSupported();

        int numOfPulls = 5;
        if (isNotificationsEnabled) {
            numOfPulls = 6;
        }

        final int latchCounter = numOfPulls;
        //semaphore is used to wait for two parallel downloading results
        final CountDownLatch latch = new CountDownLatch(latchCounter);

        //thread to conditional wait with timeout of 10 seconds
        //for all downloading be complete, only then the top callback success method
        //will be called.
        final PullingController downloadController = new PullingController() {
            public void run() {
                try {
                    if (!latch.await(10, TimeUnit.SECONDS)) {
                        Logger.log.d(TAG, " PullingController timeout, return AirlockException.");
                        callback.onFailure(new AirlockException(AirlockMessages.ERROR_SERVER_TIMEOUT_PULL_FEATURES));
                    } else {
                        final long downloadControllerStart = System.currentTimeMillis();
                        Logger.log.d(TAG, "downloadController.done");
                        if (getException() != null) {
                            callback.onFailure(getException());
                            return;
                        }

                        //if the new features list were downloaded,calculate random numbers for all new features
                        //and store them
                        if (tempJSONResultsHolder.containsKey(RAW_RULES_TEMP_JSON_OBJ) && tempJSONResultsHolder.get(RAW_RULES_TEMP_JSON_OBJ) != null) {
                            try {
                                JSONObject newFeaturesRandoms = RandomUtils.calculateFeatureRandoms(tempJSONResultsHolder.get(RAW_RULES_TEMP_JSON_OBJ),
                                        persistenceHandler.read(Constants.SP_USER_RANDOM_NUMBER, -1),
                                        persistenceHandler.getFeaturesRandomMap() == null ? new JSONObject() : persistenceHandler.getFeaturesRandomMap());
                                persistenceHandler.setFeaturesRandomMap(newFeaturesRandoms);
                            } catch (Exception e) {
                                callback.onFailure(e);
                                return;
                            }
                        }

                        persistTempResult(Constants.SP_RAW_RULES, RAW_RULES_TEMP);
                        persistTempResult(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, SP_LAST_FEATURES_FULL_DOWNLOAD_TIME_TEMP);

                        persistTempResult(Constants.SP_RAW_TRANSLATIONS, RAW_TRANSLATIONS_TEMP);
                        persistTempResult(Constants.SP_LAST_TRANS_FULL_DOWNLOAD_TIME, SP_LAST_TRANS_FULL_DOWNLOAD_TIME_TEMP);

                        persistTempResult(Constants.SP_RAW_JS_FUNCTIONS, JSON_JS_FUNCTIONS_FIELD_NAME_TEMP);
                        persistTempResult(Constants.SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, SP_LAST_JS_UTILS_DOWNLOAD_TIME_TEMP);

                        // clear SP_FEATURES_PERCENAGE_MAP because we want to reload it when the PercentageManager
                        // methods will be call
                        persistenceHandler.write(Constants.SP_FEATURES_PERCENAGE_MAP, "{}");

                        //Streams
                        if (tempResultsHolder.get(STREAMS_TEMP) != null) {
                            JSONObject usageStreams = null;
                            try {
                                usageStreams = new JSONObject(tempResultsHolder.get(STREAMS_TEMP));
                            } catch (JSONException e) {
                                //Do nothing
                            }
                            if (usageStreams != null && usageStreams.length() > 0) {
                                try {
                                    JSONObject newStreamsRandoms = RandomUtils.calculateStreamsRandoms(usageStreams,
                                            persistenceHandler.getStreamsRandomMap().length() > 0 ? persistenceHandler.getStreamsRandomMap() : new JSONObject());
                                    persistenceHandler.setStreamsRandomMap(newStreamsRandoms);
                                } catch (Exception e) {
                                    callback.onFailure(e);
                                    return;
                                }
                            }

                            //Need to call the upateStreams method - so we do not call the regular persistTempResult method
                            persistenceHandler.write(Constants.SP_FEATURE_USAGE_STREAMS, tempResultsHolder.get(STREAMS_TEMP));
                            streamsManager.updateStreams();
                        }
                        persistTempResult(Constants.SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, SP_LAST_STREAMS_FULL_DOWNLOAD_TIME_TEMP);

                        persistTempResult(Constants.SP_FEATURE_UTILS_STREAMS, STREAMS_JS_UTILS_TEMP);
                        persistTempResult(Constants.SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME_TEMP);

                        //clear temp data
                        tempResultsHolder.remove(STREAMS_TEMP);
                        tempResultsHolder.remove(STREAMS_JS_UTILS_TEMP);

                        if (isNotificationsEnabled) {
                            persistTempResult(Constants.SP_NOTIFICATIONS, NOTIFICATIONS_TEMP);
                            persistTempResult(Constants.SP_LAST_NOTIFICATIONS_FULL_DOWNLOAD_TIME, SP_LAST_NOTIFICATIONS_FULL_DOWNLOAD_TIME_TEMP);
                            notificationsManager.initNotifications();
                            tempResultsHolder.remove(NOTIFICATIONS_TEMP);
                            tempResultsHolder.remove(SP_LAST_NOTIFICATIONS_FULL_DOWNLOAD_TIME_TEMP);
                        }

                        tempJSONResultsHolder.clear();
                        tempResultsHolder.clear();

                        callback.onSuccess("");
                        printPerformanceLog("Total time to  persist config files :", downloadControllerStart);
                    }
                } catch (InterruptedException e) {
                    callback.onFailure(e);
                }
            }
        };
        doPullFeatures(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof AirlockMismatchSeasonException) {
                    fetchUpdatedSeasonId(new AirlockCallback() {
                        @Override
                        public void onFailure(Exception e) {
                            downloadController.setException(e);
                            Logger.log.e(TAG, AirlockMessages.LOG_FAILED_TO_PULL_SEASON_ID, e);
                            releaseLatch(latchCounter, latch);
                        }

                        @Override
                        public void onSuccess(String msg) {
                            pullFeaturesAndTranslationTable(new AirlockCallback() {
                                @Override
                                public void onFailure(Exception e) {
                                    downloadController.setException(e);
                                    releaseLatch(latchCounter, latch);
                                }

                                @Override
                                public void onSuccess(String msg) {
                                    latch.countDown();
                                }
                            });
                        }
                    });
                } else {
                    downloadController.setException(e);
                    releaseLatch(latchCounter, latch);
                }
            }

            @Override
            public void onSuccess(String msg) {
                //notified about pulling completion
                latch.countDown();
            }
        });

        doPullTranslation(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                downloadController.setException(e);
                releaseLatch(latchCounter, latch);
            }

            @Override
            public void onSuccess(String msg) {
                //notified about pulling completion
                latch.countDown();
            }
        });

        doPullJSUtils(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                downloadController.setException(e);
                releaseLatch(latchCounter, latch);
            }

            @Override
            public void onSuccess(String msg) {
                //notified about pulling completion
                latch.countDown();
            }
        });

        pullStreams(new AirlockCallback() {
            @Override
            public void onFailure(Exception ex) {
                if (persistenceHandler.read(Constants.SP_FEATURE_USAGE_STREAMS, "").isEmpty()) {
                    latch.countDown();
                } else {
                    downloadController.setException(ex);
                    releaseLatch(latchCounter, latch);
                }
            }

            @Override
            public void onSuccess(String msg) {
                //notified about pulling completion
                latch.countDown();
            }
        });

        pullFeatureUsageUtils(new AirlockCallback() {
            @Override
            public void onFailure(Exception ex) {
                if (persistenceHandler.read(Constants.SP_FEATURE_UTILS_STREAMS, "").isEmpty()) {
                    latch.countDown();
                } else {
                    downloadController.setException(ex);
                    releaseLatch(latchCounter, latch);
                }
            }

            @Override
            public void onSuccess(String msg) {
                //notified about pulling completion
                latch.countDown();
            }
        });

        if (isNotificationsEnabled) {
            pullNotifications(new AirlockCallback() {
                @Override
                public void onFailure(Exception ex) {
                    if (persistenceHandler.read(Constants.SP_NOTIFICATIONS, "").isEmpty()) {
                    } else {
                        //TODO notify about a problem
                        //downloadController.setException(ex);
                    }
                    latch.countDown();
                }

                @Override
                public void onSuccess(String msg) {
                    latch.countDown();
                }
            });
        }

        //thread to conditional wait with timeout of 10 seconds
        //for all downloading be complete, only then the top callback success method
        //will be called.
        Logger.log.d(TAG, "before downloadController.start()");
        downloadController.start();
    }

    private void releaseLatch(int countDown, CountDownLatch latch) {
        for (int i = 0; i < countDown; i++) {
            latch.countDown();
        }
    }

    public void pullFeaturesInDevelopMode(final AirlockCallback callback) {
        pullFeaturesAndTranslationTable(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }

            @Override
            public void onSuccess(String msg) {
                persistenceHandler.write(Constants.SP_LAST_FEATURES_PULL_TIME, System.currentTimeMillis());
                callback.onSuccess("");
            }
        });
    }

    public void pullFeaturesInProductionMode(final AirlockCallback callback) {
        //if the last features pull time is not set meaning all remote config haven't downloaded yet
        //to the device. There is no need to check the 'ifProductChanged' file indicator
        if (persistenceHandler.read(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED) == Constants.TIME_NOT_DEFINED) {
            final long startPull = System.currentTimeMillis();
            pullFeaturesAndTranslationTable(new AirlockCallback() {
                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }

                @Override
                public void onSuccess(String msg) {
                    persistenceHandler.write(Constants.SP_LAST_FEATURES_PULL_TIME, System.currentTimeMillis());
                    callback.onSuccess("");
                    printPerformanceLog("Total pull time of all files without IfProductChanged:", startPull);
                }
            });
        } else {
            final long startPullWithIfProductChanged = System.currentTimeMillis();
            AirlockDAO.checkIfProductChanged(this, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    printPerformanceLog("Total pull time of IfProductChanged file:", startPullWithIfProductChanged);
                    //the resource hasn't been updated since the last fetching, do nothing.
                    if (response.code() == 304) {
                        response.body().close();
                        callback.onSuccess("");
                        return;
                    }

                    response.body().close();
                    final long startPullFeaturesAndTranslationTable = System.currentTimeMillis();
                    pullFeaturesAndTranslationTable(new AirlockCallback() {
                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }

                        @Override
                        public void onSuccess(String msg) {
                            printPerformanceLog("Total pull of all remote config files:", startPullFeaturesAndTranslationTable);
                            if (response.header("Last-Modified") != null) {
                                persistenceHandler.write(Constants.SP_LAST_TIME_PRODUCT_CHANGED, response.header("Last-Modified"));
                            }

                            persistenceHandler.write(Constants.SP_LAST_FEATURES_PULL_TIME, System.currentTimeMillis());
                            callback.onSuccess("");
                            printPerformanceLog("Total pull time of all files:", startPullFeaturesAndTranslationTable);
                        }
                    });
                }
            });
        }
    }

    public void pullFeatures(final AirlockCallback callback) {
        resetLocale();
        translatedStringsCache.clear();
        // the user group is empty we can assume that the SDK is running in the production mode
        List<String> userGroups = persistenceHandler.getDeviceUserGroups();
        if (userGroups.isEmpty()) {
            Logger.log.d(TAG, AirlockMessages.LOG_SDK_GET_PROD_CONFIG);
            pullFeaturesInProductionMode(callback);
        } else {
            Logger.log.d(TAG, AirlockMessages.LOG_SDK_GET_DEV_CONFIG);
            pullFeaturesInDevelopMode(callback);
        }
    }

    private void doPullFeatures(final AirlockCallback callback) {
        try {
            final long doPullFeaturesStart = System.currentTimeMillis();
            AirlockDAO.pullFeaturesConfiguration(this, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Logger.log.d(TAG, String.format(AirlockMessages.LOG_PULL_FEATURES_RESPONSE_CODE_FORMATTED, response.code()));
                    printPerformanceLog("Total time for doPullFeatures:", doPullFeaturesStart);
                    final long processPullFeaturesStart = System.currentTimeMillis();
                    //the resource hasn't been updated since the last fetching, do nothing.
                    if (response.code() == 304) {
                        response.body().close();
                        callback.onSuccess("");
                        printPerformanceLog("Total time to process doPullFeatures response:", processPullFeaturesStart);
                        return;
                    }
                    if (response.code() != 200) {
                        response.body().close();
                        callback.onFailure(new AirlockException(String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code())));
                        return;
                    }

                    String responseBody = response.body().string();
                    response.body().close();

                    if (responseBody.equals("")) {
                        callback.onFailure(new AirlockException(AirlockMessages.ERROR_RESPONSE_BODY_IS_EMPTY));
                        return;
                    }

                    String minVersion;
                    String maxVersion;
                    JSONObject jsonResult;
                    try {
                        jsonResult = new JSONObject(responseBody);
                        minVersion = jsonResult.optString(Constants.JSON_FEATURE_FIELD_MIN_VERSION);
                        maxVersion = jsonResult.optString(Constants.JSON_FEATURE_FIELD_MAX_VERSION);
                    } catch (JSONException e) {
                        callback.onFailure(e);
                        return;
                    }

                    if (!minVersion.equals("") && !verifySeason(minVersion, maxVersion)) {
                        callback.onFailure(new AirlockMismatchSeasonException(String.format("season = %s, min = %s, max = %s prodVersion = %s",
                                persistenceHandler.read(Constants.SP_SEASON_ID, ""), minVersion, maxVersion, productVersion)));
                        return;
                    } else {
                        // update only if we call callback.onSuccess (version ok ...)
                        tempResultsHolder.put(RAW_RULES_TEMP, responseBody);
                        tempJSONResultsHolder.put(RAW_RULES_TEMP_JSON_OBJ, (jsonResult == null ? new JSONObject() : jsonResult));
                        if (response.header("Last-Modified") != null) {
                            tempResultsHolder.put(SP_LAST_FEATURES_FULL_DOWNLOAD_TIME_TEMP, response.header("Last-Modified"));
                        }
                    }
                    callback.onSuccess("");
                    printPerformanceLog("Total time to process doPullFeatures response:", processPullFeaturesStart);
                }
            });
        } catch (RuntimeException e) {
            Logger.log.e(TAG, "", e);
            callback.onFailure(e);
        }
    }

    @Deprecated
    public void pullProductList(final Servers.Server server, final AirlockCallback airlockCallback) {
        AirlockDAO.pullProducts(this, server, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.log.e(TAG, AirlockMessages.LOG_FAILED_TO_PULL_PRODUCTS + e);
                airlockCallback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // code 304 not relevant here because we need the data from teh file to continue pull request
                if (response.code() != 200) {
                    response.body().close();
                    Logger.log.d(TAG, "onResponse to getProductList - response code != 200 - return");
                    airlockCallback.onFailure(new AirlockException(String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code())));
                    return;
                }
                try {
                    String responseBody = response.body().string();
                    response.body().close();
                    JSONObject responseAsJson = new JSONObject(responseBody);
                    JSONArray products = responseAsJson.getJSONArray(Constants.JSON_FEATURE_FIELD_PRODUCTS);
                    List<Servers.Product> productList = new ArrayList<Servers.Product>();
                    if (products != null && products.length() > 0) {
                        // find my product by the id
                        for (int i = 0; i < products.length(); i++) {
                            JSONObject currentProduct = products.getJSONObject(i);
                            String productId = currentProduct.optString(Constants.JSON_FEATURE_FIELD_PRODUCT_UNIQUE_ID);
                            String productName = currentProduct.optString(Constants.JSON_FEATURE_FIELD_PRODUCT_NAME);
                            Logger.log.d(TAG, "Found product " + productName);
                            JSONArray seasons = currentProduct.getJSONArray(Constants.JSON_FEATURE_FIELD_SEASONS);
                            if (seasons == null) {
                                Logger.log.d(TAG, "No seasons in product " + productName);
                            } else {
                                //find the correct seasonId
                                for (int j = 0; j < seasons.length(); j++) {
                                    JSONObject currentSeason = seasons.getJSONObject(j);
                                    String maxVersion = currentSeason.optString(Constants.JSON_FEATURE_FIELD_MAX_VERSION);
                                    String minVersion = currentSeason.optString(Constants.JSON_FEATURE_FIELD_MIN_VERSION);
                                    // this is the match season
                                    if (verifySeason(minVersion, maxVersion)) {
                                        Logger.log.d(TAG, String.format("Found match season for product " + productName));
                                        String sessionId = currentSeason.optString(Constants.JSON_PRODUCT_SEASON_UNIQUE_ID);
                                        productList.add(new Servers.Product(productName, productId, sessionId, server));
                                        break;
                                    }
                                }
                            }
                        }
                        server.setProducts(productList);
                        airlockCallback.onSuccess("");
                    } else {
                        airlockCallback.onFailure(new AirlockException("No Products in this server"));
                    }
                } catch (JSONException e) {
                    Logger.log.e(TAG, "", e);
                    airlockCallback.onFailure(e);
                }
            }
        });
    }

    // Called after seasonId mismatch.
    private void fetchUpdatedSeasonId(final AirlockCallback airlockCallback) {
        AirlockDAO.pullProducts(this, null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                airlockCallback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // code 304 not relevant here because we need the data from teh file to continue pull request
                if (response.code() != 200) {
                    response.body().close();
                    Logger.log.d(TAG, "onResponse to fetchUpdatedSeasonId - response code != 200 - return");
                    airlockCallback.onFailure(new AirlockException(String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code())));
                    return;
                }
                try {
                    String responseBody = response.body().string();
                    response.body().close();
                    JSONObject productRuntime = new JSONObject(responseBody);
                    JSONArray seasons = productRuntime.optJSONArray(Constants.JSON_FEATURE_FIELD_SEASONS);
                    if (seasons == null) {
                        Logger.log.d(TAG, "No seasons in Relevant product");
                        airlockCallback.onFailure(new AirlockException(String.format(AirlockMessages.ERROR_MISSING_OR_EMPTY_VALUE_FORMATTED, Constants.JSON_FEATURE_FIELD_SEASONS)));
                        return;
                    }
                    //find the correct seasonId
                    for (int j = 0; j < seasons.length(); j++) {
                        JSONObject currentSeason = seasons.getJSONObject(j);
                        String currentMaxVersion = currentSeason.optString(Constants.JSON_FEATURE_FIELD_MAX_VERSION);
                        String currentMinVersion = currentSeason.optString(Constants.JSON_FEATURE_FIELD_MIN_VERSION);
                        // this is the match season
                        if (verifySeason(currentMinVersion, currentMaxVersion)) {
                            Logger.log.d(TAG, String.format("Found match season, id = %s, minVersion = %s, maxVersion = %s, productVersion = %s",
                                    currentSeason.optString(Constants.JSON_PRODUCT_SEASON_UNIQUE_ID), currentMinVersion, currentMaxVersion, productVersion));
                            String updatedSessionId = currentSeason.optString(Constants.JSON_PRODUCT_SEASON_UNIQUE_ID);
                            persistenceHandler.write(Constants.SP_SEASON_ID, updatedSessionId);
                            getSyncedFeaturedList().clear();
                            getPreSyncedFeaturedList().clear();
                            resetSPOnNewSeasonId();
                            try {
                                parseDefaultFile(persistenceHandler.read(Constants.SP_DEFAULT_FILE, ""), true);
                            } catch (AirlockInvalidFileException e) {
                                // should not happened because we already read this default file.
                                //If we here, don't do anything.
                                Logger.log.e(TAG, "", e);
                            }
                            airlockCallback.onSuccess(updatedSessionId);
                            return;
                        }
                    }

                    airlockCallback.onFailure(new AirlockMismatchSeasonException(""));
                    return;

                } catch (JSONException e) {
                    Logger.log.e(TAG, "", e);
                    airlockCallback.onFailure(new AirlockMismatchSeasonException(""));
                }
            }
        });
    }

    /**
     * Calculates the status of the features according to the pullFeatures results and return the Features as a tree.
     *
     * @param context an user profile context
     * @param locale
     */
    public Feature calculateFeatures(@Nullable JSONObject context, String locale) {
        final JSONObject runtimeFeatures = persistenceHandler.readJSON(Constants.SP_RAW_RULES);
        if (runtimeFeatures == null) {
            Logger.log.e(TAG, AirlockMessages.LOG_CALCULATE_MISSING_PULL_RESULT);
            return new Feature("ROOT", true, Feature.Source.MISSING);
        }

        JSONObject translations = null;
        if (translatedStringsCache == null) {
            translations = persistenceHandler.readJSON(Constants.SP_RAW_TRANSLATIONS);
        } else {

            translations = (JSONObject) translatedStringsCache.get(Constants.SP_RAW_TRANSLATIONS + "_" + locale);
            if (translations == null || translations.toString().equals("{}")) {
                //try to download the translated strings corresponding to the give locale
                String translatedStringsTable = pullTranslationSynchronously(locale);

                if (translatedStringsTable == null) {
                    translations = persistenceHandler.readJSON(Constants.SP_RAW_TRANSLATIONS);
                } else {
                    translations = new JSONObject(translatedStringsTable);
                    // in cache for 30 minutes
                    translatedStringsCache.put(Constants.SP_RAW_TRANSLATIONS + "_" + locale, translations, 30 * 60 * 1000);
                }

            }
        }

        JSONObject translationStrings = translations.has(Constants.JSON_TRANSLATION_STRING) ? translations.getJSONObject(Constants.JSON_TRANSLATION_STRING) : new JSONObject();
        JSONObject ruleResult = calculateRules(runtimeFeatures, null, context, getDeviceUserGroups(), translationStrings, null);
        updateFeatureListSource(getPreSyncedFeaturedList(), Feature.Source.SERVER, Feature.Source.CACHE);
        try {
            return BaseRawFeaturesJsonParser.getInstance().getFeaturesTree(ruleResult, Feature.Source.SERVER);
        } catch (JSONException | ScriptExecutionException e) {
            Logger.log.e(TAG, "", e);
        }
        return new Feature("ROOT", true, Feature.Source.MISSING);
    }


    /**
     * Calculates the status of the features according to the pullFeatures results.
     * No feature status changes are exposed until the syncFeatures method is called.
     *
     * @param userProfile    an user profile
     * @param airlockContext the device airlock context could contain user provide as a part of it.
     */
    public void calculateFeatures(@Nullable JSONObject userProfile, @Nullable JSONObject airlockContext) {
        calculateFeatures(userProfile, airlockContext, null);
    }

    /**
     * Calculates the status of the features according to the pullFeatures results.
     * No feature status changes are exposed until the syncFeatures method is called.
     *
     * @param userProfile         an user profile
     * @param airlockContext      the airlock context could contain user provide as a part of it.
     * @param purchasedProductIds a list of purchased products
     */
    public void calculateFeatures(@Nullable JSONObject userProfile,
                                  @Nullable JSONObject airlockContext,
                                  @Nullable Collection<String> purchasedProductIds) {

        addAirlyticsShardToContext(airlockContext);
        final JSONObject runtimeFeatures = persistenceHandler.readJSON(Constants.SP_RAW_RULES);
        if (runtimeFeatures == null) {
            Logger.log.e(TAG, AirlockMessages.LOG_CALCULATE_MISSING_PULL_RESULT);
            return;
        }
        JSONObject translations = persistenceHandler.readJSON(Constants.SP_RAW_TRANSLATIONS);
        if (translations == null) {
            translations = new JSONObject();
        }
        JSONObject translationStrings = translations.has(Constants.JSON_TRANSLATION_STRING) ?
                translations.getJSONObject(Constants.JSON_TRANSLATION_STRING) : new JSONObject();

        JSONObject ruleResult = calculateRules(
                runtimeFeatures,
                userProfile,
                airlockContext,
                getDeviceUserGroups(),
                translationStrings,
                purchasedProductIds);
        // will not be updated if calculateRules fails (and throw exception)
        // Change all features status to cache before update
        long mergeStart = System.currentTimeMillis();
        updateFeatureListSource(getPreSyncedFeaturedList(), Feature.Source.SERVER, Feature.Source.CACHE);
        updatePreSyncServerMap(ruleResult);
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().reportValue(AirlockEnginePerformanceMetric.FEATURES_NUMBER, getPreSyncedFeaturedList().size());
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().report(AirlockEnginePerformanceMetric.MERGE_CALCULATION_RESULTS, mergeStart);

        new Thread() {
            public void run() {
                try {
                    JSONArray contextFieldsToAnalytics = (JSONArray) runtimeFeatures.opt(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS);
                    if (contextFieldsToAnalytics != null && contextFieldsToAnalytics.length() > 0) {
                        persistenceHandler.setContextFieldsForAnalytics(contextFieldsToAnalytics.toString());
                    }
                    persistenceHandler.setServerFeatureMap(getPreSyncedFeaturedList());
                    persistenceHandler.setPreSyncedFeaturesMap(getPreSyncServerFeatureList());
                    persistenceHandler.write(Constants.SP_LAST_CALCULATE_TIME, System.currentTimeMillis());
                } catch (Exception e) {
                    Logger.log.w(TAG, e.getMessage());
                }
            }
        }.start();
    }

    private void addAirlyticsShardToContext(@Nullable JSONObject airlockContext) {
        if (airlockContext != null) {
            final int NUMBER_OF_SHARDS = 1000;
            byte[] value = getAirlockUserUniqueId().getBytes();
            long no = Murmur2.hash(value, value.length, 894157739);
            airlockContext.put(JSON_AIRLYTICS_SHARD, ((Long) (no % NUMBER_OF_SHARDS)).intValue());
        }
    }

    public Map<String, CalculateErrorItem> getLastJSCalculateErrors() {
        if (lastJSCalculateErrors == null) {
            return new HashMap<>();
        }
        return lastJSCalculateErrors;
    }

    // if minVersion <= productVersion and maxVersion > productVersion return true.
    private boolean verifySeason(String minVersion, String maxVersion) {
        AirlockVersionComparator comparator = new AirlockVersionComparator();
        if (comparator.compare(minVersion, productVersion) <= 0) {
            if (maxVersion.equalsIgnoreCase("null") || maxVersion.isEmpty() ||
                    comparator.compare(maxVersion, productVersion) > 0) {
                return true;
            }
        }
        return false;
    }

    private void populateWithBuildInAirlockContextFields(@Nullable JSONObject airlockContext) {
        if (airlockContext == null) {
            return;
        }
        JSONObject airlockElement = new JSONObject();
        airlockElement.put(Constants.JSON_USER_ID, getAirlockUserUniqueId());
        airlockContext.put(Constants.JSON_AIRLOCK_BUILD_IN_FIELDS, airlockElement);
    }

    private JSONObject calculateRules(@Nullable JSONObject runtimeFeatures,
                                      @Nullable JSONObject userProfile,
                                      @Nullable JSONObject airlockContext,
                                      @Nullable List<String> profileGroups,
                                      JSONObject translationStrings,
                                      @Nullable Collection<String> purchaseIds) {

        JSONObject clonedFeatures = runtimeFeatures;
        if (runtimeFeatures != null){
            clonedFeatures = new JSONObject(runtimeFeatures.toString());
        }
        long start = System.currentTimeMillis();
        Map<String, FeaturesCalculator.Fallback> fallbackMap = getFallbacksMap();
        JSONObject featuresRandoms = persistenceHandler.getFeaturesRandomMap();
        try {
            //put user profile be element in the context
            if (userProfile != null && airlockContext != null) {
                airlockContext.put(Constants.JS_PROFILE_VAR_NAME, userProfile);
            }
            String jsFunctions = persistenceHandler.read(Constants.SP_RAW_JS_FUNCTIONS, "");

            this.populateWithBuildInAirlockContextFields(airlockContext);
            this.airlockContextManager.overideRuntimeWithCurrentContext();
            this.airlockContextManager.getRuntimeContext().mergeWith(airlockSharedContext);
            this.airlockContextManager.setJsTranslationsScript(translationStrings.toString());
            this.airlockContextManager.setJsUtilsScript(jsFunctions);


            final ExperimentsCalculator.CalculationResults results = new ExperimentsCalculator().calculate(this, clonedFeatures,
                    airlockContext, jsFunctions, translationStrings, profileGroups, fallbackMap,
                    productVersion, featuresRandoms, purchaseIds, true);


            new Thread() {
                public void run() {
                    saveExperimentList(results.getFeatures());
                    savePreSyncedEntitlements(results.getEntitlements());
                }
            }.start();

            preSyncedEntitlementsJSON.put(PRE_SYNC_ENTITLEMENTS_JSON, results.getEntitlements());

            AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().report(AirlockEnginePerformanceMetric.CALCULATION_TOTAL, start);
            return results.getFeatures();

        } catch (JSONException | ScriptInitException | FeaturesBranchMerger.MergeException e) {
            Logger.log.e(TAG, "", e);
            return new JSONObject();
        }
    }

    private JSONObject readLastEntitlementsResults() {
        return persistenceHandler.readJSON(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
    }

    private void putSyncedEntitlements(JSONObject entitlements) {
        syncedEntitlementsJSON.put(SYNC_ENTITLEMENTS_JSON, entitlements);
    }

    private void saveEntitlements(JSONObject result) {
        persistenceHandler.write(Constants.SP_SYNCED_ENTITLEMENTS_LIST, result);
    }

    private void savePreSyncedEntitlements(JSONObject result) {
        persistenceHandler.write(Constants.SP_PRE_SYNCED_ENTITLEMENTS_LIST, result);
    }

    private void saveExperimentList(JSONObject result) {
        JSONObject experimentInfo = result.optJSONObject(Constants.SP_EXPERIMENT_INFO);
        if (experimentInfo != null) {
            persistenceHandler.write(Constants.SP_EXPERIMENT_INFO, experimentInfo.toString());
        } else {
            persistenceHandler.clearExperiments();
        }
    }

    //todo: Move to featureTree class
    private void updatePreSyncServerMap(JSONObject result) {
        FeaturesList tmp = new FeaturesList();
        tmp.putAll(getPreSyncedFeaturedList());
        // before merge with the new features - change feature source from server to cache.
        updateFeatureListSource(tmp, Feature.Source.SERVER, Feature.Source.CACHE);
        FeaturesList<Feature> resultMap = new FeaturesList();
        try {
            resultMap = BaseRawFeaturesJsonParser.getInstance().getFeatures(result, Feature.Source.SERVER);
        } catch (JSONException | ScriptExecutionException e) {
            Logger.log.e(TAG, "", e);
        }
        // calculate lastJSCalculateErrors.
        lastJSCalculateErrors.clear();
        for (Feature current : resultMap.getFeatures().values()) {
            String traceInfo = current.getTraceInfo();
            if (Result.RULE_CONFIG_FALLBACK.equals(traceInfo)) {
                lastJSCalculateErrors.put(current.getName(), new CalculateErrorItem(current.getName(), traceInfo));
            }
        }
        getPreSyncedFeaturedList().merge(resultMap);
    }

    private void updateFeatureListSource(FeaturesList<Feature> featureList, Feature.Source oldSource, Feature.Source newSource) {
        for (Feature feature : featureList.getFeatures().values()) {
            if (feature.getSource().equals(oldSource)) {
                feature.setSource(newSource);
            }
        }
    }

    private Map<String, FeaturesCalculator.Fallback> getFallbacksMap() {
        Map<String, FeaturesCalculator.Fallback> result = new Hashtable<>();

        // add features
        for (Map.Entry<String, Feature> entry : getSyncedFeaturedList().getFeatures().entrySet()) {
            Feature feature = entry.getValue();
            FeaturesCalculator.Fallback cachedFallback = new FeaturesCalculator.Fallback(feature.isOn(),
                    feature.getPremiumRuleResult() == ScriptInvoker.Result.TRUE, feature.getConfiguration());
            result.put(entry.getKey(), cachedFallback);
        }

        // add entitlement
        JSONObject entitlementJSON = readLastEntitlementsResults();
        if (entitlementJSON.has(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)) {
            JSONArray purchasesLastResults = entitlementJSON.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
            for (int i = 0; i < purchasesLastResults.length(); i++) {
                JSONObject purchaseResult = purchasesLastResults.getJSONObject(i);
                String name = purchaseResult.optString(Constants.JSON_FEATURE_FIELD_NAME);
                boolean isON = purchaseResult.optBoolean(Constants.JSON_FEATURE_IS_ON);
                JSONObject config = purchaseResult.optJSONObject(Constants.JSON_FEATURES_ATTRS);

                if (name == null) {
                    continue;
                }

                FeaturesCalculator.Fallback cachedFallback = new FeaturesCalculator.Fallback(isON, config);
                result.put(name, cachedFallback);
            }
        }

        return result;
    }

    /*
     * will receive the default file R id and parse it's seasonId, productId, s3Path and default features.
     */
    private void parseDefaultFile(String fileContent, boolean featuresOnly) throws AirlockInvalidFileException {
        if (fileContent == null || fileContent.length() == 0) {
            throw new AirlockInvalidFileException(AirlockMessages.ERROR_DEFAULT_EMPTY_OR_NULL);
        }

        syncFeatureList.put(SYNC_FEATURE_MAP,
                DefaultFileParser.parseDefaultFile(persistenceHandler, fileContent, featuresOnly, !sharedPreferenceHandlerInitialized),
                getFeaturesMapTimeToLive());

        syncedEntitlementsJSON.put(SYNC_ENTITLEMENTS_JSON,
                DefaultFileParser.getEntitlementDefaultsAsList(fileContent), getFeaturesMapTimeToLive());
    }

    /**
     * Returns default language
     *
     * @return The default languages taken from the default file
     */
    private String getDefaultLanguage() {
        return persistenceHandler.read(Constants.SP_DEFAULT_LANGUAGE, "en");
    }


    /**
     * Returns supported country by language, if there is any return empty string
     *
     * @param language
     * @return supported country by language
     */
    public String getSupportedCountryByLanguage(String language) {
        JSONArray arrayLanguages;
        String country = "";
        try {
            arrayLanguages = new JSONArray(persistenceHandler.read(Constants.SP_SUPPORTED_LANGUAGES, "[]"));
            for (int i = 0; i < arrayLanguages.length(); i++) {
                if (arrayLanguages.getString(i).contains(language)
                        && arrayLanguages.getString(i).contains("_")) {
                    return arrayLanguages.getString(i).split("_")[1];
                }
            }
        } catch (Exception e) {
            return "";
        }
        return country;
    }

    /**
     * Returns supported languages
     *
     * @return list of the default languages taken from the default file
     */
    public Set<String> getSupportedLanguages() {
        JSONArray arrayLanguages;
        HashSet<String> languageSet = new HashSet<>();
        try {
            arrayLanguages = new JSONArray(persistenceHandler.read(Constants.SP_SUPPORTED_LANGUAGES, "[]"));
        } catch (JSONException e) {
            return new HashSet<>();
        }

        for (int i = 0; i < arrayLanguages.length(); i++) {
            try {
                languageSet.add(arrayLanguages.get(i).toString());
            } catch (JSONException e) {
                //do nothing
            }
        }
        return languageSet;
    }

    public void resetSPOnNewSeasonId() {
        clearRuntimeData();
        persistenceHandler.write(Constants.SP_RAW_JS_FUNCTIONS, "");
        persistenceHandler.write(Constants.SP_RAW_RULES, "");
        persistenceHandler.write(Constants.SP_RAW_TRANSLATIONS, "");
        persistenceHandler.write(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_CALCULATE_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_SYNC_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_TRANS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_TIME_PRODUCT_CHANGED, "");
        persistenceHandler.write(Constants.SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, "");
    }

    public void resetLocale() {
        String currentDeviceLanguage = localeProvider.getLocale().toString();
        String lastDeviceLanguage = "";
        if (sharedPreferenceHandlerInitialized) {
            lastDeviceLanguage = persistenceHandler.read(Constants.SP_CURRENT_LOCALE, "");
        }
        if (lastDeviceLanguage.equals(currentDeviceLanguage)) {
            return;
        }
        persistenceHandler.write(Constants.SP_CURRENT_LOCALE, currentDeviceLanguage);
        clearRuntimeData();
    }

    // to be called from init, pull and application.
    // 1. update persistenceHandler from system - if changed
    // 2 . clearRuntimeData

    public void clearTimeStamps() {
        persistenceHandler.write(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_CALCULATE_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_SYNC_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_TRANS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_TIME_PRODUCT_CHANGED, "");
        persistenceHandler.write(Constants.SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, "");
    }

    public void clearRuntimeData() {
        getSyncedFeaturedList().clear();
        getPreSyncedFeaturedList().clear();
        preSyncedEntitlementsJSON.clear();
        syncedEntitlementsJSON.clear();
        persistenceHandler.write(Constants.SP_SERVER_FEATURE_LIST, "");
        persistenceHandler.write(Constants.SP_PRE_SYNCED_FEATURES_LIST, "");
        persistenceHandler.write(Constants.SP_SYNCED_FEATURES_LIST, "");
        persistenceHandler.write(Constants.SP_SYNCED_ENTITLEMENTS_LIST, "");
        persistenceHandler.write(Constants.SP_PRE_SYNCED_ENTITLEMENTS_LIST, "");
        persistenceHandler.write(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_CALCULATE_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_SYNC_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_TRANS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_TIME_PRODUCT_CHANGED, "");
        persistenceHandler.write(Constants.SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, "");
    }

    public void clearPreSyncRuntimeData() {
        getPreSyncServerFeatureList().clear();
        persistenceHandler.write(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");
    }

    @TestOnly
    public void resetFeatureLists() {
        FeaturesList syncedList = getSyncedFeaturedList();
        if (syncedList != null) {
            syncedList.clear();
        }
        FeaturesList preSyncedList = getPreSyncedFeaturedList();
        if (preSyncedList != null) {
            preSyncedList.clear();
        }
    }

    public void resetEntitlementsToDefault() {
        preSyncedEntitlementsJSON.clear();
        syncedEntitlementsJSON.clear();
        try {
            saveEntitlements(DefaultFileParser.getEntitlementDefaultsAsList(persistenceHandler.read(Constants.SP_DEFAULT_FILE, "{}")));
        } catch (Exception e) {
            Logger.log.e(TAG, String.format(AirlockMessages.LOG_CANT_READ_FROM_DEFAULT_FORMATTED, "Entitlements"), e);
        }
    }

    public void resetFeaturesToDefault() {
        getSyncedFeaturedList().clear();
        getPreSyncedFeaturedList().clear();
        getSyncedFeaturedList().merge(DefaultFileParser.cloneDefaultFile(persistenceHandler.read(Constants.SP_DEFAULT_FILE, "{}")));
    }

    public JSONObject getSyncedEntitlements() {
        return getSyncedEntitlementsJSON();
    }

    public FeaturesList getSyncFeatureList() {
        return getSyncedFeaturedList();
    }

    @CheckForNull
    public JSONObject getRawFeaturesConfiguration() {
        return persistenceHandler.readJSON(Constants.SP_RAW_RULES);//        returnValue += persistenceHandler.read(Constants.SP_RAW_TRANSLATIONS, "");
    }

    @CheckForNull
    public JSONArray getContextFieldsForAnalytics() {
        JSONArray contextFields = null;
        try {
            contextFields = new JSONArray(persistenceHandler.read(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, "[]"));
        } catch (JSONException e) {
            Logger.log.e(TAG, String.format(AirlockMessages.LOG_CANT_PARSE_FORMATTED, Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS), e);
        }
        return contextFields;
    }

    public AirlockContextManager getAirlockContextManager() {
        return this.airlockContextManager;
    }

    private class PullingController extends Thread {
        @Nullable
        private Exception e;

        @Nullable
        public Exception getException() {
            return e;
        }

        public void setException(Exception e) {
            this.e = e;
        }
    }

    public NotificationsManager getNotificationsManager() {
        return notificationsManager;
    }

    public void setAirlockSharedContext(@Nullable StateFullContext airlockSharedContext) {
        this.airlockSharedContext = airlockSharedContext;
    }


    @TestOnly
    public InMemoryCache getCachedSyncFeatureList() {
        return syncFeatureList;
    }

    @TestOnly
    public InMemoryCache getCachedPreSyncFeatureList() {
        return preSyncServerFeatureList;
    }

    @TestOnly
    public FeaturesList getPreSyncServerFeatureList() {
        return getPreSyncedFeaturedList();
    }

    @TestOnly
    public int getUserRandomNumber() {
        return persistenceHandler.read(Constants.SP_USER_RANDOM_NUMBER, -1);
    }

    @TestOnly
    public void setUserRandomNumber(int randomNumber) {
        persistenceHandler.write(Constants.SP_USER_RANDOM_NUMBER, randomNumber);
    }

}
