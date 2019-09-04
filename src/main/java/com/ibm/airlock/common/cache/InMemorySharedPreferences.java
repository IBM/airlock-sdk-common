package com.ibm.airlock.common.cache;

import org.jetbrains.annotations.TestOnly;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used for unit tests
 */
@SuppressWarnings("unused")
@TestOnly
public class InMemorySharedPreferences implements SharedPreferences {

    private final Map<String, Object> preferences = new ConcurrentHashMap<>();
    private final Editor editor = new InMemoryEditor();

    @Override
    public boolean isBooleanTrue(String key, boolean defValue) {
        if(!preferences.containsKey(key)){
            return defValue;
        }
        return Boolean.valueOf(preferences.get(key).toString());
    }

    @Override
    public long getLong(String key, long defValue) {
        if(!preferences.containsKey(key)){
            return defValue;
        }
        return Long.valueOf(preferences.get(key).toString());
    }

    @Override
    public Map<String, ?> getAll() {
        return Collections.unmodifiableMap(preferences);
    }

    @CheckForNull
    @Override
    public String getString(String key, @Nullable String defValue) {
        if(!preferences.containsKey(key)){
            return defValue;
        }
        return String.valueOf(preferences.get(key).toString());
    }

    @Override
    public Editor edit() {
        return editor;
    }

    @Override
    public int getInt(String key, int defValue) {
        return Integer.valueOf(preferences.get(key).toString());
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, Object o) {
        return null;
    }

    @Override
    public void removeNode() {

    }

    @SuppressWarnings({"unused"})
    class InMemoryEditor implements Editor {

        @Override
        public Editor remove(String key) {
            preferences.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            preferences.clear();
            return this;
        }

        @Override
        public boolean doCommit() {
            return true;
        }

        @Override
        public void apply() {

        }

        @Override
        public Editor putInt(String key, int value) {
            preferences.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            preferences.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            preferences.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            preferences.put(key, value);
            return this;
        }

        @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
        @Override
        public void putString(String key, String value) {
            preferences.put(key,value);
        }
    }
}
