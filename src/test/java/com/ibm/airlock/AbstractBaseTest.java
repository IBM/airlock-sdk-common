package com.ibm.airlock;

import com.ibm.airlock.common.*;
import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.SeasonPersistenceHandler;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.net.RemoteConfigurationAsyncFetcher;
import com.ibm.airlock.common.net.BaseOkHttpClientBuilder;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.net.OkHttpConnectionManager;
import com.ibm.airlock.common.streams.AirlockStream;
import com.ibm.airlock.common.util.AirlockVersionComparator;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;

/**
 * @author Denis Voloshin
 */

public abstract class AbstractBaseTest {

    public static final String URL_AIRLOCK_DEFAULTS_STRING_FORMAT = "%sseasons/%s/%s/AirlockDefaults.json";
    public static final String ADMIN_URL_AIRLOCK_DEFAULTS_STRING_FORMAT = "%s/products/seasons/%s/defaults";


    protected AirlockProductSeasonManager airlockProductSeasonManager;
    protected AirlockProductManager manager;
    protected Context mockedContext;
    protected AirlockClient airlockClient;

    private static Hashtable<String, ByteArrayInputStream> m_defaultFiles = new Hashtable<String, ByteArrayInputStream>();
    private static Hashtable<String, JSONObject> seasons = new Hashtable<String, JSONObject>();

    protected String m_serverUrl;
    protected String m_adminUrl;
    protected String m_productName;
    protected String m_appVersion;
    protected String m_key;

    public void setup(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, JSONException, AirlockInvalidFileException {
        m_adminUrl = adminUrl;
        m_serverUrl = serverUrl;
        m_productName = productName;
        m_appVersion = version;
        m_key = key;
        setup(m_appVersion, m_key, null, "en", null, true, true, true);
    }

    public void setup(String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException, JSONException {
        m_serverUrl = serverUrl;
        m_productName = productName;
        m_appVersion = version;
        m_key = key;
        setup(m_appVersion, m_key, null, "en", null, true, true, true);
    }

    public void setup(String serverUrl, String productName, String version) throws IOException, AirlockInvalidFileException, JSONException {
        m_serverUrl = serverUrl;
        m_productName = productName;
        m_appVersion = version;
        m_key = "";
        setup(m_appVersion, m_key, null, "en", null, true, true, true);
    }

    public void recreateClient(String version) throws IOException, AirlockInvalidFileException {
        m_appVersion = version;
        setup(version, m_key, null, null, null, true, true, true);
    }

    public void setup(String version, ArrayList<String> groups, String locale, String randoms, boolean setUpMockups, boolean reset, boolean cleanStreams) throws IOException, AirlockInvalidFileException, JSONException {
        setup(version, null, groups, locale, randoms, setUpMockups, reset, cleanStreams);
    }

    public void setup(String version, String key, ArrayList<String> groups, String locale,
                      String randoms, boolean setUpMockUps, boolean reset, boolean cleanStreams, String defaults) throws IOException, JSONException, AirlockInvalidFileException {
        //set up mocks
        if (setUpMockUps) {
            setUpMockUps();
        }
        //reset
        if (reset) {
            //manager.reset();
        }
        //set user groups
        if (groups != null) {
            manager.getUserGroupsService().setDeviceUserGroups(groups);
        }
        //set locale
        if (locale != null) {
            Locale.setDefault(new Locale(locale));
        }
        //write required randoms
        if (randoms != null) {
            manager.getInfraAirlockService().getPersistenceHandler().write(Constants.SP_RANDOMS, randoms);
        }
        //clean streams
        if (cleanStreams) {
            manager.getStreamsService().clearAllStreams();
        }
    }

    public void setup(String version, String key, ArrayList<String> groups, String locale, String randoms, boolean setUpMockups, boolean reset, boolean cleanStreams) throws IOException, AirlockInvalidFileException, JSONException {
        //set up mocks
        if (setUpMockups) {
            setUpMockUps();
        }
        //reset
        if (reset) {
            manager.reset();
        }
        //set user groups
        if (groups != null) {
            manager.getUserGroupsService().setDeviceUserGroups(groups);
        }
        //set locale
        if (locale != null) {
            Locale.setDefault(new Locale(locale));
        }
        //write required randoms
        if (randoms != null) {
            manager.getInfraAirlockService().getPersistenceHandler().write(Constants.SP_RANDOMS, randoms);
        }
        //clean streams
        if (cleanStreams) {
            manager.getStreamsService().clearAllStreams();
        }
    }

    public static String slurp(final InputStream is, final int bufferSize) {
        if (is == null) {
            return "";
        }
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        try (Reader in = new InputStreamReader(is, "UTF-8")) {
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0) {
                    break;
                }
                out.append(buffer, 0, rsz);
            }
        } catch (UnsupportedEncodingException ex) {
            /* ... */
        } catch (IOException ex) {
            /* ... */
        }
        return out.toString();
    }


