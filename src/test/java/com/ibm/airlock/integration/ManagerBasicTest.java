package com.ibm.airlock.integration;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.net.RemoteConfigurationAsyncFetcher;
import com.ibm.airlock.BaseTestModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;



/**
 * @author Denis Voloshin
 */

public class ManagerBasicTest extends BaseTestModel {

    private static String VERSION = "7.8";

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "ManagerBasicTest";
        return getConfigs();
    }


    public ManagerBasicTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        testHelper.setup(m_appVersion, m_ug, null, null, false, false, false);

    }

    private String m_failMessage = null;
    private String m_successMessage = null;

    private void pullCalcSync() throws AirlockNotInitializedException, InterruptedException {
        testHelper.pull();
        testHelper.calcSync(null, null);
    }


    @Test
    public void getFeaturesConfigurationTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.recreateClient(VERSION);
        pullCalcSync();
        AirlockProductManager manager = testHelper.getManager();
        try {
            JSONObject confs = manager.getFeaturesService().getFeaturesConfigurationFromServer();
            Assert.assertTrue("A NULL was returned from manager.getFeaturesConfigurationFromServer() method", confs != null);
            Assert.assertTrue(manager.getProductInfoService().getProductId().equals(confs.get("productId")));
            Assert.assertTrue(manager.getProductInfoService().getSeasonId().equals(confs.get("seasonId")));
            JSONObject root = (JSONObject) confs.get("root");
            JSONArray features = (JSONArray) root.get("features");
            Assert.assertTrue("Only production features are expected", features.length() == 8);
        } catch (JSONException e) {
            Assert.fail(e.getClass() + ": " + e.getMessage());
        }
    }

    @Test
    public void getRootFeaturesTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.recreateClient(VERSION);
        AirlockProductManager manager = testHelper.getManager();
        List<Feature> features = manager.getFeaturesService().getRootFeatures();
        Assert.assertTrue("NULL was returned from getRootFeatures() method", features != null);
        //parent, f1, f2, no.cache.f
        Assert.assertTrue(features.size() >= 4);
    }

    @Ignore
    @Test
    public void getAirlockVersionTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {

        AirlockProductManager manager = testHelper.getManager();
        Assert.assertTrue("NULL was returned from getAirlockVersion() method", manager.getProductInfoService().getAirlockVersion() != null);
        testHelper.recreateClient("7.10");
        pullCalcSync();
        try {
            JSONObject confs = manager.getFeaturesService().getFeaturesConfigurationFromServer();
            Assert.assertTrue("A NULL was returned from manager.getFeaturesConfigurationFromServer() method", confs != null);
            Assert.assertTrue("The conf version and getAirlockVersion result are not the same", confs.get("version").equals(manager.getProductInfoService().getAirlockVersion()));
            //validate update
            testHelper.recreateClient("7.10");
            pullCalcSync();
            confs = manager.getFeaturesService().getFeaturesConfigurationFromServer();
            Assert.assertTrue("A NULL was returned from manager.getFeaturesConfigurationFromServer() method", confs != null);
            // Assert.assertTrue("The conf version and getAirlockVersion result are not the same",confs.get("version").equals(manager.getAirlockVersion()));
        } catch (JSONException e) {
            Assert.fail(e.getClass() + ": " + e.getMessage());
        }
    }


    //TODO
    //Add get root features after pull-calc-sync test (verify against the server)
    @Ignore
    @Test
    public void getRootFeaturesAfterSyncTest() {
        final AirlockProductManager manager = testHelper.getManager();
        try {
            testHelper.recreateClient(VERSION);
        } catch (Exception e) {
            Assert.fail("An exception was thrown when trying to init sdk. Message: " + e.getMessage());
        }
        List<Feature> features = manager.getFeaturesService().getRootFeatures();
        //parent, f1, f2, no.cache.f
        Assert.assertTrue(features.size() >= 4);

        //Now pull-calc-sync
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            manager.getFeaturesService().pullFeatures(new AirlockCallback() {
                @Override
                public void onFailure(Exception e) {
                    m_failMessage = e.getMessage();
                    latch.countDown();
                }

                @Override
                public void onSuccess(String msg) {
                    try {
                        manager.getFeaturesService().calculateFeatures((JSONObject) null, (JSONObject) null);
                        manager.getFeaturesService().syncFeatures();
                        latch.countDown();
                    } catch (JSONException e) {
                        m_failMessage = e.getMessage();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail("An InterruptedException was thrown when waiting to pull features. Message: " + e.getMessage());
        }

        if (m_failMessage != null) {
            Assert.fail(m_failMessage);
        }
        features = manager.getFeaturesService().getRootFeatures();
        //parent, f1, f2, no.cache.f
        Assert.assertTrue(features.size() >= 5);
    }

    @Test
    public void dataProviderModeTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.setup(m_appVersion, m_ug, null, null, false, false, false);
        Assert.assertTrue("Direct mode should not be the default", testHelper.getManager().getDataProviderType() != RemoteConfigurationAsyncFetcher.DataProviderType.DIRECT_MODE);
    }
}
