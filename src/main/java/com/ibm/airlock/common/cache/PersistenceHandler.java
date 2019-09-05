package com.ibm.airlock.common.cache;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.model.FeaturesList;
import com.ibm.airlock.common.services.StreamsService;

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
public interface PersistenceHandler {

    public void init(Context context, AirlockCallback callback);

    public void init(Context context);

    public void reset(Context context);

    FeaturesList getCachedFeatureMap();

    FeaturesList getCachedSyncedFeaturesMap();

    FeaturesList getCachedPreSyncedFeaturesMap();

    @CheckForNull
    public JSONObject getFeaturesRandomMap();

    @CheckForNull
    public JSONObject getStreamsRandomMap();

    public void setFeaturesRandomMap(JSONObject randomMap);

    @CheckForNull
    public JSONObject getPurchasesRandomMap();

    public void setPurchasesRandomMap(JSONObject randomMap);

    public void setStreamsRandomMap(JSONObject randomMap);

    /**
     * Stores a map of user groups
     * each group could be selected on not.
     *
     * @throws JSONException
     */
    public void storeDeviceUserGroups(@Nullable List<String> userGroups, StreamsService streamsService);


    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    public String getDevelopBranchName();

    /**
     * Stores a selected develop branch name to the local store
     */
    public void setDevelopBranchName(String selectedDevelopBranchName);

    /**
     * returns the last branch name from the local store
     *
     * @return the name of the last branch name used
     */
    public String getLastBranchName();

    /**
     * Stores the current branch name to the local store
     */
    public void setLastBranchName(String selectedDevelopBranchName);

    /**
     * Return a selected develop branch id from the local store
     *
     * @return the identification of a selected develop branch
     */
    public String getDevelopBranchId();

    /**
     * Stores a selected develop branch id to the local store
     */
    public void setDevelopBranchId(String selectedDevelopBranchId);

    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    public String getDevelopBranch();

    /**
     * Stores a selected develop branch config (in JSON) to the local store
     */
    public void setDevelopBranch(String selectedDevelopBranch);


    /**
     * Return a list from the local store of user groups
     * each group could be selected on not.
     *
     * @return a list of user groups
     */
    public List<String> getDeviceUserGroups();

    public JSONObject readJSON(String key);

    @CheckForNull
    public Set<String> readSet(String key);

    public String read(String key, String defaultValue);

    public void write(String key, String value);

    public void write(String key, JSONObject value);
    /**
     * The reason this has a seperate method is because it is called when app stopps - so we need to persist synchronously
     *
     * @param jsonAsString
     */
    public void writeStream(String name, String jsonAsString);

    /**
     * The reason this has a seperate method is because it is called when app stopps - so we need to persist synchronously
     */
    public JSONObject readStream(String name);

    public long read(String key, long defaultValue);

    public boolean isBooleanTrue(String key, boolean defaultValue);

    public void write(String key, long value);

    public void write(String key, int value);

    public int read(String key, int defaultValue);

    public void write(String key, boolean value);

    public void clearExperiments();

    public void clear();

    //This method is to simulate app restarting after definition exists on SharedPreferences and file system (but not in memory)
    public void clearInMemory();

    /**
     * If the season is not set the SDK hasn't been initialized yet.
     *
     * @return true is the airlock product season was set otherwise false.
     */
    public boolean isInitialized();

    public void setContextFieldsForAnalytics(String s);

    public void setServerFeatureMap(FeaturesList<Feature> preSyncServerFeatureList);

    public void setSyncedFeaturesMap(FeaturesList<Feature> syncServerFeatureList);

    public void setPreSyncedFeaturesMap(FeaturesList<Feature> preSyncServerFeatureList);

    public JSONObject getNotificationsRandomMap();
    public void setNotificationsRandomMap(JSONObject randomMap);

    public JSONObject getExperimentsRandomMap();
    public void setExperimentsRandomMap(JSONObject randomMap);

    void clearRuntimeData();

    JSONObject getRandomMap();
}
