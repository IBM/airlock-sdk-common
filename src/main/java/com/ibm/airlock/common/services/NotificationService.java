package com.ibm.airlock.common.services;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.engine.ScriptExecutionException;
import com.ibm.airlock.common.engine.ScriptInitException;
import com.ibm.airlock.common.engine.ScriptInvoker;
import com.ibm.airlock.common.engine.context.AirlockContextManager;
import com.ibm.airlock.common.notifications.AirlockNotification;
import com.ibm.airlock.common.notifications.AirlockNotificationRestriction;
import com.ibm.airlock.common.notifications.PendingNotification;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.RandomUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ibm.airlock.common.engine.Result.CANCEL_RULE_EVAL;
import static com.ibm.airlock.common.engine.features.FeaturesCalculator.createJsObjects;

public class NotificationService {
    private static final String TAG = "NotificationService";

    private static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";


    @Inject
    public Context context;

    @Inject
    public PersistenceHandler persistenceHandler;

    @Inject
    public String appVersion;

    @Inject
    AirlockContextManager airlockContextManager;

    private boolean isSupported;
    private Map<String, AirlockNotification> notifications;
    private JSONArray notificationsLimitations;
    private Object notificationIntent;

    public void init(ProductDiComponent productDiComponent) {
        productDiComponent.inject(this);
    }

    private boolean isDisabled() {
        return notifications == null || notifications.isEmpty();
    }

    public void calculateAndSaveNotifications(ScriptInvoker invoker) throws JSONException {

        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        if (isDisabled()) {
            return;
        }

        JSONArray pendingNotifications = new JSONArray(persistenceHandler.read(Constants.SP_PENDING_NOTIFICATIONS, "[]"));
        Collection<String> pendingNotificationIds = new HashSet<>();
        for (int i = 0; i < pendingNotifications.length(); i++) {
            JSONObject singleNotification = pendingNotifications.getJSONObject(i);
            String notificationUniqueId = singleNotification.optString("name");//uniqueId
            pendingNotificationIds.add(notificationUniqueId);
        }

        Collection<String> notificationsToCancel = new HashSet<>();
        List<JSONObject> notificationsToAdd = new ArrayList<>();
        //If thread was changed need to init the context again...
        Collection<String> serverNotificationIds = new HashSet<>();
        DateFormat df = new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.US);
        for (AirlockNotification notification : notifications.values()) {
            String nameId = notification.getName();
            serverNotificationIds.add(nameId);
            if (notification.isProcessingEnabled()) {
                //check the max notifications on notification level
                if (notification.getMinInterval() > 0 && notification.getMaxNotifications() > -1 &&
                        notificationsShownForInterval(notification.getMinInterval(), notification.getFiredHistory()) >= notification.getMaxNotifications()) {
                    continue;
                }
                try {
                    ScriptInvoker.Output needToCancel = invoker.evaluate(notification.getCancellationRule());
                    if (needToCancel.result == ScriptInvoker.Result.TRUE) {
                        if (pendingNotificationIds.contains(nameId)) {
                            //cancellation is true - notification should be cancelled....
                            notificationsToCancel.add(notification.getName());
                            notification.pushRegistrationHistory(df.format(System.currentTimeMillis()) + ": " + CANCEL_RULE_EVAL);
                            notification.setTraceInfo(CANCEL_RULE_EVAL);
                        }
                    } else {
                        if (!pendingNotificationIds.contains(nameId)) {
                            ScriptInvoker.Output shouldRegister = invoker.evaluate(notification.getRegistrationRule());
                            if (shouldRegister.result == ScriptInvoker.Result.TRUE) {
                                //get configuration values;
                                JSONObject evaluatedConfig = invoker.evaluateConfiguration(notification.getConfiguration());
                                evaluatedConfig = evaluatedConfig.optJSONObject("notification");
                                if (evaluatedConfig == null) {
                                    evaluatedConfig = new JSONObject();
                                }
                                evaluatedConfig.put("uniqueId", notification.getId());
                                evaluatedConfig.put("name", notification.getName());
                                long dueDate = evaluatedConfig.optLong("dueDate");
                                if (dueDate > System.currentTimeMillis()) {
                                    //Add notification to pending only if due date is on future
                                    if (notification.getMinInterval() > 0 && notification.getMaxNotifications() > -1 &&
                                            notificationsShownForInterval(notification.getMinInterval(), notification.getFiredHistory()) >= notification.getMaxNotifications()) {
                                        //if reached notifications limit - do not add it to pending
                                        continue;//TODO improve
                                    }
                                    notificationsToAdd.add(evaluatedConfig);
                                    notification.pushRegistrationHistory(df.format(System.currentTimeMillis()) + ": Registration rule evaluated as true and the Notification was Scheduled");
                                    notification.setTraceInfo("Registration rule evaluated as true");
                                } else {
                                    notification.setTraceInfo("Registration rule evaluated as true, but dueDate was in the past");
                                }
                            } else if (shouldRegister.result == ScriptInvoker.Result.FALSE) {
                                notification.setTraceInfo("Registration rule evaluated as false");
                            } else {
                                notification.setTraceInfo("Error calculating registration rule: " + shouldRegister.error);
                            }
                        }

                    }
                } catch (ScriptExecutionException e) {
                    notification.setTraceInfo("Registration rule evaluated as true but there was an error in calculating the Notification's Configuration: " + e.getLocalizedMessage());
                } catch (Throwable th) {
                    //If single notification processing fails do not stop processing of other notifications...
                }

            } else {
                //notification was disabled after it was added to pending list - remove it
                if (pendingNotificationIds.contains(nameId)) {
                    notificationsToCancel.add(nameId);
                }
            }
        }

