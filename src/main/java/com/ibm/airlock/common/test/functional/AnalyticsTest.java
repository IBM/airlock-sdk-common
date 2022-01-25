package com.ibm.airlock.common.test.functional;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.test.common.BaseTestModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by iditb on 25/04/17.
 */

public class AnalyticsTest extends BaseTestModel {


    public static final String PRODUCT_NAME = "QA.WhiteList";
    private static String VERSION = "2.0";

    public AnalyticsTest(String adminUrl ,String serverUrl, String productName, String version,String key) throws Exception{
        super(adminUrl, serverUrl, productName, version, key);
        testHelper.customSetUp(m_version,m_ug,null,null,false,true,false);
    }


    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "AnalyticsTest";
        return getConfigs();
    }

    @Test
    public void getContextFieldTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.reInitSDK("2.0");
        testHelper.pull();
        testHelper.calcSync(null,null);
        // wait for contextFieldsForAnalytics be stored
        Thread.sleep(500);
        JSONArray fields = testHelper.getManager().getContextFieldsForAnalytics();
        Assert.assertTrue(fields!=null);
        String fieldsStr = fields.toString().replace("[","").replace("]","");
        String[] split = fieldsStr.split(",");
        Assert.assertTrue(split.length==3);
        Assert.assertTrue(split[0].trim().equals("\"context.userLocation.country\""));
        Assert.assertTrue(split[1].trim().equals("\"context.device.localeCountryCode\""));
        Assert.assertTrue(split[2].trim().equals("\"context.device.datetime\""));
    }

    @Test
    public void getContextFieldOldSeasonTest() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        testHelper.reInitSDK("1.0");
        testHelper.pull();
        testHelper.calcSync(null,null);
        Thread.sleep(500);
        JSONArray fields = testHelper.getManager().getContextFieldsForAnalytics();
        Assert.assertTrue(fields!=null);
        Assert.assertTrue(fields.length()==0);
        String fieldsStr = fields.toString().replace("[","").replace("]","");
        Assert.assertTrue(fieldsStr.isEmpty());
    }

    @Test
    public void sendToAnalyticsTest() throws AirlockNotInitializedException, AirlockInvalidFileException, InterruptedException, IOException {
        testHelper.reInitSDK("2.0");
        testHelper.pull();
        testHelper.calcSync(null,null);
        Assert.assertTrue(testHelper.getManager().getFeature("wl.f1").isEnabledForAnalytics());
        Assert.assertTrue(!testHelper.getManager().getFeature("wl.f2").isEnabledForAnalytics());
    }

    @Test
    public void sendToAnalyticsOldSeasonTest() throws AirlockNotInitializedException, AirlockInvalidFileException, InterruptedException, IOException{
        testHelper.reInitSDK("1.0");
        testHelper.pull();
        testHelper.calcSync(null,null);
        Assert.assertTrue(!testHelper.getManager().getFeature("wl.f1").isEnabledForAnalytics());
    }

    @Test
    public void toJsonAnalyticsTest() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        testHelper.reInitSDK("2.0");
        testHelper.pull();
        testHelper.calcSync(null,null);
        JSONObject json = testHelper.getManager().getDebuggableCache().getSyncFeatureList().toJsonObject();
        try {
            JSONObject root = (JSONObject) json.get("root");
            JSONArray features = (JSONArray) root.get("features");
            JSONObject f1 = features.getJSONObject(0);
            JSONArray atts = f1.getJSONArray("configAttributesForAnalytics");
            Assert.assertTrue(atts.get(0).equals("numOfClicks"));
        } catch (JSONException e) {
            Assert.fail(e.getClass()+": "+e.getMessage() );
        }
    }
}
