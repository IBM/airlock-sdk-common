package com.ibm.airlock.common.engine;


import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;

/**
 * The class represents rule engine result for purchase element,
 * the class has to members {@link Result#accept} whether the rule was accepted or not {@link Result#trace} trace
 * information holds addition details.
 *
 * @author Denis Voloshin
 */
public class EntitlementResult extends Result {

    List<String> puchasesOptions = Collections.emptyList();
    String storePoductId;

    public EntitlementResult(Result result) {
        this(result.isAccept(), result.getTrace());

        this.attributes = result.attributes;
        this.configRuleStatuses = result.configRuleStatuses;
        this.analyticsAppliedRules = result.getAnalyticsAppliedRules();
        this.percentage = result.getPercentage();;
        this.orderingWeight = result.getOrderingWeight();
        this.type = result.getType();
        this.isPremium = result.isPremium();;
        this.isPurchased = result.isPurchased();;
    }

    public EntitlementResult(boolean accept, String trace) {
        super(accept, trace);
    }

    @CheckForNull
    @Override
    public List<String> getPuchasesOptions() {
        return puchasesOptions;
    }

    public void setPuchasesOptions(List<String> puchasesOptions) {
        this.puchasesOptions = puchasesOptions;
    }

    public String getStorePoductId() {
        return storePoductId;
    }

    public void setStorePoductId(String storePoductId) {
        this.storePoductId = storePoductId;
    }
}
