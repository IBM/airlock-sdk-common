package com.ibm.airlock.common.test.functional;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.net.AirlockDAO;
import com.ibm.airlock.common.test.common.BaseTestModel;

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

import edu.umd.cs.findbugs.annotations.NonNull;


/**
 * Created by Denis Voloshin on 25/12/2017.
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
        testHelper.customSetUp(m_version, m_ug, null, null, false, false, false);

    }

    private String m_failMessage = null;
    private String m_successMessage = null;

    private void pullCalcSync() throws AirlockNotInitializedException, InterruptedException {
        testHelper.pull();
        testHelper.calcSync(null, null);
    }


    @Test
    public void getFeaturesConfigurationTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.reInitSDK(VERSION);
        pullCalcSync();
        AirlockProductManager manager = testHelper.getManager();
        try {
            JSONObject confs = manager.getFeaturesConfigurationFromServer();
            Assert.assertTrue("A NULL was returned from manager.getFeaturesConfigurationFromServer() method", confs != null);
            Assert.assertTrue(manager.getProductId().equals(confs.get("productId")));
            Assert.assertTrue(manager.getSeasonId().equals(confs.get("seasonId")));
            JSONObject root = (JSONObject) confs.get("root");
            JSONArray features = (JSONArray) root.get("features");
            Assert.assertTrue("Only production features are expected", features.length() == 8);
        } catch (JSONException e) {
            Assert.fail(e.getClass() + ": " + e.getMessage());
        }
    }

    @Test
    public void getRootFeaturesTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.reInitSDK(VERSION);
        AirlockProductManager manager = testHelper.getManager();
        List<Feature> features = manager.getRootFeatures();
        Assert.assertTrue("NULL was returned from getRootFeatures() method", features != null);
        //parent, f1, f2, no.cache.f
        Assert.assertTrue(features.size() >= 4);
    }

    @Ignore
    @Test
    public void getAirlockVersionTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {

        AirlockProductManager manager = testHelper.getManager();
        Assert.assertTrue("NULL was returned from getAirlockVersion() method", manager.getAirlockVersion() != null);
        testHelper.reInitSDK("7.10");
        pullCalcSync();
        try {
            JSONObject confs = manager.getFeaturesConfigurationFromServer();
            Assert.assertTrue("A NULL was returned from manager.getFeaturesConfigurationFromServer() method", confs != null);
            Assert.assertTrue("The conf version and getAirlockVersion result are not the same", confs.get("version").equals(manager.getAirlockVersion()));
            //validate update
            testHelper.reInitSDK("7.10");
            pullCalcSync();
            confs = manager.getFeaturesConfigurationFromServer();
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
            testHelper.reInitSDK(VERSION);
        } catch (Exception e) {
            Assert.fail("An exception was thrown when trying to init sdk. Message: " + e.getMessage());
        }
        List<Feature> features = manager.getRootFeatures();
        //parent, f1, f2, no.cache.f
        Assert.assertTrue(features.size() >= 4);

        //Now pull-calc-sync
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            manager.pullFeatures(new AirlockCallback() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    m_failMessage = e.getMessage();
                    latch.countDown();
                }

                @Override
                public void onSuccess(String msg) {
                    try {
                        manager.calculateFeatures((JSONObject) null, (JSONObject) null);
                        manager.syncFeatures();
                        latch.countDown();
                    } catch (AirlockNotInitializedException e) {
                        m_failMessage = e.getMessage();
                    } catch (JSONException e) {
                        m_failMessage = e.getMessage();
                    }
                }
            });
        } catch (AirlockNotInitializedException e) {
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
        features = manager.getRootFeatures();
        //parent, f1, f2, no.cache.f
        Assert.assertTrue(features.size() >= 5);
    }

    @Test
    public void dataProviderModeTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.customSetUp(m_version, m_ug, null, null, false, false, false);
        Assert.assertTrue("Direct mode should not be the default", testHelper.getManager().getDataProviderType() != AirlockDAO.DataProviderType.DIRECT_MODE);
    }

//    @Test
//    public void minVersionNegativeTest(){
//        AirlockManager manager = AirlockManager.getInstance() ;
//        int defaults = com.weather.airlock.sdk.test.R.raw.airlock_defaults_qa_product;
//        try {
//            manager.reInitSDK(mockedContext, 0, "7.0");
//        } catch (Exception e) {
//            Assert.fail("Was unable to init the SDK");
//        }
//        m_failMessage = null ;
//        final CountDownLatch latch = new CountDownLatch(1);
//        try{
//            manager.pullFeatures(new AirlockCallback() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    m_failMessage = e.getClass().getName();
//                    latch.countDown();
//                }
//
//                @Override
//                public void onSuccess(@NonNull String msg) {
//                    m_successMessage = msg ;
//                    latch.countDown();
//                }
//            });
//        }catch(AirlockNotInitializedException e){
//            Assert.fail(e.getMessage());
//        }
//
//        try {
//            latch.await();
//        } catch (InterruptedException e) {
//            Assert.fail("An InterruptedException was thrown when waiting to pull features. Message: " + e.getMessage());
//        }
//
//        Assert.assertTrue(m_failMessage!=null);
//        Assert.assertTrue("Mismatch season exception is expected when product version is lower then required.",m_failMessage.contains("AirlockMismatchSeasonException"));
//    }

}
