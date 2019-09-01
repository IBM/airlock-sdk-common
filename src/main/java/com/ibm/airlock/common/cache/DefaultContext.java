package com.ibm.airlock.common.cache;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * @author Denis Voloshin on 2019-06-28.
 */
public class DefaultContext implements Context {

    private static final String DEFAULT_SEASON_ID = "DEFAULT_SEASON_ID";
    private static final String DEFAULT_PRODUCT_ID = "DEFAULT_PRODUCT_ID";

    private final File contextFolder;
    private final String airlockProductName;
    private final String encryptionKey;
    private String appVersion;
    private final String instanceId;
    private final String productId;
    private final String seasonId;


    public DefaultContext(String instanceId, String rootFolder, String defaults, String encryptionKey, String appVersion) {
        this(instanceId, rootFolder, defaults, getProductName(new JSONObject(defaults)), encryptionKey, appVersion);
    }

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
            contextFolder.mkdirs();
        }
    }

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

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public File getFilesDir() {
        return this.contextFolder;
    }

    @Override
    public SharedPreferences getSharedPreferences(String spName, int modePrivate) {
        return null;
    }

    @Override
    public void deleteFile(String key) {
        new File(key).delete();
    }

    @Override
    public FileInputStream openFileInput(String preferenceName) throws FileNotFoundException {
        return new FileInputStream(new File(preferenceName));
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

    public static String getSeasonId(JSONObject defaultsJSON) {
        return defaultsJSON.optString("seasonId");
    }

    public static String getProductId(JSONObject defaultsJSON) {
        return defaultsJSON.optString("productId");
    }

    public static String getProductName(JSONObject defaultsJSON) {
        return defaultsJSON.optString("productName");
    }

}