    private String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public abstract void setUpMockUps() throws JSONException, AirlockInvalidFileException;

    public InputStream getDefaultFile() throws JSONException {
        String s3Path = m_serverUrl;
        if (s3Path != null) {
            if (!s3Path.trim().endsWith("/")) {
                s3Path += "/";
            }
        } else {
            Assert.fail("s3 path is null");
        }

        String adminUrl = m_adminUrl;
        if (adminUrl != null) {
            if (!adminUrl.trim().endsWith("/")) {
                adminUrl += "/";
            }
        }


        JSONObject season = null;
        if (seasons.containsKey(m_productName + s3Path + m_appVersion)) {
            System.out.print("Product Name: [" + m_productName + "] " + s3Path + m_appVersion + " is found");
            season = seasons.get(m_productName + s3Path + m_appVersion);
        } else {
            try {
                JSONArray seasonsArray = getSeasonsByProductNameFromS3(m_productName, adminUrl == null ? (s3Path + "products.json") : (adminUrl + "products/seasons"));
                if (seasonsArray == null) {
                    Assert.fail("No season found for product:" + m_productName + " version:" + m_appVersion);
                }
                for (int i = 0; i < seasonsArray.length(); i++) {
                    JSONObject seasonJSON = seasonsArray.getJSONObject(i);
                    if (verifySeason(seasonJSON.optString("minVersion", ""), seasonJSON.optString("maxVersion", "null"), m_appVersion)) {
                        season = seasonJSON;
                    }
                }
            } catch (JSONException e) {
                Assert.fail(stackTraceToString(e));
            }
        }

        if (season != null) {
            seasons.put(m_productName + s3Path + m_appVersion, season);
            String productID = season.getString("productId");
            String seasonID = season.getString("uniqueId");
            String remoteDefaultUrl = getDefaultsFileUrl(m_adminUrl, m_serverUrl, productID, seasonID);

            if (m_defaultFiles.containsKey(remoteDefaultUrl)) {
                ByteArrayInputStream ba = m_defaultFiles.get(remoteDefaultUrl);
                ba.reset();
                return ba;
            }
            // check if the default file stream is empty (means that there is no default file found for the url)
            ByteArrayInputStream defaultFile = new ByteArrayInputStream(getJSONStreamFromS3(remoteDefaultUrl).getBytes());
            m_defaultFiles.put(remoteDefaultUrl, defaultFile);
            return defaultFile;
        } else {
            Assert.fail("season was not determined for " + m_productName + s3Path + m_appVersion);
            return null;
        }
    }

    private String getDefaultsFileUrl(String adminUrl, String s3Path, String productId, String seasonId) {
        if (adminUrl == null) {
            return String.format(URL_AIRLOCK_DEFAULTS_STRING_FORMAT, s3Path, productId, seasonId);
        } else {
            return String.format(ADMIN_URL_AIRLOCK_DEFAULTS_STRING_FORMAT, adminUrl, seasonId);
        }

    }

