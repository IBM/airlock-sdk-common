package com.ibm.airlock.integration;

import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.BaseTestModel;

import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Denis Voloshin
 */

public class NotificationPercentageRealTest extends BaseTestModel {

    private static String PRODUCT_NAME = "AndroidWithStreams";
    private static String VERSION = "8.0.1";
    private int resultsCounter = 0;

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "NotificationsDevTest";
        return getConfigs();
    }

    public NotificationPercentageRealTest(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        m_ug.add("AndroidDEV");
        testHelper.setup(m_appVersion, m_ug, null, null, false, false, false);
        testHelper.pull();
    }

//    @Test
//    public void percentage1000DevicesTest() throws Exception {
//        JSONObject metadata = new JSONObject();
//        metadata.put("rolloutPercentage", 60.0);
//        PersistenceHandler sp = testHelper.getManager().getInfraAirlockService().getPersistenceHandler();
//        for (int i = 0; i < 1000; i++) {
//            sp.write(Constants.SP_RANDOMS, "{}");
//            List<PendingNotification> pendingNotifications = runSingleScenario(metadata);
//            resultsCounter += pendingNotifications.size();
//        }
//        Assert.assertTrue("Unexpected result: " + resultsCounter, resultsCounter > 550);
//        Assert.assertTrue("Unexpected result: " + resultsCounter, resultsCounter < 700);
//    }
//
//    private List<PendingNotification> runSingleScenario(JSONObject notification) throws JSONException, ScriptInitException {
//        return runSingleScenario(notification, true);
//    }
//
//    private List<PendingNotification> runSingleScenario(JSONObject notification, boolean cleanPending) throws JSONException, ScriptInitException {
//
//        testHelper.getManager().getInfraAirlockService().getPersistenceHandler().write(Constants.SP_NOTIFICATIONS, createDefaultNotification(notification).toString());
//        if (cleanPending) {
//            testHelper.getManager().getInfraAirlockService().getPersistenceHandler().write(Constants.SP_PENDING_NOTIFICATIONS, "[]");
//        }
//        testHelper.getManager().getNotificationService().initNotifications();
//
//        String context = "{\n" +
//                "  \"userPreferences\": {\n" +
//                "    \"metricSystem\": true,\n" +
//                "    \"temperature\": \"C\",\n" +
//                "    \"velocity\": \"KMH\"\n" +
//                "  },\n" +
//                "  \"viewedLocation\": {\n" +
//                "    \"country\": \"US\",\n" +
//                "    \"region\": \"AZ\",\n" +
//                "    \"lon\": 33,\n" +
//                "    \"lat\": 43\n" +
//                "  },\n" +
//                "  \"currentLocation\": {\n" +
//                "    \"country\": \"US\",\n" +
//                "    \"region\": \"AZ\",\n" +
//                "    \"lon\": 50,\n" +
//                "    \"lat\": -30\n" +
//                "  },\n" +
//                "  \"device\": {\n" +
//                "    \"datetime\": \"2015-03-25T18:51:04.419Z\",\n" +
//                "    \"localeCountryCode\": \"US\",\n" +
//                "    \"localeLanguage\": \"EN\",\n" +
//                "    \"locale\": \"en_US\",\n" +
//                "    \"osVersion\": \"9.0.3\",\n" +
//                "    \"connectionType\": \"3G\",\n" +
//                "    \"version\": \"8\",\n" +
//                "    \"screenWidth\": 23,\n" +
//                "    \"screenHeight\": 50\n" +
//                "  }}";
//        JSONObject jsonContext = new JSONObject(context);
//
//        testHelper.getManager().getNotificationService().calculateAndSaveNotifications(jsonContext);
//
//        List<PendingNotification> pendingNotifications = testHelper.getManager().getNotificationService().getPendingNotifications();
//        return pendingNotifications;
//
//    }
//
//    private JSONObject createDefaultNotification(JSONObject singleOverwrite) throws JSONException {
//        JSONObject notifications = new JSONObject();
//        JSONArray notificationItems = new JSONArray();
//        JSONObject singleNotificationItem = createSingleNotification(singleOverwrite);
//        notificationItems.put(singleNotificationItem);
//        notifications.put("notifications", notificationItems);
//        return notifications;
//    }
//
//    private JSONObject createDefaultConfiguration(String key, Object value) throws JSONException {
//        JSONObject configurationDetails = new JSONObject();
//        configurationDetails.put("title", "title1");
//        configurationDetails.put("text", "text1");
//        configurationDetails.put("deepLink", "mainApp");
//        configurationDetails.put("sound", "soundURL");
//        configurationDetails.put("thumbnail", "thumbnailURL");
//        configurationDetails.put("dueDate", System.currentTimeMillis() + 30000);
//        configurationDetails.put("additionalInfo", "");
//        if (key != null) {
//            configurationDetails.put(key, value);
//        }
//        JSONObject configuration = new JSONObject();
//        configuration.put("notification", configurationDetails);
//        return configuration;
//    }
//
//
//    private JSONObject createSingleNotification(JSONObject overwriteValues) throws JSONException {
//        JSONObject singleNotificationItem = new JSONObject();
//        if (overwriteValues == null) {
//            overwriteValues = new JSONObject();
//        }
//        boolean enabled = overwriteValues.optBoolean("enabled", true);
//        singleNotificationItem.put("enabled", enabled);
//
//        JSONObject regRule = overwriteValues.optJSONObject("registrationRule");
//        if (regRule == null) {
//            regRule = new JSONObject("{\"ruleString\":\"true\"}");
//        }
//        singleNotificationItem.put("registrationRule", regRule);
//
//        JSONObject cancelRule = overwriteValues.optJSONObject("cancellationRule");
//        if (cancelRule == null) {
//            cancelRule = new JSONObject("{\"ruleString\":\"false\"}");
//        }
//        singleNotificationItem.put("cancellationRule", cancelRule);
//
//        JSONArray arrayOfUserGroups = new JSONArray();
//        String userGroup = overwriteValues.optString("internalUserGroup", "AndroidDEV");
//        arrayOfUserGroups.put(userGroup);
//        singleNotificationItem.put("internalUserGroups", arrayOfUserGroups);
//
//        String minAppVersion = overwriteValues.optString("minAppVersion", "8.0.1");
//        singleNotificationItem.put("minAppVersion", minAppVersion);
//
//        String name = overwriteValues.optString("name", "notification1");
//        singleNotificationItem.put("name", name);
//
//        double rolloutPercentage = overwriteValues.optDouble("rolloutPercentage", 100.0);
//        singleNotificationItem.put("rolloutPercentage", rolloutPercentage);
//
//        String seasonId = overwriteValues.optString("seasonId", "d30f6620-4d1c-48c2-a501-7d60dde35dd0");
//        singleNotificationItem.put("seasonId", seasonId);
//
//        String stage = overwriteValues.optString("stage", "DEVELOPMENT");
//        singleNotificationItem.put("stage", stage);
//
//        String uniqueId = overwriteValues.optString("uniqueId", "5a3e64af-708a-4b01-9068-b6699282c769");
//        singleNotificationItem.put("uniqueId", uniqueId);
//
//
//        JSONObject configuration = overwriteValues.optJSONObject("configuration");
//        if (configuration == null) {
//            configuration = createDefaultConfiguration(null, null);
//        }
//        singleNotificationItem.put("configuration", configuration);
//
//        return singleNotificationItem;
//    }
}
