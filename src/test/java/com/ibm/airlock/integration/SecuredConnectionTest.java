package com.ibm.airlock.integration;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.BaseTestModel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Denis Voloshin
 */

public class SecuredConnectionTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "QA.SecureConnection";

    public SecuredConnectionTest(String adminUrl, String serverUrl, String productName, String version) throws Exception {
        super(adminUrl, serverUrl, productName, version, "KRPD4SU1UQRCNRUR");
        testHelper.setup(m_appVersion, "KRPD4SU1UQRCNRUR", m_ug, null, null, false, true, false);
    }


    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "SecuredConnectionTest";
        return getConfigs();
    }


    @Test
    public void pullSecuredProductTest() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        testHelper.recreateClient("1.0");
        testHelper.pull();
        testHelper.calcSync(null, null);
        Thread.sleep(500);
        Feature feature = testHelper.getManager().getFeaturesService().getFeature("features.Feature1");
        Assert.assertEquals(feature.isOn(), true);
    }

    @Test
    public void getUserGroups() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        testHelper.recreateClient("1.0");
        final CountDownLatch waiter = new CountDownLatch(1);
        testHelper.getManager().getUserGroupsService().getServerUserGroups(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                waiter.countDown();
                Assert.fail(e.getMessage());
            }

            @Override
            public void onSuccess(String userGroupsJson) {
                waiter.countDown();
                JSONArray userGroups = new JSONArray(userGroupsJson);
                Assert.assertNotNull(userGroups);
                Assert.assertEquals(userGroups.toString(), "[\"dev\",\"qa\"]");
            }
        });

        if (!waiter.await(10, TimeUnit.SECONDS)) {
            Assert.fail("Time-out happened");
        }
    }

    @Test
    public void getBranches() throws IOException, AirlockInvalidFileException, InterruptedException {
        testHelper.recreateClient("1.0");
        final CountDownLatch waiter = new CountDownLatch(1);
        testHelper.getManager().getBranchesService().getProductBranches(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                Assert.fail(e.getMessage());
                waiter.countDown();
            }

            @Override
            public void onSuccess(String userGroupsJson) {
                waiter.countDown();
                JSONArray userGroups = new JSONArray(userGroupsJson);
                Assert.assertNotNull(userGroups);
                Assert.assertEquals(userGroups.length(), 1);
                JSONObject branch = userGroups.getJSONObject(0);
                Assert.assertEquals(branch.optString("name"), "Develop");
            }
        });

        if (!waiter.await(10, TimeUnit.SECONDS)) {
            Assert.fail("Time-out happened");
        }
    }
}
