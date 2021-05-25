package com.ibm.airlock.common.cache;

/**
 * Created by Denis Voloshin on 05/11/2017.
 */

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.FeaturesList;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.streams.StreamsManager;
import com.ibm.airlock.common.util.Constants;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.ibm.airlock.common.util.Constants.JSON_FEATURE_FIELD_FEATURES;
import static com.ibm.airlock.common.util.Constants.JSON_FIELD_EXPERIMENTS;
import static com.ibm.airlock.common.util.Constants.JSON_FIELD_FEATURES;
import static com.ibm.airlock.common.util.Constants.JSON_FIELD_NOTIFICATIONS;
import static com.ibm.airlock.common.util.Constants.JSON_FIELD_ROOT;
import static com.ibm.airlock.common.util.Constants.JSON_FIELD_STREAMS;


/**
 * Created by Denis Voloshin on 01/11/2017.
 */

public abstract class BasePersistenceHandler implements PersistenceHandler {

    protected final static Object lock = new Object();

    protected final String TAG = "BasePersistenceHandler";

    protected final Set<String> filePersistPreferences = new HashSet<>(Arrays.asList(
            new String[]{Constants.SP_CURRENT_CONTEXT, Constants.SP_DEFAULT_FILE, Constants.SP_RAW_RULES,
                    Constants.SP_RAW_JS_FUNCTIONS, Constants.SP_RAW_TRANSLATIONS, Constants
                    .SP_RANDOMS, Constants.SP_FEATURE_USAGE_STREAMS,
                    Constants.SP_NOTIFICATIONS, Constants.SP_FIRED_NOTIFICATIONS, Constants.SP_NOTIFICATIONS_HISTORY,
                    Constants.SP_FEATURES_PERCENAGE_MAP, Constants.SP_SYNCED_FEATURES_LIST,
                    Constants.SP_SERVER_FEATURE_LIST, Constants.SP_PRE_SYNCED_FEATURES_LIST,
                    Constants.SP_FEATURE_UTILS_STREAMS
            }
    ));
    protected final Set<String> saveAsJSONPreferences = new HashSet<>(Arrays.asList(
            new String[]{Constants.SP_CURRENT_CONTEXT, Constants.SP_DEFAULT_FILE, Constants.SP_RAW_RULES,
                    Constants.SP_RAW_TRANSLATIONS, Constants.SP_RANDOMS, Constants
                    .SP_FEATURE_USAGE_STREAMS, Constants.SP_NOTIFICATIONS,
                    Constants.SP_FIRED_NOTIFICATIONS, Constants.SP_NOTIFICATIONS_HISTORY,
                    Constants.SP_FEATURES_PERCENAGE_MAP, Constants.SP_SYNCED_FEATURES_LIST,
                    Constants.SP_SERVER_FEATURE_LIST, Constants.SP_PRE_SYNCED_FEATURES_LIST,
                    Constants.SP_PRE_SYNCED_ENTITLEMENTS_LIST, Constants.SP_SYNCED_ENTITLEMENTS_LIST
            }
    ));

    protected Cacheable inMemoryPreferences = new InMemoryCache();

    protected SharedPreferences preferences;

    protected Context context;

    public BasePersistenceHandler() {
        super();
    }

    public BasePersistenceHandler(Context c) {
        init(c);
    }

    public void setContext(Context context) {
        this.context = context;
        init(context);
    }

    public abstract void init(Context c, AirlockCallback callback);

    public abstract void init(Context c);

    public abstract void reset(Context c);


    public FeaturesList getCachedPreSyncedFeaturesMap() {
        String featureValues = read(Constants.SP_PRE_SYNCED_FEATURES_LIST, "");
        if (featureValues.length() == 0) {
            return new FeaturesList();
        }
        return new FeaturesList(featureValues, Feature.Source.UNKNOWN);
    }

    public FeaturesList getCachedSyncedFeaturesMap() {
        String featureValues = read(Constants.SP_SYNCED_FEATURES_LIST, "");
        if (featureValues.length() == 0) {
            return new FeaturesList();
        }
        return new FeaturesList(featureValues, Feature.Source.UNKNOWN);
    }


