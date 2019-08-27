package com.ibm.airlock.common.debug;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckForNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.engine.ScriptInitException;


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
    void setDeviceUserGroups(String... userGroups) throws TimeoutException;


    /**
     * @return Array of airlock users group names
     * @throws InterruptedException
     */
    JSONArray getUserGroups() throws InterruptedException, TimeoutException, IOException;

    /**
     * synchronously downloads the current remote airlock configuration from the server.
     *
     * @throws AirlockNotInitializedException if the Airlock SDK has not been initialized.
     */
    void pullFeatures() throws AirlockNotInitializedException, InterruptedException, TimeoutException, IOException;

    /**
     * Initializes AirlockProductManager with application information.
     * InitSDK loads the defaults file provided as a string in JSON format and
     * merges it with the current feature set.
     *
     * @param directory            Airlock context location on the disk
     * @param defaultConfiguration Defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                             Code Assistant plugin.
     * @param productVersion       The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    void initSDK(String directory, String defaultConfiguration, String productVersion) throws AirlockInvalidFileException, IOException;


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
    JSONArray getDevelopmentBranches() throws InterruptedException, TimeoutException, IOException;


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
    void runStreamProcessing(String name) throws ScriptInitException;


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
     * @param enable
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
