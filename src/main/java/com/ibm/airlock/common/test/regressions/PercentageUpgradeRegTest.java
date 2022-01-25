package com.ibm.airlock.common.test.regressions;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.test.common.BaseTestModel;
import com.ibm.airlock.common.util.Constants;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by iditb on 27/12/17.
 */

public class PercentageUpgradeRegTest extends BaseTestModel {

    //This product contains old season (7.8) with the old percentage behavior (V2.1)
    //and new season (7.10) with additional features with the new percentage behavior (V2.5)
    private final String PRODUCT_NAME = "NoRulePercentageUpgraded";
    private String[] m_78Features = {"ns1.R1 L1 100", "ns1.R1 0", "ns1.R2 0",
            "ns1.R2 L1 60", "ns1.R32 L2 30", "ns1.R33 L2 10",
            "ns1.R31 L2 50", "ns1.R3 L1 80", "ns1.R41 L2 30",
            "ns1.R42 L2 20", "ns1.R4 L1 40", "ns1.R51 L2 40",
            "ns1.R521 L3 50", "ns1.R522 L3 100", "ns1.R523 L3 20",
            "ns1.R52 L2 40", "ns1.R5 L1 50", "ns1.R2b 0",
            "ns1.R2a 0", "ns1.R3a 0", "ns1.R3b 0", "ns1.R3 0"};
    private String[] m_710AddFeatures = {"new.New Feature1", "ns1.R5 L1 50 New", "ns1.R51 L2 40 New",
            "ns1.R52 L2 40 New", "ns1.R521 L3 50 New", "ns1.R522 L3 100 New",
            "ns1.R3 L1 80 New", "ns1.R31 L2 50 New", "ns1.R32 L2 30 New",
            "ns1.R33 L2 10 New"};

    private ArrayList<String> ug = new ArrayList<String>();