        //persist notifications - active and cancelled...
        JSONArray outputArray = new JSONArray();
        for (int i = 0; i < pendingNotifications.length(); i++) {
            JSONObject singleNotification = pendingNotifications.getJSONObject(i);
            String notificationUniqueId = singleNotification.optString("name");
            if (!notificationsToCancel.contains(notificationUniqueId) && serverNotificationIds.contains(notificationUniqueId)) {
                outputArray.put(singleNotification);
            }
        }

        //create array of allowed + period and check against each entry
        List<AirlockNotificationRestriction> notificationsRestrictions = calculeteRestrictions();
        if (!notificationsToAdd.isEmpty() && !notificationsRestrictions.isEmpty()) {
            for (int i = 0; i < notificationsToAdd.size(); i++) {
                for (AirlockNotificationRestriction restriction : notificationsRestrictions) {
                    if (!isShowingAllowed(restriction.getMaxAllowed(), restriction.getMinInterval())) {
                        notificationsToAdd.remove(i);
                    }
                }
            }
        }

        // add new notifications
        for (JSONObject addedNotification : notificationsToAdd) {
            if (!pendingNotificationIds.contains(addedNotification.optString("name", "NONE"))) {
                outputArray.put(addedNotification);
            }
        }

        persist(outputArray);
        //noinspection VariableNotUsedInsideIf
        if (notificationIntent != null) {
            schedulePendingNotifications();
        }
    }

    private List<AirlockNotificationRestriction> calculeteRestrictions() {
        List<AirlockNotificationRestriction> restrictions = new ArrayList<>();
        for (int i = 0; i < notificationsLimitations.length(); i++) {
            int viewedCounter = 0;
            JSONObject limitation = notificationsLimitations.getJSONObject(i);
            int maxAllowed = limitation.optInt("maxAllowed", -1);
            int minInterval = limitation.optInt("minInterval", 0);
            if (maxAllowed > -1 && minInterval > 0) {
                for (AirlockNotification notification : notifications.values()) {
                    viewedCounter += (notificationsShownForInterval(limitation.getInt("minInterval"), notification.getFiredHistory()));
                }
                restrictions.add(new AirlockNotificationRestriction(limitation.optString("name"), maxAllowed - viewedCounter, minInterval));
            }
        }
        return restrictions;
    }

    private boolean isShowingAllowed(long maxAllowed, long minInterval) {
        int counter = 0;
        for (AirlockNotification notification : notifications.values()) {
            counter += (notificationsShownForInterval(minInterval, notification.getFiredHistory()));
        }
        return counter < maxAllowed;
    }

    private int notificationsShownForInterval(long minInterval, JSONArray firedHistory) {
        long currentTime = System.currentTimeMillis() / 1000;
        int counter = 0;
        if (firedHistory != null) {
            for (int index = firedHistory.length() - 1; index >= 0; index--) {
                Number singleFiredTime = (Number) firedHistory.opt(index);
                if (singleFiredTime.longValue() > (currentTime - minInterval)) {
                    counter++;
                }
            }
        }
        return counter;
    }

    public void calculateAndSaveNotifications(@Nullable JSONObject deviceProfile) throws JSONException, ScriptInitException {

        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        JSONObject translations = persistenceHandler.readJSON(Constants.SP_RAW_TRANSLATIONS);
        if (translations == null) {
            translations = new JSONObject();
        }

        ScriptInvoker invoker = createJsObjects(airlockContextManager, deviceProfile, persistenceHandler.read(Constants.SP_RAW_JS_FUNCTIONS, ""), translations, new LinkedList<String>());

        calculateAndSaveNotifications(invoker);
    }

    @TestOnly
    public void clearNotifications() {

    }

    private JSONArray getAirlockNotifications() throws JSONException {
        JSONObject jsonObject = persistenceHandler.readJSON(Constants.SP_NOTIFICATIONS);
        notificationsLimitations = jsonObject.optJSONArray("notificationsLimitations");
        if (notificationsLimitations == null || !persistenceHandler.getDeviceUserGroups().isEmpty()) {
            //if dev mode - disable the global restrictions
            notificationsLimitations = new JSONArray();
        }
        JSONArray notificationsArray = jsonObject.optJSONArray("notifications");
        if (notificationsArray == null) {
            notificationsArray = new JSONArray();
        }
        return notificationsArray;
    }

    public synchronized void initNotifications() {

        if (persistenceHandler.readJSON(Constants.SP_NOTIFICATIONS).length() == 0) {
            return;
        }

        try {
            JSONArray notificationsArray = getAirlockNotifications();
            //notifications are stored as LinkedHashMap since the order on airlock is important for defining the maxNotifications implementation
            notifications = new LinkedHashMap<>();

            if (notificationsArray != null) {
                try {
                    JSONObject newNotificationsRandoms = RandomUtils.calculateNotificationsRandoms(notificationsArray,
                            persistenceHandler.getNotificationsRandomMap().length() > 0 ? new JSONObject() : persistenceHandler.getNotificationsRandomMap());
                    persistenceHandler.setNotificationsRandomMap(newNotificationsRandoms);
                } catch (Exception e) {
                    //todo nothing ??
                }
                for (int i = 0; i < notificationsArray.length(); i++) {
                    JSONObject obj = notificationsArray.getJSONObject(i);
                    AirlockNotification notification = new AirlockNotification(obj, persistenceHandler, appVersion);
                    notifications.put(notification.getName(), notification);
                }
            }
        } catch (JSONException e) {
//            .write("Initializing notifications got an exception: " + e.getMessage());
        }
        JSONObject firedNotifications = persistenceHandler.readJSON(Constants.SP_FIRED_NOTIFICATIONS);
        Iterator<?> firedKeys = firedNotifications.keys();
        while (firedKeys.hasNext()) {
            String key = (String) firedKeys.next();
            if (notifications.containsKey(key)) {
                notifications.get(key).setFiredHistory(firedNotifications.optJSONArray(key));
            }
        }

        JSONObject notificationsHistory = persistenceHandler.readJSON(Constants.SP_NOTIFICATIONS_HISTORY);
        firedKeys = notificationsHistory.keys();
        while (firedKeys.hasNext()) {
            String key = (String) firedKeys.next();
            if (notifications.containsKey(key)) {
                notifications.get(key).setRegistrationHistory(notificationsHistory.optJSONArray(key));
            }
        }
    }

    public void updateNotificationsEnablement() {

        if (isDisabled()) {
            return;
        }
        for (com.ibm.airlock.common.notifications.AirlockNotification notification : notifications.values()) {
            notification.setProcessingEnablement();
        }
    }

    @TestOnly
    public void updateNotificationsConfigurations(List<String> configurations) {
        if (configurations == null || configurations.isEmpty()) {
            return;
        }
        int counter = 0;
        for (com.ibm.airlock.common.notifications.AirlockNotification notification : notifications.values()) {
            if (configurations.size() > counter) {
                notification.setConfiguration(configurations.get(counter));
            }
            notification.setProcessingEnablement();
            counter++;
        }
    }

    @TestOnly
    public int getNotificationsSize() {
        int size = 0;
        if (notifications != null) {
            size = notifications.size();
        }
        return size;
    }

    protected void scheduleNotificationAlarm(long dueDate) {
        //On base class do nothing - on Extending class should implement real alarm set

        /*
         * The below implementation is an example for the android implementation

         Context context = InfraAirlockService.getInstance().getContext();
         AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
         if (alarmManager != null &&  notificationIntent != null) {
         alarmManager.setExact(AlarmManager.RTC_WAKEUP, dueDate,  notificationIntent);
         }
         */
    }

    private void schedulePendingNotifications() {
        List<PendingNotification> pendingNotifications = getPendingNotifications();
        boolean expiredNotificationsExist = false;
        for (PendingNotification notification : pendingNotifications) {
            long dueDate = notification.getDueDate();
            if (dueDate > (System.currentTimeMillis())) {
                scheduleNotificationAlarm(notification.getDueDate());
            } else {
                expiredNotificationsExist = true;
            }
        }
        if (expiredNotificationsExist) {
            scheduleNotificationAlarm(System.currentTimeMillis() + 100);
        }
    }

    /**
     * Get the list of pending notifications. pending means that it was added on some stage to pending list
     *
     * @return list og pending notifications
     */
    private List<PendingNotification> getPendingNotifications() {

        ArrayList<PendingNotification> pendingNotifications = new ArrayList<>();
        JSONArray jsonPendingNotifications;
        try {
            jsonPendingNotifications = new JSONArray(persistenceHandler.read(Constants.SP_PENDING_NOTIFICATIONS, "[]"));
        } catch (JSONException e) {
            jsonPendingNotifications = new JSONArray();
        }
        for (int i = 0; i < jsonPendingNotifications.length(); i++) {
            JSONObject singleNotification;
            try {
                singleNotification = jsonPendingNotifications.getJSONObject(i);
            } catch (JSONException e) {
                singleNotification = new JSONObject();
            }
            pendingNotifications.add(new PendingNotification(singleNotification));
        }

        return pendingNotifications;
    }

    /**
     * method to update the pending notification list by deleting
     *
     * @param notificationsToRemove list of notification names to remove
     */
    public void removeFiredNotifications(List<String> notificationsToRemove) {

        if (notificationsToRemove == null || notificationsToRemove.isEmpty()) {
            return;
        }
        JSONArray outputArray = new JSONArray();
        JSONArray pendingNotifications;
        try {
            pendingNotifications = new JSONArray(persistenceHandler.read(Constants.SP_PENDING_NOTIFICATIONS, "[]"));
        } catch (JSONException e) {
            pendingNotifications = new JSONArray();
        }
        for (int i = 0; i < pendingNotifications.length(); i++) {
            JSONObject singleNotification;
            try {
                singleNotification = pendingNotifications.getJSONObject(i);
            } catch (JSONException e) {
                singleNotification = new JSONObject();
            }
            String notificationName = singleNotification.optString("name");
            if (notificationsToRemove.contains(notificationName)) {
                notifications.get(notificationName).pushHistory(System.currentTimeMillis() / 1000);
                continue;
            }
            outputArray.put(singleNotification);
        }
        persist(outputArray);
    }

    private synchronized void persist(JSONArray pendingNotifications) {
        persistenceHandler.write(Constants.SP_PENDING_NOTIFICATIONS, pendingNotifications.toString());
        JSONObject firedNotifications = new JSONObject();
        JSONObject notificationsHistory = new JSONObject();
        for (AirlockNotification notification : notifications.values()) {
            JSONArray firedHistory = notification.getFiredHistory();
            if (firedHistory != null && firedHistory.length() > 0) {
                try {
                    firedNotifications.put(notification.getName(), firedHistory);
                } catch (JSONException e) {
                    //do nothing
                }
            }
            JSONArray registrationHistory = notification.getFiredHistory();
            if (registrationHistory != null && registrationHistory.length() > 0) {
                try {
                    notificationsHistory.put(notification.getName(), registrationHistory);
                } catch (JSONException e) {
                    //do nothing
                }
            }
        }
        persistenceHandler.write(Constants.SP_FIRED_NOTIFICATIONS, firedNotifications.toString());
        persistenceHandler.write(Constants.SP_NOTIFICATIONS_HISTORY, notificationsHistory.toString());
    }


    public void setNotificationIntent(Object notificationIntent) {
        this.notificationIntent = notificationIntent;
        isSupported = notificationIntent != null;
    }

    public Map<String, AirlockNotification> getNotifications() {
        return notifications;
    }


    public boolean isSupported() {
        return isSupported;
    }

    public void setSupported(boolean supported) {
        isSupported = supported;
    }

    public AirlockNotification getNotification(String name) {
        return notifications.get(name);
    }

    public PendingNotification getPendingNotificationById(String id) {
        try {
            JSONArray jsonPendingNotifications = new JSONArray(persistenceHandler.read(Constants.SP_PENDING_NOTIFICATIONS, "[]"));
            for (int i = 0; i < jsonPendingNotifications.length(); i++) {
                JSONObject singleNotification = jsonPendingNotifications.getJSONObject(i);
                if (singleNotification.getString("uniqueId").equals(id)) {
                    return new PendingNotification(singleNotification);
                }
            }

        } catch (JSONException ignored) {
        }
        return null;
    }
}
