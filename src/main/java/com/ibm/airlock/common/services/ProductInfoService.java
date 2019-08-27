package com.ibm.airlock.common.services;

import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.util.Constants;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import java.util.Map;

public class ProductInfoService {

    private static final String TAG = "ProductInfoService";
    private static final boolean init = true;
    private boolean allowExperimentEvaluation;

    @Inject
    InfraAirlockService infraAirlockService;


    public void init(ProductDiComponent productDiComponent) {
        productDiComponent.inject(this);
    }


    public boolean isAllowExperimentEvaluation() {
        return allowExperimentEvaluation;
    }

    public void setAllowExperimentEvaluation(boolean allowExperimentEvaluation) {
        this.allowExperimentEvaluation = allowExperimentEvaluation;
    }


    /**
     * @return default file the product was init with
     */
    public String getDefaultFile() {
        return this.infraAirlockService.getPersistenceHandler().read(Constants.SP_DEFAULT_FILE, "{}");
    }

    /**
     * Returns the airlock session id the airlock instance was initialized with
     * or null if {@link #init(ProductDiComponent)} method was not called yet.
     */
    @CheckForNull
    public String getSeasonId() {
        return !init ? null : infraAirlockService.getSeasonId();
    }

    /**
     * Returns the airlock version from the airlock instance was initialized with
     * or null if {@link #init(ProductDiComponent)} method was not called yet.
     */
    @CheckForNull
    public String getAirlockVersion() {
        return !init ? null : infraAirlockService.getAirlockVersion();
    }


    /**
     * Returns the airlock product id the airlock instance was initialized with or null
     * if {@link #init(ProductDiComponent)} method was not called yet.
     */
    @CheckForNull
    public String getAppVersion() {
        return !init ? null : infraAirlockService.getAppVersion();
    }

    /**
     * Returns the airlock product id the airlock instance was initialized with or null
     * if {@link #init(ProductDiComponent)} method was not called yet.
     */
    @CheckForNull
    public String getProductId() {
        return !init ? null : infraAirlockService.getProductId();
    }

    /**
     * Returns the airlock ExperimentInfo
     */
    @CheckForNull
    public Map<String, String> getExperimentInfo() {
        return !init ? null : infraAirlockService.getExperimentInfo();
    }
}
