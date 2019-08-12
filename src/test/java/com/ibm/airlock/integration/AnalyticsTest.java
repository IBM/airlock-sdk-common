package com.ibm.airlock.integration;

import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;

import com.ibm.airlock.BaseTestModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Denis Voloshin
 */

public class AnalyticsTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "QA.WhiteList";

    public AnalyticsTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        testHelper.setup(m_appVersion, m_ug, null, null, false, true, false);
    }

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "AnalyticsTest";
        return getConfigs();
    }

    @Test
    public void getContextFieldTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.recreateClient("2.0");
        testHelper.pull();
        testHelper.calcSync(null, null);
        // wait for contextFieldsForAnalytics be stored
        Thread.sleep(500);
        JSONArray fields = testHelper.getManager().getAnalyticsService().getContextFieldsForAnalytics();
        Assert.assertTrue(fields != null);
        String fieldsStr = fields.toString().replace("[", "").replace("]", "");
        String[] split = fieldsStr.split(",");
        Assert.assertTrue(split.length == 3);
        Assert.assertTrue(split[0].trim().equals("\"context.userLocation.country\""));
        Assert.assertTrue(split[1].trim().equals("\"context.device.localeCountryCode\""));
        Assert.assertTrue(split[2].trim().equals("\"context.device.datetime\""));
    }

    @Test
    public void getContextFieldOldSeasonTest() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        testHelper.recreateClient("1.0");
        testHelper.pull();
        testHelper.calcSync(null, null);
        Thread.sleep(500);
        JSONArray fields = testHelper.getManager().getAnalyticsService().getContextFieldsForAnalytics();
        Assert.assertTrue(fields != null);
        Assert.assertTrue(fields.length() == 0);
        String fieldsStr = fields.toString().replace("[", "").replace("]", "");
        Assert.assertTrue(fieldsStr.isEmpty());
    }

    @Test
    public void sendToAnalyticsTest() throws AirlockNotInitializedException, AirlockInvalidFileException, InterruptedException, IOException {
        testHelper.recreateClient("2.0");
        testHelper.pull();
        testHelper.calcSync(null, null);
        Assert.assertTrue(testHelper.getManager().getFeaturesService().getFeature("wl.f1").isEnabledForAnalytics());
        Assert.assertTrue(!testHelper.getManager().getFeaturesService().getFeature("wl.f2").isEnabledForAnalytics());
    }

    @Test
    public void sendToAnalyticsOldSeasonTest() throws AirlockNotInitializedException, AirlockInvalidFileException, InterruptedException, IOException {
        testHelper.recreateClient("1.0");
        testHelper.pull();
        testHelper.calcSync(null, null);
        Assert.assertTrue(!testHelper.getManager().getFeaturesService().getFeature("wl.f1").isEnabledForAnalytics());
    }

    @Test
    public void toJsonAnalyticsTest() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        testHelper.recreateClient("2.0");
        testHelper.pull();
        testHelper.calcSync(null, null);
        JSONObject json = testHelper.getManager().getInfraAirlockService().getSyncFeatureList().toJsonObject();
        try {
            JSONObject root = (JSONObject) json.get("root");
            JSONArray features = (JSONArray) root.get("features");
            JSONObject f1 = features.getJSONObject(0);
            JSONArray atts = f1.getJSONArray("configAttributesForAnalytics");
            Assert.assertTrue(atts.get(0).equals("numOfClicks"));
        } catch (JSONException e) {
            Assert.fail(e.getClass() + ": " + e.getMessage());
        }
    }
}
