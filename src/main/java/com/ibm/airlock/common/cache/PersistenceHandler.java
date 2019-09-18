package com.ibm.airlock.common.cache;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.model.FeaturesList;
import com.ibm.airlock.common.services.StreamsService;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;


/**
 * To manage shared preference
 *
 * @author Denis Voloshin
 */
@SuppressWarnings("unused")
public interface PersistenceHandler {

    void init(Context c, AirlockCallback callback);

    void init(Context c);

    void reset(Context c);

    FeaturesList getCachedFeatureMap();

    FeaturesList getCachedSyncedFeaturesMap();

    FeaturesList getCachedPreSyncedFeaturesMap();

    @CheckForNull
    JSONObject getFeaturesRandomMap();

    @CheckForNull
    JSONObject getNotificationsRandomMap();

    @CheckForNull
    JSONObject getStreamsRandomMap();

    void setFeaturesRandomMap(JSONObject randomMap);

    @CheckForNull
    JSONObject getPurchasesRandomMap();

    void setPurchasesRandomMap(JSONObject randomMap);

    void setStreamsRandomMap(JSONObject randomMap);

    /**
     * Stores a map of user groups
     * each group could be selected on not.
     *
     * @throws JSONException the JSON exception thrown
     */
    void storeDeviceUserGroups(@Nullable List<String> userGroups, StreamsService streamsManage);


    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    String getDevelopBranchName();

    /**
     * Stores a selected develop branch name to the local store
     */
    void setDevelopBranchName(String selectedDevelopBranchName);

    /**
     * returns the last branch name from the local store
     *
     * @return the name of the last branch name used
     */
    String getLastBranchName();

    /**
     * Stores the current branch name to the local store
     */
    void setLastBranchName(String selectedDevelopBranchName);

    /**
     * Return a selected develop branch id from the local store
     *
     * @return the identification of a selected develop branch
     */
    String getDevelopBranchId();

    /**
     * Stores a selected develop branch id to the local store
     */
    void setDevelopBranchId(String selectedDevelopBranchId);

    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    String getDevelopBranch();

    /**
     * Stores a selected develop branch config (in JSON) to the local store
     */
    void setDevelopBranch(String selectedDevelopBranch);


    /**
     * Return a list from the local store of user groups
     * each group could be selected on not.
     *
     * @return a list of user groups
     */
    List<String> getDeviceUserGroups();

    JSONObject readJSON(String key);

    @CheckForNull
    Set<String> readSet(String key);

    String read(String key, String defaultValue);

    void write(String key, String value);

    void write(String key, JSONObject value);

    /**
     *
     * The reason this has a separate method is because it is called when app stops - so we need to persist synchronously
     *
     * @param name the stream name that is being written
     * @param jsonAsString the stream as json string
     */
    void writeStream(String name, String jsonAsString);

    /**
     * The reason this has a separate method is because it is called when app stops - so we need to persist synchronously
     */
    JSONObject readStream(String name);

    long read(String key, long defaultValue);

    boolean isBooleanTrue(String key, boolean defaultValue);

    void write(String key, long value);

    void write(String key, int value);

    int read(String key, int defaultValue);

    void write(String key, boolean value);

    void clearExperiments();

    @SuppressWarnings("unused")
    @TestOnly
    void clear();

    //This method is to simulate app restarting after definition exists on SharedPreferences and file system (but not in memory)
    @TestOnly
    void clearInMemory();

    /**
     * If the season is not set the SDK hasn't been initialized yet.
     *
     * @return true is the airlock product season was set otherwise false.
     */
    boolean isInitialized();

    void setContextFieldsForAnalytics(String s);

    void setServerFeatureMap(FeaturesList preSyncServerFeatureList);

    void setSyncedFeaturesMap(FeaturesList syncServerFeatureList);

    void setPreSyncedFeaturesMap(FeaturesList syncServerFeatureList);

    void setNotificationsRandomMap(JSONObject randomMap);

    JSONObject getExperimentsRandomMap();

    void setExperimentsRandomMap(JSONObject randomMap);

    void clearRuntimeData();

    JSONObject getRandomMap();
}