    public FeaturesList getCachedFeatureMap() {
        String featureValues = read(Constants.SP_SERVER_FEATURE_LIST, "");
        if (featureValues.length() == 0) {
            return new FeaturesList();
        }
        return new FeaturesList(featureValues, Feature.Source.UNKNOWN);
    }

    public JSONObject getRandomMap(String key) {
        JSONObject randoms = readJSON(Constants.SP_RANDOMS);
        JSONObject result = randoms.optJSONObject(key);
        if (result == null){
            result = new JSONObject();
        }
        return result;
    }

    public JSONObject getRandomMap() {
        return readJSON(Constants.SP_RANDOMS);
    }

    @CheckForNull
    public JSONObject getFeaturesRandomMap() {
        return getRandomMap(JSON_FIELD_FEATURES);
    }

    @CheckForNull
    public JSONObject getStreamsRandomMap() {
        return getRandomMap(JSON_FIELD_STREAMS);
    }

    @CheckForNull
    public JSONObject getExperimentsRandomMap() {
        return getRandomMap(JSON_FIELD_EXPERIMENTS);
    }

    @CheckForNull
    public JSONObject getNotificationsRandomMap() {
        return getRandomMap(JSON_FIELD_NOTIFICATIONS);
    }

    public void setFeaturesRandomMap(JSONObject randomsMap) {
        setRandomMap(JSON_FEATURE_FIELD_FEATURES, randomsMap);
    }

    public void setStreamsRandomMap(JSONObject randomsMap) {
        setRandomMap(JSON_FIELD_STREAMS, randomsMap);
    }

    public void setExperimentsRandomMap(JSONObject randomsMap) {
        setRandomMap(JSON_FEATURE_FIELD_FEATURES, randomsMap);
    }

    public void setNotificationsRandomMap(JSONObject randomsMap) {
        setRandomMap(JSON_FIELD_NOTIFICATIONS, randomsMap);
    }

    private void setRandomMap(String key, JSONObject randomsMap) {
        JSONObject randoms = readJSON(Constants.SP_RANDOMS);
        randoms.put(key, randomsMap);
        write(Constants.SP_RANDOMS, randoms.toString());
    }

    /**
     * Stores a map of user groups
     * each group could be selected on not.
     *
     * @throws JSONException
     */
    public void storeDeviceUserGroups(@Nullable List<String> userGroups, @Nullable StreamsManager streamsManager) {
        if (userGroups == null) {
            write(Constants.SP_USER_GROUPS, "");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userGroups.size(); i++) {
            sb.append(userGroups.get(i));
            if (i < userGroups.size() - 1) {
                sb.append(",");
            }
        }
        write(Constants.SP_USER_GROUPS, sb.toString());
        if (streamsManager != null) {
            streamsManager.updateStreamsEnablement();
        }
    }


    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    public String getDevelopBranchName() {
        return read(Constants.SP_DEVELOP_BRANCH_NAME, "").trim();
    }

    /**
     * Stores a selected develop branch name to the local store
     */
    public void setDevelopBranchName(String selectedDevelopBranchName) {
        write(Constants.SP_DEVELOP_BRANCH_NAME, selectedDevelopBranchName);
    }

    /**
     * returns the last branch name from the local store
     *
     * @return the name of the last branch name used
     */
    public String getLastBranchName() {
        return read(Constants.SP_BRANCH_NAME, JSON_FIELD_ROOT);
    }

    /**
     * Stores the current branch name to the local store
     */
    public void setLastBranchName(String selectedDevelopBranchName) {
        write(Constants.SP_BRANCH_NAME, selectedDevelopBranchName);
    }

    /**
     * Return a selected develop branch id from the local store
     *
     * @return the id of a selected develop branch
     */
    public String getDevelopBranchId() {
        return read(Constants.SP_DEVELOP_BRANCH_ID, "").trim();
    }

    /**
     * Stores a selected develop branch id to the local store
     */
    public void setDevelopBranchId(String selectedDevelopBranchId) {
        write(Constants.SP_DEVELOP_BRANCH_ID, selectedDevelopBranchId);
    }

    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    public String getDevelopBranch() {
        return read(Constants.SP_DEVELOP_BRANCH, "").trim();
    }


