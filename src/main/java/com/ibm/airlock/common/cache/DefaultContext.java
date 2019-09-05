package com.ibm.airlock.common.cache;

import com.ibm.airlock.common.log.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Default implementation of Context class base on the File System persistence
 *
 * @author Denis Voloshin
 */
public class DefaultContext implements Context {

    private static final String TAG = "DefaultContext";
    private static final String DEFAULT_SEASON_ID = "DEFAULT_SEASON_ID";
    private static final String DEFAULT_PRODUCT_ID = "DEFAULT_PRODUCT_ID";

    private final File contextFolder;
    private final String airlockProductName;
    private final String encryptionKey;
    private String appVersion;
    private final String instanceId;
    private final String productId;
    private final String seasonId;


    @SuppressWarnings({"ConstructorWithTooManyParameters", "JavaDoc"})
    public DefaultContext(String instanceId, String rootFolder, String defaults, String encryptionKey, String appVersion) {
        this(instanceId, rootFolder, defaults, getProductName(new JSONObject(defaults)), encryptionKey, appVersion);
    }

    @SuppressWarnings({"ConstructorWithTooManyParameters", "JavaDoc"})
    public DefaultContext(String instanceId, String rootFolder, String defaults, String airlockProductName, String encryptionKey, String appVersion) {
        JSONObject defaultsJson = new JSONObject(defaults);
        seasonId = getSeasonId(defaultsJson).isEmpty() ? DEFAULT_SEASON_ID : getSeasonId(defaultsJson);
        this.instanceId = instanceId;
        this.encryptionKey = encryptionKey;
        productId = getProductId(defaultsJson).isEmpty() ? DEFAULT_PRODUCT_ID : getProductId(defaultsJson);
        this.airlockProductName = airlockProductName;
        this.appVersion = appVersion;

        contextFolder = new File(rootFolder + File.separator + airlockProductName + File.separator + seasonId);
        if (!contextFolder.exists()) {
            if (!contextFolder.mkdirs()) {
                Logger.log.w(TAG, "Context folders creation " + contextFolder + " failed");
            }
        }
    }

    @SuppressWarnings("unused")
    public String getProductId() {
        return productId;
    }

    @Override
    public String getSeasonId() {
        return seasonId;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String getAirlockProductName() {
        return airlockProductName;
    }

    @Override
    public String getEncryptionKey() {
        return encryptionKey;
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    @SuppressWarnings("unused")
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public File getFilesDir() {
        return contextFolder;
    }

    @Override
    public SharedPreferences getSharedPreferences(String spName, int modePrivate) {
        return new InMemorySharedPreferences();
    }

    @Override
    public void deleteFile(String fileName) {
        if (new File(fileName).delete()) {
            Logger.log.w(TAG, "File " + fileName + " delete failed");
        }
    }

    @Override
    public FileInputStream openFileInput(String preferenceName) throws FileNotFoundException {
        return new FileInputStream(new File(preferenceName));
    }

    @Override
    public File openFile(String filePath) {
        return new File(filePath);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return new FileOutputStream(new File(name));
    }

    @Override
    public Object getSystemService(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream openRawResource(int name) {
        throw new UnsupportedOperationException();
    }

    private static String getSeasonId(JSONObject defaultsJSON) {
        return defaultsJSON.optString("seasonId");
    }

    private static String getProductId(JSONObject defaultsJSON) {
        return defaultsJSON.optString("productId");
    }

    private static String getProductName(JSONObject defaultsJSON) {
        return defaultsJSON.optString("productName");
    }
}
