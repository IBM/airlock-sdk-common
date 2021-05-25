package com.ibm.airlock.common.net;

import com.ibm.airlock.common.AirlockException;
import com.ibm.airlock.common.cache.CacheManager;
import com.ibm.airlock.common.data.Servers;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Constants;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

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
public class AirlockDAO {

    //indicates whether any of remote config file has been changed
    public static final String URL_PRODUCT_CHANGED_FILE = "%sseasons/%s/%s/productionChanged.txt";

    private static final String URL_REFRESH_FEATURES_STRING_FORMAT = "%sseasons/%s/%s/AirlockRuntime%s.json"; //s3path, productId, seasonId, dev/prod
    private static final String URL_REFRESH_JS_UTILS_STRING_FORMAT = "%sseasons/%s/%s/AirlockUtilities%s.txt"; //s3path, productId, seasonId, dev/prod
    private static final String URL_DOWNLOAD_DEFAULTS_STRING_FORMAT = "%sseasons/%s/%s/AirlockDefaults.json"; //s3path, productId, seasonId

    private static final String URL_PRODUCT_STRING_FORMAT = "%sseasons/%s/%s/productRuntime.json";//s3path
    //private static final String URL_PRODUCT_STRING_FORMAT = "%sproducts.json";//s3path
    private static final String URL_USER_GROUPS_STRING_FORMAT = "%sseasons/%s/%s/AirlockUserGroupsRuntime.json"; //s3path seasonId
    private static final String URL_BRANCHES_FORMAT = "%sseasons/%s/%s/AirlockBranchesRuntime.json";//s3path seasonId
    private static final String URL_BRANCH_FORMAT = "%sseasons/%s/%s/branches/%s/AirlockRuntimeBranch%s.json";//s3path seasonId

    private static final String URL_SERVER_LIST_STRING_FORMAT = "%sops/airlockServers.json";//s3path
    private static final String URL_FEATURE_USAGE_RULES_STRING_FORMAT = "%sseasons/%s/%s/AirlockStreams%s.json";//s3path
    private static final String URL_FEATURE_USAGE_UTILS_STRING_FORMAT = "%sseasons/%s/%s/AirlockStreamsUtilities%s.txt";//s3path
    private static final String URL_NOTIFICATIONS_STRING_FORMAT = "%sseasons/%s/%s/AirlockNotifications%s.json";//s3path
    //s3path, productId, seasonId,language, (can be empty)country, dev/prod
    private static final String URL_TRANSLATIONS_STRING_FORMAT = "%sseasons/%s/%s/translations/strings__%s%s%s.json";
    private static final String URL_PROD_SUFIX = "PRODUCTION";
    private static final String URL_DEV_SUFIX = "DEVELOPMENT";
    private final static String TAG = "airlock.AirlockDAO";
    private final static String IF_MODIFIED_SINCE = "If-Modified-Since";
    //by default the data comes from the cached channel

    public static void pullFeaturesConfiguration(CacheManager cache, final Callback callbackListener) {
        String url = getPullFeaturesUrl(cache);
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");        AirlockDAOCallback wrapperCallback = new AirlockDAOCallback(callbackListener);
        cache.getConnectionManager().sendRequest(url, createIdModifiedSinceHeader(lastTime), wrapperCallback);
    }

    public static void pullServerList(CacheManager cache, Callback callbackListener) {
        String productsUrl = String.format(URL_SERVER_LIST_STRING_FORMAT, getDefaultServerUrl(cache));
        cache.getConnectionManager().sendRequest(productsUrl, callbackListener);
    }

