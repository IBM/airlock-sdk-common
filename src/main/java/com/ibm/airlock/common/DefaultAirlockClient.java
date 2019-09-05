package com.ibm.airlock.common;

import com.ibm.airlock.common.model.Feature;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;


/**
 * The default implementation of AirlockClient interface
 */
public class DefaultAirlockClient implements AirlockClient {

    private final AirlockProductManager airlockProductManager;
    private final String id;

    /**
     * Defines DefaultAirlockClient
     * @param airlockProductManager airlock product the client will be associated
     * @param id airlock instance client id
     */
    public DefaultAirlockClient(AirlockProductManager airlockProductManager, String id) {
        this.airlockProductManager = airlockProductManager;
        this.id = id;
    }

    /**
     * Returns client unique id
     * @return
     */
    public String getId() {
        return id;
    }

    @Override
    public AirlockProductManager getAirlockProductManager() {
        return airlockProductManager;
    }

    /**
     * Returns a cloned list of the ROOT children features.
     * This method is safe to traverse through its returned List, since it clones all the child features.
     *
     * @return A cloned list of the ROOT children.
     */
    @Override
    public List<Feature> getFeatures() {
        return airlockProductManager.getFeaturesService().getRootFeatures();
    }

    /**
     * Returns the feature object by its name.
     * If the feature doesn't exist in the feature set, getFeature returns a new Feature object
     * with the given name, isOn=false, and source=missing.
     *
     * @param featureName Feature name in the format namespace.name.
     * @return Returns the feature object.
     */

    @Override
    public Feature getFeature(String featureName) {
        return airlockProductManager.getFeaturesService().getFeature(featureName);
    }

    /**
     * Calculates the status of the features according to the pullFeatures results and return the Features as a tree.
     *
     * @param context             the airlock context provided by caller
     * @param purchasedProductIds the list of purchased product an user bought so far.
     */
    @Override
    public void calculateFeatures(@Nullable JSONObject context, Collection<String> purchasedProductIds) {
        // if we are on the debug mode. check if there is
        // purchase option which a set as purchased for debugging purposes
        if (!airlockProductManager.getUserGroupsService().getDeviceUserGroups().isEmpty()) {
            purchasedProductIds.addAll(airlockProductManager.getFeaturesService().getPurchasedProductIdsForDebug());
        }
        airlockProductManager.getFeaturesService().calculateFeatures(context, purchasedProductIds);
    }

    /**
     * Calculates the status of the features according to the pullFeatures results.
     * No feature status changes are exposed until the syncFeatures method is called.
     *
     * @param userProfile    the user profile
     * @param airlockContext the airlock runtime context
     */
    @Override
    public void calculateFeatures(@Nullable JSONObject userProfile, @Nullable JSONObject airlockContext) {
        airlockProductManager.getFeaturesService().calculateFeatures(userProfile, airlockContext);
    }

    /**
     * Asynchronously downloads the current list of features from the server.
     *
     * @param callback Callback to be called when the function returns.
     */
    @Override
    public void pullFeatures(final AirlockCallback callback) {
        airlockProductManager.getFeaturesService().pullFeatures(callback);
    }

    /**
     * Synchronizes the latest refreshFeatures results with the current feature set.
     * Updates LastSyncTime.
     */
    @Override
    public void syncFeatures() {
        airlockProductManager.getFeaturesService().syncFeatures();
    }
}
