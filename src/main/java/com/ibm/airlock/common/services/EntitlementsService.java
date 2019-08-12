package com.ibm.airlock.common.services;

import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.model.Entitlement;
import com.ibm.airlock.common.model.PurchaseOption;
import com.ibm.airlock.common.util.Constants;
import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class EntitlementsService {

    @Inject
    InfraAirlockService infraAirlockService;

    public void init(ProductDiComponent productDiComponent) {
        productDiComponent.inject(this);
    }


    /**
     * Returns the purchased entitlements list by product ids.
     * if a product id is associated with more than one entitlement
     * all entitlements will be returned if an entitlement includes another entitlements (a bundle)
     * all entitlements are considered as purchased
     *
     * @param productIds the list of product ids an Entitlement is associated with
     * @return Returns the list of Entitlement object.
     */
    public Collection<Entitlement> getPurchasedEntitlements(Collection<String> productIds) {
        Collection<Entitlement> entitlements = getEntitlements();
        List<Entitlement> purchasedEntitlements = new ArrayList<>();
        for (Entitlement entitlement : entitlements) {
            isEntitlementPurchased(entitlement, productIds, purchasedEntitlements);
        }
        return purchasedEntitlements;
    }


    /**
     * Returns a cloned list of the entitlements.
     * This method is safe to traverse through its returned List
     *
     * @return A cloned list of the entitlements
     */
    public Collection<Entitlement> getEntitlements() {
        List<Entitlement> result = Collections.emptyList();
        JSONObject rootEntitlements = infraAirlockService.getSyncedEntitlements();
        if (rootEntitlements != null && rootEntitlements.length() > 0) {
            JSONArray entitlements = rootEntitlements.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
            if (entitlements != null && entitlements.length() > 0) {
                result = new ArrayList<>();
                for (int i = 0; i < entitlements.length(); i++) {
                    JSONObject entitlementObj = entitlements.getJSONObject(i);
                    result.add(new Entitlement(entitlementObj));
                }
            }
        }
        return result;
    }

    private void isEntitlementPurchased(Entitlement entitlement, Collection<String> productIds,
                                        Collection<Entitlement> purchasedEntitlements) {
        if (!entitlement.getEntitlementChildren().isEmpty()) {
            for (Entitlement child : entitlement.getEntitlementChildren()) {
                isEntitlementPurchased(child, productIds, purchasedEntitlements);
            }
        }
        for (PurchaseOption purchaseOption : entitlement.getPurchaseOptions()) {
            if (productIds.contains(purchaseOption.getProductId())) {
                purchasedEntitlements.add(entitlement);
                // add included entitlements
                for (String entitlementName : entitlement.getIncludedEntitlements()) {
                    purchasedEntitlements.add(getEntitlement(entitlementName));
                }
            }
        }
    }

    /**
     * Returns a cloned list of the entitlements.
     * This method is safe to traverse through its returned List.
     *
     * @return A cloned list of the entitlements.
     */
    private Map<String, Entitlement> getEntitlementsTree() {
        Map<String, Entitlement> result = new Hashtable();
        JSONObject rootEntitlements = infraAirlockService.getSyncedEntitlements();
        if (rootEntitlements != null && rootEntitlements.length() > 0) {
            JSONArray entitlements = rootEntitlements.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
            if (entitlements != null && entitlements.length() > 0) {
                for (int i = 0; i < entitlements.length(); i++) {
                    JSONObject entitlementObj = entitlements.getJSONObject(i);
                    Entitlement entitlement = new Entitlement(entitlementObj);
                    result.put(entitlement.getName(), entitlement);
                }
            }
        }
        return result;
    }

    /**
     * Returns the Entitlement object by its name.
     * If the Entitlement doesn't exist in the purchases set, getEntitlement returns a new Entitlement object
     * with the given name, isOn=false, and source=missing.
     *
     * @param entitlementName Entitlement name in the format namespace.name.
     * @return Returns the Entitlement object.
     */
    public Entitlement getEntitlement(String entitlementName) {
        Entitlement entitlement = getEntitlementsTree().get(entitlementName == null ? "" : entitlementName);
        return entitlement == null ? new Entitlement() : entitlement;
    }

    @TestOnly
    public void addPurchasedProductsId(String productId) {
        JSONArray productIdsArray = new JSONArray(infraAirlockService.getPersistenceHandler().
                read(Constants.PURCHASED_IDS_FOR_DEBUG, "[]"));

        if (productIdsArray.length() == 0) {
            productIdsArray.put(productId);
        } else {
            boolean found = false;
            for (int i = 0; i < productIdsArray.length(); i++) {
                if (productIdsArray.get(i).equals(productId)) {
                    found = true;
                }
            }
            if (!found) {
                productIdsArray.put(productId);
            }
        }
        infraAirlockService.getPersistenceHandler().write(Constants.PURCHASED_IDS_FOR_DEBUG, productIdsArray.toString());
    }

    @TestOnly
    public void removePurchasedProductId(String productId) {
        JSONArray productIdsArray = new JSONArray(infraAirlockService.getPersistenceHandler().
                read(Constants.PURCHASED_IDS_FOR_DEBUG, "[]"));
        JSONArray newProductIdsArray = new JSONArray();
        for (int i = 0; i < productIdsArray.length(); i++) {
            if (!productIdsArray.get(i).equals(productId)) {
                newProductIdsArray.put(productIdsArray.get(i));
            }
        }
        infraAirlockService.getPersistenceHandler().write(Constants.PURCHASED_IDS_FOR_DEBUG, newProductIdsArray.toString());
    }

    @TestOnly
    public void clearPurchasedProductId(String productId) {
        infraAirlockService.getPersistenceHandler().write(Constants.PURCHASED_IDS_FOR_DEBUG, new JSONArray().toString());
    }


}
