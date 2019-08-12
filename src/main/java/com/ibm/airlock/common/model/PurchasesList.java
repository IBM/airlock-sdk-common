package com.ibm.airlock.common.model;

import com.ibm.airlock.common.engine.ScriptExecutionException;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.RawEntitlementsJsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;

/**
 * Created by Denis Voloshin on 04/02/2019.
 */
public class PurchasesList extends FeaturesList {

    private static final String TAG = "Airlock.PurchasesList";

    public PurchasesList() {
        features = new Hashtable<>();
    }

    public PurchasesList(String json, Feature.Source source) {
        try {
            if (json == null || json.isEmpty()) {
                features = new Hashtable<>();
            }
            JSONObject root = new JSONObject(json);
            JSONObject featuresArray = root.optJSONObject(getRooName());
            FeaturesList res = RawEntitlementsJsonParser.getInstance().getFeatures(featuresArray, source);
            features = res.features;
        } catch (JSONException | ScriptExecutionException e) {
            Logger.log.e(TAG, e.getMessage(), e);
            features = new Hashtable<>();
        }
    }


    @Override
    protected String getRooName() {
        return Constants.JSON_FIELD_ENTITLEMENT_ROOT;
    }
}
