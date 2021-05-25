package com.ibm.airlock.common.test.regressions;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.test.common.BaseTestModel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Created by iditb on 27/12/17.
 */

public class BranchesDiffBugRegTest extends BaseTestModel {

    public BranchesDiffBugRegTest(String adminUrl, String serverUrl, String productName, String version, String key)
            throws IOException, AirlockInvalidFileException {
        super(adminUrl, serverUrl, productName, version, key);
    }

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "BranchesDiffBugRegTest";
        return getConfigs();
    }


    @Test
    public void verify() throws IOException, AirlockInvalidFileException, JSONException, AirlockNotInitializedException, InterruptedException {


        AirlockProductManager manager = testHelper.getManager();

        manager.setAllowExperimentEvaluation(true);
        JSONObject context1 = new JSONObject(testHelper.getDataFileContent("test_data/multi_branches/DevProdWithRulesTest/device_contexts/DeviceContextV1.json"));
        JSONObject context2 = new JSONObject(testHelper.getDataFileContent("test_data/multi_branches/DevProdWithRulesTest/device_contexts/DeviceContextV12.json"));

        if (context1.has("context")) {
            context1 = context1.getJSONObject("context");
        }

        final JSONObject deviceContext1 = context1;

        if (context2.has("context")) {
            context2 = context2.getJSONObject("context");
        }

        final JSONObject deviceContext2 = context2;

        final CountDownLatch latch = new CountDownLatch(1);

        manager.pullFeatures(new AirlockCallback() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.err.println("FAILURE " + e.getClass() + " : " + e.getMessage());
                latch.countDown();
            }

            @Override
            public void onSuccess(@NonNull String msg) {
                latch.countDown();
            }
        });
        latch.await();
        manager.calculateFeatures(null, deviceContext1);
        manager.syncFeatures();
        List<Feature> features = manager.getRootFeatures();
        // CacheManager.getInstance().getSyncFeatureList();
        boolean found1 = false;
        for (Feature f : features) {
            if (f.getName().equals("b1ns.FeatureInBranch1")) found1 = true;
        }

        Assert.assertTrue("Expected feature of Branch1 is missing.", found1);

        //Now switch branch
        final CountDownLatch latch2 = new CountDownLatch(1);
        manager.pullFeatures(new AirlockCallback() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.err.println("FAILURE " + e.getClass() + " : " + e.getMessage());
                latch2.countDown();
            }

            @Override
            public void onSuccess(@NonNull String msg) {
                latch2.countDown();
            }
        });
        latch2.await();
        manager.calculateFeatures(null, deviceContext2);
        manager.syncFeatures();

        features = manager.getRootFeatures();
        found1 = false;
        boolean found2 = false;
        for (Feature f : features) {
            if (f.getName().equals("b1ns.FeatureInBranch1")) found1 = true;
            if (f.getName().equals("b2ns.FeatureInBranch2")) found2 = true;
        }

        Assert.assertTrue("Expected feature of Branch2 is missing", found2);
        Assert.assertTrue("REGRESSION: Feature from branch1 should not appear in the root features list after branch switch", !found1);

        //Verify the feature is not in the synced list but not as a root feature
        Map<String, Feature> fs;
        Thread.sleep(15000);
        fs = testHelper.getFeatures();
        Set<String> keys = fs.keySet();
        found1 = false;
        for (String key : keys) {
            if (fs.get(key).getName().equals("b1ns.FeatureInBranch1")) found1 = true;
        }

        Assert.assertTrue("REGRESSION: Branch1 feature was found in the synced list after branch switch (but not as a root feature)", !found1);
    }
}
