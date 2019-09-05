package com.ibm.airlock.common.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by Denis Voloshin on 01/11/2017.
 */

public interface Context {

    String getAirlockProductName();

    String getEncryptionKey();

    String getSeasonId();

    String getInstanceId();

    String getAppVersion();

    File getFilesDir();

    SharedPreferences getSharedPreferences(String spName, int modePrivate);

    void deleteFile(String fileName);

    InputStream openFileInput(String preferenceName) throws FileNotFoundException;

    File openFile(String filePath) throws FileNotFoundException;

    OutputStream openFileOutput(String name, int mode) throws FileNotFoundException;

    Object getSystemService(String name);

    InputStream openRawResource(int name);
}
