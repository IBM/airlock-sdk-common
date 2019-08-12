package com.ibm.airlock.integration;

import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.BaseTestModel;

import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author SEitan
 */

public class NotificationsDevTest extends BaseTestModel {

    private static String PRODUCT_NAME = "AndroidWithStreams";
    private static String VERSION = "8.0.1";
    private int resultsCounter = 0;

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "NotificationsDevTest";
        return getConfigs();
    }

    public NotificationsDevTest(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        m_ug.add("AndroidDEV");
        testHelper.setup(m_appVersion, m_ug, null, null, false, false, false);
        testHelper.pull();
    }

//    @Test
//    public void pullNotificationsTest() throws JSONException, ScriptInitException, InterruptedException, AirlockNotInitializedException {
//        final CountDownLatch latch = new CountDownLatch(1);
//        AirlockCallback callback = new AirlockCallback() {
//            @Override
//            public void onFailure(Exception e) {
//                latch.countDown();
//            }
//
//            @Override
//            public void onSuccess(String s) {
//                latch.countDown();
//            }
//        };
//        testHelper.getManager().getNotificationService().setSupported(true);
//        testHelper.getManager().pullFeatures(callback);
//        latch.await();
//        //overwrite due date for notifications....
//        ArrayList<String> configurations = new ArrayList();
//        int numNotifications = testHelper.getManager().getNotificationService().getNotificationsSize();
//        for (int i = 0; i < numNotifications; i++) {
//            JSONObject configToUpdate = new JSONObject();
//            configToUpdate.put("dueDate", System.currentTimeMillis() + 30000 + i * 1000);
//            JSONObject configParent = new JSONObject();
//            configParent.put("notification", configToUpdate);
//            configurations.add(configParent.toString());
//        }
//        testHelper.getManager().getNotificationService().updateNotificationsConfigurations(configurations);
//        testHelper.getManager().getNotificationService().calculateAndSaveNotifications(new JSONObject());
//        List<PendingNotification> pendingNotifications = testHelper.getManager().getNotificationService().getPendingNotifications();
//        Assert.assertTrue(pendingNotifications.size() == 1);
//        testHelper.getManager().getNotificationService().setSupported(false);
//    }
//
//    @Test
//    public void notificationDefaultTest() throws JSONException, ScriptInitException {
//        List<PendingNotification> pendingNotifications = runSingleScenario(null);
//        Assert.assertTrue(pendingNotifications.size() == 1);
//    }
//
//    @Test
//    public void notificationFalseRegistrationTest() throws JSONException, ScriptInitException {
//        JSONObject falseReg = new JSONObject();
//        falseReg.put("registrationRule", createRuleStringObj("false"));
//        List<PendingNotification> pendingNotifications = runSingleScenario(falseReg);
//        Assert.assertTrue(pendingNotifications.size() == 0);
//        falseReg.put("registrationRule", createRuleStringObj("context.viewedLocation.country == \"DU\""));
//        pendingNotifications = runSingleScenario(falseReg);
//        Assert.assertTrue(pendingNotifications.size() == 0);
//    }
//
//    @Test
//    public void notificationTrueRegistrationTest() throws JSONException, ScriptInitException {
//        JSONObject trueReg = new JSONObject();
//
//        trueReg.put("registrationRule", createRuleStringObj("true"));
//        List<PendingNotification> pendingNotifications = runSingleScenario(trueReg);
//        Assert.assertTrue(pendingNotifications.size() == 1);
//        trueReg.put("registrationRule", createRuleStringObj("context.viewedLocation.country == \"US\""));
//        pendingNotifications = runSingleScenario(trueReg);
//        Assert.assertTrue(pendingNotifications.size() == 1);
//
//        //Now put false registration after the notification is already on pending list (should not remove the notification)
//        trueReg.put("registrationRule", createRuleStringObj("context.viewedLocation.country == \"IL\""));
//        pendingNotifications = runSingleScenario(trueReg, false);
//        Assert.assertTrue(pendingNotifications.size() == 1);
//
//        //Now put true cancellation after the notification is already on pending list (should remove the notification)
//        trueReg.put("cancellationRule", createRuleStringObj("context.viewedLocation.country == \"US\""));
//        pendingNotifications = runSingleScenario(trueReg, false);
//        Assert.assertTrue(pendingNotifications.size() == 0);
//    }
//
//    private JSONObject createRuleStringObj(String rule) throws JSONException {
//        JSONObject ruleString = new JSONObject();
//        ruleString.put("ruleString", rule);
//        return ruleString;
//    }
//
//    @Test
//    public void notificationFalseCancellationTest() throws JSONException, ScriptInitException {
//        JSONObject falseCancel = new JSONObject();
//        falseCancel.put("cancellationRule", createRuleStringObj("false"));
//        List<PendingNotification> pendingNotifications = runSingleScenario(falseCancel);
//        Assert.assertTrue(pendingNotifications.size() == 1);
//        falseCancel.put("cancellationRule", createRuleStringObj("context.viewedLocation.country == \"IL\""));
//        pendingNotifications = runSingleScenario(falseCancel);
//        Assert.assertTrue(pendingNotifications.size() == 1);
//    }
//
//    @Test
//    public void notificationWithCancelRegistrationTest() throws JSONException, ScriptInitException {
//        JSONObject falseReg = new JSONObject();
//        falseReg.put("cancellationRule", createRuleStringObj("true"));
//        List<PendingNotification> pendingNotifications = runSingleScenario(falseReg);
//        Assert.assertTrue(pendingNotifications.size() == 0);
//        falseReg.put("cancellationRule", createRuleStringObj("context.viewedLocation.country == \"US\""));
//        pendingNotifications = runSingleScenario(falseReg);
//        Assert.assertTrue(pendingNotifications.size() == 0);
//    }
//
//    @Test
//    public void notificationOldDueDateTest() throws JSONException, ScriptInitException {
//        JSONObject configurationContent = createDefaultConfiguration("dueDate", System.currentTimeMillis() - 999000);
//        JSONObject configuration = new JSONObject();
//        configuration.put("configuration", configurationContent);
//        List<PendingNotification> pendingNotifications = runSingleScenario(configuration);
//        Assert.assertTrue("expected size 0 but got " + pendingNotifications.size(), pendingNotifications.size() == 0);
//    }
//
//    @Test
//    public void notificationMetadataTest() throws JSONException, ScriptInitException {
//        JSONObject metadata = new JSONObject();
//        metadata.put("enabled", false);
//        List<PendingNotification> pendingNotifications = runSingleScenario(metadata);
//        Assert.assertTrue(pendingNotifications.size() == 0);
//        metadata = new JSONObject();
//        metadata.put("minAppVersion", "99.0.0");
//        pendingNotifications = runSingleScenario(metadata);
//        Assert.assertTrue(pendingNotifications.size() == 0);
//        metadata = new JSONObject();
//        metadata.put("internalUserGroup", "kuku");
//        pendingNotifications = runSingleScenario(metadata);
//        Assert.assertTrue(pendingNotifications.size() == 0);
//    }
//
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
