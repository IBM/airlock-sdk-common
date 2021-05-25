package com.ibm.airlock.common.test.functional;

import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.test.common.BaseTestModel;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static com.ibm.airlock.common.engine.Result.FEATURE_IS_PREMIUM_NOT_PURCHASED;
import static com.ibm.airlock.common.engine.Result.FEATURE_IS_PREMIUM_PURCHASED;
import static com.ibm.airlock.common.engine.Result.FEATURE_PREMIUM_RULE_OFF_NO_PURCHASED;
import static com.ibm.airlock.common.engine.Result.FEATURE_PREMIUM_RULE_OFF_PURCHASED;

/**
 * Created by Denis Voloshin on 31/01/2019.
 */
public class TraceEntitlementsTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "Q4prod";
    private static final String KEY = "";
    private static final String devBranchId = "e725e986-4a5c-403c-a120-e38babe8e01a";
    private static final String devBranchName = "Ent.QA.Branch";
    private static Collection<String> purchasedProductId = new ArrayList<>();


    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "TraceEntitlementsTest";
        return getConfigs();
    }


    public TraceEntitlementsTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        purchasedProductId.add("product.id.7");
        m_ug = new ArrayList<>();
        m_ug.add("QA");
        testHelper.customSetUp(m_version, m_ug, null, null, false, false, false);
    }


    @Test
    public void testParseDefaultEntitlementSectionTest() throws Exception {
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        // FEATURE_IS_PREMIUM_NOT_PURCHASED = "Feature is premium and not purchased";
        Feature feature = testHelper.getManager().getFeature("ns1.F1");
        Assert.assertFalse(feature.isOn());
        Assert.assertTrue(feature.isPremium());
        Assert.assertFalse(feature.isPurchased());
        Assert.assertEquals(FEATURE_IS_PREMIUM_NOT_PURCHASED, feature.getTraceInfo());

        // FEATURE_IS_PREMIUM_PURCHASED = "Feature is premium and purchased";
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        feature = testHelper.getManager().getFeature("ns1.F1");
        Assert.assertTrue(feature.isOn());
        Assert.assertTrue(feature.isPremium());
        Assert.assertTrue(feature.isPurchased());
        Assert.assertEquals(FEATURE_IS_PREMIUM_PURCHASED, feature.getTraceInfo());

        // FEATURE_PREMIUM_RULE_OFF_PURCHASED = "Feature premium rule is OFF and purchased";
        testHelper.calcWithProductIdsAndSync(null, purchasedProductId);
        feature = testHelper.getManager().getFeature("ns1.F21");
        Assert.assertTrue(feature.isOn());
        Assert.assertFalse(feature.isPremium());
        Assert.assertTrue(feature.isPurchased());
        Assert.assertEquals(FEATURE_PREMIUM_RULE_OFF_PURCHASED, feature.getTraceInfo());

        // FEATURE_PREMIUM_RULE_OFF_NO_PURCHASED = "Feature premium rule is OFF and not purchased";
        testHelper.calcWithProductIdsAndSync(null, null);
        feature = testHelper.getManager().getFeature("ns1.F21");
        Assert.assertTrue(feature.isOn());
        Assert.assertFalse(feature.isPremium());
        Assert.assertFalse(feature.isPurchased());
        Assert.assertEquals(FEATURE_PREMIUM_RULE_OFF_NO_PURCHASED, feature.getTraceInfo());
    }
}
