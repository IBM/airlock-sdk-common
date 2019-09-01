package com.ibm.airlock.common.debug;

import java.util.List;
import javax.annotation.CheckForNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.ibm.airlock.common.model.Feature;


/**
 * Created by Denis Voloshin on 07/11/2017.
 */

public interface AirlockProductDebugger {
    /**
     * Returns a list of airlock user groups selected for the device.
     *
     * @return a list of user groups selected for the device.
     */
    @CheckForNull
    List<String> getSelectedDeviceUserGroups();


    /**
     * Specifies a list of user groups selected for the device.
     *
     * @param userGroups List of the selected user groups.
     * @throws JSONException if the provided value breaks the JSON serialization process.
     */
    void setDeviceUserGroups(String... userGroups);


    /**
     * @return Array of airlock users group names
     */
    JSONArray getUserGroups();

    /**
     * synchronously downloads the current remote airlock configuration from the server.
     *
     */
    void pullFeatures();

    /**
     * Initializes AirlockProductManager with application information.
     * InitSDK loads the defaults file provided as a string in JSON format and
     * merges it with the current feature set.
     *
     * @param directory            Airlock context location on the disk
     * @param defaultConfiguration Defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                             Code Assistant plugin.
     * @param productVersion       The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     */
    void initSDK(String directory, String defaultConfiguration, String productVersion);


    /**
     * @return returns the last airlock context the feature rules were calculated with.
     */
    @CheckForNull
    JSONObject getLastCalculatedContext();


    /**
     * @return returns translated strings table.
     */
    @CheckForNull
    JSONObject getTranslatedStringsTable();


    /**
     * @return returns Streams java-script utils.
     */
    @CheckForNull
    JSONObject getStreamsJavaScriptUils();


    /**
     * @return returns Java Script utils.
     */
    @CheckForNull
    JSONObject getJavaScriptUils();


    /**
     * @return returns remote features configuration
     */
    @CheckForNull
    JSONObject getRawFeatureConfiguration();


    /**
     * @return returns the list of development branches
     */
    @CheckForNull
    JSONArray getDevelopmentBranches();


    /**
     * @return returns the list of streams
     */
    @CheckForNull
    List<String> getStreams();


    /**
     * @return returns a stream configuration as a string in JSON format.
     */
    @CheckForNull
    JSONObject getStream(String name);

    /**
     * Clears a stream model
     */
    void clearStream(String name);


    /**
     * Triggers a stream processing
     */
    void runStreamProcessing(String name);


    /**
     * Sets a new value for  airlock stream roll-out percentage.
     */
    void setStreamRolloutPercentage(String name, long percentage);


    /**
     * Sets a development branch, the airlock product is running with.
     */
    void setDevelopmentBranch(String branchName);

    /**
     * Gets a development branch, the airlock product is running with.
     */
    String getSelectedDevelopmentBranch();


    /**
     * Enables or disable airlock responsive mode.
     *
     * @param enable true to enable responsive mode
     */
    void enableResponsiveMode(boolean enable);


    /**
     * Disable all development branches.
     */
    void disableAllDevelopmentBranches();


    /**
     * Returns a list of airlock features list as a string in JSON format.
     *
     * @return a list of user groups selected for the device.
     */
    @CheckForNull
    List<Feature> getCurrentFeaturesState();


    /**
     * Sets a new value for  airlock feature roll-out percentage.
     */
    void setFeatureRolloutPercentage(String featureName, int percentage);
}
