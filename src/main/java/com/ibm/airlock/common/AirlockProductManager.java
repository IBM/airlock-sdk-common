package com.ibm.airlock.common;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.net.RemoteConfigurationAsyncFetcher;
import com.ibm.airlock.common.services.*;

import java.util.Date;


/**
 * Created by Denis Voloshin on 05/11/2017.
 */

public interface AirlockProductManager {

    InfraAirlockService getInfraAirlockService();

    PercentageService getPercentageService();

    StreamsService getStreamsService();

    BranchesService getBranchesService();

    UserGroupsService getUserGroupsService();

    FeaturesService getFeaturesService();

    EntitlementsService getEntitlementsService();

    ProductInfoService getProductInfoService();

    AnalyticsService getAnalyticsService();

    StringsService getStringsService();

    NotificationService getNotificationService();

    AirlockClient createClient(String clientId) throws AirlockInvalidFileException;

    void initServices(ProductDiComponent productDiComponent) throws AirlockInvalidFileException;

    /**
     * Returns the model provider mode that the SDK is using.
     *
     * @return The model provider mode that the SDK is using.
     */
    RemoteConfigurationAsyncFetcher.DataProviderType getDataProviderType();

    /**
     * Sets the model provider mode that is configured for the SDK instance. Airlock must be initialized;
     * otherwise, the method has no effect.
     *
     * @param type The DataProviderType to be used.
     */
    void setDataProviderType(RemoteConfigurationAsyncFetcher.DataProviderType type);


    /**
     * Method nullifies the last pull timestamp to initial value.
     */
    void resetLastPullTime();

    /**
     * Returns the date and time of the last calculate.
     *
     * @return the date and time of the last calculate.
     */
    Date getLastCalculateTime();

    /**
     * Returns the date and time when calculate results were synchronized with the current feature set.
     *
     * @return the date of the last sync time.
     */
    Date getLastSyncTime();

    /**
     * Returns the date and time of the last successfully completed pull request from the server.
     *
     * @return the date and time
     */
    Date getLastPullTime();


    /**
     * Asynchronously downloads the current list of features from the server.
     *
     * @param callback Callback to be called when the function returns.
     */
    void pullFeatures(final AirlockCallback callback);


    void reset(boolean simulateUninstall);

    void reset();

    Context getContext();

    String getAirlockUserUniqueId() throws AirlockNotInitializedException;
}