    public PercentageUpgradeRegTest(String adminUrl, String serverUrl, String productName, String version, String key)
            throws IOException, AirlockInvalidFileException {
        super(adminUrl, serverUrl, productName, version, key);
        ug.add("QA");
        ug.add("DEV");
        ug.add("AndroidDEV");
    }

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "PercentageUpgradeRegTest";
        return getConfigs();
    }

    private void verifyAllFeaturesReturnedFromServer(String[] features) {
        Map<String, Feature> featuresFromManager = testHelper.getFeatures();
        for (int i = 0; i < features.length; i++) {
            Assert.assertTrue("Feature " + features[i] + " was not returned from the server", featuresFromManager.get(features[i].toLowerCase()).getSource().equals(Feature.Source.SERVER));
        }
    }


    private void stage1() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        testHelper.reInitSDK("7.8");
        AirlockProductManager manager = testHelper.getManager();
        //Add relevant user group
        manager.setDeviceUserGroups(ug);
        manager.setAllowExperimentEvaluation(false);
        //1.pull-calc-sync
        testHelper.pull();
        testHelper.calcSync(null, null);
        //Verify all expected features returned from the server
        verifyAllFeaturesReturnedFromServer(m_78Features);
        //This feature is defined to be 100% and should be ON
        Assert.assertTrue(manager.getFeature("ns1.R1 L1 100").getSource().equals(Feature.Source.SERVER) && manager.getFeature("ns1.R1 L1 100").isOn());
        //These features are defined to be 0% and should be OFF
        Assert.assertTrue(manager.getFeature("ns1.R1 0").getSource().equals(Feature.Source.SERVER) && !manager.getFeature("ns1.R1 0").isOn());
        Assert.assertTrue(manager.getFeature("ns1.R2 0").getSource().equals(Feature.Source.SERVER) && !manager.getFeature("ns1.R2 0").isOn());

    }

    @Ignore
    @Test
    public void oldToNewSeasonKeepStatusTest() throws InterruptedException, AirlockInvalidFileException, AirlockNotInitializedException, IOException {
        stage1();
        //Keep old season features status
        Map<String, Feature> oldSeasonStatus = new HashMap<String, Feature>(testHelper.getFeatures());
        //Now upgrade
        //Add relevant user group
        testHelper.customSetUp("7.10", ug, null, null, false, true, false);
        //pull-calc-sync
        testHelper.pull();
        testHelper.calcSync(null, null);
        testHelper.getFeatures();
        //First verify all expected features returned from the server
        verifyAllFeaturesReturnedFromServer(m_78Features);
        verifyAllFeaturesReturnedFromServer(m_710AddFeatures);
        //Verify features status (that should remain the same - percentage only grow)
        Map<String, Feature> features = testHelper.getFeatures();
        Assert.assertTrue(features.get("ns1.R1 L1 100".toLowerCase()).getSource().equals(Feature.Source.SERVER) && features.get("ns1.R1 L1 100".toLowerCase()).isOn());
        Assert.assertTrue(features.get("ns1.R1 0".toLowerCase()).getSource().equals(Feature.Source.SERVER) && !features.get("ns1.R1 0".toLowerCase()).isOn());
        Assert.assertTrue(features.get("ns1.R3 L1 80".toLowerCase()).isOn() == oldSeasonStatus.get("ns1.R3 L1 80".toLowerCase()).isOn());
        for (int i = 0; i < m_78Features.length; i++)
            Assert.assertTrue("Feature " + m_78Features[i] + " returned unexpected status.", features.get(m_78Features[i].toLowerCase()).isOn() == oldSeasonStatus.get(m_78Features[i].toLowerCase()).isOn());
    }

    @Ignore
    @Test
    public void oldToNewSeasonIncreasePercentageTest() throws Exception {
        stage1();
        //Keep old season features status


        Map<String, Feature> oldSeasonStatus = new HashMap<String, Feature>();
        oldSeasonStatus.putAll(testHelper.getFeatures());
        //Now upgrade
        //Add relevant user group
        testHelper.customSetUp("7.10", ug, null, null, false, true, false);
        //Now simulate a server change: increase percentage
        String change = testHelper.getDataFileContent("test_data/percentage/increase.json");
        testHelper.sdkChange(Constants.SP_RAW_RULES, change, false);
        //calc-sync
        testHelper.calcSync(null, null);
        //All features except of ns1.R4 L1 40 should remain the same
        Map<String, Feature> features = testHelper.getFeatures();
        for (int i = 0; i < m_78Features.length; i++)
            if (!m_78Features[i].equals("ns1.R4 L1 40"))
                Assert.assertTrue("Feature " + m_78Features[i] + " returned unexpected status. Trace Info: " + features.get(m_78Features[i].toLowerCase()).getTraceInfo(), features.get(m_78Features[i].toLowerCase()).isOn() == oldSeasonStatus.get(m_78Features[i].toLowerCase()).isOn());
        //Feature ns1.R4 L1 40 should be ON
        Assert.assertTrue("Feature ns1.R4 L1 40 should be ON", features.get("ns1.R4 L1 40".toLowerCase()).isOn());
    }

    @Test
    public void oldToNewSeasonDecreasePercentageTest() throws Exception {
        stage1();
        //Keep old season features status
        Map<String, Feature> oldSeasonStatus = testHelper.getFeatures();
        //Feature ns1.R1 L1 100 should be ON
        Assert.assertTrue("Feature ns1.R1 L1 100 should be ON", oldSeasonStatus.get("ns1.R1 L1 100".toLowerCase()).isOn());
        //Now upgrade
        //Add relevant user group
        testHelper.customSetUp("7.10", ug, null, null, false, false, false);//Now simulate a server change: increase percentage
        String change = testHelper.getDataFileContent("test_data/percentage/decrease.json");
        //String change = NegativeSDK.readFromAssets("test_data/percentage/decrease.json");
        testHelper.sdkChange(Constants.SP_RAW_RULES, change, false);
        //calc-sync
        testHelper.calcSync(null, null);
        //All features except of ns1.R4 L1 40 should remain the same
        Map<String, Feature> features = testHelper.getFeatures();
        //for (int i=0;i<m_78Features.length;i++)
        //if (!m_78Features[i].equals("ns1.R1 L1 100"))
        // Assert.assertTrue("Feature "+m_78Features[i]+" returned unexpected status.",features.get(m_78Features[i].toLowerCase()).isOn()
        // ==oldSeasonStatus.get(m_78Features[i].toLowerCase()).isOn());
        //Feature ns1.R1 L1 100 should be OFF
        Assert.assertTrue("Feature ns1.R1 L1 100 should be OFF", !features.get("ns1.R1 L1 100".toLowerCase()).isOn());
    }

    @Test
    public void keepRandomsMapTest() throws InterruptedException, AirlockInvalidFileException, AirlockNotInitializedException, IOException {
        /*
        This method is an important regression test
         */
        stage1();
        PersistenceHandler ph = testHelper.getManager().getDebuggableCache().getPersistenceHandler();
        String randomsMap = ph.getFeaturesRandomMap().toString();
        //2. Pull-calc-sync
        testHelper.pull();
        testHelper.calcSync(null, null);
        Assert.assertTrue("REGRESSION: Randoms map should not change after pull-calc-sync", randomsMap.equals(ph.getFeaturesRandomMap().toString()));
        //3. Upgrade
        testHelper.customSetUp("7.10", ug, null, null, false, false, false);
        testHelper.pull();
        Assert.assertTrue("REGRESSION: Randoms map should not change after upgrade", randomsMap.equals(ph.getFeaturesRandomMap().toString()));
    }

    @Test
    public void notZerosRandomsTest() throws InterruptedException, AirlockInvalidFileException, AirlockNotInitializedException, IOException {
        /*
        This method is an important regression test
         */
        stage1();
        String randomsMap = testHelper.getManager().getDebuggableCache().getPersistenceHandler().getFeaturesRandomMap().toString();
        Assert.assertTrue(randomsMap != null);
        Assert.assertTrue(!randomsMap.replace("{}", "").trim().isEmpty());
        String[] randoms = randomsMap.split(",");
        for (int i = 0; i < randoms.length; i++) {
            String[] parse = randoms[i].split(":");
            Assert.assertTrue("REGRESSION: zero value was found in the randoms map.", parse[1] != "0");
        }
    }
}