    public static void pullStreams(CacheManager cache, Callback callbackListener) {
        String url = getPullStreamsUrl(cache);
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, "");        AirlockDAOCallback wrapperCallback = new AirlockDAOCallback(callbackListener);
        cache.getConnectionManager().sendRequest(getPullStreamsUrl(cache), createIdModifiedSinceHeader(lastTime), wrapperCallback);
    }

    public static void pullFeatureUsageUIUtils(CacheManager cache, Callback callbackListener) {
        String url =getPullStreamsUtilitiesUrl(cache);
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, "");        AirlockDAOCallback wrapperCallback = new AirlockDAOCallback(callbackListener);
        cache.getConnectionManager().sendRequest(getPullStreamsUtilitiesUrl(cache), createIdModifiedSinceHeader(lastTime), wrapperCallback);
    }

    public static void pullNotifications(CacheManager cache, Callback callbackListener) {
        String url =getPullNotificationsUrl(cache);
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_NOTIFICATIONS_FULL_DOWNLOAD_TIME, "");        AirlockDAOCallback wrapperCallback = new AirlockDAOCallback(callbackListener);
        cache.getConnectionManager().sendRequest(getPullNotificationsUrl(cache), createIdModifiedSinceHeader(lastTime), wrapperCallback);
    }

    public static void pullDefaultFile(CacheManager cache, Servers.Server server, Servers.Product product, Callback callbackListener) {
        String pullDefaultUtl = String.format(URL_DOWNLOAD_DEFAULTS_STRING_FORMAT, getServerUrl(cache, server),
                product.getProductId(), product.getSeasonId());
        cache.getConnectionManager().sendRequest(pullDefaultUtl, callbackListener);
    }

    public static void pullUserGroups(CacheManager cache, Callback callbackListener) {
        String url = String.format(URL_USER_GROUPS_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""),
                cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""));
        cache.getConnectionManager().sendRequest(url, callbackListener);
    }

    public static void pullBranches(CacheManager cache, Callback callbackListener) {
        String url = String.format(URL_BRANCHES_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""),
                cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""));
        cache.getConnectionManager().sendRequest(url, callbackListener);
    }

    public static void pullBranchById(CacheManager cache, String branchId, Callback callbackListener) {
        String url = String.format(URL_BRANCH_FORMAT, getServerUrl(cache), cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""),
                cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), branchId, getUrlSufix(cache));
        cache.getConnectionManager().sendRequest(url, callbackListener);
    }

    public static void checkIfProductChanged(CacheManager cache, String server, String productId, String sessionId, Callback callbackListener) {
        String translationsUrl = String.format(URL_PRODUCT_CHANGED_FILE, server,
                productId, sessionId);
        AirlockDAOCallback wrapperCallback = new AirlockDAOCallback(callbackListener);
        cache.getConnectionManager().sendRequest(translationsUrl, createIdModifiedSinceHeader(""), wrapperCallback);
    }

    public static void checkIfProductChanged(CacheManager cache, Callback callbackListener) {

        String translationsUrl = String.format(URL_PRODUCT_CHANGED_FILE, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""));
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_TIME_PRODUCT_CHANGED, "");        AirlockDAOCallback wrapperCallback = new AirlockDAOCallback(callbackListener);
        cache.getConnectionManager().sendRequest(translationsUrl, createIdModifiedSinceHeader(lastTime), wrapperCallback);
    }

    public static void pullJSUtils(CacheManager cache, Callback callbackListener) {
        String jsUtilsUrl = String.format(URL_REFRESH_JS_UTILS_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), getUrlSufix(cache));
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, "");        AirlockDAOCallback wrapperCallback = new AirlockDAOCallback(callbackListener);
        cache.getConnectionManager().sendRequest(jsUtilsUrl, createIdModifiedSinceHeader(lastTime), wrapperCallback);
    }

    public static void pullTranslationsTable(CacheManager cache, String language, String country, Callback callbackListener) {
        String url = getPullTranslationsUrl(cache, language, country);
        String lastTime = cache.getPersistenceHandler().read(Constants.SP_LAST_TRANS_FULL_DOWNLOAD_TIME, "");        AirlockDAOCallback wrapperCallback = new AirlockDAOCallback(callbackListener);
        cache.getConnectionManager().sendRequest(url, createIdModifiedSinceHeader(lastTime), wrapperCallback);
    }

    //public for tests
    public static String getPullFeaturesUrl(CacheManager cache) {
        return String.format(URL_REFRESH_FEATURES_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), getUrlSufix(cache));
    }

    //public for tests
    public static String getPullStreamsUrl(CacheManager cache) {
        return String.format(URL_FEATURE_USAGE_RULES_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), getUrlSufix(cache));
    }

    //public for tests
    public static String getPullStreamsUtilitiesUrl(CacheManager cache) {
        return String.format(URL_FEATURE_USAGE_UTILS_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), getUrlSufix(cache));
    }

    //public for tests
    public static String getPullNotificationsUrl(CacheManager cache) {
        return String.format(URL_NOTIFICATIONS_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), getUrlSufix(cache));
    }

    //public for tests
    public static String getPullTranslationsUrl(CacheManager cache, String language, String country) {
        return String.format(URL_TRANSLATIONS_STRING_FORMAT, getServerUrl(cache),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""), cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""), language, country, getUrlSufix(cache));
    }

    private static String getUrlSufix(CacheManager cache) {
        List<String> userGroups = cache.getPersistenceHandler().getDeviceUserGroups();
        if (userGroups.isEmpty()) {
            return URL_PROD_SUFIX;
        }
        return URL_DEV_SUFIX;
    }


    public static String pullTranslationsTable(final CacheManager cache, final String language, final String country) {

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
                if (responseBody.equals("")) {
                    completeBlock.countDown();
                    return;
                }

                stringsTable.append(responseBody);
                completeBlock.countDown();
            }
        };
        cache.getConnectionManager().sendRequest(getPullTranslationsUrl(cache, language, country), createIdModifiedSinceHeader(""), wrapperCallback);

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

    private static String getServerUrl(CacheManager cache) {
        return getServerUrl(cache, cache.getServers().getCurrentServer());
    }

    private static String getDefaultServerUrl(CacheManager cache) {
        String serverUrl;
        if (cache.getConnectionManager().getDataProviderType() == DataProviderType.DIRECT_MODE) {
            serverUrl = cache.getServers().getDefaultServer().getUrl();
        } else {
            serverUrl = cache.getServers().getDefaultServer().getCdnOverride();
        }
        serverUrl = serverUrl.trim();
        return serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
    }

    private static String getServerUrl(CacheManager cache, @Nullable Servers.Server server) {
        String serverUrl;
        if (server == null) {
            server = cache.getServers().getCurrentServer();
        }
        if (cache.getConnectionManager().getDataProviderType() == DataProviderType.DIRECT_MODE) {
            serverUrl = server.getUrl();
        } else {
            serverUrl = server.getCdnOverride();
        }
        serverUrl = serverUrl.trim();
        return serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
    }

    private static String getAmazonS3UrlOld(CacheManager cache) {
        String amazonS3Url;
        if (cache.getConnectionManager().getDataProviderType() == DataProviderType.DIRECT_MODE) {
            amazonS3Url = cache.getPersistenceHandler().read(Constants.SP_DIRECT_S3PATH, "");
        } else {
            amazonS3Url = cache.getPersistenceHandler().read(Constants.SP_CACHED_S3PATH, "");
        }
        amazonS3Url = amazonS3Url.trim();
        return amazonS3Url.endsWith("/") ? amazonS3Url : amazonS3Url + "/";
    }

    public static void pullProducts(CacheManager cache, @Nullable Servers.Server server, Callback callbackListener) {
        String productsUrl = String.format(URL_PRODUCT_STRING_FORMAT, getServerUrl(cache, server),
                cache.getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, ""),
                cache.getPersistenceHandler().read(Constants.SP_SEASON_ID, ""));
        cache.getConnectionManager().sendRequest(productsUrl, callbackListener);
    }

    private static Hashtable<String, String> createIdModifiedSinceHeader(String lastModifiedData) {
        Hashtable<String, String> headers = new Hashtable<>();
        if (lastModifiedData != null && !lastModifiedData.trim().equals("")) {
            headers.put(IF_MODIFIED_SINCE, lastModifiedData);
        }
        return headers;
    }

    /**
     * Data provider modes the airlock supports
     */
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
}
