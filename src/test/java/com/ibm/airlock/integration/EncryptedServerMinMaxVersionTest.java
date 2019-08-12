package com.ibm.airlock.integration;

/**
 * @author Denis Voloshin
 */


import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.BaseTestModel;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;


/**
 * @author Denis Voloshin
 */

public class EncryptedServerMinMaxVersionTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "QA.PullProducts";
    private static final String KEY = "JRQZPGDR5GGD1Q5P";

    public static String defaults = "{\n" +
            "  \"devS3Path\": \"https://airlockstorage.blob.core.windows.net/dev3runtime/\",\n" +
            "  \"defaultLanguage\": \"en\",\n" +
            "  \"productId\": \"720b6ec2-1279-4d03-b50e-10faf1cbcdb6\",\n" +
            "  \"s3Path\": \"https://airlockstorage.blob.core.windows.net/dev3runtime\",\n" +
            "  \"supportedLanguages\": [\n" +
            "    \"en\"\n" +
            "  ],\n" +
            "  \"seasonId\": \"ec5bb66d-650f-4a57-93e9-915f83621321\",\n" +
            "  \"root\": {\n" +
            "    \"features\": [],\n" +
            "    \"type\": \"ROOT\",\n" +
            "    \"uniqueId\": \"99f51bd7-4a16-4ee9-8dce-939f599f12f2\"\n" +
            "  },\n" +
            "  \"version\": \"V2.5\",\n" +
            "  \"productName\": \"QA.PullProducts\"\n" +
            "}";

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "EncryptedServerMinMaxVersionTest";
        return getConfigs();
    }


    public EncryptedServerMinMaxVersionTest(String adminUrl, String serverUrl, String productName, String version) throws Exception {
        super(adminUrl, serverUrl, productName, version, KEY);
        testHelper.setup(m_appVersion, KEY, m_ug, null, null, false, true, false);
    }


    @Test
    public void fetchNewSeasonTest() throws IOException, AirlockInvalidFileException, JSONException, AirlockNotInitializedException, InterruptedException {
        m_appVersion = "5.0.0";
        testHelper.setM_appVersion(m_appVersion);
        testHelper.setup(m_appVersion, KEY, m_ug, null, null, false, true, false, defaults);
        testHelper.pull();
        testHelper.calcSync(null, null);
        Assert.assertTrue((testHelper.getManager().getLastPullTime().getTime() > 0));

    }

}