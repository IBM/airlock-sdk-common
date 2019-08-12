package com.ibm.airlock.common;

import com.ibm.airlock.common.model.Feature;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 *  Defines the external airlock client API
 *  which allows to get airlock basic services
 */
public interface AirlockClient {


    AirlockProductManager getAirlockProductManager();

    /**
     * Returns a cloned list of the ROOT children features.
     * This method is safe to traverse through its returned List, since it clones all the child features.
     *
     * @return A cloned list of the ROOT children.
     */
     List<Feature> getRootFeatures();

    /**
     * Returns the feature object by its name.
     * If the feature doesn't exist in the feature set, getFeature returns a new Feature object
     * with the given name, isOn=false, and source=missing.
     *
     * @param featureName Feature name in the format namespace.name.
     * @return Returns the feature object.
     */

     Feature getFeature(String featureName);


    /**
     * Calculates the status of the features according to the pullFeatures results and return the Features as a tree.
     *
     * @param context             the airlock context provided by caller
     * @param purchasedProductIds the list of purchased product an user bought so far.
     * @throws JSONException                  if the pullFeature results, the userProfile or the deviceProfile is not in the correct JSON format.
     */
     void calculateFeatures(@Nullable JSONObject context, Collection<String> purchasedProductIds);


    /**
     * Calculates the status of the features according to the pullFeatures results.
     * No feature status changes are exposed until the syncFeatures method is called.
     *
     * @param userProfile    the user profile
     * @param airlockContext the airlock runtime context
     * @throws JSONException if the pullFeature results, the userProfile or the deviceProfile is not in the correct JSON format.
     */
     void calculateFeatures(@Nullable JSONObject userProfile, @Nullable JSONObject airlockContext);
    /**
     * Asynchronously downloads the current list of features from the server.
     *
     * @param callback Callback to be called when the function returns.
     */
     void pullFeatures(final AirlockCallback callback);


    /**
     * Synchronizes the latest refreshFeatures results with the current feature set.
     * Updates LastSyncTime.
     *
     */
     void syncFeatures();
}
