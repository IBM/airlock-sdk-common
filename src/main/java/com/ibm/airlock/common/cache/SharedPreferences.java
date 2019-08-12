package com.ibm.airlock.common.cache;

import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import javax.annotation.Nullable;


/**
 * Created by Denis Voloshin on 02/11/2017.
 */

public interface SharedPreferences {

    /**
     * Retrieve a boolean value from the preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws ClassCastException if there is a preference with this name that is not a boolean.
     * @throws ClassCastException
     */
    boolean isBooleanTrue(String key, boolean defValue);

    /**
     * Retrieve a long value from the preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws ClassCastException if there is a preference with this name that is not a long.
     * @throws ClassCastException
     */
    long getLong(String key, long defValue);


    /**
     * Retrieve all values from the preferences.
     *
     * <p>Note that you <em>must not</em> modify the collection returned
     * by this method, or alter any of its contents.  The consistency of your
     * stored data is not guaranteed if you do.
     *
     * @return Returns a map containing a list of pairs key/value representing
     * the preferences.
     *
     * @throws NullPointerException
     */
    Map<String, ?> getAll();


    /**
     * Retrieve a String value from the preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws ClassCastException if there is a preference with this name that is not a String.
     */
    @Nullable
    String getString(String key, @Nullable String defValue);


    /**
     * Create a new Editor for these preferences, through which you can make
     * modifications to the model in the preferences and atomically doCommit those
     * changes back to the SharedPreferences object.
     * <p>
     * <p>Note that you <em>must</em> call {@link Editor#doCommit} to have any
     * changes you perform in the Editor actually show up in the
     * SharedPreferences.
     *
     * @return Returns a new instance of the {@link Editor} interface, allowing you to modify the values in this SharedPreferences object.
     */
    Editor edit();


    /**
     * Retrieve an int value from the preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws ClassCastException if there is a preference with this name that is not an int.
     * @throws ClassCastException
     */
    int getInt(String key, int defValue);

    @Nullable
    Set<String> getStringSet(String key, Object o);


    void removeNode() throws BackingStoreException;

    /**
     * Interface used for modifying values in a {@link SharedPreferences}
     * object.  All changes you make in an editor are batched, and not copied
     * back to the original {@link SharedPreferences} until you call {@link #doCommit}
     * or {@link #apply}
     */
    public interface Editor {

        /**
         * Mark in the editor that a preference value should be removed, which
         * will be done in the actual preferences once {@link #doCommit} is
         * called.
         * <p>
         * <p>Note that when committing back to the preferences, all removals
         * are done first, regardless of whether you called remove before
         * or after put methods on this editor.
         *
         * @param key The name of the preference to remove.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        Editor remove(String key);

        /**
         * Mark in the editor to remove <em>all</em> values from the
         * preferences.  Once doCommit is called, the only remaining preferences
         * will be any that you have defined in this editor.
         * <p>
         * <p>Note that when committing back to the preferences, the clear
         * is done first, regardless of whether you called clear before
         * or after put methods on this editor.
         *
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        Editor clear();

        /**
         * Commit your preferences changes back from this Editor to the
         * {@link SharedPreferences} object it is editing.  This atomically
         * performs the requested modifications, replacing whatever is currently
         * in the SharedPreferences.
         * <p>
         * <p>Note that when two editors are modifying preferences at the same
         * time, the last one to call doCommit wins.
         * <p>
         * <p>If you don't care about the return value and you're
         * using this from your application's main thread, consider
         * using {@link #apply} instead.
         *
         * @return Returns true if the new values were successfully written to persistent storage.
         */
        boolean doCommit();

        /**
         * Commit your preferences changes back from this Editor to the
         * {@link SharedPreferences} object it is editing.  This atomically
         * performs the requested modifications, replacing whatever is currently
         * in the SharedPreferences.
         * <p>
         * <p>Note that when two editors are modifying preferences at the same
         * time, the last one to call apply wins.
         * <p>
         * <p>Unlike {@link #doCommit}, which writes its preferences out
         * to persistent storage synchronously,
         * commits its changes to the in-memory
         * {@link SharedPreferences} immediately but starts an
         * asynchronous doCommit to disk and you won't be notified of
         * any failures.  If another editor on this
         * {@link SharedPreferences} does a regular {@link #doCommit}
         * while a  is still outstanding, the
         * {@link #doCommit} will block until all async commits are
         * completed as well as the doCommit itself.
         * <p>
         * <p>As {@link SharedPreferences} instances are singletons within
         * a process, it's safe to replace any instance of {@link #doCommit} with
         *  if you were already ignoring the return value.
         * <p>
         * <p>You don't need to worry about Android component
         * lifecycles and their interaction with {@code apply()}
         * writing to disk.  The framework makes sure in-flight disk
         * writes from {@code apply()} complete before switching
         * states.
         * <p>
         * <p class='note'>The SharedPreferences.Editor interface
         * isn't expected to be implemented directly.  However, if you
         * previously did implement it and are now getting errors
         * about missing {@code apply()}, you can simply call
         * {@link #doCommit} from {@code apply()}.
         */
        void apply();

        /**
         * Set an int value in the preferences editor, to be written back once
         * {@link #doCommit} or {@link #apply} are called.
         *
         * @param key   The name of the preference to modify.
         * @param value The new value for the preference.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        Editor putInt(String key, int value);

        /**
         * Set a long value in the preferences editor, to be written back once
         * {@link #doCommit} or {@link #apply} are called.
         *
         * @param key   The name of the preference to modify.
         * @param value The new value for the preference.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        Editor putLong(String key, long value);

        /**
         * Set a float value in the preferences editor, to be written back once
         * {@link #doCommit} or {@link #apply} are called.
         *
         * @param key   The name of the preference to modify.
         * @param value The new value for the preference.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        Editor putFloat(String key, float value);

        /**
         * Set a boolean value in the preferences editor, to be written back
         * once {@link #doCommit} or {@link #apply} are called.
         *
         * @param key   The name of the preference to modify.
         * @param value The new value for the preference.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        Editor putBoolean(String key, boolean value);


        public void putString(String spSeasonId, String seasonId);
        
        
    }
}
