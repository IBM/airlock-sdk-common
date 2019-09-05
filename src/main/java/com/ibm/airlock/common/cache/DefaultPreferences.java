package com.ibm.airlock.common.cache;

import com.ibm.airlock.common.log.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.ibm.airlock.common.cache.DefaultPersistenceHandler.DEFAULT_IN_MEMORY_EXPIRATION_PERIOD;

/**
 * Default implementation of SharedPreferences interface
 * <p>
 * @author  Denis Voloshin
 */
public class DefaultPreferences implements SharedPreferences {

    private static final String TAG = "DefaultPreferences";
    private static final String PRODUCT_PREF = "airlock.product.pref";

    private final Preferences preferences;
    private final String productPreferencesFolder;
    private final DefaultPreferencesEditor editor;
    private final Cacheable<String, Object> inMemoryCache;


    DefaultPreferences(String productPreferencesFolder) {
        this.productPreferencesFolder = productPreferencesFolder;
        preferences = Preferences.userRoot().node(productPreferencesFolder + File.separator + PRODUCT_PREF);
        editor = new DefaultPreferencesEditor(preferences);
        inMemoryCache = new InMemoryCache<>();
    }

    @SuppressWarnings("unused")
    public boolean isExists() throws BackingStoreException {
        return Preferences.userRoot().nodeExists(productPreferencesFolder + File.separator + PRODUCT_PREF);

    }

    void removeNode() throws BackingStoreException {
        if (Preferences.userRoot().nodeExists(productPreferencesFolder + File.separator + PRODUCT_PREF)) {
            Preferences.userRoot().node(productPreferencesFolder + File.separator + PRODUCT_PREF).removeNode();
        }
        if (Preferences.userRoot().nodeExists(productPreferencesFolder)) {
            Preferences.userRoot().node(productPreferencesFolder).removeNode();
        }
    }


    @Override
    public boolean isBooleanTrue(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> all = new HashMap<>();
        try {
            for (String key : preferences.keys()) {
                all.put(key, preferences.get(key, ""));
            }
        } catch (Exception e) {
            Logger.log.w(TAG, e.getMessage());
        }
        return all;
    }

    @CheckForNull
    @Override
    public String getString(String key, @Nullable String defValue) {
        String result = null;
        try {
            if (inMemoryCache.containsKey(key)) {
                return (String) inMemoryCache.get(key);
            }
            result = preferences.get(key, defValue);
        } catch (Exception e) {
            Logger.log.w(TAG, e.getMessage());
        }
        if (result != null || defValue != null) {
            inMemoryCache.put(key, result == null ? defValue : result, DEFAULT_IN_MEMORY_EXPIRATION_PERIOD);
        }
        return result == null ? defValue : result;
    }

    @Override
    public Editor edit() {
        return editor;
    }

    @Override
    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, @Nullable Object o) {
        if (preferences.get(key, o == null ? null : o.toString()) == null) {
            return new HashSet<>();
        }
        try {
            JSONArray array = new JSONArray(preferences.get(key, o == null ? null : o.toString()));
            Set<String> set = new HashSet<>();
            int len = array.length();
            for (int i = 0; i < len; i++) {
                set.add(array.get(i).toString());
            }
            return set;
        } catch (JSONException e) {
            return new HashSet<>();
        }
    }

    @SuppressWarnings("JavaDoc")
    class DefaultPreferencesEditor implements Editor {

        private final Preferences preferences;

        DefaultPreferencesEditor(Preferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public Editor remove(String key) {
            try {
                preferences.remove(key);
                inMemoryCache.remove(key);
            } catch (Exception e) {
                Logger.log.w(TAG, e.getMessage());
            }
            return this;
        }

        @Override
        public Editor clear() {
            try {
                String[] keys = preferences.keys();
                for (String key : keys) {
                    preferences.remove(key);
                }
                inMemoryCache.clear();

            } catch (BackingStoreException e) {
                Logger.log.e(TAG, e.getMessage());
            }
            return this;
        }

        @Override
        public boolean doCommit() {
            try {
                preferences.flush();
            } catch (BackingStoreException e) {
                return false;
            }
            return true;
        }

        @Override
        public void apply() {
            doCommit();
        }

        @Override
        public Editor putInt(String key, int value) {
            preferences.putInt(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            preferences.putLong(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            preferences.putBoolean(key, value);
            return this;
        }

        @Override
        public void putString(String key, @Nullable String value) {
            if (value == null) {
                return;
            }
            try {
                preferences.put(key, value);
                inMemoryCache.put(key, value, DEFAULT_IN_MEMORY_EXPIRATION_PERIOD);
            } catch (Exception e) {
                Logger.log.e(TAG, e.getMessage());
            }
        }
    }
}
