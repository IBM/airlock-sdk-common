package com.ibm.airlock.common.net;

import com.ibm.airlock.common.exceptions.AirlockException;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.model.Servers;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Constants;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Class provides static set of APIs to get access to the remote airlock resources which are hosted primarily on S3
 *
 * @author Denis Voloshin
 */
@SuppressWarnings("SpellCheckingInspection")
public class RemoteConfigurationAsyncFetcher {

    //indicates whether any of remote config file has been changed
    private static final String URL_PRODUCT_CHANGED_FILE = "%sseasons/%s/%s/productionChanged.txt";

    private static final String URL_REFRESH_FEATURES_STRING_FORMAT = "%sseasons/%s/%s/AirlockRuntime%s.json"; //s3path, productId, seasonId, dev/prod
    private static final String URL_REFRESH_JS_UTILS_STRING_FORMAT = "%sseasons/%s/%s/AirlockUtilities%s.txt"; //s3path, productId, seasonId, dev/prod
    private static final String URL_DOWNLOAD_DEFAULTS_STRING_FORMAT = "%sseasons/%s/%s/AirlockDefaults.json"; //s3path, productId, seasonId

    private static final String URL_PRODUCT_STRING_FORMAT = "%sseasons/%s/%s/productRuntime.json";//s3path
    //private static final String URL_PRODUCT_STRING_FORMAT = "%sproducts.json";//s3path
    private static final String URL_USER_GROUPS_STRING_FORMAT = "%sseasons/%s/%s/AirlockUserGroupsRuntime.json"; //s3path seasonId
    private static final String URL_BRANCHES_FORMAT = "%sseasons/%s/%s/AirlockBranchesRuntime.json";//s3path seasonId
    private static final String URL_BRANCH_FORMAT = "%sseasons/%s/%s/branches/%s/AirlockRuntimeBranch%s.json";//s3path seasonId

    private static final String URL_SERVER_LIST_STRING_FORMAT = "%sops/airlockServers.json";//s3path
    //s3path, productId, seasonId,language, (can be empty)country, dev/prod
    private static final String URL_TRANSLATIONS_STRING_FORMAT = "%sseasons/%s/%s/translations/strings__%s%s%s.json";
    private static final String URL_PROD_SUFFIX = "PRODUCTION";
    private static final String URL_DEV_SUFFIX = "DEVELOPMENT";
    private static final String TAG = "airlock.RemoteConfigurationAsyncFetcher";
    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    //by default the model comes from the cached channel

    public static void pullFeaturesConfiguration(InfraAirlockService cache, final Callback callbackListener) {
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");
        sendRequest(cache, callbackListener, getPullFeaturesUrl(cache), lastTime);
    }

    public static void pullServerList(InfraAirlockService cache, Callback callbackListener) {
        String productsUrl = String.format(URL_SERVER_LIST_STRING_FORMAT, getDefaultServerUrl(cache));
        sendRequest(cache, productsUrl, callbackListener);
    }

