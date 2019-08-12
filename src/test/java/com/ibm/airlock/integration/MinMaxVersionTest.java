package com.ibm.airlock.integration;

/**
 * @author Denis Voloshin
 */


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;
import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.BaseTestModel;



/**
 * @author Denis Voloshin
 */

public class MinMaxVersionTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "QA.MaxVersionProduct";
    private static final String KEY = "2NHYW33IJZDXFBR3";


    String m_failMessage;
    String m_successMessage;


    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "MinMaxVersionTest";
        return getConfigs();
    }


    public MinMaxVersionTest(String adminUrl, String serverUrl, String productName, String version) throws Exception {
        super(adminUrl, serverUrl, productName, version, KEY);
        m_ug = new ArrayList<>();
    }

    private void minVersionTest(String version, boolean expectException, boolean clearCache) {

        AirlockProductManager manager = testHelper.getManager();
        try {
            //Product version should be >= 7.6 (Mismatch season bug was here - should be verified)
            String defaultFile = testHelper.getDataFileContent("test_data/defaults/airlock_defaults_qa_max_version_product_78.json");
            testHelper.setup(version, KEY, m_ug, null, null, false, true, false, defaultFile);
            if (clearCache) {
                manager.getInfraAirlockService().clearRuntimeData();
            }
        } catch (Exception e) {
            Assert.fail("Was unable to parse default file. " + e.getMessage());
        }

        final CountDownLatch latch = new CountDownLatch(1);
        try {
            manager.getFeaturesService().pullFeatures(new AirlockCallback() {
                @Override
                public void onFailure(Exception e) {
                    m_failMessage = e.getClass().getName() + ": " + e.getMessage();
                    latch.countDown();
                }

                @Override
                public void onSuccess(String msg) {
                    m_successMessage = msg;
                    latch.countDown();
                }
            });
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail("An InterruptedException was thrown when waiting to pull features. Message: " + e.getMessage());
        }

        if (expectException) {
            Assert.assertTrue(m_failMessage != null);
        }
        if (expectException) {
            Assert.assertTrue("Mismatch season exception is expected", m_failMessage.contains
                    ("AirlockMismatchSeasonException"));
        }

    }

    @Test
    public void minVersionTwiceBugVerification() throws IOException, AirlockInvalidFileException, InterruptedException, AirlockNotInitializedException {
        minVersionTest("7.0", true, false);
        minVersionTest("7.0", true, false);
    }

    @Test
    public void fetchNewSeasonTest() throws IOException, AirlockInvalidFileException, JSONException, AirlockNotInitializedException, InterruptedException {

        m_ug.add("QA");
        testHelper.setM_appVersion("7.8");
        testHelper.setup("7.8",KEY, m_ug, null, null, false, true, false);

        testHelper.pull();

        testHelper.calcSync(null, null);
        AirlockProductManager manager = testHelper.getManager();

        Assert.assertTrue("f1 feature should return from the server", manager.getFeaturesService().getFeature("ns.f1").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("f1 feature should be ON", manager.getFeaturesService().getFeature("ns.f1").isOn());

        testHelper.setM_appVersion("8.0");
        testHelper.setup("8.0",KEY, m_ug, null, null, false, true, false);

        testHelper.pull();
        testHelper.calcSync(null, null);
        manager = testHelper.getManager();
        Set<String> features = manager.getInfraAirlockService().getSyncFeatureList().getFeatures().keySet();
        for (String name : features) {
            Feature f = manager.getFeaturesService().getFeature(name);
            Assert.assertTrue("SERVER source is expected for all features. Got: " + f.getSource() + " for " + f.getName(), f.getSource().equals(Feature.Source.SERVER));
        }
        Assert.assertTrue("f1 feature should be ON", manager.getFeaturesService().getFeature("ns.f1").isOn());
        //  Assert.assertTrue("sf1 feature should be ON",manager.getFeature("ns.sf1").isOn());
        //Only one from f2,f3 should be on
        if (manager.getFeaturesService().getFeature("ns.f2").isOn()) {
            Assert.assertTrue(!manager.getFeaturesService().getFeature("ns.f3").isOn());
        }
        if (manager.getFeaturesService().getFeature("ns.f3").isOn()) {
            Assert.assertTrue(!manager.getFeaturesService().getFeature("ns.f2").isOn());
        }
        //The most important test here is to verify that f6 is in (returned from the server) and ON
        Assert.assertTrue("f6 feature should be in and ON", manager.getFeaturesService().getFeature("ns.f6").isOn());
    }

    private void pullCalcSyncTest(String version) throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        m_ug.add("QA");
        //TODO good test to add a group to the test
        testHelper.setM_appVersion(version);
        testHelper.setup(version, KEY, m_ug, null, null, false, true, true);
        testHelper.pull();
        testHelper.calcSync(null, null);

        //Other features remain the same
        Collection<Feature> features = testHelper.getFeatures().values();


        for (Feature f : features) {
            if (version.compareTo("8.0") < 0) {
                if (f.getName().contains("f6") || f.getName().contains("f7")) {
                    break;
                }
            }
            Assert.assertTrue("SERVER source is expected for all features. Got: " + f.getSource() + " for " + f.getName(), f.getSource().equals(Feature.Source.SERVER));
        }
    }

    private void featureRevertToDevelopSeasonTest(String version) throws IOException, AirlockInvalidFileException, JSONException,
            AirlockNotInitializedException,
            InterruptedException {

        pullCalcSyncTest(version);
        AirlockProductManager manager = testHelper.getManager();

        //f6 was revert to development and no user group is associated with it
        Assert.assertTrue("f6 Feature should be OFF", !manager.getFeaturesService().getFeature("ns.f6").isOn());
        Assert.assertTrue("f6 Feature should contain trace info indicating no user group", manager.getFeaturesService().getFeature("ns.f6").getTraceInfo().contains("device is not associated"));
        //f7 feature was added in production stage, should be in and ON
        Assert.assertTrue("f7 Feature should be ON", manager.getFeaturesService().getFeature("ns.f7").isOn());

        Assert.assertTrue("f1 feature should be ON", manager.getFeaturesService().getFeature("ns.f1").isOn());
        //  Assert.assertTrue("sf1 feature should be ON",manager.getFeature("ns.sf1").isOn());
        //Only one from f2,f3 should be on
        if (manager.getFeaturesService().getFeature("ns.f2").isOn()) {
            Assert.assertTrue(!manager.getFeaturesService().getFeature("ns.f3").isOn());
        }
        if (manager.getFeaturesService().getFeature("ns.f3").isOn()) {
            Assert.assertTrue(!manager.getFeaturesService().getFeature("ns.f2").isOn());
        }
    }

    @Test
    public void fetchNewSeasonRevertToDevelopTest() throws IOException, AirlockInvalidFileException, InterruptedException, AirlockNotInitializedException {
        // featureRevertToDevelopSeasonTest("8.0");
        pullCalcSyncTest("8.0");
        AirlockProductManager manager = testHelper.getManager();
        //f6 is production now
        Assert.assertTrue("f6 Feature should be ON", manager.getFeaturesService().getFeature("ns.f6").isOn());

        Assert.assertTrue("f1 feature should be ON", manager.getFeaturesService().getFeature("ns.f1").isOn());

        //Only one from f2,f3 should be on
        if (manager.getFeaturesService().getFeature("ns.f2").isOn()) {
            Assert.assertTrue(!manager.getFeaturesService().getFeature("ns.f3").isOn());
        }
        if (manager.getFeaturesService().getFeature("ns.f3").isOn()) {
            Assert.assertTrue(!manager.getFeaturesService().getFeature("ns.f2").isOn());
        }
    }

    @Test
    public void sameVersionFetchSeasonTest() throws IOException, AirlockInvalidFileException, InterruptedException, AirlockNotInitializedException {
        featureRevertToDevelopSeasonTest("8.5");
    }

    @Test
    public void fromOldestToNewestTest() throws IOException, AirlockInvalidFileException, InterruptedException, AirlockNotInitializedException {
        pullCalcSyncTest("7.8");
        AirlockProductManager manager = testHelper.getManager();
        //f7 is not in yet
        Assert.assertTrue("f7 Feature should have MISSING source", manager.getFeaturesService().getFeature("ns.f7").getSource().equals(Feature.Source.MISSING));

        Assert.assertTrue("f1 feature should be ON", manager.getFeaturesService().getFeature("ns.f1").isOn());

        //Only one from f2,f3 should be on
        if (manager.getFeaturesService().getFeature("ns.f2").isOn()) {
            Assert.assertTrue(!manager.getFeaturesService().getFeature("ns.f3").isOn());
        }
        if (manager.getFeaturesService().getFeature("ns.f3").isOn()) {
            Assert.assertTrue(!manager.getFeaturesService().getFeature("ns.f2").isOn());
        }

        pullCalcSyncTest("8.12");
        //f7 should be in and on now
        Assert.assertTrue("f7 Feature should have SERVER source", testHelper.getManager().getFeaturesService().getFeature("ns.f7").getSource().equals(Feature.Source.SERVER));

        Assert.assertTrue("f1 feature should be ON", testHelper.getManager().getFeaturesService().getFeature("ns.f1").isOn());

        //Only one from f2,f3 should be on
        if (manager.getFeaturesService().getFeature("ns.f2").isOn()) {
            Assert.assertTrue(!testHelper.getManager().getFeaturesService().getFeature("ns.f3").isOn());
        }
        if (manager.getFeaturesService().getFeature("ns.f3").isOn()) {
            Assert.assertTrue(!testHelper.getManager().getFeaturesService().getFeature("ns.f2").isOn());
        }
    }
}