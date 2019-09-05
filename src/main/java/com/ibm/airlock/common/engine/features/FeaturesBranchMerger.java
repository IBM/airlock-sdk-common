package com.ibm.airlock.common.engine.features;

import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.*;


/**
 * The class is responsible for airlock branches merge process
 */
public class FeaturesBranchMerger {


    public Map<String, JSONObject> getItemMap(JSONObject in, boolean useId) throws JSONException {
        Map<String, JSONObject> out = new HashMap<>();
        mapItem(in, out, useId);
        return out;
    }

    protected String getChildrenName() {
        return Constants.JSON_FEATURE_FIELD_FEATURES;
    }

    protected String getRootName() {
        return Constants.JSON_FIELD_ROOT;
    }

    protected String getBranchItemsName() {
        return Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS;
    }


    public void mapItem(JSONObject in, Map<String, JSONObject> out, boolean useId) throws JSONException {
        String key = useId ? getId(in) : getName(in);
        out.put(key, in);

        JSONArray array = in.optJSONArray(getChildrenName());
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                mapItem(array.getJSONObject(i), out, useId);
            }
        }

        array = in.optJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                mapItem(array.getJSONObject(i), out, useId);
            }
        }

        array = in.optJSONArray(Constants.JSON_FEATURE_FIELD_BASED_RULES_ORDERING);
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                mapItem(array.getJSONObject(i), out, useId);
            }
        }
    }

    private Feature.Type resolveFeatureType(@Nullable String type, Feature.Type defaultValue) {

        if (type == null) {
            return defaultValue;
        }

        try {
            return Feature.Type.valueOf(type.trim());
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

    protected Feature.Type getNodeType(JSONObject obj) throws JSONException {
        String str = obj.getString(Constants.JSON_FEATURE_FIELD_TYPE);
        return resolveFeatureType(str, Feature.Type.FEATURE); // null on error
    }

    private Feature.BranchStatus getBranchStatus(JSONObject obj) throws JSONException {
        String str = obj.getString(Constants.JSON_FIELD_BRANCH_STATUS);
        return Feature.BranchStatus.valueOf(str); // null on error
    }

    protected void setBranchStatus(JSONObject obj, Feature.BranchStatus status) throws JSONException {
        obj.put(Constants.JSON_FIELD_BRANCH_STATUS, status.toString());
    }

    protected String getName(JSONObject obj) throws JSONException {
        switch (getNodeType(obj)) {
            case ROOT:
                return "ROOT";

            case MUTUAL_EXCLUSION_GROUP:
            case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
            case CONFIG_MUTUAL_EXCLUSION_GROUP:
            case ENTITLEMENT_MUTUAL_EXCLUSION_GROUP:
            case PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP:
                return "mx." + getId(obj);

            default:
                String name = obj.getString(Constants.JSON_FIELD_NAME);
                String namespace = obj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE);
                return namespace + '.' + name;
        }
    }

    protected String getId(JSONObject obj) throws JSONException {
        return obj.getString(Constants.JSON_FIELD_UNIQUE_ID);
    }

    public JSONObject merge(@Nullable JSONObject clonedMaster, @Nullable Map<String, JSONObject> nameMap, JSONObject branch) throws MergeException {

        if (clonedMaster == null || nameMap == null) {
            return new JSONObject();
        }

        try {
            JSONArray branchItems = branch.getJSONArray(getChildrenName());

            // mark old nodes for deletion
            for (int i = 0; i < branchItems.length(); ++i) {
                JSONObject override = branchItems.getJSONObject(i);
                markForDeletion(override, nameMap);
            }

            // replace and add items
            for (int i = 0; i < branchItems.length(); ++i) {
                JSONObject override = branchItems.getJSONObject(i);
                override = (JSONObject) cloneJson(override, true);
                if (isRoot(override)) {
                    mergeRootNames(override, clonedMaster);
                    resolveChildren(override, nameMap, getBranchItemsName(), getChildrenName());
                    nameMap.put(getName(override), override);
                } else {
                    overrideItem(override, nameMap);
                }
            }

            // remove any remaining references to old features that have been overridden or moved.
            // needed for cases such as ROOT--> A--> B changed to ROOT--> B--> A
            removeResiduals(clonedMaster);
            return clonedMaster;
        } catch (Exception e) {
            throw new MergeException("Merge error: " + e.getMessage());
        }
    }

    private boolean isRoot(JSONObject override) {
        String parentName = override.optString(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME);
        return parentName == null || parentName.isEmpty();
    }

    private void mergeRootNames(JSONObject override, JSONObject clonedMaster) throws JSONException {
        // if the override of the root is checked out, the child names are taken as is
        // i.e. the children are frozen; new additions to the master root will not appear in the result.
        if (getBranchStatus(override) == Feature.BranchStatus.CHECKED_OUT) {
            return;
        }

        // else, the override child names are extended by merging them with the root's current child names.
        // i.e. children added to the master root after the branch was created will appear in the result

        // get child names from master
        JSONArray newNames = new JSONArray();
        TreeSet<String> nameSet = new TreeSet<>();
        JSONArray source = clonedMaster.optJSONArray(getChildrenName());

        if (source != null) {
            for (int i = 0; i < source.length(); ++i) {
                JSONObject child = source.getJSONObject(i);
                if (getBranchStatus(child) == Feature.BranchStatus.TEMPORARY) {
                    continue; // marked for deletion
                }

                String childName = getName(child);
                newNames.put(childName);
                nameSet.add(childName);
            }
        }

        // get child names from override, append the additional ones to the new name list
        JSONArray overrideNames = override.getJSONArray(getBranchItemsName());
        for (int i = 0; i < overrideNames.length(); ++i) {
            String overrideName = overrideNames.getString(i);
            if (!nameSet.contains(overrideName)) {
                newNames.put(overrideName);
            }
        }
        // replace name list in override
        override.put(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS, newNames);
    }

    private void overrideItem(JSONObject override, Map<String, JSONObject> nameMap) throws Exception {
        String parentName = override.getString(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME);
        JSONObject parent = nameMap.get(parentName);
        if (parent == null) {
            throw new Exception("parent does not exist: " + parentName);
        }

        String overrideName = getName(override);
        JSONObject original = nameMap.get(overrideName);

        if (getBranchStatus(override) != Feature.BranchStatus.NEW) {
            if (original == null) {
                // this happens when an overridden DEVELOPMENT node is filtered out in a PRODUCTION master. Treat the overriding node as NEW
                if (getBranchStatus(override) == Feature.BranchStatus.CHECKED_OUT) {
                    setBranchStatus(override, Feature.BranchStatus.NEW); // the override should contain all fields, not just a delta
                }
            } else {
                setBranchStatus(original, Feature.BranchStatus.TEMPORARY);  // mark for deletion in case it was NONE rather than CHECKED_OUT
            }
        }

        Feature.Type type = baseType(override);

        JSONArray source = getChildrenByType(parent, type);
        if (source == null) {
            source = new JSONArray(); // is it OK to start with empty parent?
        }

        // find feature in parent and replace it, or add it as new
        JSONArray newChildren = new JSONArray();
        boolean found = false;

        for (int i = 0; i < source.length(); ++i) {
            JSONObject child = source.getJSONObject(i);
            if (getName(child).equals(overrideName)) {
                newChildren.put(override);
                found = true;
            } else {
                newChildren.put(child);
            }
        }
        if (!found) {
            newChildren.put(override);
        }

        // point parent to the replacement
        getChildrenByType(parent, type, newChildren);

        // resolve children of the override
        resolve(override, nameMap);
        nameMap.put(overrideName, override);
    }

    private void getChildrenByType(JSONObject parent, Feature.Type type, JSONArray children) {
        switch (type) {
            case CONFIGURATION_RULE:
            case CONFIG_MUTUAL_EXCLUSION_GROUP:
                parent.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, children);
                break;

            case ORDERING_RULE:
            case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
                parent.put(Constants.JSON_FEATURE_FIELD_BASED_RULES_ORDERING, children);
                break;

            default:
                parent.put(getChildrenName(), children);
        }

    }

    @CheckForNull
    private JSONArray getChildrenByType(JSONObject parent, Feature.Type type) {
        switch (type) {
            case CONFIGURATION_RULE:
            case CONFIG_MUTUAL_EXCLUSION_GROUP:
                return parent.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);

            case ORDERING_RULE:
            case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
                return parent.getJSONArray(Constants.JSON_FEATURE_FIELD_BASED_RULES_ORDERING);

            default:
                return parent.getJSONArray(getChildrenName());
        }

    }


    protected Feature.Type baseType(JSONObject item) {
        switch (getNodeType(item)) {
            case CONFIGURATION_RULE:
            case CONFIG_MUTUAL_EXCLUSION_GROUP:
                return Feature.Type.CONFIGURATION_RULE;

            case ORDERING_RULE:
            case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
                return Feature.Type.ORDERING_RULE;

            default:
                return Feature.Type.FEATURE;
        }
    }


    protected void resolve(JSONObject override, Map<String, JSONObject> nameMap) throws Exception {
        mergeDelta(override, nameMap);
        resolveChildren(override, nameMap, getBranchItemsName(), getChildrenName());
        resolveChildren(override, nameMap, Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS, Constants.JSON_FEATURE_FIELD_BASED_RULES_ORDERING);
        resolveChildren(override, nameMap, Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
    }

    protected void resolveChildren(JSONObject override, Map<String, JSONObject> nameMap, String nameKey, String childKey) throws Exception {
        JSONArray names = override.optJSONArray(nameKey);
        JSONArray source = override.optJSONArray(childKey);
        if (names == null || names.length() == 0) {
            return;
        }

        Map<String, JSONObject> overrideKids = new HashMap<>();
        if (source != null) {
            for (int i = 0; i < source.length(); ++i) {
                JSONObject child = source.getJSONObject(i);
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

    // find old nodes that are being replaced and mark them for deletion
    private void markForDeletion(JSONObject override, Map<String, JSONObject> nameMap) throws JSONException {
        if (getBranchStatus(override) == Feature.BranchStatus.CHECKED_OUT) {
            String key = getName(override);
            JSONObject original = nameMap.get(key);
            if (original != null) {
                setBranchStatus(original, Feature.BranchStatus.TEMPORARY); // mark the original node for deletion
            } else // this happens when an overridden DEVELOPMENT node is filtered out in a PRODUCTION master. Treat the overriding node as NEW
            {
                setBranchStatus(override, Feature.BranchStatus.NEW); // the override should contain all fields, not just a delta
            }
        }
        markChildren(override, nameMap, getChildrenName());
        markChildren(override, nameMap, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
    }

    private void markChildren(JSONObject override, Map<String, JSONObject> nameMap, String childKey) throws JSONException {
        JSONArray array = override.optJSONArray(childKey);
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                JSONObject feature = array.getJSONObject(i);
                markForDeletion(feature, nameMap);
            }
        }
    }

    private void removeResiduals(JSONObject item) throws JSONException {
        removeChildren(item, getChildrenName());
        removeChildren(item, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
    }

    private void removeChildren(JSONObject item, String childKey) throws JSONException {
        JSONArray array = item.optJSONArray(childKey);
        if (array == null || array.length() == 0) {
            return;
        }

        JSONArray newChildren = new JSONArray();
        for (int i = 0; i < array.length(); ++i) {
            JSONObject feature = array.getJSONObject(i);
            if (getBranchStatus(feature) != Feature.BranchStatus.TEMPORARY) {
                newChildren.put(feature);
                removeResiduals(feature);
            }
        }

        if (array.length() != newChildren.length()) {
            item.put(childKey, newChildren);
        }
    }

    // copy missing items from the original to the checkout
    private void mergeDelta(JSONObject override, Map<String, JSONObject> nameMap) throws JSONException {
        String nodeName = getName(override);
        JSONObject original = nameMap.get(nodeName);
        if (getBranchStatus(override) != Feature.BranchStatus.CHECKED_OUT || original == null) {
            return;
        }

        Iterator<?> overrideKeys = override.keys();
        Set<String> overrideKeysSet = new HashSet<>();
        while (overrideKeys.hasNext()) {
            String key = (String) overrideKeys.next();
            overrideKeysSet.add(key);
        }

        Iterator<?> keys = original.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (key.equals(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) || key.equals(getChildrenName())) {
                continue;
            }

            if (!overrideKeysSet.contains(key)) {
                Object obj = original.get(key);
                override.put(key, obj);
            }
        }
    }

    public Object cloneJson(Object obj, boolean deepClone) throws JSONException {
        if (obj instanceof JSONArray) {
            JSONArray in = (JSONArray) obj;
            JSONArray out = new JSONArray();//TODO verify ....new JSONArray(in.length());
            for (int i = 0; i < in.length(); ++i) {
                Object item = in.get(i);
                if (deepClone) {
                    item = cloneJson(item, true);
                }
                out.put(item);
            }
            return out;
        }
        if (obj instanceof JSONObject) {
            JSONObject in = (JSONObject) obj;
            JSONObject out = new JSONObject();
            Iterator<String> keys = in.keys();
            while (keys.hasNext()) {
                String key = keys.next();

                Object item = in.get(key);
                if (deepClone) {
                    item = cloneJson(item, true);
                }
                out.put(key, item);
                // Get key somehow? itr.getKey() ???
            }

            return out;
        }

        //the rest are immutable so no need to clone (null, Boolean, String, Integer, Double, Short)
        return obj;
    }


    @SuppressWarnings("serial")
    public static class MergeException extends Exception {
        MergeException(String error) {
            super(error);
        }
    }
}
