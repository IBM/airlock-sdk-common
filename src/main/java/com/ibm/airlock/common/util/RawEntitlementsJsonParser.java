package com.ibm.airlock.common.util;

import com.ibm.airlock.common.model.Entitlement;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.model.FeaturesList;
import com.ibm.airlock.common.model.PurchaseOption;
import com.ibm.airlock.common.model.PurchasesList;
import com.ibm.airlock.common.engine.features.FeaturesCalculator;
import com.ibm.airlock.common.engine.ScriptExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by Denis Voloshin on 04/02/2019.
 */
public class RawEntitlementsJsonParser extends BaseRawFeaturesJsonParser {

    static class SingletonHolder {
        static final RawEntitlementsJsonParser HOLDER_INSTANCE = new RawEntitlementsJsonParser();
    }

    public static RawEntitlementsJsonParser getInstance() {
        return RawEntitlementsJsonParser.SingletonHolder.HOLDER_INSTANCE;
    }

    @Override
    public Feature getFeaturesTree(JSONObject root, Feature.Source source) throws JSONException, ScriptExecutionException {
        Feature feature = new Entitlement();
        FeaturesList out = new PurchasesList();
        feature.setSource(source == Feature.Source.UNKNOWN ? getSource(root) : source);
        descend(root, feature, source, out);
        return feature;
    }

    public FeaturesList getPurchases(JSONObject root, Feature.Source source) throws JSONException, ScriptExecutionException {
        Feature purchase = new Entitlement(FeaturesCalculator.FeatureType.ROOT.toString().toLowerCase(Locale.getDefault()), true,
                source == Feature.Source.UNKNOWN ? getSource(root) : source);
        FeaturesList out = new PurchasesList();
        descend(root, purchase, source, out);
        return out;
    }

    @Override
    protected void populateFeatureChild(Feature childFeature, Feature.Source source, JSONObject childAsJson) throws JSONException, ScriptExecutionException {
        super.populateFeatureChild(childFeature, source, childAsJson);

        if (childAsJson.has(Constants.JSON_FIELD_PURCHASE_OPTIONS)) {
            JSONArray purchaseOptionsAsJson = childAsJson.getJSONArray(Constants.JSON_FIELD_PURCHASE_OPTIONS);

            // create dummy root json object for purchase options
            JSONObject purchaseOptionsRoot = new JSONObject();
            purchaseOptionsRoot.put(Constants.JSON_FEATURE_FIELD_TYPE, Feature.Type.PURCHASE_OPTIONS);
            purchaseOptionsRoot.put(Constants.JSON_FIELD_PURCHASE_OPTIONS, purchaseOptionsAsJson);
            if (childFeature instanceof Entitlement) {
                ((Entitlement) childFeature).setPurchaseOptions(getPurchasOptions(purchaseOptionsRoot, source).getMutableChildrenList());
            } else if (childFeature instanceof PurchaseOption) {
                ((PurchaseOption) childFeature).setStores(childAsJson.getJSONArray(Constants.JSON_FIELD_STORE_PRODUCTS));
            }
        }
    }


    private FeaturesList getPurchasOptions(JSONObject root, Feature.Source source) throws JSONException, ScriptExecutionException {
        PurchaseOption feature = new PurchaseOption(FeaturesCalculator.FeatureType.ROOT.toString().toLowerCase(Locale.getDefault()), true,
                source == Feature.Source.UNKNOWN ? getSource(root) : source);
        FeaturesList out = new FeaturesList();
        descend(root, feature, source, out);
        return out;
    }


    @Override
    protected JSONArray getChildren(JSONObject feature) {
        String childName = Constants.JSON_FEATURE_FIELD_ENTITLEMENTS;
        if (getType(feature) == Feature.Type.PURCHASE_OPTIONS) {
            childName = Constants.JSON_FIELD_PURCHASE_OPTIONS;
        }

        JSONArray out = feature.optJSONArray(childName);
        if (out == null) {
            out = new JSONArray();
        }
        return out;
    }

    @Override
    protected Feature createEmptyFeature(Feature.Type type) {
        if (type == Feature.Type.ENTITLEMENT) {
            return new Entitlement();
        } else if (type == Feature.Type.PURCHASE_OPTIONS) {
            return new PurchaseOption();
        } else {
            return new Entitlement();
        }
    }

    @Override
    protected boolean isMutex(JSONObject feature) {

        switch (getType(feature)) {
            case CONFIG_MUTUAL_EXCLUSION_GROUP:
            case ENTITLEMENT_MUTUAL_EXCLUSION_GROUP:
            case PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP:
                return true;
            default:
                return false;
        }
    }
}
