package com.ibm.airlock.common;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.net.RemoteConfigurationAsyncFetcher;
import com.ibm.airlock.common.services.*;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Constants;

import javax.inject.Inject;
import java.util.Date;
import java.util.UUID;


/**
 * @author Denis Voloshin
 */

public abstract class AbstractAirlockProductManager implements AirlockProductManager {

    private static final String TAG = "AbstractAirlockProductManager";

    protected ProductDiComponent productDiComponent;
    protected String productName;
    protected Context context;
    protected String airlockDefaults;
    protected String encryptionKey;
    protected String appVersion;

    @Inject
    public StreamsService streamsService;

    @Inject
    public InfraAirlockService infraAirlockService;

    @Inject
    public PersistenceHandler persistenceHandler;

    @Inject
    protected ConnectionManager connectionManager;

    @Inject
    protected UserGroupsService userGroupsService;

    @Inject
    protected BranchesService branchesService;

    @Inject
    protected FeaturesService featuresService;

    @Inject
    protected EntitlementsService entitlementsService;

    @Inject
    protected PercentageService percentageService;

    @Inject
    protected ProductInfoService productInfoService;

    @Inject
    protected AnalyticsService analyticsService;

    @Inject
    protected StringsService stringsService;

    @Inject
    protected NotificationService notificationService;

    public Context getContext() {
        return context;
    }

    public AbstractAirlockProductManager(String productName, String airlockDefaults, String encryptionKey, String appVersion) {
        this.productName = productName;
        this.airlockDefaults = airlockDefaults;
        this.encryptionKey = encryptionKey;
        this.appVersion = appVersion;
    }

    protected void initServices(ProductDiComponent productDiComponent, InfraAirlockService infraAirlockService) {
        this.infraAirlockService = infraAirlockService;
        userGroupsService.init(productDiComponent);
        branchesService.init(productDiComponent);
        featuresService.init(productDiComponent);
        entitlementsService.init(productDiComponent);
        percentageService.init(productDiComponent);
        productInfoService.init(productDiComponent);
        analyticsService.init(productDiComponent);
        stringsService.init(productDiComponent);
    }


    public AirlockClient createClient() throws AirlockInvalidFileException {
        return createClient(UUID.randomUUID().toString());
    }

    abstract protected void setLocale(PersistenceHandler persistenceHandler);

    AbstractAirlockProductManager() {
        infraAirlockService = new InfraAirlockService();
    }

    public InfraAirlockService getInfraAirlockService() {
        return infraAirlockService;
    }

    /**
     * Returns the model provider mode that the SDK is using.
     *
     * @return The model provider mode that the SDK is using.
     */
    @Override
    public RemoteConfigurationAsyncFetcher.DataProviderType getDataProviderType() {
        if (connectionManager != null) {
            return connectionManager.getDataProviderType();
        }
        return RemoteConfigurationAsyncFetcher.DataProviderType.CACHED_MODE;
    }

    /**
     * Sets the model provider mode that is configured for the SDK instance. Airlock must be initialized;
     * otherwise, the method has no effect.
     *
     * @param type The DataProviderType to be used.
     */
    @Override
    public void setDataProviderType(RemoteConfigurationAsyncFetcher.DataProviderType type) {
        if (connectionManager != null) {
            connectionManager.setDataProviderType(type);
        }
    }


    /**
     * Method nullifies the last pull timestamp to initial value.
     */
    @Override
    public void resetLastPullTime() {
        persistenceHandler.write(Constants.SP_LAST_TRANS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_FEATURES_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED);
        persistenceHandler.write(Constants.SP_LAST_JS_UTILS_FULL_DOWNLOAD_TIME, "");
        persistenceHandler.write(Constants.SP_LAST_TIME_PRODUCT_CHANGED, "");
    }


    /**
     * Returns the date and time of the last calculate.
     *
     * @return the date and time of the last calculate.
     */
    @Override
    public Date getLastCalculateTime() {
        long lastTime = infraAirlockService.getPersistenceHandler().read(Constants.SP_LAST_CALCULATE_TIME, Constants.TIME_NOT_DEFINED);
        return new Date(lastTime);
    }

    /**
     * Returns the date and time when calculate results were synchronized with the current feature set.
     *
     * @return the date of the last sync time.
     */
    @Override
    public Date getLastSyncTime() {
        long lastTime = infraAirlockService.getPersistenceHandler().read(Constants.SP_LAST_SYNC_TIME, Constants.TIME_NOT_DEFINED);
        return new Date(lastTime);
    }

    /**
     * Returns the date and time of the last successfully completed pull request from the server.
     *
     * @return the date and time
     */
    @Override
    public Date getLastPullTime() {
        long lastTime = infraAirlockService.getPersistenceHandler().read(Constants.SP_LAST_FEATURES_PULL_TIME, Constants.TIME_NOT_DEFINED);
        return new Date(lastTime);
    }


    @Override
    public abstract void reset(boolean simulateUninstall);

    @Override
    public void reset() {
        infraAirlockService.getServers().nullifyServerList();
        reset(true);
    }


    public String getAirlockUserUniqueId() throws AirlockNotInitializedException {
        if (infraAirlockService.getAirlockUserUniqueId() == null) {
            throw new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
        }
        return infraAirlockService.getAirlockUserUniqueId();
    }


    @Override
    public BranchesService getBranchesService() {
        return branchesService;
    }

    @Override
    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    @Override
    public EntitlementsService getEntitlementsService() {
        return entitlementsService;
    }

    @Override
    public ProductInfoService getProductInfoService() {
        return productInfoService;
    }

    @Override
    public AnalyticsService getAnalyticsService() {
        return analyticsService;
    }

    @Override
    public StringsService getStringsService() {
        return stringsService;
    }

    @Override
    public NotificationService getNotificationService() {
        return notificationService;
    }

    @Override
    public UserGroupsService getUserGroupsService() {
        return userGroupsService;
    }

    @Override
    public PercentageService getPercentageService() {
        return percentageService;
    }

    @Override
    public StreamsService getStreamsService() {
        return streamsService;
    }
}
