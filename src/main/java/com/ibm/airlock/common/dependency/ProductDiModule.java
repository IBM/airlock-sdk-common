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
    public InfraAirlockService providsBaseAirlockService() {
        return infraAirlockService;
    }

    @Provides
    public StreamsService providsStreamsManagere() {
        return streamsService;
    }

    @Provides
    public UserGroupsService providsUserGroupsService() {
        return userGroupsService;
    }

    @Provides
    public BranchesService providsBranchesService() {
        return branchesService;
    }

    @Provides
    public EntitlementsService providsEntitlementsService() {
        return entitlementsService;
    }

    @Provides
    public FeaturesService providsFeaturesService() {
        return featuresService;
    }

    @Provides
    public ContextService providsContextService() {
        return contextService;
    }

    @Provides
    public PercentageService providsPersistenceService() {
        return percentageService;
    }

    @Provides
    public StringsService providsStreamsService() {
        return stringsService;
    }

    @Provides
    public ProductInfoService providsProductInfoService() {
        return productInfoService;
    }

    @Provides
    public AnalyticsService providsAnalyticsService() {
        return analyticsService;
    }

    @Provides
    public NotificationService providsNotificationService() {
        return notificationService;
    }

    @Provides
    public Context providsContext() {
        return productContext;
    }

    @Provides
    @Named("defaultFile")
    public String providsDefaultFile() {
        return defaultFile;
    }

    @Provides
    public String providsAppVersiob() {
        return appVersion;
    }

    @Provides
    public AirlockContextManager providsAirlockContextManager() {
        return contextManager;
    }
}
