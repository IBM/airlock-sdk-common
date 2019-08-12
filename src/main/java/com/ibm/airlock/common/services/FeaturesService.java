package com.ibm.airlock.common.services;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.model.FeaturesList;
import com.ibm.airlock.common.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FeaturesService {

    @Nullable
    private JSONObject lastCalculatedAirlockContext;

    @Inject
    InfraAirlockService infraAirlockService;


    @Inject
    UserGroupsService userGroupsService;


    public void init(ProductDiComponent productDiComponent) {
        productDiComponent.inject(this);
    }


    /**
     * Returns a cloned list of the ROOT children features.
     * This method is safe to traverse through its returned List, since it clones all the child features.
     *
     * @return A cloned list of the ROOT children.
     */
    public List<Feature> getRootFeatures() {
        return infraAirlockService.getSyncFeatureList().getRootFeatures();
    }

    /**
     * Returns the feature object by its name.
     * If the feature doesn't exist in the feature set, getFeature returns a new Feature object
     * with the given name, isOn=false, and source=missing.
     *
     * @param featureName Feature name in the format namespace.name.
     * @return Returns the feature object.
     */

    public Feature getFeature(String featureName) {
        return infraAirlockService.getFeature(featureName);
    }


    public Collection<String> getPurchasedProductIdsForDebug() {
        return getPurchasedProductIds();
    }


    private Collection<String> getPurchasedProductIds() {
        JSONArray productIdsArray = new JSONArray(infraAirlockService.getPersistenceHandler().
                read(Constants.PURCHASED_IDS_FOR_DEBUG, "[]"));

        List<String> productIdsList = new ArrayList<>(productIdsArray.length());
        for (int i = 0; i < productIdsArray.length(); i++) {
            productIdsList.add(productIdsArray.getString(i));
        }
        return productIdsList;
    }


    /**
     * Calculates the status of the features according to the pullFeatures results and return the Features as a tree.
     *
     * @param context             the airlock context provided by caller
     * @param purchasedProductIds the list of purchased product an user bought so far.
     * @throws AirlockNotInitializedException if the Airlock SDK has not been initialized
     * @throws JSONException                  if the pullFeature results, the userProfile or the deviceProfile is not in the correct JSON format.
     */
    public void calculateFeatures(@Nullable JSONObject context, Collection<String> purchasedProductIds) throws JSONException {

        // store last context the feature calculation was done with
        lastCalculatedAirlockContext = context;

        // if we are on the debug mode. check if there is
        // purchase option which a set as purchased for debugging purpose
        if (!userGroupsService.getDeviceUserGroups().isEmpty()) {
            purchasedProductIds.addAll(getPurchasedProductIdsForDebug());
        }
        infraAirlockService.calculateFeatures(null, context, purchasedProductIds);
    }

    /**
     * The getter for last calculated Airlock context
     *
     * @return last calculated Airlock context as JSONObject object
     */
    @Nullable
    public JSONObject getLastCalculatedAirlockContext() {
        return lastCalculatedAirlockContext;
    }

    /**
     * Calculates the status of the features according to the pullFeatures results.
     * No feature status changes are exposed until the syncFeatures method is called.
     *
     * @param userProfile    the user profile
     * @param airlockContext the airlock runtime context
     */
    public void calculateFeatures(@Nullable JSONObject userProfile, @Nullable JSONObject airlockContext) {
        infraAirlockService.calculateFeatures(userProfile, airlockContext);
    }

    /**
     * Asynchronously downloads the current list of features from the server.
     *
     * @param callback Callback to be called when the function returns.
     */
    public void pullFeatures(final AirlockCallback callback) {
        infraAirlockService.pullFeatures(callback);
    }

    /**
     * Gets the raw list of features and their rules from server.
     */
    @CheckForNull
    public JSONObject getFeaturesConfigurationFromServer() {
        return infraAirlockService.getRawFeaturesConfiguration();
    }


    /**
     * Synchronizes the latest refreshFeatures results with the current feature set.
     * Updates LastSyncTime.
     */
    public void syncFeatures() {
        infraAirlockService.syncFeatures();
    }

    public FeaturesList getSyncFeatureList() {
        return infraAirlockService.getSyncFeatureList();
    }
}
