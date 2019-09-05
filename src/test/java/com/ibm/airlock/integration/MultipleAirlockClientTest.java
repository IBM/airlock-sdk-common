package com.ibm.airlock.integration;

import com.ibm.airlock.BaseTestModel;
import com.ibm.airlock.CommonSdkTestDataManager;

import com.ibm.airlock.common.*;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("CharsetObjectCanBeUsed")
public class MultipleAirlockClientTest extends BaseTestModel {

    private static final String DEFAULT_FILES_ROOT_LOCATION = "test_data/defaults";
    private CommonSdkTestDataManager commonSdkTestDataManager;

    @Parameterized.Parameters
    public static Collection<Object[]> params() throws IOException {
        testClassName = "ManagerBasicTest";
        return getConfigs();
    }

    @Before
    public void setUpMockUps() {
        commonSdkTestDataManager = new CommonSdkTestDataManager();
    }


    @Test
    public void multiAirlockClientsFromSameAirlockProductShouldBeIndependent() throws JSONException, AirlockInvalidFileException, InterruptedException {
        AirlockProductSeasonManager airlockProductSeasonManager = new AirlockProductSeasonManager("ManagerBasicTest", slurp(getDefaultFile()), "", m_appVersion);

        AirlockClient client1 = airlockProductSeasonManager.createClient("111-111-111");
        AirlockClient client2 = airlockProductSeasonManager.createClient("111-111-112");

        pull(airlockProductSeasonManager);
        client1.calculateFeatures(new JSONObject("{'viewedLocation':{'lon':'IL'}}"), Collections.<String>emptyList());
        client2.calculateFeatures(new JSONObject("{'viewedLocation':{'lon':'US'}}"), Collections.<String>emptyList());
        client1.syncFeatures();
        client2.syncFeatures();
        Assert.assertFalse(client1.getFeature("ns.f8").isOn());
        Assert.assertTrue(client2.getFeature("ns.f8").isOn());
        client1.getAirlockProductManager().reset();
        client2.getAirlockProductManager().reset();
        airlockProductSeasonManager.destroy();
    }

    @Test
    public void multiAirlockClientsFromDiffAirlockProductShouldBeIndependent() throws JSONException, AirlockInvalidFileException, InterruptedException {

        AirlockProductSeasonManager airlockProductSeasonManager = new AirlockProductSeasonManager("ManagerBasicTest", slurp(getDefaultFile()), "", "9.0");
        AirlockClient client1 = airlockProductSeasonManager.createClient("111-111-111");

        AirlockProductSeasonManager airlockProductSeasonManager1 = new AirlockProductSeasonManager("ManagerBasicTest", slurp(getDefaultFile()), "", "8.0");
        AirlockClient client2 = airlockProductSeasonManager1.createClient("111-111-112");


        pull(airlockProductSeasonManager1);
        pull(airlockProductSeasonManager);
        client1.calculateFeatures(new JSONObject("{'viewedLocation':{'lon':'IL'}}"), Collections.<String>emptyList());
        client2.calculateFeatures(new JSONObject("{'viewedLocation':{'lon':'US'}}"), Collections.<String>emptyList());
        client1.syncFeatures();
        client2.syncFeatures();
        Assert.assertFalse(client1.getFeature("ns.f8").isOn());
        Assert.assertTrue(client2.getFeature("ns.f8").isOn());
        client1.getAirlockProductManager().reset();
        client2.getAirlockProductManager().reset();
        airlockProductSeasonManager.destroy();
    }

    private void pull(AirlockProductSeasonManager airlockProductSeasonManager) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final List<Exception> error = new ArrayList<>();
        airlockProductSeasonManager.pullFeatures(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                error.add(e);
                latch.countDown();
            }

            @Override
            public void onSuccess(String msg) {
                latch.countDown();
            }
        });
        latch.await();
        if (!error.isEmpty()) {
            Assert.fail(error.get(0).getMessage());
        }
    }

    @SuppressWarnings("ConstructorWithTooManyParameters")
    public MultipleAirlockClientTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
    }


    @SuppressWarnings("MagicCharacter")
    private InputStream getDefaultFile() throws JSONException {
        try {
            String defaultsAsString = getDataFileContent(DEFAULT_FILES_ROOT_LOCATION + File.separator +
                    m_productName + '_' + m_appVersion + ".json");
            if (defaultsAsString.isEmpty()) {
                Assert.fail('[' + DEFAULT_FILES_ROOT_LOCATION + File.separator +
                        m_productName + '_' + m_appVersion + ".json] not found");
            }
            return new ByteArrayInputStream(defaultsAsString.getBytes("UTF-8"));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }


    @SuppressWarnings("CharsetObjectCanBeUsed")
    private static String slurp(final InputStream is) {
        final char[] buffer = new char[1024];
        final StringBuilder out = new StringBuilder();
        try (Reader in = new InputStreamReader(is, "UTF-8")) {
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0) {
                    break;
                }
                out.append(buffer, 0, rsz);
            }
        } catch (IOException ignored) {
            /* ... */
        }
        return out.toString();
    }

    private String getDataFileContent(String pathInDataFolder) throws IOException {
        return commonSdkTestDataManager.getFileContent(pathInDataFolder);
    }

}
