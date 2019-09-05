package com.ibm.airlock.common.cache.pref;


import javax.annotation.Nullable;
import java.io.File;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;


/**
 * Custom factory object that generates Java Preferences objects.
 */
public class FilePreferencesFactory implements PreferencesFactory {

    private static final String CACHE_ROOT = "cache";
    private static final String CACHE_AIRLOCK_ROOT = "airlock";

    @Nullable
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
        return CACHE_ROOT + File.separator + CACHE_AIRLOCK_ROOT;
    }
}