package com.ibm.airlock.common.test.functional;

import com.ibm.airlock.common.data.Entitlement;
import com.ibm.airlock.common.data.PurchaseOption;
import com.ibm.airlock.common.test.common.BaseTestModel;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Denis Voloshin on 31/01/2019.
 */
public class PercentageEntitlementsTest extends BaseTestModel {

    private static Collection<String> purchasedProductId = new ArrayList<>();


    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "PercentageEntitlementsTest";
        return getConfigs();
    }


    public PercentageEntitlementsTest(String adminUrl, String serverUrl, String productName, String version, String key) throws Exception {
        super(adminUrl, serverUrl, productName, version, key);
        purchasedProductId.add("product.id.7");
        m_ug = new ArrayList<>();
        m_ug.add("QA");
        testHelper.customSetUp(m_version, m_ug, null, null, false, false, false);
    }


    @Test
    public void testPercentageEntitlementSectionTest() throws Exception {
        testHelper.pull();
        testHelper.calcSync(null, (JSONObject) null);
        Entitlement entitlement = testHelper.getManager().getEntitlement("airlockEntitlement.Ent1");
        Assert.assertTrue(entitlement.getPercentage() == 50);
        Collection<PurchaseOption> options = entitlement.getPurchaseOptions();
        for (PurchaseOption option : options) {
            if (entitlement.isOn()) {
                Assert.assertTrue(option.isOn());
                Assert.assertTrue(option.getPercentage() == 100);
            } else {
                Assert.assertTrue(!option.isOn());
                Assert.assertTrue(option.getPercentage() == 100);
            }
        }
    }
}