    /**
     * Stores a selected develop branch config (in JSON) to the local store
     */
    public synchronized void setDevelopBranch(String selectedDevelopBranch) {
        write(Constants.SP_DEVELOP_BRANCH, selectedDevelopBranch);
    }


    /**
     * Return a list from the local store of user groups
     * each group could be selected on not.
     *
     * @return a list of user groups
     */
    public List<String> getDeviceUserGroups() {
        List<String> result = new ArrayList<>();
        String groups = read(Constants.SP_USER_GROUPS, "");
        if (groups.equals("")) {
            return result;
        }
        String[] groupsArray = groups.split(",");
        return Arrays.asList(groupsArray);
    }

    public JSONObject readJSON(String key) {
        JSONObject value;
        synchronized (lock) {
            if (inMemoryPreferences.containsKey(key)) {
                value = (JSONObject) inMemoryPreferences.get(key);
            } else {
                value = (JSONObject) readSinglePreferenceFromFileSystem(key);
            }
        }
        if (value == null) {
            value = new JSONObject();
        }
        return value;
    }

    @CheckForNull
    public Set<String> readSet(String key) {
        if (this.preferences.getString(key, key) == null) {
            return null;
        }
        try {
            JSONArray array = new JSONArray(this.preferences.getString(key, key));
            Set<String> set = new HashSet<String>() {
            };
            int len = array.length();
            for (int i = 0; i < len; i++) {
                set.add(array.get(i).toString());
            }
            return set;
        } catch (JSONException e) {
            return null;
        }
    }


    protected String readFromMemory(String key, String defaultValue) {
        synchronized (lock) {
            if (inMemoryPreferences.containsKey(key) && inMemoryPreferences.get(key) != null) {
                return inMemoryPreferences.get(key).toString();
            } else {
                Object fileContent = readSinglePreferenceFromFileSystem(key);
                if (fileContent != null) {
                    return fileContent.toString();
                } else {
                    return defaultValue;
                }
            }
        }
    }

    public String read(String key, String defaultValue) {
        String value;
        if (filePersistPreferences.contains(key)) {
           return readFromMemory(key,defaultValue);
        } else {
            if (preferences != null) {
                value = preferences.getString(key, defaultValue);
            } else {
                value = defaultValue;
            }
        }
        return value;
    }

    public abstract void write(String key, String value);

    public abstract void write(String key, JSONObject value);

    /**
     * The reason this has a seperate method is because it is called when app stopps - so we need to persist synchronously
     *
     * @param jsonAsString
     */
    public abstract void writeStream(String name, String jsonAsString);

    public abstract void deleteStream(String name);

    /**
     * The reason this has a seperate method is because it is called when app stopps - so we need to persist synchronously
     */
    public abstract JSONObject readStream(String name);

    public long read(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }

    public boolean readBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public void write(String key, long value) {
        Logger.log.d(TAG, "Write to SP  " + key + " = " + value);
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.putLong(key, value);
        spEditor.apply();
    }

    public void write(String key, int value) {
        Logger.log.d(TAG, "Write to SP  " + key + " = " + value);
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.putInt(key, value);
        spEditor.apply();
    }

