package com.ibm.airlock.common.services;

import com.ibm.airlock.common.dependency.ProductDiComponent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.inject.Inject;

import static com.ibm.airlock.common.util.BaseRawFeaturesJsonParser.getFieldValueFromJsonObject;

public class AnalyticsService {
    private static final String TAG = "AnalyticsService";

    @Inject
    InfraAirlockService infraAirlockService;


    public void init(ProductDiComponent productDiComponent) {
        productDiComponent.inject(this);
    }


    @CheckForNull
    public JSONArray getContextFieldsForAnalytics() {
        return infraAirlockService.getContextFieldsForAnalytics();
    }

    @CheckForNull
    public JSONObject getContextFieldsValuesForAnalytics(JSONObject contextObject) {

        JSONObject calculatedFeatures = new JSONObject();
        JSONArray contextFields = infraAirlockService.getContextFieldsForAnalytics();

        int contextFieldsLength = 0;
        if (contextFields != null) {
            contextFieldsLength = contextFields.length();
        }
        for (int i = 0; i < contextFieldsLength; i++) {
            String contextFieldName = null;
            try {
                contextFieldName = contextFields.get(i).toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (contextFieldName != null) {
                String contextFieldValue = getFieldValueFromJsonObject(contextObject, contextFieldName.split("\\."));
                if (contextFieldValue != null) {
                    calculatedFeatures.put(contextFieldName, contextFieldValue);
                }
            }
        }
        return calculatedFeatures;
    }
}
