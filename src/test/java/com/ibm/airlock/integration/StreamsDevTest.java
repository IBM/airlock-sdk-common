package com.ibm.airlock.integration;

import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.engine.ScriptInitException;
import com.ibm.airlock.BaseTestModel;
import com.ibm.airlock.common.util.Constants;

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


/**
 * @author Denis Voloshin
 */

public class StreamsDevTest extends BaseTestModel {

    private static String VERSION = "8.0.1";

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "StreamsDevTest";
        return getConfigs();
    }


    public StreamsDevTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        m_ug.add("AndroidDEV");
        testHelper.setup(m_appVersion, m_ug, null, null, false, false, false);
        testHelper.pull();
    }

    @Test
    @Ignore
    public void featuresUsageBasicTest() throws JSONException, ScriptInitException, AirlockNotInitializedException {

        String streams = testHelper.getManager().getInfraAirlockService().getPersistenceHandler().read(Constants.SP_FEATURE_USAGE_STREAMS, "");
        int counter = 0;
        while (streams.isEmpty() && counter < 10) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            streams = testHelper.getManager().getInfraAirlockService().getPersistenceHandler().read(Constants.SP_FEATURE_USAGE_STREAMS, "");
            counter++;
        }

        Assert.assertTrue(!streams.isEmpty());

        JSONArray events = new JSONArray();

        String eventJsonString = "{\n" +
                "  \"dateTime\": 1499837541338,\n" +
                "  \"eventData\": {\n" +
                "    \"id\": \"3fd41b84-6216-4d68-9246-05202b71af12\",\n" +
                "    \"source\": \"rightnowmodel\",\n" +
                "    \"pos\": 0,\n" +
                "    \"playlist\": \"pl-crazimals\",\n" +
                "    \"playMethod\": \"user\",\n" +
                "    \"caption\": \"Time for a Baby Goat vs Chicken Smackdown!\",\n" +
                "    \"thumbnailURL\": \"https://dsx.weather.com//util/image/v/GOAT_VS_CHICKEN_1280x720_14718533645.jpg?v\\u003dat\\u0026w\\u003d650\\u0026h\\u003d366\\u0026api\\u003d7db9fe61-7414-47b5-9871-e17d87b8b6a0\",\n" +
                "    \"adSecs\": 0,\n" +
                "    \"contentSecs\": 32,\n" +
                "    \"watchedSecs\": 7,\n" +
                "    \"adWatchedSecs\": 0,\n" +
                "    \"adClicked\": false\n" +
                "  },\n" +
                "  \"name\": \"video-played\"\n" +
                "}";
        events.put(new JSONObject(eventJsonString));
        eventJsonString = "{\n" +
                "  \"dateTime\": 1500988091011,\n" +
                "  \"eventData\": {\n" +
                "    \"clicked\": false,\n" +
                "    \"slot\": \"weather.feed1\",\n" +
                "    \"successful\": false,\n" +
                "    \"type\": \"BAN\"\n" +
                "  },\n" +
                "  \"name\": \"ad-viewed\"\n" +
                "}";
        events.put(new JSONObject(eventJsonString));
        testHelper.getManager().getStreamsService().calculateAndSaveStreams(events, true);


        // allow in memory cache to be expired, to check if it will be loaded correctly
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertNull(testHelper.getManager().getStreamsService().getStreamByName("videoPlayedCounter").getCacheForTest());
        Assert.assertNull(testHelper.getManager().getStreamsService().getStreamByName("videoPlayedCounter").getResultForTest());
        String featureUsageSummary = testHelper.getManager().getStreamsService().getStreamsSummary();
        JSONObject featureUsageSummaryJson = new JSONObject(featureUsageSummary);
        testHelper.getManager().getStreamsService().persist();
        Assert.assertTrue(featureUsageSummaryJson.opt("videoPlayedCounter") != null);
    }
}
