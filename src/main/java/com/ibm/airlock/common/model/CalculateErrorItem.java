package com.ibm.airlock.common.model;

/**
 * Object represents calculation error.
 *
 * @author Denis Voloshin
 */

@SuppressWarnings({"unused", "ClassOnlyUsedInOnePackage"})
public class CalculateErrorItem {

    private String featureName;
    private String resultError;

    /**
     * Constructor for CalculateErrorItem class
     *
     * @param featureName feature name
     * @param resultError error details
     */
    public CalculateErrorItem(String featureName, String resultError) {
        this.featureName = featureName;
        this.resultError = resultError;
    }

    /**
     * Returns the name of the feature
     *
     * @return the name of the feature
     */
    public String getFeatureName() {
        return featureName;
    }

    /**
     * Sets the name of the feature
     *
     * @param featureName the name of the feature
     */
    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    /**
     * Returns a string that represents the error that occurred when calculating a specific feature.
     *
     * @return A string that represents the error that occurred when calculating a specific feature.
     */
    public String getResultError() {
        return resultError;
    }

    /**
     * Set the error that occurred when calculating a specific feature.
     *
     * @param resultError The error that occurred when calculating a specific feature.
     */
    public void setResultError(String resultError) {
        this.resultError = resultError;
    }
}
