package com.ibm.airlock.common.cache;

import com.ibm.airlock.common.AirlockException;
import com.ibm.airlock.common.BaseAirlockProductManager;
import com.ibm.airlock.common.net.interceptors.ResponseExtractor;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.Decryptor;
import com.ibm.airlock.common.util.Gzip;
import com.ibm.airlock.common.util.RandomUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.security.GeneralSecurityException;

import static com.ibm.airlock.common.util.Constants.SP_RAW_RULES;

public abstract class RuntimeLoader {

    private final static String TAG = "airlock.RuntimeLoader";
    private final static String FEATURES = "features";
    private final static String FEATURES_UTILS = "jsFunctions";
    private final static String TRANSLATIONS = "translations";
    private final static String STREAMS = "streams";
    private final static String STREAMS_UTILS = "jsStreamsFunctions";
    private final static String NOTIFICATIONS = "notifications";

    BaseAirlockProductManager productManager;

    protected String pathToFiles;
    protected String encryptionKey;

    protected RuntimeLoader(String pathToFiles, String encryptionKey){
        this.encryptionKey = encryptionKey;
        this.pathToFiles = pathToFiles;
    }

    //executes pull features and translation json on parallel and wait for their completion
    public void loadRuntimeFilesOnStartup(BaseAirlockProductManager productManager) throws AirlockException {

        this.productManager = productManager;
        PersistenceHandler persistenceHandler = productManager.getCacheManager().persistenceHandler;

        //load features
        String runtimeContent = loadRuntimeFile(FEATURES);

        try {
            JSONObject newFeaturesRandoms = RandomUtils.calculateFeatureRandoms(new JSONObject(runtimeContent),
                    persistenceHandler.read(Constants.SP_USER_RANDOM_NUMBER, -1),
                    persistenceHandler.getFeaturesRandomMap() == null ? new JSONObject() : persistenceHandler.getFeaturesRandomMap());
            persistenceHandler.setFeaturesRandomMap(newFeaturesRandoms);
        } catch (Exception e) {
            return;
        }
        persistenceHandler.write(SP_RAW_RULES, runtimeContent);

        //load js utils
        runtimeContent = loadRuntimeFile(FEATURES_UTILS);
        productManager.getCacheManager().persistenceHandler.write(Constants.SP_RAW_JS_FUNCTIONS, runtimeContent);

        //load streams
        runtimeContent = loadRuntimeFile(STREAMS);
        if (!runtimeContent.isEmpty()) {
            JSONObject usageStreams = null;
            try {
                usageStreams = new JSONObject(runtimeContent);
            } catch (JSONException e) {
                //Do nothing
            }
            if (usageStreams != null && usageStreams.length() > 0) {
                try {
                    JSONObject newStreamsRandoms = RandomUtils.calculateStreamsRandoms(usageStreams,
                            persistenceHandler.getStreamsRandomMap().length() > 0 ? persistenceHandler.getStreamsRandomMap() : new JSONObject());
                    persistenceHandler.setStreamsRandomMap(newStreamsRandoms);
                } catch (Exception e) {
                    return;
                }
            }

            //Need to call the upateStreams method - so we do not call the regular persistTempResult method
            persistenceHandler.write(Constants.SP_FEATURE_USAGE_STREAMS, runtimeContent);
            productManager.getStreamsManager().updateStreams();
        }

        //Need to call the upateStreams method - so we do not call the regular persistTempResult method
        persistenceHandler.write(Constants.SP_FEATURE_USAGE_STREAMS, runtimeContent);
        productManager.getStreamsManager().updateStreams();

        //load streams utils
        runtimeContent = loadRuntimeFile(STREAMS_UTILS);
        productManager.getCacheManager().persistenceHandler.write(Constants.SP_FEATURE_UTILS_STREAMS, runtimeContent);

        //load translations
        runtimeContent = loadRuntimeFile(TRANSLATIONS);
        productManager.getCacheManager().persistenceHandler.write(Constants.SP_RAW_TRANSLATIONS, runtimeContent );


        //load notifications
        if (productManager.getNotificationsManager().isSupported()) {
            runtimeContent = loadRuntimeFile(NOTIFICATIONS);
            productManager.getCacheManager().persistenceHandler.write(Constants.SP_NOTIFICATIONS, runtimeContent);
            productManager.getNotificationsManager().initNotifications();
        }

        // clear SP_FEATURES_PERCENAGE_MAP because we want to reload it when the PercentageManager
        // methods will be call
        productManager.getCacheManager().persistenceHandler.write(Constants.SP_FEATURES_PERCENAGE_MAP, "{}");
    }

    protected abstract InputStream getInputStream(String featuresUtils);

    private String loadRuntimeFile(String name) throws AirlockException {

        InputStream inputStream = getInputStream(name);

        if (inputStream == null){
            throw new AirlockException("faild to get " +  name + " output stream");
        }

        String runtimeContent = readContentAndDecrypt(inputStream);

        if (runtimeContent == null || runtimeContent.equals("")) {
            throw new AirlockException("runtime " + name + " file is empty");
        }

        return  runtimeContent;
    }

    private String decrypt(byte[] data) throws GeneralSecurityException {
        if (encryptionKey == null || encryptionKey.isEmpty()){
            try {
                return new String(data, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                //utf-8 is always supported
            }
        }
        try {
            data = Decryptor.decryptAES(data, encryptionKey.getBytes());
            if (ResponseExtractor.isGZipped(data)){
                data = Gzip.decompress(data);
            }
            return new String(data
                    , "UTF-8");
        } catch (IOException e) {
            //log decompress error
        }
        return null;
    }

    private String readContentAndDecrypt(InputStream inputStream) throws AirlockException {
        ByteArrayOutputStream ous = null;
        byte[] outBuffer;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
            if (ous != null){
                outBuffer =  ous.toByteArray();
                return decrypt(outBuffer);
            }
        } catch (GeneralSecurityException e) {
            throw  new AirlockException(e.getMessage());
        } catch (IOException e) {
            throw  new AirlockException(e.getMessage());
        } finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException ignored) {
            }

            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}
