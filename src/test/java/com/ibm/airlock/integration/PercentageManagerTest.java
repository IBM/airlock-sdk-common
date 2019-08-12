package com.ibm.airlock.integration;

/**
 * @author Denis Voloshin
 */


import com.ibm.airlock.common.percentage.PercentageManager;
import com.ibm.airlock.BaseTestModel;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Denis Voloshin
 */

public class PercentageManagerTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "TestJavaSDK";

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "PercentageManagerTest";
        return getConfigs();
    }


    public PercentageManagerTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        m_ug.add("Adina");
        testHelper.setup(m_appVersion, m_ug, null, null, false, false, false);
        testHelper.pull();
        testHelper.calcSync(null, null);
    }

    @Test
    public void setInRangePercentageTest() {
        testHelper.getManager().getPercentageService().setDeviceInItemPercentageRange(PercentageManager.Sections.FEATURES, "sdk.sdkFeature", true);
        Assert.assertTrue(testHelper.getManager().getPercentageService().isDeviceInItemPercentageRange(PercentageManager.Sections.FEATURES, "sdk.sdkFeature"));

        testHelper.getManager().getPercentageService().setDeviceInItemPercentageRange(PercentageManager.Sections.STREAMS, "testStreamWithPercentage", true);
        Assert.assertTrue(testHelper.getManager().getPercentageService().isDeviceInItemPercentageRange(PercentageManager.Sections.STREAMS, "testStreamWithPercentage"));

        testHelper.getManager().getPercentageService().setDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiment1.variant1", true);
        Assert.assertTrue(testHelper.getManager().getPercentageService().isDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiment1.variant1"));

        testHelper.getManager().getPercentageService().setDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiments.experiment1", true);
        Assert.assertTrue(testHelper.getManager().getPercentageService().isDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiments.experiment1"));


    }

    @Test
    public void setOutRangePercentageTest() {
        testHelper.getManager().getPercentageService().setDeviceInItemPercentageRange(PercentageManager.Sections.FEATURES, "sdk.sdkFeature", false);
        Assert.assertFalse(testHelper.getManager().getPercentageService().isDeviceInItemPercentageRange(PercentageManager.Sections.FEATURES, "sdk.sdkFeature"));

        testHelper.getManager().getPercentageService().setDeviceInItemPercentageRange(PercentageManager.Sections.STREAMS, "testStreamWithPercentage", false);
        Assert.assertFalse(testHelper.getManager().getPercentageService().isDeviceInItemPercentageRange(PercentageManager.Sections.STREAMS, "testStreamWithPercentage"));

        testHelper.getManager().getPercentageService().setDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiment1.variant1", false);
        Assert.assertFalse(testHelper.getManager().getPercentageService().isDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiment1.variant1"));

        testHelper.getManager().getPercentageService().setDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiments.experiment1", false);
        Assert.assertFalse(testHelper.getManager().getPercentageService().isDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiments.experiment1"));

    }
}