    public int read(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    public void write(String key, boolean value) {
        Logger.log.d(TAG, "Write to SP  " + key + " = " + value);
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.putBoolean(key, value);
        spEditor.apply();
    }


    protected void updateSeasonIdAndClearRuntimeData(String seasonId) {
        String currentId = preferences.getString(Constants.SP_SEASON_ID, "");
        if (!currentId.equalsIgnoreCase(seasonId)) {
            resetSeason(seasonId);
        }
    }

    @Override
    public void clearRuntimeData() {
        resetSeason("");
        write(Constants.SP_RAW_RULES, "");
        write(Constants.SP_RAW_TRANSLATIONS, "");
        write(Constants.SP_RAW_JS_FUNCTIONS, "");
        clearExperiments();
    }

    protected void resetSeason(String seasonId) {
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.putString(Constants.SP_SEASON_ID, seasonId);
        spEditor.putLong(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED);
        spEditor.putLong(Constants.SP_LAST_CALCULATE_TIME, Constants.TIME_NOT_DEFINED);
        spEditor.putLong(Constants.SP_LAST_SYNC_TIME, Constants.TIME_NOT_DEFINED);
        spEditor.putString(Constants.SP_LAST_TRANS_FULL_DOWNLOAD_TIME, "");
        spEditor.putString(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");
        spEditor.putString(Constants.SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, "");
        spEditor.putString(Constants.SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, "");
        spEditor.putString(Constants.SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, "");
        spEditor.putString(Constants.SP_LAST_TIME_PRODUCT_CHANGED, "");
        spEditor.remove(Constants.SP_SERVER_FEATURE_LIST);
        spEditor.apply();
        synchronized (lock) {
            inMemoryPreferences.remove(Constants.SP_RAW_RULES);
            inMemoryPreferences.remove(Constants.SP_RAW_TRANSLATIONS);
            inMemoryPreferences.remove(Constants.SP_RAW_JS_FUNCTIONS);
        }
    }

    public void clearExperiments() {
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.remove(Constants.SP_EXPERIMENT_INFO);
        spEditor.apply();
    }

    public void clear() {
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.clear().apply();
        //On test scenario's context could be null or the filesDir could be null
        if (context != null && context.getFilesDir() != null) {
            synchronized (lock) {
                for (String key : (Set<String>) inMemoryPreferences.keySet()) {
                    context.deleteFile(key);
                }
            }
            Hashtable<String, String> filesShouldBeDeleted = new Hashtable<>();
            for (String file : filePersistPreferences) {
                filesShouldBeDeleted.put(file, file);
            }

            File list = new File(context.getFilesDir().toString());
            File[] files = list.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (filesShouldBeDeleted.containsKey(files[i].getName())) {
                    files[i].delete();
                }
            }
        }
        inMemoryPreferences.clear();
    }

    //This method is to simulate app restarting after definition exists on SharedPreferences and file system (but not in memory)
    @TestOnly
    public void clearInMemory() {
        synchronized (lock) {
            inMemoryPreferences.clear();
        }
    }

    /**
     * If the season is not set the SDK hasn't been initialized yet.
     *
     * @return true is the airlock product season was set otherwise false.
     */
    public boolean isInitialized() {
        boolean result = false;
        if (preferences != null) {
            result = !read(Constants.SP_SEASON_ID, "").isEmpty();
        }
        return result;
    }

    public void setPreSyncedFeaturesMap(FeaturesList map) {
        String features = map.toJsonObject().toString();
        write(Constants.SP_PRE_SYNCED_FEATURES_LIST, features);
    }

    public void setSyncedFeaturesMap(FeaturesList map) {
        String features = map.toJsonObject().toString();
        write(Constants.SP_SYNCED_FEATURES_LIST, features);
    }

    public void setServerFeatureMap(FeaturesList map) {
        String features = map.toJsonObject().toString();
        write(Constants.SP_SERVER_FEATURE_LIST, features);
    }

    public void setContextFieldsForAnalytics(String contextFieldsForAnalytics) {
        write(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, contextFieldsForAnalytics);
    }


    @CheckForNull
    @Nullable
    protected Object readSinglePreferenceFromFileSystem(String preferenceName) {
        //because of synchronization it is possible to reach this method but the value is inMemory...
        Object preferenceValue = null;

        synchronized (lock) {
            if (inMemoryPreferences.containsKey(preferenceName)) {
                return inMemoryPreferences.get(preferenceName);
            }
            final long startTime = System.currentTimeMillis();
            FileInputStream fis = null;
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try {
                fis = context.openFileInput(preferenceName);
                int fisLength = (int) fis.getChannel().size();
                if (fisLength > 0) {
                    byte[] buffer = new byte[(int) fis.getChannel().size()];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                }
                if (saveAsJSONPreferences.contains(preferenceName)) {
                    preferenceValue = new JSONObject(result.toString("UTF-8"));
                } else {
                    preferenceValue = result.toString("UTF-8");
                }
            } catch (FileNotFoundException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. File not found.");
            } catch (IOException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to string");
            } catch (JSONException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to JSON");
            } catch (Exception e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to JSON");
            } finally {
                try {
                    fis.close();
                } catch (Throwable ignore) {
                }
            }
            Logger.log.d(TAG, "Read from file system of : " + preferenceName + " took : " + (System.currentTimeMillis() - startTime));
        }
        return preferenceValue;
    }
}

