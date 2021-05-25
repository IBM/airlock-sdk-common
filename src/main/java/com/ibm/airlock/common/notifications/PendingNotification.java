package com.ibm.airlock.common.notifications;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by SEitan on 21/11/2017.
 */
public class PendingNotification {
    private String uniqueId;

    private String title;
    private String name;
    private String text;
    private String sound;
    private String deepLink;
    private String thumbnail;
    private long dueDate;
    private JSONArray actions;
    private JSONObject additionalInfo;

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