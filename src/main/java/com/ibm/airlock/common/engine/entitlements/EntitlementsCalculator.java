package com.ibm.airlock.common.engine.entitlements;

import com.ibm.airlock.common.engine.Result;
import com.ibm.airlock.common.engine.ScriptInvoker;
import com.ibm.airlock.common.engine.features.FeaturesCalculator;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * The Rule engine for airlock purchases
 *
 * @author Denis Voloshin
 */
public class EntitlementsCalculator extends FeaturesCalculator {


    @Override
    protected JSONObject getRoot(@Nullable JSONObject feature) {
        if (feature == null) {
            return new JSONObject();
        }
        return feature.optJSONObject(Constants.JSON_FIELD_ENTITLEMENT_ROOT);
    }

    @Override
    protected String getChildrenName() {
        return Constants.JSON_FEATURE_FIELD_ENTITLEMENTS;
    }

    @Override
    protected String getRootName() {
        return Constants.JSON_FIELD_ENTITLEMENT_ROOT;
    }


    @Override
    protected int mutexCount(JSONObject feature) {
        Feature.Type type = getNodeType(feature);
        switch (type) {
            case CONFIG_MUTUAL_EXCLUSION_GROUP:
            case ENTITLEMENT_MUTUAL_EXCLUSION_GROUP:
            case PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP:
                return feature.optInt(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON, 1);
            default:
                return 0;
        }

    }


    @Override
    protected Result doFeature(JSONObject feature, ScriptInvoker invoker, AdditionalData additionalData, Map<String, Fallback> fallback) {
        Result result = super.doFeature(feature,
                invoker,
                additionalData,
                fallback);


        // process purchase options (have mutex and configuration)

        JSONArray options = feature.optJSONArray(Constants.JSON_FIELD_PURCHASE_OPTIONS);
        if (options != null && options.length() > 0) {
            for (int i = 0; i < options.length(); i++) {

                JSONObject option = options.getJSONObject(i);

                JSONArray array = getChildren(option, Constants.JSON_FIELD_PURCHASE_OPTIONS);
                if (array == null || array.length() == 0) {
                    if (results != null){
                        if(!result.isAccept()){
                            results.put(getName(option), new Result(false, Result.RULE_PARENT_FAILED));
                        }else{
                            results.put(getName(option), doFeature(option, invoker, additionalData, fallback));
                        }
                    }
                } else {
                    if (results == null){
                        results = new TreeMap<>();
                    }
                    doFeatureGroup(
                            options.getJSONObject(i),
                            Constants.JSON_FIELD_PURCHASE_OPTIONS,
                            invoker,
                            additionalData,
                            -1,
                            fallback,
                            results);
                }
            }
        }


        // after process logic,only purchase related.
        EntitlementResult purchaseResult = new EntitlementResult(result);

        if (getNodeType(feature) == Feature.Type.PURCHASE_OPTIONS) {
            purchaseResult.setStoreProductId(getPurchaseStoreIds(feature));
        }

        if (getNodeType(feature) == Feature.Type.ENTITLEMENT) {
            purchaseResult.setPurchasesOptions(getPurchaseOptions(feature));
        }

        return result;
    }


    private List<String> getPurchaseOptions(JSONObject feature) {
        List<String> purchaseOptions = new ArrayList<>();
        if (feature.has(Constants.JSON_FIELD_PURCHASE_OPTIONS)) {
            JSONArray purchaseOptionsArray = feature.optJSONArray(Constants.JSON_FIELD_PURCHASE_OPTIONS);
            for (int i = 0; i < purchaseOptionsArray.length(); i++) {
                JSONObject option = purchaseOptionsArray.getJSONObject(i);
                purchaseOptions.add(getName(option));
            }
        }
        return purchaseOptions;
    }


    @CheckForNull
    private String getPurchaseStoreIds(JSONObject feature) {
        JSONArray storePurchaseIds = feature.optJSONArray(Constants.JSON_FIELD_STORE_PRODUCTS);
        if (storePurchaseIds == null) {
            return null;
        }
        for (int i = 0; i < storePurchaseIds.length(); i++) {
            JSONObject storePurchase = storePurchaseIds.getJSONObject(i);
            if (storePurchase.has(Constants.JSON_FIELD_STORE_TYPE) &&
                    storePurchase.getString(Constants.JSON_FIELD_STORE_TYPE).equals(Constants.JSON_FIELD_PURCHASE_ANDROID_STORE_TYPE)) {
                return storePurchase.getString(Constants.JSON_FIELD_STORE_PRODUCT_ID);
            }
        }
        return null;
    }


