package com.ibm.airlock.common.cache;

import com.google.common.annotations.VisibleForTesting;
import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.cache.pref.FilePreferencesFactory;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.ibm.airlock.common.util.Constants.*;

/**
 * Created by Denis Voloshin
 */

@SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext", "FieldAccessedSynchronizedAndUnsynchronized", "OverlyComplexClass", "unused"})
public abstract class BasePersistenceHandler implements PersistenceHandler {

    private static final String TAG = "BasePersistenceHandler";
    @SuppressWarnings("WeakerAccess")
    public static final int DEFAULT_IN_MEMORY_EXPIRATION_PERIOD = 10 * 1000; // on minute
    private static final String USER_GROUP_NAMES_DELIMITER = ",";
    protected final Hashtable<String, ReentrantReadWriteLock> persistenceFilesReadWriteLocks = new Hashtable<>();
    protected final String productName;

    @SuppressWarnings("WeakerAccess")
    protected final Collection<String> filePersistPreferences = new HashSet<>(Arrays.asList(
            SP_CURRENT_CONTEXT, SP_DEFAULT_FILE, SP_RAW_RULES,
            SP_RAW_JS_FUNCTIONS, SP_RAW_TRANSLATIONS,
            SP_RANDOMS, SP_FEATURE_USAGE_STREAMS,
            SP_NOTIFICATIONS, SP_FIRED_NOTIFICATIONS, SP_NOTIFICATIONS_HISTORY,
            SP_FEATURES_PERCENAGE_MAP, SP_SYNCED_FEATURES_LIST,
            SP_SERVER_FEATURE_LIST, SP_PRE_SYNCED_FEATURES_LIST,
            SP_FEATURE_UTILS_STREAMS

    ));
    @SuppressWarnings("WeakerAccess")
    protected final Collection<String> saveAsJSONPreferences = new HashSet<>(Arrays.asList(
            SP_CURRENT_CONTEXT, SP_DEFAULT_FILE, SP_RAW_RULES,
            SP_RAW_TRANSLATIONS, SP_RANDOMS,
            SP_FEATURE_USAGE_STREAMS, SP_NOTIFICATIONS,
            SP_FIRED_NOTIFICATIONS, SP_NOTIFICATIONS_HISTORY,
            SP_FEATURES_PERCENAGE_MAP, SP_SYNCED_FEATURES_LIST,
            SP_SERVER_FEATURE_LIST, SP_PRE_SYNCED_FEATURES_LIST,
            SP_PRE_SYNCED_ENTITLEMENTS_LIST, SP_SYNCED_ENTITLEMENTS_LIST

    ));
    @SuppressWarnings("WeakerAccess")
    protected Cacheable<String, Object> inMemoryPreferences = new InMemoryCache<>();

    @SuppressWarnings("unused")
    protected SharedPreferences preferences;

    protected Context context;

    /**
     * Default constructor
     *
     * @param context airlock context
     */
    @SuppressWarnings("WeakerAccess")
    protected BasePersistenceHandler(Context context) {
        this.context = context;
        productName = this.context.getAirlockProductName();

        preferences = new DefaultPreferences(FilePreferencesFactory.getAirlockCacheDirectory() + File.separator +
                context.getAirlockProductName() + File.separator + context.getSeasonId());


        //init read/write locks
        for (String filePersistPreference : filePersistPreferences) {
            if (!persistenceFilesReadWriteLocks.containsKey(filePersistPreference)) {
                persistenceFilesReadWriteLocks.put(filePersistPreference, new ReentrantReadWriteLock(true));
            }
        }
    }


    protected void setContext(Context context) {
        synchronized (context) {
            this.context = context;
            init(context);
        }
    }

    public String getProductName() {
        return productName;
    }

    @SuppressWarnings("ParameterHidesMemberVariable")
    @Override
    public abstract void init(Context context, AirlockCallback callback);

    @SuppressWarnings("ParameterHidesMemberVariable")
    @Override
    public abstract void init(Context context);

    @SuppressWarnings("ParameterHidesMemberVariable")
    @Override
    public abstract void reset(Context context);

    public abstract void destroy();

    protected abstract String getResourcePersistenceLocation(String resourceName);

    @Override
    public FeaturesList<Feature> getCachedPreSyncedFeaturesMap() {
        String featureValues = read(SP_PRE_SYNCED_FEATURES_LIST, "");
        if (featureValues.isEmpty()) {
            return new FeaturesList<>();
        }
        return new FeaturesList<>(featureValues, Feature.Source.UNKNOWN);
    }

