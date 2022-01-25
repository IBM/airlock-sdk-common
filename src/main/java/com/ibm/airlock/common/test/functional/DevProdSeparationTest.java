package com.ibm.airlock.common.test.functional;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.net.AirlockDAO;
import com.ibm.airlock.common.test.common.BaseTestModel;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Created by Denis Voloshin on 25/12/2017.
 */

public class DevProdSeparationTest extends BaseTestModel {

    private static String PRODUCT_NAME = "QA.SimpleProduct";
    private static String VERSION = "7.10";
    private String s3Path, devS3Path, seasonId, productId;

    private static final String URL_REFRESH_FEATURES_STRING_FORMAT = "%sseasons/%s/%s/AirlockRuntime%s.json"; //s3path, productId, seasonId, dev/prod
    //s3path, productId, seasonId,language, (can be empty)country, dev/prod
    private static final String URL_TRANSLATIONS_STRING_FORMAT = "%sseasons/%s/%s/translations/strings__%s%s%s.json";

    private static final String URL_PROD_SUFIX = "PRODUCTION";
    private static final String URL_DEV_SUFIX = "DEVELOPMENT";

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "DevProdSeparationTest";
        return getConfigs();
    }


    public DevProdSeparationTest(String adminUrl, String serverUrl, String productName, String version, String key)
            throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        super(adminUrl, serverUrl, productName, version, key);
        testHelper.customSetUp(m_version, null, null, null, true, true, true);
        testHelper.pull();
        parseFile();
    }

    private void parseFile() {
        try {
            JSONObject json = new JSONObject(testHelper.slurp(testHelper.getDefaultFile(), 1024));
            seasonId = json.optString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
            productId = json.optString(Constants.JSON_FEATURE_FIELD_PRODUCT_ID);
            s3Path = json.optString(Constants.JSON_FEATURE_FIELD_CACHED_S3PATH);
            if (!s3Path.endsWith("/")) {
                s3Path = s3Path + "/";
            }
            devS3Path = json.optString(Constants.JSON_FEATURE_FIELD_DIRECT_S3PATH);
        } catch (JSONException e) {
            Assert.fail("Can't parse default file");
        }
    }


    @Test
    public void testDevProdSeparationTest() {

        AirlockProductManager manager = testHelper.getManager();

        String airlockPullFeaturesUrl;
        String pullFeaturesUrl;
        String airlockPullTransUrl;
        String pullTransUrl;

        manager.setDataProviderType(AirlockDAO.DataProviderType.CACHED_MODE);

        // -----------mode cached, no user groups (prod)
        airlockPullFeaturesUrl = AirlockDAO.getPullFeaturesUrl(manager.getDebuggableCache());
        pullFeaturesUrl = String.format(URL_REFRESH_FEATURES_STRING_FORMAT, s3Path, productId, seasonId, URL_PROD_SUFIX);
        Assert.assertEquals(airlockPullFeaturesUrl, pullFeaturesUrl);

        airlockPullTransUrl = AirlockDAO.getPullTranslationsUrl(manager.getDebuggableCache(), "en", "us");
        pullTransUrl = String.format(URL_TRANSLATIONS_STRING_FORMAT, s3Path, productId, seasonId, "en", "us", URL_PROD_SUFIX);
        Assert.assertEquals(airlockPullTransUrl, pullTransUrl);

        // ----------mode direct, no user groups (prod)
        manager.setDataProviderType(AirlockDAO.DataProviderType.DIRECT_MODE);

        airlockPullFeaturesUrl = AirlockDAO.getPullFeaturesUrl(manager.getDebuggableCache());
        pullFeaturesUrl = String.format(URL_REFRESH_FEATURES_STRING_FORMAT, devS3Path, productId, seasonId, URL_PROD_SUFIX);
        Assert.assertEquals(airlockPullFeaturesUrl, pullFeaturesUrl);

        airlockPullTransUrl = AirlockDAO.getPullTranslationsUrl(manager.getDebuggableCache(), "en", "us");
        pullTransUrl = String.format(URL_TRANSLATIONS_STRING_FORMAT, devS3Path, productId, seasonId, "en", "us", URL_PROD_SUFIX);
        Assert.assertEquals(airlockPullTransUrl, pullTransUrl);

        List<String> userGroups = new ArrayList<>();
        userGroups.add("QA");
        manager.setDeviceUserGroups(userGroups);
        manager.setDataProviderType(AirlockDAO.DataProviderType.CACHED_MODE);

        // mode cached, with user groups (prod)
        airlockPullFeaturesUrl = AirlockDAO.getPullFeaturesUrl(manager.getDebuggableCache());
        pullFeaturesUrl = String.format(URL_REFRESH_FEATURES_STRING_FORMAT, s3Path, productId, seasonId, URL_DEV_SUFIX);
        Assert.assertEquals(airlockPullFeaturesUrl, pullFeaturesUrl);

        airlockPullTransUrl = AirlockDAO.getPullTranslationsUrl(manager.getDebuggableCache(), "en", "us");
        pullTransUrl = String.format(URL_TRANSLATIONS_STRING_FORMAT, s3Path, productId, seasonId, "en", "us", URL_DEV_SUFIX);
        Assert.assertEquals(airlockPullTransUrl, pullTransUrl);

        // mode direct, with user groups (prod)
        manager.setDataProviderType(AirlockDAO.DataProviderType.DIRECT_MODE);
        airlockPullFeaturesUrl = AirlockDAO.getPullFeaturesUrl(manager.getDebuggableCache());
        pullFeaturesUrl = String.format(URL_REFRESH_FEATURES_STRING_FORMAT, devS3Path, productId, seasonId, URL_DEV_SUFIX);
        Assert.assertEquals(airlockPullFeaturesUrl, pullFeaturesUrl);

        airlockPullTransUrl = AirlockDAO.getPullTranslationsUrl(manager.getDebuggableCache(), "en", "us");
        pullTransUrl = String.format(URL_TRANSLATIONS_STRING_FORMAT, devS3Path, productId, seasonId, "en", "us", URL_DEV_SUFIX);
        Assert.assertEquals(airlockPullTransUrl, pullTransUrl);

        // remove user groups - prod mode
        manager.setDeviceUserGroups(new ArrayList<String>());
        manager.setDataProviderType(AirlockDAO.DataProviderType.CACHED_MODE);

        // mode cached, with user groups (prod)
        airlockPullFeaturesUrl = AirlockDAO.getPullFeaturesUrl(manager.getDebuggableCache());
        pullFeaturesUrl = String.format(URL_REFRESH_FEATURES_STRING_FORMAT, s3Path, productId, seasonId, URL_PROD_SUFIX);
        Assert.assertEquals(airlockPullFeaturesUrl, pullFeaturesUrl);

        airlockPullTransUrl = AirlockDAO.getPullTranslationsUrl(manager.getDebuggableCache(), "en", "us");
        pullTransUrl = String.format(URL_TRANSLATIONS_STRING_FORMAT, s3Path, productId, seasonId, "en", "us", URL_PROD_SUFIX);
        Assert.assertEquals(airlockPullTransUrl, pullTransUrl);

    }

}
