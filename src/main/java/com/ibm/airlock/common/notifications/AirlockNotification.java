package com.ibm.airlock.common.notifications;

import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.util.AirlockVersionComparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by SEitan on 21/11/2017.
 */
@SuppressWarnings("unused")
public class AirlockNotification {
    private final String registrationRule;
    private final String cancellationRule;

    private String configuration;

    //basic fields
    private String name;
    private final boolean enabled;
    private final JSONArray internalUserGroups;
    private final String minAppVersion;
    private final long rolloutPercentage;
    private final String stage;
    private final long maxNotifications;
    private final long minInterval;
    private JSONArray firedHistory;
    private JSONArray registrationHistory;
    private boolean isPending;
    private final String id;
    private final PersistenceHandler ph;
    private final String appVersion;

    private String traceInfo="";

    //This boolean is a result of calculation and not only the "enabled" flag
    private boolean processingEnabled;

    private static final int MAX_HISTORY_SIZE = 200;

    public AirlockNotification(JSONObject obj,PersistenceHandler ph, String appVersion){
        this.ph = ph;
        this.appVersion = appVersion;
        JSONObject registrationJsonRule = obj.optJSONObject("registrationRule");
        JSONObject cancellationJsonRule = obj.optJSONObject("cancellationRule");

        if (registrationJsonRule != null){
            registrationRule = registrationJsonRule.optString("ruleString","");
        }else{
            registrationRule = "";
        }

        if (cancellationJsonRule != null){
            cancellationRule = cancellationJsonRule.optString("ruleString","");
        }else{
            cancellationRule = "";
        }
        configuration = obj.optString("configuration");

        name = obj.optString("name");
        //noinspection DynamicRegexReplaceableByCompiledPattern
        name = name.replaceAll("[. ]","_");
        enabled = obj.optBoolean("enabled",false);
        internalUserGroups = obj.optJSONArray("internalUserGroups");
        minAppVersion = obj.optString("minAppVersion");
        rolloutPercentage = obj.optLong("rolloutPercentage");
        stage = obj.optString("stage");
        maxNotifications = obj.optLong("maxNotifications");
        minInterval = obj.optLong("minInterval");
        id = obj.optString("uniqueId");
        isPending = false;
        setProcessingEnablement();
    }

    public String getName() {
        return name;
    }

    public void setProcessingEnablement() {
        boolean isProcessingEnabled = true;
        String disableReason ="";
        if (enabled) {
            //check minapp version
            AirlockVersionComparator comparator = new AirlockVersionComparator();
            if (this.minAppVersion == null || comparator.compare(this.minAppVersion, appVersion) > 0) {
                isProcessingEnabled = false;
                disableReason = "app version is too low";
            }
        }else {
            // Notification is disabled - clear the result field
            isProcessingEnabled = false;
        }

        //check percentage
        JSONObject notificationsRandomNumber = ph.getNotificationsRandomMap();
        if(isProcessingEnabled && notificationsRandomNumber != null && notificationsRandomNumber.length() > 0) {
            double threshold = this.rolloutPercentage;
            if (threshold <= 0) {
                isProcessingEnabled = false;
            } else if (threshold < 100.0) {
                int userFeatureRand = notificationsRandomNumber.optInt(getName(), -1);
                if (userFeatureRand == -1) {
                    isProcessingEnabled = false;
                }else{
                    isProcessingEnabled = userFeatureRand <= threshold * 10000;
                }
                if (!isProcessingEnabled){
                    disableReason = "Notification did not reach rollout percentage";
                }
            }
        }

        if (isProcessingEnabled) {
            //Stage and user groups
            if (this.stage.equals("DEVELOPMENT")) {
                JSONArray supportedUserGroups = this.internalUserGroups;
                List<String> existingUserGroups = ph.getDeviceUserGroups();
                isProcessingEnabled = false;
                for (String userGroup : existingUserGroups) {
                    for (int i = 0; i < supportedUserGroups.length(); i++) {
                        try {
                            if (userGroup.equals(supportedUserGroups.get(i))) {
                                isProcessingEnabled = true;
                                break;
                            }
                        } catch (JSONException e) {
                            //Do nothing - group was not found
                        }
                    }
                    if (isProcessingEnabled) {
                        break;
                    }
                }
            }
            if (!isProcessingEnabled){
                disableReason = "App do not have the required user group definitions";
            }
        }
        if (!isProcessingEnabled){
            setProcessingEnabled(disableReason);
        }else{
            setProcessingEnabled(null);
        }
    }

    private synchronized void setProcessingEnabled(@Nullable String disableReason){
        if (disableReason == null){
            this.processingEnabled = true;
            setTraceInfo("");
        }else{
            this.processingEnabled = false;
            setTraceInfo("Notification can not be processed because of the following: "+disableReason);
        }
    }

    public boolean isProcessingEnabled() {
        return processingEnabled;
    }

    public String getRegistrationRule() {
        return registrationRule;
    }

    public String getCancellationRule() {
        return cancellationRule;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public long getMaxNotifications() {
        return maxNotifications;
    }

    public long getMinInterval() {
        return minInterval;
    }

    public String getId() {
        return id;
    }

    public JSONArray getFiredHistory() {
        return this.firedHistory;
    }

    public void setFiredHistory(JSONArray firedHistory) {
        this.firedHistory = firedHistory;
    }

    public void pushHistory(long time) {
        if (firedHistory == null){
            firedHistory = new JSONArray();
        }
        if (this.firedHistory.length() > MAX_HISTORY_SIZE){
            this.firedHistory.remove(MAX_HISTORY_SIZE);
        }
        this.firedHistory.put(time);
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public boolean isEnabled(){
        return enabled;
    }

    public long getRolloutPercentage(){
        return rolloutPercentage;
    }

    public String getTraceInfo() {
        return traceInfo;
    }

    public void setTraceInfo(String traceInfo) {
        this.traceInfo = traceInfo;
    }

    public JSONArray getRegistrationHistory() {
        return registrationHistory;
    }

    public void setRegistrationHistory(JSONArray registrationHistory) {
        this.registrationHistory = registrationHistory;
    }

    public void pushRegistrationHistory(String history) {
        if (registrationHistory == null){
            registrationHistory = new JSONArray();
        }
        if (this.registrationHistory.length() > MAX_HISTORY_SIZE){
            this.registrationHistory.remove(MAX_HISTORY_SIZE);
        }
        this.registrationHistory.put(history);
    }
}
