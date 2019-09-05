package com.ibm.airlock.common.model;

import com.ibm.airlock.common.util.BaseRawFeaturesJsonParser;
import com.ibm.airlock.common.util.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Object represents a Entitlement in the Airlock configuration
 * The Entitlement object represents purchasable element in the airlock configuration
 *
 * @author Eitan Schreiber
 */
public class Entitlement extends Feature {

    private Collection<PurchaseOption> purchaseOptions;
    private Collection<String> includedEntitlements;

    @Nullable
    private Entitlement parent;

    public Entitlement() {
        purchaseOptions = new CopyOnWriteArrayList<>();
        includedEntitlements = new CopyOnWriteArrayList<>();
        setSource(Source.MISSING);
    }

    public Entitlement(JSONObject json) {
        super(json);

        // set configuration
        if (json.has(Constants.JSON_FEATURES_ATTRS)) {
            String tmpConfig = json.optString(Constants.JSON_FEATURES_ATTRS);
            setConfiguration(tmpConfig == null ? new JSONObject() : new JSONObject(tmpConfig));
        }

        purchaseOptions = new CopyOnWriteArrayList<>();
        includedEntitlements = new CopyOnWriteArrayList<>();

        // the very first time an Entitlement is parsed from the default the source is DEFAULT
        if (json.has(Constants.JSON_FEATURE_FIELD_DEFAULT)) {
            setSource(Source.DEFAULT);
            name = json.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + '.' + json.optString(Constants.JSON_FEATURE_FIELD_NAME);
        }

        JSONArray purchaseOptionsArray = json.optJSONArray(Constants.JSON_FIELD_PURCHASE_OPTIONS);
        if (purchaseOptionsArray != null) {
            for (int i = 0; i < purchaseOptionsArray.length(); i++) {
                JSONObject purchaseOption = purchaseOptionsArray.getJSONObject(i);

                // skip MX
                if (purchaseOption.optString(Constants.JSON_FEATURE_FIELD_TYPE) != null &&
                        Type.valueOf(purchaseOption.optString(Constants.JSON_FEATURE_FIELD_TYPE)) == Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP) {
                    if (purchaseOption.optString(Constants.JSON_FIELD_PURCHASE_OPTIONS) != null) {
                        JSONArray mxChildrenPurchaseOptionsArray = purchaseOption.optJSONArray(Constants.JSON_FIELD_PURCHASE_OPTIONS);
                        for (int j = 0; j < mxChildrenPurchaseOptionsArray.length(); j++) {
                            JSONObject mxChild = mxChildrenPurchaseOptionsArray.getJSONObject(j);
                            purchaseOptions.add(new PurchaseOption(mxChild));
                        }
                    }
                } else {
                    purchaseOptions.add(new PurchaseOption(purchaseOptionsArray.getJSONObject(i)));
                }
            }
        }

        JSONArray includedEntitlementsArray = json.optJSONArray(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS);
        if (includedEntitlementsArray != null) {
            for (int i = 0; i < includedEntitlementsArray.length(); i++) {
                includedEntitlements.add(includedEntitlementsArray.getString(i));
            }
        }

        setEnabledForAnalytics(BaseRawFeaturesJsonParser.isSendToAnalytics(json));
        setAttributesForAnalytics(BaseRawFeaturesJsonParser.getConfigAttributesToAnalytics(json));
        setAnalyticsAppliedRules(BaseRawFeaturesJsonParser.getAnalyticsAppliedRules(json));
        setAnalyticsOrderedFeatures(BaseRawFeaturesJsonParser.getReorderedChildren(json));
        setAnalyticsAppliedOrderRules(BaseRawFeaturesJsonParser.getAppliedOrderRules(json));

        JSONArray childrenEntitlementsArray = json.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
        if (childrenEntitlementsArray != null) {
            for (int i = 0; i < childrenEntitlementsArray.length(); i++) {
                JSONObject child = childrenEntitlementsArray.getJSONObject(i);

                // skip MX
                if (child.optString(Constants.JSON_FEATURE_FIELD_TYPE) != null &&
                        Type.valueOf(child.optString(Constants.JSON_FEATURE_FIELD_TYPE)) == Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) {
                    if (child.optString(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS) != null) {
                        JSONArray mxChildrenEntitlementsArray = child.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
                        for (int j = 0; j < mxChildrenEntitlementsArray.length(); j++) {
                            JSONObject mxChild = mxChildrenEntitlementsArray.getJSONObject(j);
                            Entitlement entitlement = new Entitlement(mxChild);
                            entitlement.setParent(this);
                            addUpdateChild(entitlement);
                        }
                    }
                } else {
                    Entitlement entitlement = new Entitlement(childrenEntitlementsArray.getJSONObject(i));
                    entitlement.setParent(this);
                    addUpdateChild(entitlement);
                }
            }
        }
    }

    public boolean hasProductId(@Nullable String productId) {
        if (productId == null) {
            return false;
        }
        for (PurchaseOption purchaseOption : getPurchaseOptions()) {
            String purchaseProductId = purchaseOption.getProductId();
            if (purchaseProductId != null && purchaseProductId.equals(productId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isOn() {
        return isOn;
    }


    /**
     * @return the list of Entitlement children
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    public Collection<Entitlement> getEntitlementChildren() {
        //noinspection SuspiciousToArrayCall
        return Arrays.asList(children.toArray(new Entitlement[0]));
    }

    /**
     * @return the list of the included Entitlements, meaning the Entitlement is a bundle
     */
    public Collection<String> getIncludedEntitlements() {
        return includedEntitlements;
    }

    /**
     * @return the list of available purchase options
     */
    public Collection<PurchaseOption> getPurchaseOptions() {
        return purchaseOptions;
    }

    public void setPurchaseOptions(Collection<PurchaseOption> purchaseOptions) {
        this.purchaseOptions = purchaseOptions;
    }

    @Override
    @Nullable
    public Entitlement getParent() {
        return parent;
    }

    private void setParent(@Nullable Entitlement parent) {
        this.parent = parent;
    }

    private Entitlement(String name, boolean on, Source source, Collection<PurchaseOption> purchaseOptions) {
        super(name, on, source);
        this.purchaseOptions = purchaseOptions;
    }

    public Entitlement(String name, boolean on, Source source) {
        super(name, on, source);
    }

    @Override
    protected Entitlement getNew() {
        return new Entitlement(getName(), isOn(), getSource(), new ArrayList<PurchaseOption>(purchaseOptions));
    }

}
