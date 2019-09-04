package com.ibm.airlock.common.cache;

import com.google.common.annotations.VisibleForTesting;
import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.model.FeaturesList;
import com.ibm.airlock.common.services.StreamsService;
import com.ibm.airlock.common.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

import static com.ibm.airlock.common.util.Constants.*;

/**
 * Created by Denis Voloshin
 */

public abstract class BasePersistenceHandler implements PersistenceHandler {

    protected static final Object lock = new Object();

    private static final String TAG = "BasePersistenceHandler";

    protected final Set<String> filePersistPreferences = new HashSet<>(Arrays.asList(
            Constants.SP_CURRENT_CONTEXT, Constants.SP_DEFAULT_FILE, Constants.SP_RAW_RULES,
                    Constants.SP_RAW_JS_FUNCTIONS, Constants.SP_RAW_TRANSLATIONS, Constants
                    .SP_RANDOMS, Constants.SP_FEATURE_USAGE_STREAMS,
                    Constants.SP_NOTIFICATIONS, Constants.SP_FIRED_NOTIFICATIONS, Constants.SP_NOTIFICATIONS_HISTORY,
                    Constants.SP_FEATURES_PERCENTAGE_MAP, Constants.SP_SYNCED_FEATURES_LIST,
                    Constants.SP_SERVER_FEATURE_LIST, Constants.SP_PRE_SYNCED_FEATURES_LIST,
                    Constants.SP_FEATURE_UTILS_STREAMS

    ));
    protected final Set<String> saveAsJSONPreferences = new HashSet<>(Arrays.asList(
            Constants.SP_CURRENT_CONTEXT, Constants.SP_DEFAULT_FILE, Constants.SP_RAW_RULES,
                    Constants.SP_RAW_TRANSLATIONS, Constants.SP_RANDOMS, Constants
                    .SP_FEATURE_USAGE_STREAMS, Constants.SP_NOTIFICATIONS,
                    Constants.SP_FIRED_NOTIFICATIONS, Constants.SP_NOTIFICATIONS_HISTORY,
                    Constants.SP_FEATURES_PERCENTAGE_MAP, Constants.SP_SYNCED_FEATURES_LIST,
                    Constants.SP_SERVER_FEATURE_LIST, Constants.SP_PRE_SYNCED_FEATURES_LIST,
                    Constants.SP_PRE_SYNCED_ENTITLEMENTS_LIST, Constants.SP_SYNCED_ENTITLEMENTS_LIST

    ));

    protected Cacheable inMemoryPreferences = new InMemoryCache();

    protected SharedPreferences preferences;

    protected Context context;

    BasePersistenceHandler() {
        super();
    }

    protected BasePersistenceHandler(Context c) {
        init(c);
    }

    protected void setContext(Context context) {
        this.context = context;
        init(context);
    }

    @Override
    public abstract void init(Context c, AirlockCallback callback);

    @Override
    public abstract void init(Context c);

    @Override
    public abstract void reset(Context c);


    @Override
    public FeaturesList getCachedPreSyncedFeaturesMap() {
        String featureValues = read(Constants.SP_PRE_SYNCED_FEATURES_LIST, "");
        //noinspection ConstantConditions
        if (featureValues.isEmpty()) {
            return new FeaturesList();
        }
        return new FeaturesList(featureValues, Feature.Source.UNKNOWN);
    }

    @Override
    public FeaturesList getCachedSyncedFeaturesMap() {
        String featureValues = read(Constants.SP_SYNCED_FEATURES_LIST, "");
        //noinspection ConstantConditions
        if (featureValues.isEmpty()) {
            return new FeaturesList();
        }
        return new FeaturesList(featureValues, Feature.Source.UNKNOWN);
    }


    @Override
    public FeaturesList getCachedFeatureMap() {
        String featureValues = read(Constants.SP_SERVER_FEATURE_LIST, "");
        //noinspection ConstantConditions
        if (featureValues.isEmpty()) {
            return new FeaturesList();
        }
        return new FeaturesList(featureValues, Feature.Source.UNKNOWN);
    }

    private JSONObject getRandomMap(String key) {
        JSONObject randoms = readJSON(Constants.SP_RANDOMS);
        JSONObject result = randoms.optJSONObject(key);
        if (result == null){
            result = new JSONObject();
        }
        return result;
    }

    @Override
    public JSONObject getRandomMap() {
        return readJSON(Constants.SP_RANDOMS);
    }

    @Override
    @CheckForNull
    public JSONObject getFeaturesRandomMap() {
        return getRandomMap(JSON_FIELD_FEATURES);
    }

    @Override
    @CheckForNull
    public JSONObject getStreamsRandomMap() {
        return getRandomMap(JSON_FIELD_STREAMS);
    }

