package com.ibm.airlock.common.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;


/**
 * Created by Denis Voloshin on 01/11/2017.
 */

public interface Context {

    public File getFilesDir();

    public SharedPreferences getSharedPreferences(String spName, int modePrivate);

    public void deleteFile(String key);

    public FileInputStream openFileInput(String preferenceName) throws FileNotFoundException;

    public FileOutputStream openFileOutput(String name,
            int mode) throws FileNotFoundException;

    public Object getSystemService(String name);

    public InputStream openRawResource(int name);
}
