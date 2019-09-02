package com.ibm.airlock.common.cache.pref;


import java.io.File;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class FilePreferencesFactory implements PreferencesFactory {

    private Preferences rootPreferences;

    @Override
    public Preferences systemRoot() {
        return userRoot();
    }

    @Override
    public Preferences userRoot() {
        if (rootPreferences == null) {
            //noinspection ResultOfMethodCallIgnored
            new File(getAirlockCacheDirectory()).mkdir();
            rootPreferences = new FilePreferences(null, "");
        }
        return rootPreferences;
    }

    public static String getAirlockCacheDirectory() {
        return "cache" + File.separator + "airlock";
    }
}