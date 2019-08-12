package com.ibm.airlock.integration;

import com.ibm.airlock.BaseTestModel;
import com.ibm.airlock.common.model.Entitlement;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.common.model.PurchaseOption;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Denis Voloshin
 */
public class BasicEntitlementsTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "Q4prod";
    private static final String devBranchId = "e725e986-4a5c-403c-a120-e38babe8e01a";
    private static final String devBranchName = "Ent.QA.Branch";
    private static Collection<String> purchasedProductId = new ArrayList<>();


    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "BasicEntitlementsTest";
        return getConfigs();
    }

    public BasicEntitlementsTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        purchasedProductId.clear();
        purchasedProductId.add("product.id.7");
        m_ug = new ArrayList<>();
        m_ug.add("QA");
        testHelper.setup(m_appVersion, m_ug, null, null, false, false, false);
    }

    @Test
    public void testParseDefaultEntitlementSectionTest() throws Exception {
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("purchase.P1");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(entitlement.getSource() == Feature.Source.DEFAULT);
        Assert.assertTrue(entitlement.getPercentage() == 100);
        Assert.assertTrue(entitlement.getIncludedEntitlements().size() == 1);
        Assert.assertTrue(entitlement.getChildren().size() == 1);
        Assert.assertTrue(entitlement.getEntitlementChildren().toArray(new Entitlement[]{})[0].getPurchaseOptions().size() == 1);
        Assert.assertTrue(entitlement.getIncludedEntitlements().toArray()[0].equals("airlockEntitlement.P3"));
    }


    @Test
    public void testMergeEntitlementBranchWithVariantEntShouldBeOff() throws Exception {
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.Pr4Br1");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(!entitlement.isOn());

        testHelper.setDevelopmentBranch(devBranchId, devBranchName);
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("purchase.P1");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(!entitlement.isOn());

        entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.Pr4Br1");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(entitlement.getSource() == Feature.Source.MISSING);
    }

    @Test
    public void testEntitlementOptionsCalculationCondition() throws Exception {
        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        Entitlement p5 = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P5");
        Assert.assertFalse(p5.isOn());
        Collection<PurchaseOption> options = p5.getPurchaseOptions();
        for (PurchaseOption option : options) {
            Assert.assertFalse(option.isOn());
            if (option.getName().equals("airlockEntitlement.OP6")) {
                Assert.assertTrue(option.getProductId().equals("prod6"));
            } else {
                Assert.assertTrue(option.getProductId().equals("prod7"));
            }
        }


        Entitlement p4 = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P4");
        Assert.assertTrue(p4.isOn());
        options = p4.getPurchaseOptions();
        for (PurchaseOption option : options) {
            if (option.getName().equals("airlockEntitlement.OP4")) {
                Assert.assertFalse(option.isOn());
            } else {
                Assert.assertTrue(option.isOn());
            }
        }
    }

    @Test
    public void testMergeDevelopmentBranchNewOption() throws Exception {
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P3");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(entitlement.isOn());
        Assert.assertTrue(entitlement.getPercentage() == 100);

        testHelper.setDevelopmentBranch(devBranchId, devBranchName);
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P3");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(entitlement.getPercentage() == 100);
        Collection<PurchaseOption> purchaseOptions = entitlement.getPurchaseOptions();
        Assert.assertTrue(entitlement.getPurchaseOptions().size() == 2);

        // should be a new option airlockEntitlement.OP31 New
        for (PurchaseOption purchase : purchaseOptions) {
            if (purchase.getName().equals("airlockEntitlement.OP31")) {
                Assert.assertTrue(purchase.getProductId().equals("product.id.op31"));
            }
            if (purchase.getName().equals("airlockEntitlement.OP1")) {
                Assert.assertTrue(purchase.getProductId().equals("product.id4"));
            }
        }
    }


    @Test
    public void testMergeDevelopmentBranchNewOptionMovedToAnotherParent() throws Exception {
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P6");
        Assert.assertNotNull(entitlement);
        Assert.assertFalse(entitlement.isOn());
        Assert.assertTrue(entitlement.getSource() == Feature.Source.MISSING);

        testHelper.setDevelopmentBranch(devBranchId, devBranchName);
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P6");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(entitlement.getPercentage() == 100);
        Collection<PurchaseOption> purchaseOptions = entitlement.getPurchaseOptions();
        Assert.assertTrue(entitlement.getPurchaseOptions().size() == 0);

        entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P8");
        purchaseOptions = entitlement.getPurchaseOptions();
        Assert.assertTrue(entitlement.getPurchaseOptions().size() == 1);
        // should be a new option airlockEntitlement.OP31 New
        for (PurchaseOption purchase : purchaseOptions) {
            if (purchase.getName().equals("airlockEntitlement.P8 OP1")) {
                Assert.assertTrue(purchase.getProductId().equals("product.id.p8.op1"));
            }
        }
    }

    @Test
    public void testMergeDevelopmentBranchEntitlementPurchaseShouldbe50() throws Exception {
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("purchase.P2");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(entitlement.isOn());
        Assert.assertTrue(entitlement.getPercentage() == 100);

        testHelper.setDevelopmentBranch(devBranchId, devBranchName);
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("purchase.P2");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(entitlement.getPercentage() == 50);
    }


    @Test
    public void testMergeDevelopmentEntitlementBranchEntShouldBeOff() throws Exception {
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("purchase.P1");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(entitlement.isOn());

        testHelper.setDevelopmentBranch(devBranchId, devBranchName);
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("purchase.P1");
        Assert.assertNotNull(entitlement);
        Assert.assertTrue(!entitlement.isOn());
    }


    @Test
    public void testFeatureIsPremiumAndPurchased() throws Exception {
        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        Feature f1 = testHelper.getManager().getFeaturesService().getFeature("ns1.F1");
        Assert.assertTrue(f1.isOn());
        Assert.assertTrue(f1.isPremium());
        Assert.assertTrue(f1.isPurchased());
        Feature f2 = testHelper.getManager().getFeaturesService().getFeature("ns1.F2");
        Assert.assertTrue(!f2.isOn());
        Assert.assertTrue(f2.isPremium());
        Assert.assertFalse(f2.isPurchased());


        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, Collections.<String>emptyList());
        f1 = testHelper.getManager().getFeaturesService().getFeature("ns1.F1");
        Assert.assertTrue(!f1.isOn());
        Assert.assertTrue(f1.isPremium());
        Assert.assertFalse(f1.isPurchased());
        f2 = testHelper.getManager().getFeaturesService().getFeature("ns1.F2");
        Assert.assertTrue(!f2.isOn());
        Assert.assertTrue(f2.isPremium());
        Assert.assertFalse(f2.isPurchased());
    }


    @Test
    public void testPremiumFeatureInMutexShouldCountOnIsPurchasedCondition() throws Exception {
        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        Feature f3 = testHelper.getManager().getFeaturesService().getFeature("ns3.F3");
        Assert.assertTrue(f3.getChildren().size() == 3);
        List<Feature> children = f3.getChildren();
        for (Feature child : children) {
            if (child.getName().equals("ns1.F31")) {
                Assert.assertFalse(child.isOn());
                Assert.assertTrue(child.isPremium());
                Assert.assertFalse(child.isPurchased());
            }
        }
    }

    @Test
    public void testEntitlementPurchaseAreOFFOrDisabledShouldHaveAnyConfig() throws Exception {
        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P10");
        Assert.assertTrue(entitlement.getPurchaseOptions().size() == 2);
        Collection<PurchaseOption> purchaseOptions = entitlement.getPurchaseOptions();
        for (PurchaseOption purchaseOption : purchaseOptions) {
            Assert.assertFalse(purchaseOption.isOn());
            Assert.assertTrue(purchaseOption.getConfiguration() == null ||
                    purchaseOption.getConfiguration().toString().equals("{}"));
            Assert.assertTrue(purchaseOption.getProductId() != null);
        }
    }

    @Test
    public void testGetPurchasedEntitlementsListShouldReturnListOfPurchasedEntitlements() throws Exception {
        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        Collection<Entitlement> entitlements = testHelper.getManager().getEntitlementsService().getPurchasedEntitlements(purchasedProductId);
        Assert.assertTrue(entitlements.size() == 2);
        Assert.assertTrue(entitlements.toArray(new Entitlement[]{})[0].getName().equals("purchase.P1"));
        Assert.assertTrue(entitlements.toArray(new Entitlement[]{})[1].getName().equals("airlockEntitlement.P3"));
    }

    @Test
    public void testEntitlementBranchInMXShouldHaveOneElementOn() throws Exception {
        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P11");
        Assert.assertTrue(entitlement.getEntitlementChildren().size() == 3);
        for (Entitlement child : entitlement.getEntitlementChildren()) {
            if (child.getName().equals("airlockEntitlement.P111")) {
                Assert.assertTrue(child.isOn());
            } else {
                Assert.assertFalse(child.isOn());
            }
        }

        testHelper.setDevelopmentBranch(devBranchId, devBranchName);
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);

        entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P11");
        Assert.assertTrue(entitlement.getEntitlementChildren().size() == 3);
        for (Entitlement child : entitlement.getEntitlementChildren()) {
            if (child.getName().equals("airlockEntitlement.P113")) {
                Assert.assertFalse(child.isOn());
            } else {
                Assert.assertTrue(child.isOn());
            }
        }
    }

    @Test
    public void testEntitlementConfigurationShouldBeNotEmpty() throws Exception {
        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P11");
        Assert.assertNotNull(entitlement.getConfiguration());
        Assert.assertFalse(entitlement.getConfiguration().toString().equals("{}"));
        Assert.assertTrue(entitlement.getConfiguration().optString("description").equals("Ads Free"));

        testHelper.setDevelopmentBranch(devBranchId, devBranchName);
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);

        entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P11");
        Assert.assertNotNull(entitlement.getConfiguration());
        Assert.assertFalse(entitlement.getConfiguration().toString().equals("{}"));
        Assert.assertTrue(entitlement.getConfiguration().optString("description").equals("Ads Free2"));
    }

    @Test
    public void testPurchaseOptionConfigurationShouldBeNotEmpty() throws Exception {
        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P11");
        PurchaseOption purchaseOption = entitlement.getPurchaseOptions().toArray(new PurchaseOption[]{})[0];
        Assert.assertNotNull(purchaseOption.getConfiguration());
        Assert.assertTrue(purchaseOption.getConfiguration().has("period"));
        Assert.assertTrue(purchaseOption.getConfiguration().opt("period").equals("year"));
    }

    @Test
    public void testEntitlementsAnalyticsReport() throws Exception {
        testHelper.pull();
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        Entitlement entitlement = testHelper.getManager().getEntitlementsService().getEntitlement("airlockEntitlement.P12");
        PurchaseOption purchaseOption = (PurchaseOption) entitlement.getPurchaseOptions().toArray()[0];
        Assert.assertTrue(entitlement.getAnalyticsAppliedRules().get(0).equals("airlockEntitlementP12forAnalytics.Basic"));
        Assert.assertTrue(entitlement.getAttributesForAnalytics().get(0).equals("attr1"));
        Assert.assertTrue(purchaseOption.getAnalyticsAppliedRules().get(0).equals("airlockEntitlementPurchaseOptionforAnalytisc.Basic"));
        Assert.assertTrue(purchaseOption.getAttributesForAnalytics().get(0).equals("attr2"));
    }
}