    @Override
    protected JSONObject embedOneChild(JSONObject feature, String childName, Map<String, Result> resultMap, AdditionalData additionalData) throws JSONException {
        JSONObject embedChild = super.embedOneChild(feature, childName, resultMap, additionalData);
        if (embedChild == null){
            embedChild = new JSONObject();
        }

        if (getNodeType(feature) == Feature.Type.ENTITLEMENT) {
            JSONArray children = embedChildren(feature, Constants.JSON_FIELD_PURCHASE_OPTIONS, resultMap, additionalData);
            embedChild.put(Constants.JSON_FIELD_PURCHASE_OPTIONS, children);
        }
        return embedChild;
    }

    @Override
    protected void embedOneChildAttributes(JSONObject feature, JSONObject embedChild, Map<String, Result> resultMap) {
        super.embedOneChildAttributes(feature, embedChild, resultMap);
        String type = feature.optString(Constants.JSON_FEATURE_FIELD_TYPE, Feature.Type.FEATURE.toString());

        embedChild.put(Constants.JSON_FEATURE_FIELD_TYPE, type);
        embedChild.put(Constants.JSON_FEATURE_FULL_NAME, extendedName(feature));

        if (getNodeType(feature) == Feature.Type.ENTITLEMENT) {
            embedChild.put(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS, feature.getJSONArray(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS));
        }

        // add store options
        if (getNodeType(feature) == Feature.Type.PURCHASE_OPTIONS && feature.has(Constants.JSON_FIELD_STORE_PRODUCTS)) {
            embedChild.put(Constants.JSON_FIELD_STORE_PRODUCTS, feature.getJSONArray(Constants.JSON_FIELD_STORE_PRODUCTS));
        }
    }


    public Map<String, List<String>> getPurchaseToProductIdsMap(@Nullable JSONObject purchasesJSON) {
        if (purchasesJSON == null) {
            return new Hashtable<>();
        }
        Map<String, List<String>> purchaseToProductIdMap = new Hashtable<>();
        JSONArray purchases = purchasesJSON.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
        if (purchases != null) {
            for (int i = 0; i < purchases.length(); ++i) {
                JSONObject purchase = purchases.getJSONObject(i);
                if (mutexCount(purchase) == 0) {
                    purchaseToProductIdMap.put(extendedName((purchase)),
                            getPurchaseProductIds(purchase, purchaseToProductIdMap));
                } else {
                    getPurchaseProductIds(purchase, purchaseToProductIdMap);
                }
            }
        }
        return purchaseToProductIdMap;
    }

    private List<String> getPurchaseProductIds(JSONObject purchase, Map<String, List<String>> purchaseToProductIdMap) {

        // parse mutex
        if (mutexCount(purchase) > 0) {
            JSONArray purchases = purchase.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
            if (purchases != null) {
                for (int i = 0; i < purchases.length(); ++i) {
                    JSONObject purchaseInMX = purchases.getJSONObject(i);
                    if (mutexCount(purchaseInMX) == 0) {
                        purchaseToProductIdMap.put(extendedName((purchaseInMX)),
                                getPurchaseProductIds(purchaseInMX, purchaseToProductIdMap));
                    } else {
                        getPurchaseProductIds(purchaseInMX, purchaseToProductIdMap);
                    }
                }
            }
        }

        List<String> productIds = new ArrayList<>();
        JSONArray purchaseOptions = purchase.optJSONArray(Constants.JSON_FIELD_PURCHASE_OPTIONS);
        if (purchaseOptions != null) {
            for (int i = 0; i < purchaseOptions.length(); ++i) {
                JSONObject option = purchaseOptions.getJSONObject(i);
                if (option.has(Constants.JSON_FIELD_STORE_PRODUCTS)) {
                    getPurchaseOption(option, productIds);
                }
            }
        }
        return productIds;
    }

    private void getPurchaseOption(JSONObject option, List<String> productIds) {

        // parse mutex
        if (mutexCount(option) > 0) {
            JSONArray options = option.optJSONArray(Constants.JSON_FIELD_PURCHASE_OPTIONS);
            if (options != null) {
                for (int i = 0; i < options.length(); ++i) {
                    JSONObject purchaseInMX = options.getJSONObject(i);
                    getPurchaseOption(purchaseInMX, productIds);
                    return;
                }
            }
        }

        JSONArray stores = option.optJSONArray(Constants.JSON_FIELD_STORE_PRODUCTS);
        if (stores == null) {
            return;
        }
        for (int j = 0; j < stores.length(); ++j) {
            JSONObject store = stores.getJSONObject(j);
            if (store.has(Constants.JSON_FIELD_STORE_TYPE) && store.has(Constants.JSON_FIELD_STORE_PRODUCT_ID) &&
                    store.getString(Constants.JSON_FIELD_STORE_TYPE).equals(Constants.JSON_FIELD_PURCHASE_ANDROID_STORE_TYPE)) {
                productIds.add(store.getString(Constants.JSON_FIELD_STORE_PRODUCT_ID));
            }
        }
    }

}
