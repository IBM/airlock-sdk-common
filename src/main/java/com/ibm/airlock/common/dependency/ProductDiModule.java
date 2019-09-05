package com.ibm.airlock.common.dependency;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.DefaultPersistenceHandler;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.engine.context.AirlockContextManager;
import com.ibm.airlock.common.net.BaseOkHttpClientBuilder;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.net.OkHttpConnectionManager;
import com.ibm.airlock.common.services.*;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;


@Module
public class ProductDiModule {
    protected String key;
    protected Context productContext;
    protected String productName;
    protected String appVersion;
    protected String defaultFile;
    protected InfraAirlockService infraAirlockService;
    protected ConnectionManager connectionManager;
    protected PersistenceHandler persistenceHandler;
    protected StreamsService streamsService;
    protected UserGroupsService userGroupsService;
    protected BranchesService branchesService;
    protected EntitlementsService entitlementsService;
    protected FeaturesService featuresService;
    protected ContextService contextService;
    protected PercentageService percentageService;
    protected StringsService stringsService;
    protected ProductInfoService productInfoService;
    protected AnalyticsService analyticsService;
    protected NotificationService notificationService;
    protected AirlockContextManager contextManager;

    public ProductDiModule(){

    }

    public ProductDiModule(Context productContext, String defaultFile,
                           String productName, String appVersion, String key) {
        this.productContext = productContext;
        this.productName = productName;
        this.appVersion = appVersion;
        this.key = key;
        this.defaultFile = defaultFile;
        contextManager = new AirlockContextManager(productName);
        infraAirlockService = new InfraAirlockService();
        connectionManager = new OkHttpConnectionManager(new BaseOkHttpClientBuilder(), key);
        persistenceHandler = new DefaultPersistenceHandler(productContext);
        streamsService = new StreamsService();
        userGroupsService = new UserGroupsService();
        branchesService = new BranchesService();
        entitlementsService = new EntitlementsService();
        featuresService = new FeaturesService();
        contextService = new ContextService();
        percentageService = new PercentageService();
        stringsService = new StringsService();
        productInfoService = new ProductInfoService();
        productInfoService = new ProductInfoService();
        analyticsService = new AnalyticsService();
        notificationService = new NotificationService();
    }

    @Provides
    public ConnectionManager provideConnectionManager() {
        return connectionManager;
    }

    @Provides
    public PersistenceHandler providePersistenceHandler() {
        return persistenceHandler;
    }

    @Provides
    public InfraAirlockService provideBaseAirlockService() {
        return infraAirlockService;
    }

    @Provides
    public StreamsService provideStreamsManagere() {
        return streamsService;
    }

    @Provides
    public UserGroupsService provideUserGroupsService() {
        return userGroupsService;
    }

    @Provides
    public BranchesService provideBranchesService() {
        return branchesService;
    }

    @Provides
    public EntitlementsService provideEntitlementsService() {
        return entitlementsService;
    }

    @Provides
    public FeaturesService provideFeaturesService() {
        return featuresService;
    }

    @Provides
    public ContextService provideContextService() {
        return contextService;
    }

    @Provides
    public PercentageService providePersistenceService() {
        return percentageService;
    }

    @Provides
    public StringsService provideStreamsService() {
        return stringsService;
    }

    @Provides
    public ProductInfoService provideProductInfoService() {
        return productInfoService;
    }

    @Provides
    public AnalyticsService provideAnalyticsService() {
        return analyticsService;
    }

    @Provides
    public NotificationService provideNotificationService() {
        return notificationService;
    }

    @Provides
    public Context provideContext() {
        return productContext;
    }

    @Provides
    @Named("defaultFile")
    public String provideDefaultFile() {
        return defaultFile;
    }

    @Provides
    public String provideAppVersiob() {
        return appVersion;
    }

    @Provides
    public AirlockContextManager provideAirlockContextManager() {
        return contextManager;
    }
}
