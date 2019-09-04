package com.ibm.airlock.common.cache;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;


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

    @CheckForNull
    SharedPreferences getSharedPreferences(String spName, int modePrivate);

    void deleteFile(String key);

    FileInputStream openFileInput(String preferenceName) throws FileNotFoundException;

    FileOutputStream openFileOutput(String name,
                                    int mode) throws FileNotFoundException;

    Object getSystemService(String name);

    InputStream openRawResource(int name);
}
