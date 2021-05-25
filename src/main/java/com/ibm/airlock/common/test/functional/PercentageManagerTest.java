package com.ibm.airlock.common.test.functional;

/**
 * Created by Denis Voloshin on 10/12/2017.
 */


import com.ibm.airlock.common.cache.PercentageManager;
import com.ibm.airlock.common.test.common.BaseTestModel;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Created by iditb on 30/01/2017.
 */

public class PercentageManagerTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "TestJavaSDK";

    String defaults = "{\n" +
            "	\"defaultLanguage\": \"en\",\n" +
            "	\"devS3Path\": \"https:\\/\\/s3-eu-west-1.amazonaws.com\\/airlockdev\\/DEV4\\/\",\n" +
            "	\"productId\": \"cdd52d55-df5d-4375-ac41-1086e4f1c7a3\",\n" +
            "	\"productName\": \"TestJavaSDK\",\n" +
            "	\"root\": {\n" +
            "		\"features\": [\n" +
            "			{\n" +
            "				\"defaultConfiguration\": null,\n" +
            "				\"defaultIfAirlockSystemIsDown\": false,\n" +
            "				\"features\": [\n" +
            "				],\n" +
            "				\"name\": \"sdkFeature\",\n" +
            "				\"namespace\": \"sdk\",\n" +
            "				\"noCachedResults\": false,\n" +
            "				\"type\": \"FEATURE\",\n" +
            "				\"uniqueId\": \"a6b7d521-c58f-44dc-9464-7901c199b1ec\"\n" +
            "			}\n" +
            "		],\n" +
            "		\"type\": \"ROOT\",\n" +
            "		\"uniqueId\": \"9187e207-b9a4-4b26-a7f4-54c90f605992\"\n" +
            "	},\n" +
            "	\"s3Path\": \"https:\\/\\/s3-eu-west-1.amazonaws.com\\/airlockdev\\/DEV4\\/\",\n" +
            "	\"seasonId\": \"fe26411e-fd45-4fc3-aef0-63dc154bc009\",\n" +
            "	\"supportedLanguages\": [\n" +
            "		\"en\"\n" +
            "	],\n" +
            "	\"version\": \"V2.5\"\n" +
            "}";

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "PercentageManagerTest";
        return getConfigs();
    }


    public PercentageManagerTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        m_ug.add("Adina");
        testHelper.customSetUp(m_version, m_ug, null, null, false, false, false);
        testHelper.pull();
        testHelper.calcSync(null, null);
    }

    @Test
    public void setInRangePercentageTest() {
        testHelper.getManager().setDeviceInItemPercentageRange(PercentageManager.Sections.FEATURES, "sdk.sdkFeature", true);
        Assert.assertTrue(testHelper.getManager().isDeviceInItemPercentageRange(PercentageManager.Sections.FEATURES, "sdk.sdkFeature"));

        testHelper.getManager().setDeviceInItemPercentageRange(PercentageManager.Sections.STREAMS, "testStreamWithPercentage", true);
        Assert.assertTrue(testHelper.getManager().isDeviceInItemPercentageRange(PercentageManager.Sections.STREAMS, "testStreamWithPercentage"));

        testHelper.getManager().setDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiment1.variant1", true);
        Assert.assertTrue(testHelper.getManager().isDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiment1.variant1"));

        testHelper.getManager().setDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiments.experiment1", true);
        Assert.assertTrue(testHelper.getManager().isDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiments.experiment1"));


    }

    @Test
    public void setOutRangePercentageTest() {
        testHelper.getManager().setDeviceInItemPercentageRange(PercentageManager.Sections.FEATURES, "sdk.sdkFeature", false);
        Assert.assertFalse(testHelper.getManager().isDeviceInItemPercentageRange(PercentageManager.Sections.FEATURES, "sdk.sdkFeature"));

        testHelper.getManager().setDeviceInItemPercentageRange(PercentageManager.Sections.STREAMS, "testStreamWithPercentage", false);
        Assert.assertFalse(testHelper.getManager().isDeviceInItemPercentageRange(PercentageManager.Sections.STREAMS, "testStreamWithPercentage"));

        testHelper.getManager().setDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiment1.variant1", false);
        Assert.assertFalse(testHelper.getManager().isDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiment1.variant1"));

        testHelper.getManager().setDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiments.experiment1", false);
        Assert.assertFalse(testHelper.getManager().isDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, "experiments.experiment1"));

    }
}