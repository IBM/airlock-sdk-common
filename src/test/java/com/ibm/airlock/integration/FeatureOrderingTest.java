package com.ibm.airlock.integration;


import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.BaseTestModel;
import com.ibm.airlock.common.model.Feature;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author Denis Voloshin
 */
@RunWith(Parameterized.class)
public class FeatureOrderingTest extends BaseTestModel {


    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "FeatureOrderingTest";
        return getConfigs();
    }


    public FeatureOrderingTest(String adminUrl ,String serverUrl, String productName, String version,String key) throws Exception{
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        m_ug.add("QA");
        testHelper.setup(m_appVersion,m_ug,null,null,false,true,false);
        testHelper.pull();
        testHelper.calcSync(null,null);
    }

    @Test
    public void testCalculatedOrder() throws JSONException {
        AirlockProductManager manager = testHelper.getManager();
        List<Feature> rootFeatures = manager.getFeaturesService().getRootFeatures();
        if (rootFeatures.isEmpty()) {
            return;
        }
        Assert.assertTrue(manager.getFeaturesService().getFeature("qa.P1").isOn());

        List<Feature> children = manager.getFeaturesService().getFeature("qa.P1").getChildren();
        JSONArray appliedOrderRules = manager.getFeaturesService().getFeature("qa.P1").getAnalyticsAppliedOrderRules();
        JSONArray orderedFeatures = manager.getFeaturesService().getFeature("qa.P1").getAnalyticsOrderedFeatures();

        // validate analytics report
        Assert.assertTrue(appliedOrderRules.toString().equals("[\"mxrule.mxRule2\",\"qaP1.rule1\"]"));
        Assert.assertTrue(orderedFeatures.toString().equals("[\"qa.f7\",\"qa.f5\",\"qa.f6\",\"qa.f4\",\"qa.f3\",\"qa.f2\",\"qa.f1\"]"));


        // validate total size
        Assert.assertTrue(children.size() == 7);

        Assert.assertTrue(children.get(0).getName().equals("qa.f7") && children.get(0).isOn()); //0.4
        Assert.assertTrue(children.get(1).getName().equals("qa.f5") && !children.get(1).isOn()); //0.4
        Assert.assertTrue(children.get(2).getName().equals("qa.f6") && !children.get(2).isOn()); //0.4
        Assert.assertTrue(children.get(3).getName().equals("qa.f4") && children.get(3).isOn()); //0.3
        Assert.assertTrue(children.get(4).getName().equals("qa.f3") && children.get(4).isOn()); //0.3
        Assert.assertTrue(children.get(5).getName().equals("qa.f2") && children.get(5).isOn()); //0.2
        Assert.assertTrue(children.get(6).getName().equals("qa.f1") && children.get(6).isOn()); //0.1
    }
}

