package com.ibm.airlock.common.test.long_run.percentage;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.test.common.BaseTestModel;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by iditb on 24/04/17.
 */

public class FeaturePercentageRealTest extends BaseTestModel {

    private final String PRODUCT_NAME = "NoRulePercentageUpgraded";
    private final String FEATURE_NAME = "ns1.R2 L1 60";

    private ArrayList<String> ug = new ArrayList<String>();

    private boolean[] results = new boolean[1000];
    // private JSONObject[] maps = new JSONObject[100];

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "FeaturePercentageRealTest";
        return getConfigs();
    }

    public FeaturePercentageRealTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        ug.add("QA");
        ug.add("DEV");
        ug.add("AndroidDEV");
    }


    private void test(int index) throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        testHelper.customSetUp("7.10", ug, null, null, false, true, false);
        //1.pull-calc-sync
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        // maps[index] = PersistenceHandler.getInstance().getFeaturesRandomMap();
        //This feature is defined to be 100% and should be ON
        AirlockProductManager manager = testHelper.getManager();
        Assert.assertTrue(manager.getFeature("ns1.R1 L1 100").getSource().equals(Feature.Source.SERVER) && manager.getFeature("ns1.R1 L1 100").isOn());
        //These features are defined to be 0% and should be OFF
        Assert.assertTrue(manager.getFeature("ns1.R1 0").getSource().equals(Feature.Source.SERVER) && !manager.getFeature("ns1.R1 0").isOn());
        Assert.assertTrue(manager.getFeature("ns1.R2 0").getSource().equals(Feature.Source.SERVER) && !manager.getFeature("ns1.R2 0").isOn());
        //Now verify and save our feature results
        Assert.assertTrue("Iteration #" + index + ": Feature source is not server", manager.getFeature(FEATURE_NAME).getSource().equals(Feature.Source.SERVER));
        results[index] = manager.getFeature(FEATURE_NAME).isOn();
    }

    @Test
    public void featurePercentageRealTest() throws InterruptedException, AirlockInvalidFileException, AirlockNotInitializedException, IOException {
        // test(0);
        // PersistenceHandler.getInstance().clear();
// android.content.Context mockedContext = Mockito.mock(android.content.Context.class);
// manager.reset(mockedContext);
        for (int i = 0; i < 1000; i++) test(i);
        int count = 0;
        for (int i = 0; i < 1000; i++)
            if (results[i]) count++;
        Assert.assertTrue("Unexpected result: " + count, count > 550);
        Assert.assertTrue("Unexpected result: " + count, count < 620);
    }
}