    @Override
    public FeaturesList<Feature> getCachedSyncedFeaturesMap() {
        String featureValues = read(SP_SYNCED_FEATURES_LIST, "");
        if (featureValues.isEmpty()) {
            return new FeaturesList<>();
        }
        return new FeaturesList<>(featureValues, Feature.Source.UNKNOWN);
    }

    @Override
    public FeaturesList<Feature> getCachedFeatureMap() {
        String featureValues = read(SP_SERVER_FEATURE_LIST, "");
        if (featureValues.isEmpty()) {
            return new FeaturesList<>();
        }
        return new FeaturesList<>(featureValues, Feature.Source.UNKNOWN);
    }

    private JSONObject getRandomMap(String key) {
        JSONObject randoms = readJSON(SP_RANDOMS);
        JSONObject result = randoms.optJSONObject(key);
        if (result == null) {
            result = new JSONObject();
        }
        return result;
    }

    @Override
    public JSONObject getRandomMap() {
        return readJSON(SP_RANDOMS);
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
    public void setFeaturesRandomMap(JSONObject randomMap) {
        setRandomMap(JSON_FEATURE_FIELD_FEATURES, randomMap);
    }

    @Override
    public void setStreamsRandomMap(JSONObject randomMap) {
        setRandomMap(JSON_FIELD_STREAMS, randomMap);
    }

    @Override
    public void setExperimentsRandomMap(JSONObject randomMap) {
        setRandomMap(JSON_FEATURE_FIELD_FEATURES, randomMap);
    }

    @Override
    public void setNotificationsRandomMap(JSONObject randomMap) {
        setRandomMap(JSON_FIELD_NOTIFICATIONS, randomMap);
    }

    private void setRandomMap(String key, JSONObject randomMap) {
        JSONObject randoms = readJSON(SP_RANDOMS);
        randoms.put(key, randomMap);
        write(SP_RANDOMS, randoms.toString());
    }

    /**
     * Stores a map of user groups
     * each group could be selected on not.
     */
    @Override
    public void storeDeviceUserGroups(@Nullable List<String> userGroups, @Nullable StreamsService streamsService) {
        if (userGroups == null) {
            write(SP_USER_GROUPS, "");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userGroups.size(); i++) {
            sb.append(userGroups.get(i));
            if (i < userGroups.size() - 1) {
                sb.append(USER_GROUP_NAMES_DELIMITER);
            }
        }
        write(SP_USER_GROUPS, sb.toString());
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
        return read(SP_DEVELOP_BRANCH_NAME, "").trim();
    }

    /**
     * Stores a selected develop branch name to the local store
     */
    @Override
    public void setDevelopBranchName(String selectedDevelopBranchName) {
        write(SP_DEVELOP_BRANCH_NAME, selectedDevelopBranchName);
    }

    /**
     * returns the last branch name from the local store
     *
     * @return the name of the last branch name used
     */
    @Override
    public String getLastBranchName() {
        return read(SP_BRANCH_NAME, JSON_FIELD_ROOT);
    }

    /**
     * Stores the current branch name to the local store
     */
    @Override
    public void setLastBranchName(String selectedDevelopBranchName) {
        write(SP_BRANCH_NAME, selectedDevelopBranchName);
    }

    /**
     * Return a selected develop branch id from the local store
     *
     * @return the id of a selected develop branch
     */
    @Override
    public String getDevelopBranchId() {
        return read(SP_DEVELOP_BRANCH_ID, "").trim();
    }

    /**
     * Stores a selected develop branch id to the local store
     */
    @Override
    public void setDevelopBranchId(String selectedDevelopBranchId) {
        write(SP_DEVELOP_BRANCH_ID, selectedDevelopBranchId);
    }

    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    @Override
    public String getDevelopBranch() {
        return read(SP_DEVELOP_BRANCH, "").trim();
    }


    /**
     * Stores a selected develop branch config (in JSON) to the local store
     */
    @Override
    public synchronized void setDevelopBranch(String selectedDevelopBranch) {
        write(SP_DEVELOP_BRANCH, selectedDevelopBranch);
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
        String groups = read(SP_USER_GROUPS, "");
        if (groups.isEmpty()) {
            return result;
        }
        String[] groupsArray = groups.split(",");
        return Arrays.asList(groupsArray);
    }

    @Override
    public JSONObject readJSON(String key) {
        JSONObject value;
        value = inMemoryPreferences.containsKey(key) ?
                (JSONObject) inMemoryPreferences.get(key) : (JSONObject) readSinglePreferenceFromFileSystem(key);
        if (value == null) {
            value = new JSONObject();
        }
        return value;
    }

    @Override
    @CheckForNull
    public Set<String> readSet(@Nullable String key) {
        if (key == null || preferences.getString(key, key) == null) {
            return null;
        }
        try {
            String setAsArray = preferences.getString(key, key);
            if (setAsArray != null) {
                JSONArray array = new JSONArray(setAsArray);
                Set<String> set = new HashSet<>();
                int len = array.length();
                for (int i = 0; i < len; i++) {
                    set.add(array.get(i).toString());
                }
                return set;
            }
        } catch (JSONException e) {
            return null;
        }
        return null;
    }


    String readFromMemory(String key, String defaultValue) {
        Object pref = inMemoryPreferences.get(key);
        if (pref != null) {
            return pref.toString();
        } else {
            Object fileContent = readSinglePreferenceFromFileSystem(key);
            return fileContent != null ? fileContent.toString() : defaultValue;
        }
    }

    @Override
    public String read(String key, String defaultValue) {
        String value;
        if (filePersistPreferences.contains(key)) {
            return readFromMemory(key, defaultValue);
        }
        value = preferences != null ? preferences.getString(key, defaultValue) : defaultValue;
        return value == null ? defaultValue : value;
    }

    @Override
    public void write(String key, JSONObject value) {
        write(key, value.toString());
    }

    /**
     * The reason this has a separate method is because it is called when app stops - so we need to persist synchronously
     *
     * @param jsonAsString stream in the JSON representation
     */
    @Override
    public abstract void writeStream(String name, String jsonAsString);

    /**
     * Delete a stream by name
     *
     * @param name Stream name
     */
    @SuppressWarnings("unused")
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


    /**
     * If the season id changed we the runtime data should be cleared
     *
     * @param seasonId season id
     */
    @SuppressWarnings("WeakerAccess")
    protected void updateSeasonIdAndClearRuntimeData(String seasonId) {
        String currentId = preferences.getString(SP_SEASON_ID, "");
        if (currentId != null && !currentId.equalsIgnoreCase(seasonId)) {
            resetSeason(seasonId);
        }
    }

    @Override
    public void clearRuntimeData() {
        resetSeason("");
        write(SP_RAW_RULES, "");
        write(SP_RAW_TRANSLATIONS, "");
        write(SP_RAW_JS_FUNCTIONS, "");
        clearExperiments();
    }

    private void resetSeason(String seasonId) {
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.putString(SP_SEASON_ID, seasonId);
        spEditor.putLong(SP_LAST_FEATURES_PULL_TIME, TIME_NOT_DEFINED);
        spEditor.putLong(SP_LAST_CALCULATE_TIME, TIME_NOT_DEFINED);
        spEditor.putLong(SP_LAST_SYNC_TIME, TIME_NOT_DEFINED);
        spEditor.putString(SP_LAST_TRANS_FULL_DOWNLOAD_TIME, "");
        spEditor.putString(SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");
        spEditor.putString(SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, "");
        spEditor.putString(SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, "");
        spEditor.putString(SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, "");
        spEditor.putString(SP_LAST_TIME_PRODUCT_CHANGED, "");
        spEditor.remove(SP_SERVER_FEATURE_LIST);
        spEditor.apply();
        inMemoryPreferences.remove(SP_RAW_RULES);
        inMemoryPreferences.remove(SP_RAW_TRANSLATIONS);
        inMemoryPreferences.remove(SP_RAW_JS_FUNCTIONS);
    }

    @Override
    public void clearExperiments() {
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.remove(SP_EXPERIMENT_INFO);
        spEditor.apply();
    }

    @Override
    public void clear() {
        SharedPreferences.Editor spEditor = preferences.edit();
        spEditor.clear().apply();
        synchronized (context) {
            //On test scenario's context could be null or the filesDir could be null
            if (context != null) {
                for (String key : inMemoryPreferences.keySet()) {
                    context.deleteFile(key);
                }

                Map<String, String> filesShouldBeDeleted = new HashMap<>();
                for (String file : filePersistPreferences) {
                    filesShouldBeDeleted.put(file, file);
                }

                File list = new File(context.getFilesDir().toString());
                File[] files = list.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (filesShouldBeDeleted.containsKey(file.getName())) {
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                        }
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
            result = !read(SP_SEASON_ID, "").isEmpty();
        }
        return result;
    }

    @Override
    public void setPreSyncedFeaturesMap(FeaturesList<Feature> preSyncServerFeatureList) {
        String features = preSyncServerFeatureList.toJsonObject().toString();
        write(SP_PRE_SYNCED_FEATURES_LIST, features);
    }

    @Override
    public void setSyncedFeaturesMap(FeaturesList<Feature> syncServerFeatureList) {
        String features = syncServerFeatureList.toJsonObject().toString();
        write(SP_SYNCED_FEATURES_LIST, features);
    }

    @Override
    public void setServerFeatureMap(FeaturesList<Feature> preSyncServerFeatureList) {
        String features = preSyncServerFeatureList.toJsonObject().toString();
        write(SP_SERVER_FEATURE_LIST, features);
    }

    @Override
    public void setContextFieldsForAnalytics(String contextFieldsForAnalytics) {
        write(JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, contextFieldsForAnalytics);
    }

    @CheckForNull
    protected ReentrantReadWriteLock getWriteReadLocker(String resourceName) {
        if (persistenceFilesReadWriteLocks.containsKey(resourceName)) {
            return persistenceFilesReadWriteLocks.get(resourceName);
        }
        return null;
    }

    public void write(String key, String value) {
        if (filePersistPreferences.contains(key)) {
            if (value != null && !value.isEmpty()) {
                if (saveAsJSONPreferences.contains(key)) {
                    try {
                        inMemoryPreferences.put(key, new JSONObject(value), DEFAULT_IN_MEMORY_EXPIRATION_PERIOD);
                    } catch (JSONException e) {
                        Logger.log.e(TAG, "Failed to convert content of: " + key + " to JSONObject.");
                    }
                } else {
                    inMemoryPreferences.put(key, value, DEFAULT_IN_MEMORY_EXPIRATION_PERIOD);
                }
                new Thread(new FilePreferencesPersistent(key, value)).start();

            } else {
                //remove model
                inMemoryPreferences.remove(key);
                //context.deleteFile(key);
                File fileDel = new File(context.getFilesDir(), key);
                fileDel.delete();
            }
        } else {
            if (key.equals(Constants.SP_SEASON_ID)) {
                updateSeasonIdAndClearRuntimeData(value);
                return;
            }
            preferences.edit().putString(key, value);
            preferences.edit().doCommit();
        }
    }

    abstract String getFileSystemLocationByPreferenceName(String preferenceName);


    protected synchronized Object readSinglePreferenceFromFileSystem(String preferenceName) {
        //because of synchronization it is possible to reach this method but the value is inMemory...
        if (inMemoryPreferences.containsKey(preferenceName)) {
            return inMemoryPreferences.get(preferenceName);
        }
        Object preferenceValue = null;
        InputStream fis = null;
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        try {
            // get read/write lock for the specific file.
            getWriteReadLocker(preferenceName).readLock().lock();

            File fileInput = context.openFile(getFileSystemLocationByPreferenceName(preferenceName));
            if (!fileInput.exists()) {
                return preferenceValue;
            }
            fis = PersistenceEncryptor.decryptAES(fileInput);
            if (fis != null) {
                byte[] buffer = new byte[fis.available()];
                int length;
                while ((length = fis.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                if (saveAsJSONPreferences.contains(preferenceName)) {
                    preferenceValue = new JSONObject(result.toString("UTF-8"));
                } else {
                    preferenceValue = result.toString("UTF-8");
                }
                inMemoryPreferences.put(preferenceName, preferenceValue);
            }
        } catch (IOException | JSONException e) {
            Logger.log.e(TAG, e.getMessage(), e);
        } finally {
            try {
                getWriteReadLocker(preferenceName).readLock().unlock();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                fis.close();
            } catch (Throwable ignore) {
            }
        }
        return preferenceValue;
    }

    class FilePreferencesPersistent implements Runnable {
        private final String key;
        private final String value;

        FilePreferencesPersistent(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void run() {
            FileOutputStream fos = null;
            try {
                // wait for read/write lock for the specific file.
                getWriteReadLocker(key).writeLock().lock();

                File outputFile = new File(new File(getResourcePersistenceLocation(key)), key);
                outputFile.createNewFile();
                fos = new FileOutputStream(outputFile);

                if (fos == null) {
                    //On tests that use mock context the FileOutputStream could be null...
                    return;
                }
                fos.write(value.getBytes());
                fos.flush();
                PersistenceEncryptor.encryptAES(outputFile);
            } catch (IOException e) {
                Logger.log.w(TAG, "Failed to persist content of: " + key + " to file system. Error: " + e.getMessage());
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    Logger.log.w(TAG, "Failed to close File Output stream of: " + key + " : " + e.getMessage());
                }
                getWriteReadLocker(key).writeLock().unlock();
            }
        }
    }
}

