package com.ibm.airlock.common.dependency;

import com.ibm.airlock.common.DefaultAirlockProductManager;
import com.ibm.airlock.common.services.*;
import dagger.Component;


@Component(modules = ProductDiModule.class)
public interface ProductDiComponent {
    void inject(DefaultAirlockProductManager defaultAirlockProductManager);
    void inject(UserGroupsService userGroupsService);
    void inject(StreamsService streamsService);
    void inject(InfraAirlockService infraAirlockService);
    void inject(FeaturesService featuresService);
    void inject(BranchesService branchesService);
    void inject(EntitlementsService entitlementsService);
    void inject(ContextService contextService);
    void inject(PercentageService contextService);
    void inject(StringsService stringsService);
    void inject(ProductInfoService productInfoService);
    void inject(AnalyticsService analyticsService);
    void inject(NotificationService notificationService);
}
