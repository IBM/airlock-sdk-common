package com.ibm.airlock.common.data;

import com.ibm.airlock.common.util.BaseRawFeaturesJsonParser;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.CheckForNull;

public class PurchaseOption extends Feature {

    private JSONArray storeProductIds;

    public PurchaseOption(String name, boolean on, Source source) {
        super(name, on, source);
    }

    public PurchaseOption() {
        super();
        storeProductIds = new JSONArray();
    }

    @CheckForNull
    public String getProductId() {
        String productId = null;
        for (int i = 0; i < storeProductIds.length(); i++) {
            JSONObject store = storeProductIds.getJSONObject(i);
            if (store.has(Constants.JSON_FIELD_STORE_TYPE) && store.has(Constants.JSON_FIELD_STORE_PRODUCT_ID) &&
                    store.getString(Constants.JSON_FIELD_STORE_TYPE).equals(Constants.JSON_FIELD_PURCHASE_ANDROID_STORE_TYPE)) {
                productId = store.getString(Constants.JSON_FIELD_STORE_PRODUCT_ID);
            }
        }
        return productId;
    }

    public PurchaseOption(JSONObject jsonObject) {
        super(jsonObject);

        if (jsonObject.has("featureAttributes")) {
            String tmpConfig = jsonObject.optString("featureAttributes");
            this.setConfiguration(tmpConfig == null ? new JSONObject() : new JSONObject(tmpConfig));
        }

        storeProductIds = jsonObject.optJSONArray(Constants.JSON_FIELD_STORE_PRODUCTS);

        setEnabledForAnalytics(BaseRawFeaturesJsonParser.getIsSendToAnalytics(jsonObject));
        setAttributesForAnalytics(BaseRawFeaturesJsonParser.getConfigAttributesToAnalytics(jsonObject));
        setAnalyticsAppliedRules(BaseRawFeaturesJsonParser.getAnalyticsAppliedRules(jsonObject));
        setAnalyticsOrderedFeatures(BaseRawFeaturesJsonParser.getReorderedChildren(jsonObject));
        setAnalyticsAppliedOrderRules(BaseRawFeaturesJsonParser.getAppliedOrderRules(jsonObject));
    }

    /**
     * @return the list if store details as a JSON array, each store is a JSON map
     */
    public JSONArray getStores() {
        return storeProductIds;
    }

    public void setStores(JSONArray stores) {
        this.storeProductIds = stores;
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject childJson = super.toJsonObject();
        if (storeProductIds != null) {
            childJson.put(Constants.JSON_FIELD_STORE_PRODUCTS, storeProductIds);
        }
        return childJson;
    }
}
