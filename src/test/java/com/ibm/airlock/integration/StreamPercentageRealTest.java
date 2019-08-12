package com.ibm.airlock.integration;

import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.BaseTestModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Denis Voloshin
 */

public class StreamPercentageRealTest extends BaseTestModel {


    private int resultsCounter = 0;

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "StreamPercentageRealTest";
        return getConfigs();
    }


    public StreamPercentageRealTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        m_ug.add("AndroidDEV");
    }

    private void test(int index) throws Exception {
        testHelper.setup(m_appVersion, m_ug, null, null, true, true, true);
        testHelper.pull();
        String event = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        AirlockProductManager manager = testHelper.getManager();
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event));
        manager.getStreamsService().addStreamsEvent(events, true);
        String str = manager.getStreamsService().getStreamsSummary();
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        try {
            JSONObject json = new JSONObject(str);
            JSONObject json_event = json.optJSONObject("percentage_stream_qa_test");
            if (json_event != null && json_event.getInt("count") == 1) {
                resultsCounter++;
            }
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
    }

    @Test
    public void percentage100DevicesTest() throws Exception {
        for (int i = 0; i < 100; i++) {
            test(i);
        }

        Assert.assertTrue("Unexpected result: " + resultsCounter, resultsCounter > 50);
        Assert.assertTrue("Unexpected result: " + resultsCounter, resultsCounter < 70);
    }
}