    public static void pullStreams(InfraAirlockService cache, Callback callbackListener) {
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, "");
        sendRequest(cache, callbackListener, getPullFeaturesUrl(cache), lastTime);
    }

    public static void pullFeatureUsageUIUtils(InfraAirlockService cache, Callback callbackListener) {
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, "");
        sendRequest(cache, callbackListener, getPullFeaturesUrl(cache), lastTime);
    }

    public static void pullNotifications(InfraAirlockService cache, Callback callbackListener) {
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_NOTIFICATIONS_FULL_DOWNLOAD_TIME, "");
        sendRequest(cache, callbackListener, getPullFeaturesUrl(cache), lastTime);
    }

    public static void pullDefaultFile(InfraAirlockService cache, Servers.Server server, Servers.Product product, Callback callbackListener) {
        String pullDefaultUrl = String.format(URL_DOWNLOAD_DEFAULTS_STRING_FORMAT, getServerUrl(cache, server),
                product.getProductId(), product.getSeasonId());
        sendRequest(cache, pullDefaultUrl, callbackListener);
    }

    public static void pullUserGroups(InfraAirlockService cache, Callback callbackListener) {
        String url = String.format(URL_USER_GROUPS_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""),
                cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""));
        sendRequest(cache, url, callbackListener);
    }

    public static void pullBranches(InfraAirlockService cache, Callback callbackListener) {
        String url = String.format(URL_BRANCHES_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""),
                cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""));
        sendRequest(cache, url, callbackListener);
    }

    public static void pullBranchById(InfraAirlockService cache, String branchId, Callback callbackListener) {
        String url = String.format(URL_BRANCH_FORMAT, getServerUrl(cache), cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""),
                cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), branchId, getUrlSuffix(cache));
        sendRequest(cache, url, callbackListener);
    }

    public static void checkIfProductChanged(InfraAirlockService cache, String server, String productId, String sessionId, Callback callbackListener) {
        String translationsUrl = String.format(URL_PRODUCT_CHANGED_FILE, server,
                productId, sessionId);
        sendRequest(cache, callbackListener, translationsUrl, "");
    }

    public static void checkIfProductChanged(InfraAirlockService cache, Callback callbackListener) {
        String translationsUrl = String.format(URL_PRODUCT_CHANGED_FILE, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""));
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_TIME_PRODUCT_CHANGED, "");
        sendRequest(cache, callbackListener, translationsUrl, lastTime);
    }

    public static void pullJSUtils(InfraAirlockService cache, Callback callbackListener) {
        String jsUtilsUrl = String.format(URL_REFRESH_JS_UTILS_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), getUrlSuffix(cache));
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, "");
        sendRequest(cache, callbackListener, jsUtilsUrl, lastTime);
    }

    public static void pullTranslationsTable(InfraAirlockService cache, String language, String country, Callback callbackListener) {
        String url = getPullTranslationsUrl(cache, language, country);
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_TRANS_FULL_DOWNLOAD_TIME, "");
        sendRequest(cache, callbackListener, url, lastTime);
    }

    //public for tests
    public static String getPullFeaturesUrl(InfraAirlockService cache) {
        return String.format(URL_REFRESH_FEATURES_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), getUrlSuffix(cache));
    }

    //public for tests
    public static String getPullTranslationsUrl(InfraAirlockService cache, String language, String country) {
        return String.format(URL_TRANSLATIONS_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), language, country, getUrlSuffix(cache));
    }

    private static String getUrlSuffix(InfraAirlockService cache) {
        List<String> userGroups = cache.getPersistenceHandler().getDeviceUserGroups();
        if (userGroups.isEmpty()) {
            return URL_PROD_SUFFIX;
        }
        return URL_DEV_SUFFIX;
    }


    @CheckForNull
    public static String pullTranslationsTable(final InfraAirlockService cache, final String language, final String country) {

        final CountDownLatch completeBlock = new CountDownLatch(1);
        final StringBuilder stringsTable = new StringBuilder();

        Callback wrapperCallback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                completeBlock.countDown();
                Logger.log.e(TAG, e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //the resource hasn't been updated since the last fetching, do nothing.
                Logger.log.d(TAG, String.format(AirlockMessages.LOG_PULL_TRANSLATION_RESPONSE_CODE_FORMATTED, response.code()));

                if (response.code() != 200) {
                    response.body().close();
                    completeBlock.countDown();
                    return;
                }

                String responseBody = response.body().string();
                response.body().close();
                if (responseBody.isEmpty()) {
                    completeBlock.countDown();
                    return;
                }

                stringsTable.append(responseBody);
                completeBlock.countDown();
            }
        };
        sendRequest(cache, wrapperCallback, getPullTranslationsUrl(cache, language, country), "");

        try {
            if (!completeBlock.await(10000, TimeUnit.SECONDS)) {
                return null;
            }
        } catch (InterruptedException e) {
            Logger.log.e(TAG, e.getMessage(), e);
            return null;
        }

        return stringsTable.toString();
    }

    private static String getServerUrl(InfraAirlockService cache) {
        return getServerUrl(cache, cache.getServers().getCurrentServer());
    }

    private static String getDefaultServerUrl(InfraAirlockService cache) {
        String serverUrl;
        ConnectionManager connectionManager = cache.getConnectionManager();
        if (connectionManager != null && connectionManager.getDataProviderType() == DataProviderType.DIRECT_MODE) {
            serverUrl = cache.getServers().getDefaultServer().getUrl();
        } else {
            serverUrl = cache.getServers().getDefaultServer().getCdnOverride();
        }
        serverUrl = serverUrl.trim();
        return serverUrl.endsWith("/") ? serverUrl : serverUrl + '/';
    }

    private static String getServerUrl(InfraAirlockService cache, @Nullable Servers.Server server) {
        String serverUrl;
        if (server == null) {
            server = cache.getServers().getCurrentServer();
        }
        ConnectionManager connectionManager = cache.getConnectionManager();
        if (connectionManager != null && connectionManager.getDataProviderType() == DataProviderType.DIRECT_MODE) {
            serverUrl = server.getUrl();
        } else {
            serverUrl = server.getCdnOverride();
        }
        serverUrl = serverUrl.trim();
        return serverUrl.endsWith("/") ? serverUrl : serverUrl + '/';
    }

    public static void pullProducts(InfraAirlockService cache, @Nullable Servers.Server server, Callback callbackListener) {
        String productsUrl = String.format(URL_PRODUCT_STRING_FORMAT, getServerUrl(cache, server),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""),
                cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""));
        sendRequest(cache, productsUrl, callbackListener);
    }

    private static Hashtable<String, String> createIdModifiedSinceHeader(String lastModifiedData) {
        Hashtable<String, String> headers = new Hashtable<>();
        if (!lastModifiedData.trim().isEmpty()) {
            headers.put(IF_MODIFIED_SINCE, lastModifiedData);
        }
        return headers;
    }

    /**
     * Data provider modes the airlock supports
     */
    @SuppressWarnings("unused")
    public enum DataProviderType {
        /**
         * Without cache, could be slow but the changes is effected immediately
         */
        DIRECT_MODE(0), //devS3Path
        /**
         * Cached mode fast response, a change delay is expected.
         */
        CACHED_MODE(1); // S3Path = default

        private final int value;

        DataProviderType(final int newValue) {
            value = newValue;
        }

        @SuppressWarnings("unused")
        public static DataProviderType getType(int value) {
            for (DataProviderType type : DataProviderType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return CACHED_MODE;
        }

        public int getValue() {
            return value;
        }
    }

    //callback wrapper
    private static class AirlockDAOCallback implements Callback {
        private final Callback callback;

        private AirlockDAOCallback(Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            Logger.log.e(TAG, String.format(AirlockMessages.LOG_ERROR_WHEN_ACCESSING_URL_FORMATTED, call.request().url()
                    .toString()));
            callback.onFailure(call, e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (response.code() != 200 && response.code() != 304) {
                response.close();
                callback.onFailure(call, new AirlockException(String.format(AirlockMessages.ERROR_RESPONSE_CODE_ERROR_FORMATTED, response.code() + " ,URL :" + call.request().url())));
            } else {
                if (response.code() == 304) {
                    Logger.log.d(TAG, String.format(AirlockMessages.LOG_CONTENT_HASNT_CHANGED_FORMATTED, call.request().url().toString()));
                }
                callback.onResponse(call, response);
            }
        }
    }

    private static void sendRequest(InfraAirlockService cache, Callback callbackListener, String url,String lastTime) {
        AirlockDAOCallback wrapperCallback = new AirlockDAOCallback(callbackListener);
        ConnectionManager connectionManager = cache.getConnectionManager();
        if (connectionManager != null){
            connectionManager.sendRequest(url, createIdModifiedSinceHeader(lastTime), wrapperCallback);
        }
    }

    private static void sendRequest(InfraAirlockService cache, String url, Callback callbackListener){
        ConnectionManager connectionManager = cache.getConnectionManager();
        if (connectionManager != null){
            connectionManager.sendRequest(url, callbackListener);
        }
    }
}