    private String getJSONStreamFromS3(String remoteURL) {
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder sb = new StringBuilder();


        ConnectionManager okHttpConnectionManager = null;
        if (m_key != null) {
            okHttpConnectionManager = getConnectionManager(m_key);
        } else {
            okHttpConnectionManager = getConnectionManager();
        }

        okHttpConnectionManager.sendRequest(remoteURL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    sb.append(response.body().string());
                    response.body().close();
                }
                latch.countDown();
            }
        }, getAuthHeaders("sdkTestsKey", "DQpqkM/BNfU="));
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    protected ConnectionManager getConnectionManager() {
        return new OkHttpConnectionManager(new BaseOkHttpClientBuilder());
    }

    protected ConnectionManager getConnectionManager(String m_key) {
        return new OkHttpConnectionManager(new BaseOkHttpClientBuilder(), m_key);
    }

    private boolean verifySeason(String minVersion, String maxVersion, String productVersion) {
        AirlockVersionComparator comparator = new AirlockVersionComparator();
        if (comparator.compare(minVersion, productVersion) <= 0) {
            if (maxVersion.equalsIgnoreCase("null") ||
                    comparator.compare(maxVersion, productVersion) > 0) {
                return true;
            }
        }
        return false;
    }


    @CheckForNull
    private JSONArray getSeasonsByProductNameFromS3(String productName, String productsUrl) throws JSONException {
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder sb = new StringBuilder();
        ConnectionManager okHttpConnectionManager = null;
        if (m_key != null) {
            okHttpConnectionManager = getConnectionManager(m_key);
        } else {
            okHttpConnectionManager = getConnectionManager();
        }

        okHttpConnectionManager.sendRequest(productsUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                latch.countDown();
                Assert.fail(stackTraceToString(e));

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    sb.append(response.body().string());
                    response.body().close();
                }
                latch.countDown();
            }
        }, getAuthHeaders("sdkTestsKey", "DQpqkM/BNfU="));
        try {
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        JSONObject products = new JSONObject(sb.toString());
        JSONArray productsArray = products.getJSONArray("products");
        for (int i = 0; i < productsArray.length(); i++) {
            JSONObject product = productsArray.getJSONObject(i);
            if (product.get("name").equals(productName)) {
                return product.getJSONArray("seasons");
            }
        }
        return null;
    }

    private Headers getAuthHeaders(String key, String keyPassword) {

        JSONObject body = new JSONObject();
        body.put("key", key);
        body.put("keyPassword", keyPassword);

        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder sb = new StringBuilder();
        ConnectionManager okHttpConnectionManager = null;
        if (m_key != null) {
            okHttpConnectionManager = getConnectionManager(m_key);
        } else {
            okHttpConnectionManager = getConnectionManager();
        }

        String productsUrl = m_adminUrl + "/authentication/startSessionFromKey";

        okHttpConnectionManager.sendPostRequestAsJson(productsUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                latch.countDown();
                Assert.fail(stackTraceToString(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    sb.append(response.body().string());
                    response.body().close();
                }
                latch.countDown();
            }
        }, body.toString());
        try {
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        Hashtable headers = new Hashtable();
        headers.put("sessionToken", sb.toString());
        return Headers.of(headers);
    }

    public void setDevelopmentBranch(String branchId, String branchName) throws InterruptedException {
        manager.getInfraAirlockService().getPersistenceHandler().setDevelopBranchName(branchName);
        final CountDownLatch latch = new CountDownLatch(1);

        final List<Exception> error = new ArrayList<>();
        RemoteConfigurationAsyncFetcher.pullBranchById(manager.getInfraAirlockService(), branchId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                error.add(e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //read the response to the string
                if (response.body() == null || response.body().toString().isEmpty() || !response.isSuccessful()) {

                    if (response.body() != null) {
                        response.body().close();
                    }
                    error.add(new Exception("Branch has any model"));
                    latch.countDown();
                }

                //parse server response,the response has to be in json format
                try {
                    final JSONObject branchesFullResponse = new JSONObject(response.body().string());
                    response.body().close();
                    manager.getInfraAirlockService().getPersistenceHandler().setDevelopBranch(branchesFullResponse.toString());
                    latch.countDown();
                } catch (JSONException e) {
                    // Do nothing - stay with old copy
                }
            }
        });

        latch.await();

        if (error.size() > 0) {
            Assert.fail(error.get(0).getMessage());
        }
    }


    public void pull() throws AirlockNotInitializedException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final List<Exception> error = new ArrayList<>();
        airlockProductSeasonManager.pullFeatures(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                error.add(e);
                latch.countDown();
            }

            @Override
            public void onSuccess(String s) {
                latch.countDown();
            }
        });
        latch.await();

        if (error.size() > 0) {
            Assert.fail(error.get(0).getMessage());
        }
    }

    public void calcWithProductIdsAndSync(@Nullable JSONObject airlockContext, @Nullable Collection<String> purchaseIds) throws AirlockNotInitializedException, JSONException {
        manager.getFeaturesService().calculateFeatures(airlockContext, purchaseIds);
        manager.getFeaturesService().syncFeatures();
    }

    public void calcSync(@Nullable JSONObject profile, @Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException {
        manager.getFeaturesService().calculateFeatures(profile, airlockContext);
        manager.getFeaturesService().syncFeatures();
    }

    public Map<String, Feature> getFeatures() {
        return manager.getInfraAirlockService().getSyncFeatureList().getFeatures();
    }

    public void sdkChange(String constantKey, String toWrite, boolean updateStreams) throws AirlockNotInitializedException {
        manager.getInfraAirlockService().getPersistenceHandler().write(constantKey, toWrite);
        if (updateStreams) {
            manager.getStreamsService().updateStreams();
        }
    }

    public AirlockProductSeasonManager getAirlockProductSeasonManager() {
        return airlockProductSeasonManager;
    }

    public AirlockProductManager getManager() {
        return manager;
    }

    public String processStreamsEvents(JSONArray events) {
        manager.getStreamsService().addStreamsEvent(events, true);
        return manager.getStreamsService().getStreamsSummary();
    }

    @CheckForNull
    public AirlockStream getStreamByName(String name) throws AirlockNotInitializedException {
        return manager.getStreamsService().getStreamByName(name);
    }

    public void reset() {
        manager.reset();
    }

    /*
    Test model related methods
     */
    public abstract String getDataFileContent(String pathInDataFolder) throws IOException;

    public abstract String[] getDataFileNames(String directoryPathInDataFolder) throws IOException;

    public abstract void setLocale(Locale locale);

//    public Context getContext() {
//        return mockedContext;
//    }

    public void setM_appVersion(String m_appVersion) {
        this.m_appVersion = m_appVersion;
    }
}