    @Override
    public JSONObject getExperimentsRandomMap() {
        return getRandomMap(JSON_FIELD_EXPERIMENTS);
    }

    @Override
    public JSONObject getNotificationsRandomMap() {
        return getRandomMap(JSON_FIELD_NOTIFICATIONS);
    }

    @Override
    public void setFeaturesRandomMap(JSONObject randomsMap) {
        setRandomMap(JSON_FEATURE_FIELD_FEATURES, randomsMap);
    }

    @Override
    public void setStreamsRandomMap(JSONObject randomsMap) {
        setRandomMap(JSON_FIELD_STREAMS, randomsMap);
    }

    @Override
    public void setExperimentsRandomMap(JSONObject randomsMap) {
        setRandomMap(JSON_FEATURE_FIELD_FEATURES, randomsMap);
    }

    @Override
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
    @Override
    public void storeDeviceUserGroups(@Nullable List<String> userGroups, @Nullable StreamsService streamsService) {
        if (userGroups == null) {
            write(Constants.SP_USER_GROUPS, "");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userGroups.size(); i++) {
            sb.append(userGroups.get(i));
            if (i < userGroups.size() - 1) {
                sb.append(',');
            }
        }
        write(Constants.SP_USER_GROUPS, sb.toString());
        if (streamsService != null) {
            streamsService.updateStreamsEnablement();
        }
    }


    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    @Override
    public String getDevelopBranchName() {
        //noinspection ConstantConditions
        return read(Constants.SP_DEVELOP_BRANCH_NAME, "").trim();
    }

    /**
     * Stores a selected develop branch name to the local store
     */
    @Override
    public void setDevelopBranchName(String selectedDevelopBranchName) {
        write(Constants.SP_DEVELOP_BRANCH_NAME, selectedDevelopBranchName);
    }

    /**
     * returns the last branch name from the local store
     *
     * @return the name of the last branch name used
     */
    @Override
    public String getLastBranchName() {
        //noinspection ConstantConditions
        return read(Constants.SP_BRANCH_NAME, JSON_FIELD_ROOT);
    }

    /**
     * Stores the current branch name to the local store
     */
    @Override
    public void setLastBranchName(String selectedDevelopBranchName) {
        write(Constants.SP_BRANCH_NAME, selectedDevelopBranchName);
    }

    /**
     * Return a selected develop branch id from the local store
     *
     * @return the id of a selected develop branch
     */
    @Override
    public String getDevelopBranchId() {
        //noinspection ConstantConditions
        return read(Constants.SP_DEVELOP_BRANCH_ID, "").trim();
    }

    /**
     * Stores a selected develop branch id to the local store
     */
    @Override
    public void setDevelopBranchId(String selectedDevelopBranchId) {
        write(Constants.SP_DEVELOP_BRANCH_ID, selectedDevelopBranchId);
    }

    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    @Override
    public String getDevelopBranch() {
        //noinspection ConstantConditions
        return read(Constants.SP_DEVELOP_BRANCH, "").trim();
    }


    /**
     * Stores a selected develop branch config (in JSON) to the local store
     */
    @Override
    public synchronized void setDevelopBranch(String selectedDevelopBranch) {
        write(Constants.SP_DEVELOP_BRANCH, selectedDevelopBranch);
    }


    /**
     * Return a list from the local store of user groups
     * each group could be selected on not.
     *
     * @return a list of user groups
     */
    @Override
    public List<String> getDeviceUserGroups() {
        List<String> result = new ArrayList<>();
        String groups = read(Constants.SP_USER_GROUPS, "");
        //noinspection ConstantConditions
        if (groups.isEmpty()) {
            return result;
        }
        String[] groupsArray = groups.split(",");
        return Arrays.asList(groupsArray);
    }

    @Override
    public JSONObject readJSON(String key) {
        JSONObject value;
        if (inMemoryPreferences.containsKey(key)) {
            value = (JSONObject) inMemoryPreferences.get(key);
        } else {
            value = (JSONObject) readSinglePreferenceFromFileSystem(key);
        }
        if (value == null) {
            value = new JSONObject();
        }
        return value;
    }

