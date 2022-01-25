package com.ibm.airlock.common.analytics;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockNotInitializedException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import javax.annotation.Nullable;

public interface AnalyticsApiInterface {
    enum ConstantsKeys {
        ANALYTICS_MAIN_FEATURE,
        ENVIRONMENTS_FEATURE,
        EXPERIMENT_ATTRIBUTE,
        VARIANT_ATTRIBUTE,
        DEV_USER_ATTRIBUTE,
    }

    void track(String name, @Nullable Long eventTime, @Nullable String schemaVersion, Map<String, Object> attributes);
    void trackStreamResults(Map<String, Object> attributes);
    void setUserAttributes(Map<String, Object> attributes, @Nullable String schemaVersion);
    void sendAnalyticsEventsWhenGoingToBackground();
    void updateUserId(String uuid);
    void sendContextAsUserAttributes(@Nullable JSONObject airlockContext);

    void setUserAttributesSchemaVersion(String version);
    String getUserAttributesSchemaVersion();
    void syncAnalytics();
    void addAnalyticsShardToContext(@Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException;

    boolean doesAnalyticsEnvironmentExists(String name);
    String getSessionDetails(String featureName);
    void enableAnalyticsLifecycle(boolean enable);
    void verifyAnalyticsState();

    //get airlock feature names - the ones relevant for airlytics
    String getAnalyticsFeatureName(ConstantsKeys feature);

    //Debug utilities
    Map<String, ?> generateLogList(Object context, String environmentName);
    void setDebugEnable(boolean isChecked);

    void setAirlyticsInitializationCallback(AirlockCallback callback);

}
