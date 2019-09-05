package com.ibm.airlock.common.engine.entitlements;


import com.ibm.airlock.common.engine.Result;

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * The class represents rule engine result for purchase element,
 * the class has  a member {@link Result#accept} whether the rule was accepted or not and {@link Result#getTrace()} to get trace
 * information holds addition details.
 *
 * @author Denis Voloshin
 */
class EntitlementResult extends Result {

    private List<String> purchasesOptions = Collections.emptyList();
    @Nullable
    private String storeProductId;

    public EntitlementResult(Result result) {
        this(result.isAccept(), result.getTrace());

        this.attributes = result.attributes;
        this.configRuleStatuses = result.configRuleStatuses;
        this.analyticsAppliedRules = result.getAnalyticsAppliedRules();
        this.percentage = result.getPercentage();
        this.orderingWeight = result.getOrderingWeight();
        this.type = result.getType();
        this.isPremium = result.isPremium();
        this.isPurchased = result.isPurchased();
    }

    private EntitlementResult(boolean accept, String trace) {
        super(accept, trace);
    }

    public List<String> getPurchasesOptions() {
        return purchasesOptions;
    }

    public void setPurchasesOptions(List<String> purchasesOptions) {
        this.purchasesOptions = purchasesOptions;
    }

    @CheckForNull
    public String getStoreProductId() {
        return storeProductId;
    }

    public void setStoreProductId(@Nullable String storeProductId) {
        this.storeProductId = storeProductId;
    }
}