    @Override
    @CheckForNull
    public Set<String> readSet(@Nullable String key) {
        if (key == null) {
            return null;
        }
        try {
            @SuppressWarnings("ConstantConditions") JSONArray array = new JSONArray(preferences.getString(key, key));
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


    @SuppressWarnings("ConstantConditions")
    String readFromMemory(String key, String defaultValue) {
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

    @CheckForNull
    @Override
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

    @Override
    public abstract void write(String key, String value);

    @Override
    public abstract void write(String key, JSONObject value);

    /**
     * The reason this has a separate method is because it is called when app stops - so we need to persist synchronously
     *
     * @param jsonAsString the json value as string
     */
    @Override
    public abstract void writeStream(String name, String jsonAsString);

    public abstract void deleteStream(String name);

    /**
     * The reason this has a separate method is because it is called when app stops - so we need to persist synchronously
     */
    @Override
    public abstract JSONObject readStream(String name);

    @Override
    public long read(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }

    @Override
    public boolean isBooleanTrue(String key, boolean defaultValue) {
        return preferences.isBooleanTrue(key, defaultValue);
    }

    @Override
    public void write(String key, long value) {
        Logger.log.d(TAG, "Write to SP  " + key + " = " + value);
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.putLong(key, value);
        spEditor.apply();
    }

    @Override
    public void write(String key, int value) {
        Logger.log.d(TAG, "Write to SP  " + key + " = " + value);
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.putInt(key, value);
        spEditor.apply();
    }

    @Override
    public int read(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    @Override
    public void write(String key, boolean value) {
        Logger.log.d(TAG, "Write to SP  " + key + " = " + value);
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.putBoolean(key, value);
        spEditor.apply();
    }


    protected void updateSeasonIdAndClearRuntimeData(String seasonId) {
        String currentId = preferences.getString(Constants.SP_SEASON_ID, "");
        if (currentId != null && !currentId.equalsIgnoreCase(seasonId)) {
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

    private void resetSeason(String seasonId) {
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
        inMemoryPreferences.remove(Constants.SP_RAW_RULES);
        inMemoryPreferences.remove(Constants.SP_RAW_TRANSLATIONS);
        inMemoryPreferences.remove(Constants.SP_RAW_JS_FUNCTIONS);
    }

    @Override
    public void clearExperiments() {
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.remove(Constants.SP_EXPERIMENT_INFO);
        spEditor.apply();
    }

    @Override
    public void clear() {
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.clear().apply();
        //On test scenario's context could be null or the filesDir could be null
        if (context != null && context.getFilesDir() != null) {
            for (String key : (Set<String>) inMemoryPreferences.keySet()) {
                context.deleteFile(key);
            }

            Hashtable<String, String> filesShouldBeDeleted = new Hashtable<>();
            for (String file : filePersistPreferences) {
                filesShouldBeDeleted.put(file, file);
            }

            File list = new File(context.getFilesDir().getPath());
            File[] files = list.listFiles();
            if (files != null){
                for (File file : files) {
                    if (filesShouldBeDeleted.containsKey(file.getName())) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                }
            }

        }
        inMemoryPreferences.clear();
    }

    //This method is to simulate app restarting after definition exists on SharedPreferences and file system (but not in memory)
    @Override
    @VisibleForTesting
    public void clearInMemory() {
        inMemoryPreferences.clear();
    }

    /**
     * If the season is not set the SDK hasn't been initialized yet.
     *
     * @return true is the airlock product season was set otherwise false.
     */
    @Override
    @SuppressWarnings("VariableNotUsedInsideIf")
    public boolean isInitialized() {
        boolean result = false;
        if (preferences != null) {
            //noinspection ConstantConditions
            result = !read(Constants.SP_SEASON_ID, "").isEmpty();
        }
        return result;
    }

    @Override
    public void setPreSyncedFeaturesMap(FeaturesList map) {
        String features = map.toJsonObject().toString();
        write(Constants.SP_PRE_SYNCED_FEATURES_LIST, features);
    }

    @Override
    public void setSyncedFeaturesMap(FeaturesList map) {
        String features = map.toJsonObject().toString();
        write(Constants.SP_SYNCED_FEATURES_LIST, features);
    }

    @Override
    public void setServerFeatureMap(FeaturesList map) {
        String features = map.toJsonObject().toString();
        write(Constants.SP_SERVER_FEATURE_LIST, features);
    }

    @Override
    public void setContextFieldsForAnalytics(String contextFieldsForAnalytics) {
        write(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, contextFieldsForAnalytics);
    }


    @CheckForNull
    @Nullable
    Object readSinglePreferenceFromFileSystem(String preferenceName) {
        //because of synchronization it is possible to reach this method but the value is inMemory...
        Object preferenceValue = null;

        synchronized (lock) {
            if (inMemoryPreferences.containsKey(preferenceName)) {
                return inMemoryPreferences.get(preferenceName);
            }
            final long startTime = System.currentTimeMillis();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try (FileInputStream fis = context.openFileInput(preferenceName)) {
                long fisLength = fis.getChannel().size();
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
                //inMemoryPreferences.put(preferenceName, preferenceValue);
            } catch (FileNotFoundException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. File not found.");
            } catch (IOException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to string");
            } catch (Exception e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to JSON");
            }
            Logger.log.d(TAG, "Read from file system of : " + preferenceName + " took : " + (System.currentTimeMillis() - startTime));
        }
        return preferenceValue;
    }
}

