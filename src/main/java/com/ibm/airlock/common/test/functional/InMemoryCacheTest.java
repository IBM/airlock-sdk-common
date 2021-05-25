package com.ibm.airlock.common.test.functional;

/**
 * Created by Denis Voloshin on 10/12/2017.
 */


import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.cache.CacheManager;
import com.ibm.airlock.common.test.common.BaseTestModel;
import com.ibm.airlock.common.util.Constants;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Created by iditb on 30/01/2017.
 */

public class InMemoryCacheTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "QA.InMemoryCache";
    private static final String KEY = "PLDSIXEJUOMAXSLB";

    public static String defaults = "{\n" +
            "  \"devS3Path\": \"https://airlockstorage.blob.core.windows.net/dev3runtime/\",\n" +
            "  \"defaultLanguage\": \"en\",\n" +
            "  \"productId\": \"6f78a13c-5ed6-40bb-964a-aece53b9a3c2\",\n" +
            "  \"s3Path\": \"https://airlockstorage.blob.core.windows.net/dev3runtime\",\n" +
            "  \"supportedLanguages\": [\n" +
            "    \"en\"\n" +
            "  ],\n" +
            "  \"seasonId\": \"a5c41f94-bd8f-4d3c-ae9f-bc2dcfe47099\",\n" +
            "  \"root\": {\n" +
            "    \"features\": [\n" +
            "      {\n" +
            "        \"features\": [\n" +
            "          {\n" +
            "            \"features\": [],\n" +
            "            \"defaultIfAirlockSystemIsDown\": false,\n" +
            "            \"name\": \"F11\",\n" +
            "            \"namespace\": \"ns\",\n" +
            "            \"type\": \"FEATURE\",\n" +
            "            \"uniqueId\": \"add9cd51-249a-4322-8f91-e15d79752331\",\n" +
            "            \"defaultConfiguration\": null,\n" +
            "            \"noCachedResults\": false\n" +
            "          }\n" +
            "        ],\n" +
            "        \"defaultIfAirlockSystemIsDown\": false,\n" +
            "        \"name\": \"F1\",\n" +
            "        \"namespace\": \"ns\",\n" +
            "        \"type\": \"FEATURE\",\n" +
            "        \"uniqueId\": \"49142713-b3d4-4d3a-990d-f600012f9762\",\n" +
            "        \"defaultConfiguration\": null,\n" +
            "        \"noCachedResults\": false\n" +
            "      },\n" +
            "      {\n" +
            "        \"features\": [\n" +
            "          {\n" +
            "            \"features\": [],\n" +
            "            \"defaultIfAirlockSystemIsDown\": false,\n" +
            "            \"name\": \"F22\",\n" +
            "            \"namespace\": \"ns\",\n" +
            "            \"type\": \"FEATURE\",\n" +
            "            \"uniqueId\": \"5df3c153-0e65-4f79-9e96-96eeb328dd50\",\n" +
            "            \"defaultConfiguration\": null,\n" +
            "            \"noCachedResults\": false\n" +
            "          }\n" +
            "        ],\n" +
            "        \"defaultIfAirlockSystemIsDown\": false,\n" +
            "        \"name\": \"F2\",\n" +
            "        \"namespace\": \"ns\",\n" +
            "        \"type\": \"FEATURE\",\n" +
            "        \"uniqueId\": \"9ffe1fb1-3052-4c75-89e2-68f375487cb1\",\n" +
            "        \"defaultConfiguration\": null,\n" +
            "        \"noCachedResults\": false\n" +
            "      }\n" +
            "    ],\n" +
            "    \"type\": \"ROOT\",\n" +
            "    \"uniqueId\": \"6875798b-a8cf-41c0-8f1a-59fb4362cb62\"\n" +
            "  },\n" +
            "  \"version\": \"V2.5\",\n" +
            "  \"productName\": \"QA.InMemoryCache\"\n" +
            "}";

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "InMemoryCacheTest";
        return getConfigs();
    }


    public InMemoryCacheTest(String adminUrl, String serverUrl, String productName, String version) throws Exception {
        super(adminUrl, serverUrl, productName, version, KEY);
        m_ug = new ArrayList<>();
        m_ug.add("QA");
        testHelper.customSetUp(m_version, KEY, m_ug, null, null, false, true, false);
    }


    @Test
    public void inMemoryCachedFeaturesMapTest() throws IOException, AirlockInvalidFileException, JSONException, AirlockNotInitializedException, InterruptedException {
        testHelper.customSetUp(m_version, KEY, m_ug, null, null, false, true, false, defaults);
        testHelper.pull();
        testHelper.calcSync(null, null);
        Assert.assertNotNull(testHelper.getManager().getCacheManager().getCachedSyncFeatureList().get(CacheManager.SYNC_FEATURE_MAP));
        Assert.assertNotNull(testHelper.getManager().getCacheManager().getCachedPreSyncFeatureList().get(CacheManager.PRE_SYNC_FEATURE_MAP));

        Assert.assertEquals(true, testHelper.getManager().getCacheManager().getFeature("ns.F1").isOn());
        Assert.assertEquals(false, testHelper.getManager().getCacheManager().getFeature("ns.F11").isOn());
        Thread.sleep(30000);
        Assert.assertNull(testHelper.getManager().getCacheManager().getCachedSyncFeatureList().get(CacheManager.SYNC_FEATURE_MAP));
        Assert.assertNull(testHelper.getManager().getCacheManager().getCachedPreSyncFeatureList().get(CacheManager.PRE_SYNC_FEATURE_MAP));

        Assert.assertEquals(true, testHelper.getManager().getCacheManager().getFeature("ns.F1").isOn());
        Assert.assertEquals(false, testHelper.getManager().getCacheManager().getFeature("ns.F11").isOn());
    }
}