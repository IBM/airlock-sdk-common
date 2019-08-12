package com.ibm.airlock.integration;


import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.BaseTestModel;
import com.ibm.airlock.common.util.Constants;


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
public class StreamsQATest extends BaseTestModel {

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "StreamsQATest";
        return getConfigs();
    }


    public StreamsQATest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        m_ug.add("AndroidDEV");
        m_ug.add("Rachel");
        testHelper.setup(m_appVersion, m_ug, null, null, false, false, false);
        testHelper.pull();
    }

    //todo add enable disable

    @Test
    public void setGetFeatureUsageTest() throws IOException, JSONException {
        String event = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event));
        String str = testHelper.processStreamsEvents(events);
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        try {
            JSONObject json = new JSONObject(str);
            JSONObject json_event = (JSONObject) json.get("video_played_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("sum should be 1. Got: " + sum, sum == 1);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
    }

    @Test
    public void resultCountTest() throws IOException, JSONException {
        JSONArray events = new JSONArray();
        String event = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        int index = 0;
        char c = 'a';
        for (int i = 0; i < 10; i++) {
            JSONObject json = new JSONObject(event);
            long time = json.getLong("dateTime");
            time++;
            json = json.put("dateTime", time);
            JSONObject eventData = json.getJSONObject("eventData");
            String id = eventData.getString("id");
            id = id.replace(id.charAt(index), c);
            index++;
            eventData = eventData.putOpt("id", id);
            json = json.put("eventData", eventData);
            events.put(json);
        }
        String res = testHelper.processStreamsEvents(events);
        Assert.assertTrue("NULL was returned from getFeatureUsageSummary method after add", res != null);
        Assert.assertTrue("Empty string was returned from getFeatureUsageSummary method after add", !res.isEmpty());
        try {
            JSONObject json = new JSONObject(res);
            JSONObject json_event = (JSONObject) json.get("video_played_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("sum should be 10. Got: " + sum, sum == 10);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
    }

    @Test
    public void differentEventTypesTest() throws IOException, JSONException {
        String event1 = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        String event2 = testHelper.getDataFileContent("test_data/bar_events/event2.json");
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event1));
        events.put(new JSONObject(event2));
        String str = testHelper.processStreamsEvents(events);
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        try {
            JSONObject json = new JSONObject(str);
            JSONObject json_event = (JSONObject) json.get("video_played_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("sum should be 1. Got: " + sum, sum == 1);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
    }

    @Test
    public void manyDifferentEventTypesTest() throws IOException, JSONException {
        String event1 = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        JSONObject json1 = new JSONObject(event1);
        String event2 = testHelper.getDataFileContent("test_data/bar_events/event2.json");
        JSONObject json2 = new JSONObject(event2);
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event1));
        events.put(new JSONObject(event2));
        String res = null;
        for (int i = 0; i < 100; i++) {
            res = testHelper.processStreamsEvents(events);
        }

        Assert.assertTrue("NULL was returned from getFeatureUsageSummary method after add", res != null);
        Assert.assertTrue("Empty string was returned from getFeatureUsageSummary method after add", !res.isEmpty());
        try {
            JSONObject json = new JSONObject(res);
            JSONObject json_event = (JSONObject) json.get("video_played_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("sum should be 100. Got: " + sum, sum == 100);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
    }

    @Test
    public void eventPassTwoFiltersTest() throws Exception {
        String event = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event));
        String str = testHelper.processStreamsEvents(events);
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        try {
            JSONObject json = new JSONObject(str);
            JSONObject json_event = (JSONObject) json.get("video_played_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("sum should be 1. Got: " + sum, sum == 1);
            json_event = (JSONObject) json.get("video_played_qa_test_2");
            sum = json_event.getInt("count");
            Assert.assertTrue("sum should be -1. Got: " + sum, sum == -1);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
    }

    @Test
    public void noUserGroupDevStreamTest() throws Exception {
        ArrayList<String> groups = new ArrayList<>();
        testHelper.setup(m_appVersion, groups, null, null, false, false, false);
        String event = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event));
        String str = testHelper.processStreamsEvents(events);
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        Assert.assertTrue("The feature usage summary should not contain dev stream result", !str.contains("video_played_qa_test"));
    }

    @Test
    public void wrongUserGroupDevStreamTest() throws Exception {
        ArrayList<String> groups = new ArrayList<>();
        groups.add("UserGroupThatNotExist");
        testHelper.setup(m_appVersion, groups, null, null, false, false, false);
        String event = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event));
        String str = testHelper.processStreamsEvents(events);
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        Assert.assertTrue("The feature usage summary should not contain dev stream result", !str.contains("video_played_qa_test"));
    }


    @Test
    public void productionStreamTest() throws Exception {
        ArrayList<String> groups = new ArrayList<>();
        testHelper.setup(m_appVersion, groups, null, null, false, false, false);
        String event1 = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        JSONObject json1 = new JSONObject(event1);
        String event2 = testHelper.getDataFileContent("test_data/bar_events/event2.json");
        JSONObject json2 = new JSONObject(event2);
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event1));
        events.put(new JSONObject(event2));
        String res = null;
        for (int i = 0; i < 100; i++) {
            res = testHelper.processStreamsEvents(events);
        }

        Assert.assertTrue("NULL was returned from getFeatureUsageSummary method after add", res != null);
        Assert.assertTrue("Empty string was returned from getFeatureUsageSummary method after add", !res.isEmpty());
        try {
            JSONObject json = new JSONObject(res);
            JSONObject json_event = (JSONObject) json.get("production_stream_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("sum should be 200. Got: " + sum, sum == 200);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
    }

    @Test
    public void devModeProductionStreamServerRegressionTest() throws IOException, JSONException, AirlockNotInitializedException {
        String event1 = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        JSONObject json1 = new JSONObject(event1);
        String event2 = testHelper.getDataFileContent("test_data/bar_events/event2.json");
        JSONObject json2 = new JSONObject(event2);
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event1));
        events.put(new JSONObject(event2));
        String res = null;
        for (int i = 0; i < 100; i++) {
            res = testHelper.processStreamsEvents(events);
        }

        Assert.assertTrue("NULL was returned from getFeatureUsageSummary method after add", res != null);
        Assert.assertTrue("Empty string was returned from getFeatureUsageSummary method after add", !res.isEmpty());
        try {
            JSONObject json = new JSONObject(res);
            JSONObject json_event = (JSONObject) json.get("production_stream_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("REGRESSION: sum should be 200. Got: " + sum, sum == 200);
        } catch (JSONException e) {
            Assert.fail("REGRESSION: getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
    }

    @Test
    public void minStreamVersionTest() throws Exception {
        testHelper.setup("8.0", m_ug, null, null, false, true, true);
        String event = testHelper.getDataFileContent("test_data/bar_events/event1.json");
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event));
        String str = testHelper.processStreamsEvents(events);
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        Assert.assertTrue("Empty json string is expected. Got: " + str, str.equals("{}"));
    }

    @Test
    public void processorExceptionTest() throws Exception {
        String event = testHelper.getDataFileContent("test_data/bar_events/event3.json");
        JSONArray events = new JSONArray();
        events.put(new JSONObject(event));
        String str = testHelper.processStreamsEvents(events);
        //Verify that the first event pass the processor as expected
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        try {
            JSONObject json = new JSONObject(str);
            JSONObject json_event = (JSONObject) json.get("processor_exception_stream_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("sum should be 1. Got: " + sum, sum == 1);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
        //Make a change that cause the event to fail the processing (throwable exception is expected)
        JSONObject event_change = new JSONObject(event);
        event_change.put("eventData", new JSONObject("{}"));
        events = new JSONArray();
        events.put(event_change);
        str = testHelper.processStreamsEvents(events);
        //Should remain the same
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        try {
            JSONObject json = new JSONObject(str);
            JSONObject json_event = (JSONObject) json.get("processor_exception_stream_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("sum should be 1. Got: " + sum, sum == 1);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
        //Now verify that after updating the processor and pulling the event is reprocessed (simulate)
        String update = testHelper.getDataFileContent("test_data/streams/processor_fix.txt");
        testHelper.sdkChange(Constants.SP_FEATURE_USAGE_STREAMS, update, true);
        str = testHelper.processStreamsEvents(null);
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        try {
            JSONObject json = new JSONObject(str);
            JSONObject json_event = (JSONObject) json.get("processor_exception_stream_qa_test");
            int sum = json_event.getInt("count");
            Assert.assertTrue("sum should be 2. Got: " + sum, sum == 2);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
    }

    @Test
    public void goRunStreamTest() throws IOException, JSONException, AirlockNotInitializedException {
        JSONArray events = new JSONArray();
        String appLaunchEvent = testHelper.getDataFileContent("test_data/bar_events/nicole_streams_test/app_launch.json");
        String goRunEvent = testHelper.getDataFileContent("test_data/bar_events/nicole_streams_test/detail_viewed1.json");
        long time = 1504612345;
        JSONObject appLaunchJson1 = new JSONObject(appLaunchEvent);
        appLaunchJson1 = appLaunchJson1.put("dateTime", time);
        time++;
        JSONObject goRunJson1 = new JSONObject(goRunEvent);
        goRunJson1 = goRunJson1.put("dateTime", time);
        time++;
        JSONObject goRunJson2 = new JSONObject(goRunEvent);
        goRunJson2 = goRunJson2.put("dateTime", time);
        time++;
        events.put(appLaunchJson1);
        events.put(goRunJson1);
        events.put(goRunJson2);
        testHelper.processStreamsEvents(events);
        Assert.assertTrue("GoRunUsage stream is missing", testHelper.getStreamByName("GoRunUsage") != null);
        String cache = testHelper.getStreamByName("GoRunUsage").getCache();
        JSONObject cacheJson = new JSONObject(cache);
        int numOfSessions = cacheJson.getJSONArray("sessions").length();
        Assert.assertTrue("One session is expected. Got: " + numOfSessions, numOfSessions == 1);
        Assert.assertTrue(cacheJson.getJSONArray("sessions").getJSONObject(0).getDouble("eventcount") == 2.0);
        events = new JSONArray();
        for (int i = 0; i < 30; i++) {
            JSONObject appLaunchJson = new JSONObject(appLaunchEvent);
            appLaunchJson = appLaunchJson.put("dateTime", time);
            time++;
            JSONObject goRunJson = new JSONObject(goRunEvent);
            goRunJson = goRunJson.put("dateTime", time);
            time++;
            events.put(appLaunchJson);
            events.put(goRunJson);
        }
        testHelper.processStreamsEvents(events);
        cache = testHelper.getStreamByName("GoRunUsage").getCache();
        cacheJson = new JSONObject(cache);
        numOfSessions = cacheJson.getJSONArray("sessions").length();
        Assert.assertTrue("30 sessions are expected. Got: " + numOfSessions, numOfSessions == 30);
        Assert.assertTrue(cacheJson.getJSONArray("sessions").getJSONObject(0).getDouble("eventcount") == 1.0);
        String str = testHelper.processStreamsEvents(new JSONArray());
        JSONObject result = new JSONObject(str);
        Assert.assertTrue("gorun-heavy label is expected", result.getJSONObject("GoRunUsage").getString("label").equals("gorun-heavy"));
    }


    @Test
    public void initiateVideoStreamTest() throws IOException, JSONException, AirlockNotInitializedException {
        JSONArray events = new JSONArray();
        String appLaunchEvent = testHelper.getDataFileContent("test_data/bar_events/nicole_streams_test/app_launch.json");
        String videoEvent = testHelper.getDataFileContent("test_data/bar_events/nicole_streams_test/video_played_auto.json");
        long time = 1404612345;
        JSONObject appLaunchJson1 = new JSONObject(appLaunchEvent);
        appLaunchJson1 = appLaunchJson1.put("dateTime", time);
        time++;
        JSONObject videoJson1 = new JSONObject(videoEvent);
        videoJson1 = videoJson1.put("dateTime", time);
        time++;
        JSONObject videoJson2 = new JSONObject(videoEvent);
        videoJson2 = videoJson2.put("dateTime", time);
        time++;
        events.put(appLaunchJson1);
        events.put(videoJson1);
        events.put(videoJson2);
        String str = testHelper.processStreamsEvents(events);
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        try {
            JSONObject json = new JSONObject(str);
            JSONObject json_event = (JSONObject) json.get("User_Initiated_Video_Usage");
            int sum = json_event.getInt("count");
            Assert.assertTrue("count should be 2. Got: " + sum, sum == 2);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
        Assert.assertTrue("NULL returned when asking for User_Initiated_Video_Usage stream", testHelper.getStreamByName("User_Initiated_Video_Usage") != null);
        String cache = testHelper.getStreamByName("User_Initiated_Video_Usage").getCache();
        JSONObject cacheJson = new JSONObject(cache);
        int numOfSessions = cacheJson.getJSONArray("sessions").length();
        Assert.assertTrue("One session is expected. Got: " + numOfSessions, numOfSessions == 1);
        Assert.assertTrue(cacheJson.getJSONArray("sessions").getJSONObject(0).getDouble("eventcount") == 2.0);
        //Now add three sessions and verify the first session is out

        for (int i = 0; i < 3; i++) {
            events = new JSONArray();
            JSONObject appLaunchJson2 = new JSONObject(appLaunchEvent);
            time++;
            appLaunchJson2 = appLaunchJson2.put("dateTime", time);
            JSONObject videoJson3 = new JSONObject(videoEvent);
            time++;
            videoJson3 = videoJson3.put("dateTime", time);
            events.put(appLaunchJson2);
            events.put(videoJson3);
            testHelper.processStreamsEvents(events);
        }
        str = testHelper.processStreamsEvents(new JSONArray());
        Assert.assertTrue("NULL returned from getFeatureUsageSummary method after add", str != null);
        Assert.assertTrue("Empty string returned from getFeatureUsageSummary method after add", !str.isEmpty());
        try {
            JSONObject json = new JSONObject(str);
            JSONObject json_event = (JSONObject) json.get("User_Initiated_Video_Usage");
            int sum = json_event.getInt("count");
            Assert.assertTrue("count should be 3. Got: " + sum, sum == 3);
        } catch (JSONException e) {
            Assert.fail("getFeatureUsageSummary method returned a string which is not the expected JSON format: " + e.getMessage());
        }
        cache = testHelper.getStreamByName("User_Initiated_Video_Usage").getCache();
        cacheJson = new JSONObject(cache);
        numOfSessions = cacheJson.getJSONArray("sessions").length();
        Assert.assertTrue("Three sessions are expected. Got: " + numOfSessions, numOfSessions == 3);
        Assert.assertTrue(cacheJson.getJSONArray("sessions").getJSONObject(0).getDouble("eventcount") == 1.0);
        Assert.assertTrue(cacheJson.getJSONArray("sessions").getJSONObject(1).getDouble("eventcount") == 1.0);
        Assert.assertTrue(cacheJson.getJSONArray("sessions").getJSONObject(2).getDouble("eventcount") == 1.0);
    }
}
