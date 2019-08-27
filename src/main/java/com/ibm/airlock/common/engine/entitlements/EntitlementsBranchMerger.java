package com.ibm.airlock.common.engine.entitlements;

import com.ibm.airlock.common.engine.features.FeaturesBranchMerger;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Denis Voloshin on 20/02/2019.
 */
@SuppressWarnings("MethodDoesntCallSuperMethod")
public class EntitlementsBranchMerger extends FeaturesBranchMerger {

    @Override
    protected String getChildrenName() {
        return Constants.JSON_FEATURE_FIELD_ENTITLEMENTS;
    }

    @Override
    protected String getRootName() {
        return Constants.JSON_FIELD_ENTITLEMENT_ROOT;
    }

    @Override
    protected String getBranchItemsName() {
        return Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS;
    }


    @Override
    protected Feature.Type baseType(JSONObject item) {
        switch (getNodeType(item)) {
            case CONFIGURATION_RULE:
            case CONFIG_MUTUAL_EXCLUSION_GROUP:
                return Feature.Type.CONFIGURATION_RULE;

            case PURCHASE_OPTIONS:
            case PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP:
                return Feature.Type.PURCHASE_OPTIONS;

            default:
                return Feature.Type.ENTITLEMENT;
        }
    }


    @Override
    protected void resolveChildren(JSONObject override, Map<String, JSONObject> nameMap, String nameKey, String childKey) throws Exception {
        super.resolveChildren(override, nameMap, nameKey, childKey);
        resolvePurchaseOptions(override, nameMap, Constants.JSON_FIELD_BRANCH_PURCHASE_OPTION_ITEMS, Constants.JSON_FIELD_PURCHASE_OPTIONS);
    }


    @Override
    public void mapItem(JSONObject in, Map<String, JSONObject> out, boolean useId) throws JSONException {
        super.mapItem(in, out, useId);
        JSONArray purchaseOptions = in.optJSONArray(Constants.JSON_FIELD_PURCHASE_OPTIONS);
        if (purchaseOptions != null) {
            for (int i = 0; i < purchaseOptions.length(); ++i) {
                JSONObject purchasOption = purchaseOptions.optJSONObject(i);
                if (purchasOption != null) {
                    String key = useId ? getId(purchasOption) : getName(purchasOption);
                    out.put(key, purchasOption);
                }

            }
        }
    }

    private void resolvePurchaseOptions(JSONObject override, Map<String, JSONObject> nameMap, String nameKey, String childKey) throws Exception {
        JSONArray names = override.optJSONArray(nameKey);
        JSONArray purchaseOptions = override.optJSONArray(childKey);
        if (names == null || names.length() == 0) {
            return;
        }

        Map<String, JSONObject> overrideKids = new HashMap<>();
        if (purchaseOptions != null) {
            for (int i = 0; i < purchaseOptions.length(); ++i) {
                JSONObject child = purchaseOptions.getJSONObject(i);
                overrideKids.put(getName(child), child);
            }
        }

        JSONArray newKids = new JSONArray();
        for (int i = 0; i < names.length(); ++i) {
            String childName = names.getString(i);
            // look for the child in the override first, then look in the master
            JSONObject child = overrideKids.get(childName);
            if (child == null) {
                child = nameMap.get(childName);
                if (child == null) // this happens when a DEVELOPMENT node is filtered out in a PRODUCTION master. remove it from the list of children
                {
                    continue;
                }

                // we can't just put the child as-is - it may have moved from a parent that isn't overridden and still points to it.
                // that old link needs to be identified in removeResiduals. so we clone the child, put the duplicate in the
                // override, and mark the original for deletion. the clone is shallow since we only need to override the status.

                JSONObject newChild = (JSONObject) cloneJson(child, false);
                setBranchStatus(child, Feature.BranchStatus.TEMPORARY);
                child = newChild;
            }
            newKids.put(child);
        }

        override.put(childKey, newKids);

        // recurse on the override's children
        for (JSONObject child : overrideKids.values()) {
            resolve(child, nameMap);
        }

        for (int i = 0; i < newKids.length(); i++) {
            JSONObject kid = newKids.getJSONObject(i);
            nameMap.put(getName(kid), kid);
        }
    }
}
