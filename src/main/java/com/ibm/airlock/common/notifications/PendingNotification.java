package com.ibm.airlock.common.notifications;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by SEitan on 21/11/2017.
 */
public class PendingNotification {
    private final String uniqueId;

    private final String title;
    private final String name;
    private final String text;
    private final String sound;
    private final String deepLink;
    private final String thumbnail;
    private final long dueDate;
    private final JSONArray actions;
    private final JSONObject additionalInfo;

    public PendingNotification(JSONObject obj){
        title = obj.optString("title");
        name = obj.optString("name");
        text = obj.optString("text");
        sound = obj.optString("sound");
        deepLink = obj.optString("deepLink");
        thumbnail = obj.optString("thumbnail");
        dueDate = obj.optLong("dueDate");
        uniqueId = obj.optString("uniqueId");
        actions = obj.optJSONArray("actions");
        additionalInfo = obj.optJSONObject("additionalInfo");
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public String getSound() {
        return sound;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public long getDueDate() {
        return dueDate;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public JSONArray getActions() {
        return actions;
    }

    public JSONObject getAdditionalInfo() {
        return additionalInfo;
    }
}