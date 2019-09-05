package com.ibm.airlock.common.cache;


import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.cache.pref.FilePreferences;
import com.ibm.airlock.common.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * Default persistence manager implementation is based on File System
 *
 * @author Denis Voloshin
 */
public class DefaultPersistenceHandler extends BasePersistenceHandler {

    private static final String TAG = "DefaultPersistenceHandler";
    private final ReentrantReadWriteLock streamsReadWriteLock = new ReentrantReadWriteLock(true);

    private final Collection<String> instanceRuntimeFiles = new HashSet<>(Arrays.asList(
            Constants.SP_CURRENT_CONTEXT,
            Constants.SP_FIRED_NOTIFICATIONS,
            Constants.SP_NOTIFICATIONS_HISTORY,
            Constants.SP_SYNCED_FEATURES_LIST,
            Constants.SP_SERVER_FEATURE_LIST,
            Constants.SP_PRE_SYNCED_FEATURES_LIST
    ));

    private final Collection<String> instanceReferenceKeys = new HashSet<>(Arrays.asList(
            Constants.SP_LAST_CALCULATE_TIME, Constants.SP_LAST_SYNC_TIME
    ));


    public DefaultPersistenceHandler(Context context) {
        super(context);
        init(context);

        preferences = new DefaultPreferences(FilePreferencesFactory.getAirlockCacheDirectory() + File.separator +
                context.getAirlockProductName() + File.separator + context.getSeasonId() +
                File.separator + context.getAppVersion() + File.separator + context.getInstanceId());

        for (String filePersistPreference : instanceRuntimeFiles) {
            if (!persistenceFilesReadWriteLocks.containsKey(filePersistPreference)) {
                persistenceFilesReadWriteLocks.put(filePersistPreference, new ReentrantReadWriteLock(true));
            }
        }

        //If first time the app starts or it is a tests mock app (files dir is null) - do not read from file system
        if (isInitialized() && context.getFilesDir() != null) {
            new Thread(new FilePreferencesReader()).start();
        }
    }

    @Override
    public String getResourcePersistenceLocation(String resourceName) {
        if (instanceRuntimeFiles.contains(resourceName)) {
            return context.getFilesDir() + File.separator + context.getAppVersion()
                    + File.separator + context.getInstanceId();
        } else {
            return context.getFilesDir() + File.separator + context.getAppVersion();
        }
    }

    @Override
    public void init(Context c, AirlockCallback callback) {
        callback.onSuccess("");
    }

    @Override
    public synchronized void init(Context context) {
        inMemoryPreferences = new InMemoryCache();
    }

    @Override
    @SuppressWarnings("VariableNotUsedInsideIf")
    public synchronized void reset(Context c) {
        clearRuntimeData();
        if (preferences != null) {
            clearInstanceRuntimeFiles();
            clearStreams();
            destroy();
        }
    }

    @CheckForNull
    @Override
    public JSONObject getPurchasesRandomMap() {
        return null;
    }

    @Override
    public void setPurchasesRandomMap(JSONObject randomMap) {

    }

    public synchronized void destroy() {
        clearInstanceRuntimeFiles();
        clearStreams();

        // remove instance preferences node
        try {
            ((DefaultPreferences) preferences).removeNode();
        } catch (BackingStoreException e) {
            Logger.log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void clearStreams() {
        if (context != null) {
            Context instanceContext = context;
            File folder = new File(instanceContext.getFilesDir() + File.separator + instanceContext.getAppVersion()
                    + File.separator + instanceContext.getInstanceId());
            if (folder.listFiles() != null && folder.listFiles().length > 0) {
                for (File file : folder.listFiles()) {
                    if (file.getName().startsWith(Constants.STREAM_PREFIX)) {
                        file.delete();
                    }

                }
            }
        }
    }


    private void clearInstanceRuntimeFiles() {
        //On test scenario's context could be null or the filesDir could be null
        if (context != null) {
            File folder = new File(context.getFilesDir() + File.separator + context.getAppVersion()
                    + File.separator + context.getInstanceId());
            for (String file : instanceRuntimeFiles) {
                new File(folder, file).delete();

            }
        }
        inMemoryPreferences.clear();
    }


    /**
     * The reason this has a separate method is because it is called when app stops - so we need to persist synchronously
     *
     * @param jsonAsString
     */
    @Override
    public void writeStream(String streamName, String jsonAsString) {
        if (jsonAsString != null && !jsonAsString.isEmpty()) {
            //if it is a tests mock app (files dir is null) - do not write to file system
            if (context.getFilesDir() != null) {
                final long startTime = System.currentTimeMillis();
                FileOutputStream fos = null;
                try {

                    File outputFile = new File(context.getFilesDir() + File.separator + context.getAppVersion()
                            + File.separator + context.getInstanceId(), Constants.STREAM_PREFIX + streamName);

                    outputFile.createNewFile();
                    fos = new FileOutputStream(outputFile);
                    if (fos == null) {
                        //On tests that use mock context the FileOutputStream could be null...
                        return;
                    }
                    fos.write(jsonAsString.getBytes());
                    Logger.log.d(TAG, "Write to file system of : " + streamName + " took : " + (System.currentTimeMillis() - startTime));
                } catch (IOException e) {
                    Logger.log.w(TAG, "Failed to persist content of: " + streamName + " to file system. Error: " + e.getMessage());
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Logger.log.w(TAG, "Failed to close File Output Stream for : " + streamName + ':' + e.getMessage());
                        }
                    }
                }
            }
        } else {
            deleteStream(streamName);
        }
    }

    @Override
    public void deleteStream(String name) {
        //if it is a tests mock app (files dir is null) - do not write to file system
        if (context.getFilesDir() != null) {
            context.deleteFile(Constants.STREAM_PREFIX + name);
        }
    }


    /**
     * The reason this has a separate method is because it is called when app stops - so we need to persist synchronously
     */
    @Override
    public JSONObject readStream(String name) {
        JSONObject value = null;
        name = Constants.STREAM_PREFIX + name;
        String streamValue = (String) readSinglePreferenceFromFileSystem(name);
        if (streamValue != null) {
            try {
                value = new JSONObject(streamValue);
            } catch (JSONException e) {
                //DO nothing
            }
        }
        if (value == null) {
            value = new JSONObject();
        }
        return value;
    }

    @Override
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

    @Override
    String getFileSystemLocationByPreferenceName(String preferenceName) {
        return getResourcePersistenceLocation(preferenceName) + File.separator + preferenceName;
    }

    @Override
    public void write(String key, JSONObject value) {
        write(key, value.toString());
    }

    @Override
    protected ReentrantReadWriteLock getWriteReadLocker(String resourceName) {
        ReentrantReadWriteLock lock = super.getWriteReadLocker(resourceName);
        if (lock == null) {
            return streamsReadWriteLock;
        }
        return lock;
    }

    private class FilePreferencesReader implements Runnable {
        @Override
        public void run() {
            for (String preferenceName : filePersistPreferences) {
                readSinglePreferenceFromFileSystem(preferenceName);
            }
        }
    }
}
