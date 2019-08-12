package com.ibm.airlock.common;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.net.RemoteConfigurationAsyncFetcher;
import com.ibm.airlock.common.services.*;
import org.jetbrains.annotations.TestOnly;

import java.util.Date;


/**
 * Created by Denis Voloshin on 05/11/2017.
 */

public interface AirlockProductManager {

    public InfraAirlockService getInfraAirlockService();

    public PercentageService getPercentageService();

    public StreamsService getStreamsService();

    public BranchesService getBranchesService();

    public UserGroupsService getUserGroupsService();

    public FeaturesService getFeaturesService();

    public EntitlementsService getEntitlementsService();

    public ProductInfoService getProductInfoService();

    public AnalyticsService getAnalyticsService();

    public StringsService getStringsService();

    public NotificationService getNotificationService();


//    /**
//     * Initializes AirlockManager with application information.
//     * InitSDK loads the defaults file specified by the defaultFileId and
//     * merges it with the current feature set.
//     *
//     * @param appContext     The current airlock context.
//     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
//     *                       Code Assistant plugin.
//     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
//     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
//     * @throws IOException                 Thrown when the defaults file cannot be opened.
//     */
//    public void initSDK(Context appContext, int defaultFileId, String productVersion) throws AirlockInvalidFileException, IOException;
//
//

    public AirlockClient createClient(String clientId) throws AirlockInvalidFileException;
//
//    /**
//     * Initializes AirlockManager with application information.
//     * InitSDK loads the defaults file specified by the defaultFileId and
//     * merges it with the current feature set.
//     *
//     * @param appContext     The current airlock context.
//     * @param defaultFile    Defaults file. This defaults file should be part of the application. You can get this by running the Airlock
//     *                       Code Assistant plugin.
//     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
//     * @param key            Encryption key will be used to encrype/decrypt the cached model
//     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
//     * @throws IOException                 Thrown when the defaults file cannot be opened.
//     */
//    public void initSDK(Context appContext, String defaultFile, String productVersion, String key) throws AirlockInvalidFileException, IOException;
//

//    /**
//     * Initializes AirlockManager with application information.
//     * InitSDK loads the defaults file specified by the defaultFileId and
//     * merges it with the current feature set.
//     *
//     * @param appContext     The current airlock context.
//     * @param defaultFile    Defaults file. This defaults file should be part of the application. You can get this by running the Airlock
//     *                       Code Assistant plugin.
//     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
//     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
//     * @throws IOException                 Thrown when the defaults file cannot be opened.
//     */
//    public void initSDK(Context appContext, String defaultFile, String productVersion) throws AirlockInvalidFileException, IOException;
//

    /**
     * Returns the model provider mode that the SDK is using.
     *
     * @return The model provider mode that the SDK is using.
     */
    public RemoteConfigurationAsyncFetcher.DataProviderType getDataProviderType();

    /**
     * Sets the model provider mode that is configured for the SDK instance. Airlock must be initialized;
     * otherwise, the method has no effect.
     *
     * @param type The DataProviderType to be used.
     */
    public void setDataProviderType(RemoteConfigurationAsyncFetcher.DataProviderType type);


    /**
     * Method nullifies the last pull timestamp to initial value.
     */
    public void resetLastPullTime();

    /**
     * Returns the date and time of the last calculate.
     *
     * @return the date and time of the last calculate.
     */
    public Date getLastCalculateTime();

    /**
     * Returns the date and time when calculate results were synchronized with the current feature set.
     *
     * @return the date of the last sync time.
     */
    public Date getLastSyncTime();

    /**
     * Returns the date and time of the last successfully completed pull request from the server.
     *
     * @return the date and time
     */
    public Date getLastPullTime();


    @TestOnly
    public void reset(boolean simulateUninstall);

    @TestOnly
    public void reset();

    public Context getContext();

    public String getAirlockUserUniqueId() throws AirlockNotInitializedException;
}

