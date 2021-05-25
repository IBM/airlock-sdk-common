package com.ibm.airlock.common.test.functional;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.test.common.BaseTestModel;
import com.ibm.airlock.common.util.Constants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


/**
 * Created by iditb on 26/11/17.
 */


public class FeaturesListTreeTest extends BaseTestModel {

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "FeaturesListTreeTest";
        return getConfigs();
    }

    public FeaturesListTreeTest(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException {
        super(adminUrl, serverUrl, productName, version, key);
        m_ug = new ArrayList<>();
        m_ug.add("QA");
        testHelper.customSetUp(m_version, m_ug, null, null, true, true, true);
    }

    private void verification7_8_version() {
        Map<String, Feature> features = testHelper.getFeatures();
        Assert.assertTrue("D feature is expected in the tree", features.get("ns.d") != null);
        Assert.assertTrue("D feature source should not be MISSING", !features.get("ns.d").getSource().equals(Feature.Source.MISSING));
        Assert.assertTrue("f1 feature is expected in the tree", features.get("ns.f1") != null);
        Assert.assertTrue("f1 feature source should not be MISSING", !features.get("ns.f1").getSource().equals(Feature.Source.MISSING));
        Assert.assertTrue("f2 feature is expected in the tree", features.get("ns.f2") != null);
        Assert.assertTrue("f2 feature source should not be MISSING", !features.get("ns.f2").getSource().equals(Feature.Source.MISSING));
        Assert.assertTrue("f3 feature is expected in the tree", features.get("f2.f3") != null);
        Assert.assertTrue("f3 feature source should not be MISSING", !features.get("f2.f3").getSource().equals(Feature.Source.MISSING));
        Assert.assertTrue("f4 feature is expected in the tree", features.get("f2.f4") != null);
        Assert.assertTrue("f4 feature source should not be MISSING", !features.get("f2.f4").getSource().equals(Feature.Source.MISSING));
        Assert.assertTrue("f5 feature is expected in the tree", features.get("f4.f5") != null);
        Assert.assertTrue("f5 feature source should not be MISSING", !features.get("f4.f5").getSource().equals(Feature.Source.MISSING));
    }

    private void verification7_10_version() {
        Map<String, Feature> features = testHelper.getFeatures();
        Assert.assertTrue("f1 feature is expected in the tree", features.get("ns.f1") != null);
        Assert.assertTrue("f1 feature source should not be MISSING", !features.get("ns.f1").getSource().equals(Feature.Source.MISSING));
        Assert.assertTrue("f2 feature is expected in the tree", features.get("ns.f2") != null);
        Assert.assertTrue("f2 feature source should not be MISSING", !features.get("ns.f2").getSource().equals(Feature.Source.MISSING));
        Assert.assertTrue("f3 feature is expected in the tree", features.get("f2.f3") != null);
        Assert.assertTrue("f3 feature source should not be MISSING", !features.get("f2.f3").getSource().equals(Feature.Source.MISSING));
        Assert.assertTrue("f4 feature is expected in the tree", features.get("f2.f4") != null);
        Assert.assertTrue("f4 feature source should not be MISSING", !features.get("f2.f4").getSource().equals(Feature.Source.MISSING));
        Assert.assertTrue("f5 feature is expected in the tree", features.get("f4.f5") != null);
        Assert.assertTrue("f5 feature source should not be MISSING", !features.get("f4.f5").getSource().equals(Feature.Source.MISSING));
    }

    @Test
    public void afterInitTest() throws IOException, AirlockInvalidFileException {
        verification7_8_version();
    }

    @Test
    public void getFromServerNoDFeature() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        m_version = "7.10";
        testHelper.setM_version(m_version);
        testHelper.customSetUp(m_version, m_ug, null, null, true, true, false);
        //1. tree verification
        verification7_10_version();
        //2. get all features from server

        //  testHelper.customSetUp(m_version,);
        testHelper.pull();
        testHelper.calcSync(null, null);
        //Get all features
        Map<String, Feature> features = testHelper.getFeatures();
        //1. Verify the tree
        //1.1 f1 should be ON and it's parent is root
        Assert.assertTrue("NULL was returned when trying to get f1", features.get("ns.f1") != null);
        Assert.assertTrue("Source = server is expected for f1", features.get("ns.f1").getSource().equals(Feature.Source.SERVER));
        Feature f1_p = features.get("ns.f1").getParent();
        Assert.assertTrue("f1 should not have a null parent", f1_p != null);
        Assert.assertTrue("Parent = ROOT is expected for f1", f1_p.getName().equals("ROOT"));
        Assert.assertTrue("f1 feature should be ON", features.get("ns.f1").isOn());
        //1.2 f2 should be ON and it's parent is root
        Assert.assertTrue("NULL was returned when trying to get f2", features.get("ns.f2") != null);
        Assert.assertTrue("Source = server is expected for f2", features.get("ns.f2").getSource().equals(Feature.Source.SERVER));
        Feature f2_p = features.get("ns.f2").getParent();
        Assert.assertTrue("f2 should not have a null parent", f2_p != null);
        Assert.assertTrue("Parent = ROOT is expected for f2", f2_p.getName().equals("ROOT"));
        Assert.assertTrue("f2 feature should be ON", features.get("ns.f2").isOn());
        //1.3 f3 should be ON and it's parent is f2
        Assert.assertTrue("NULL was returned when trying to get f3", features.get("f2.f3") != null);
        Assert.assertTrue("Source = server is expected for f3", features.get("f2.f3").getSource().equals(Feature.Source.SERVER));
        Feature f3_p = features.get("f2.f3").getParent();
        Assert.assertTrue("f3 should not have a null parent", f3_p != null);
        Assert.assertTrue("Parent = f2 is expected for f3", f3_p.getName().equalsIgnoreCase("ns.f2"));
        Assert.assertTrue("f3 feature should be ON", features.get("f2.f3").isOn());
        //1.4 f4 should be OFF and it's parent is f2
        Assert.assertTrue("NULL was returned when trying to get f4", features.get("f2.f4") != null);
        Assert.assertTrue("Source = server is expected for f4", features.get("f2.f4").getSource().equals(Feature.Source.SERVER));
        Feature f4_p = features.get("f2.f4").getParent();
        Assert.assertTrue("f4 should not have a null parent", f4_p != null);
        Assert.assertTrue("Parent = f2 is expected for f4", f4_p.getName().equals("ns.f2"));
        Assert.assertTrue("f4 feature should be OFF", !features.get("f2.f4").isOn());
        //1.5 f5 should be OFF (because it's parent is OFF) and it's parent is f4
        Assert.assertTrue("NULL was returned when trying to get f5", features.get("f4.f5") != null);
        Assert.assertTrue("Source = server is expected for f5", features.get("f4.f5").getSource().equals(Feature.Source.SERVER));
        Feature f5_p = features.get("f4.f5").getParent();
        Assert.assertTrue("f5 should not have a null parent", f5_p != null);
        Assert.assertTrue("Parent = f4 is expected for f5", f5_p.getParent().getName().equals("ns.f2"));
        Assert.assertTrue("f5 feature should be OFF", !features.get("f4.f5").isOn());
    }

    @Test
    public void moveF5ToF3Parent() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.customSetUp(m_version, m_ug, null, null, true, true, false);
        verification7_8_version();
        getFromServerNoDFeature();
        //1. change the tree hierarchy (move f5 to have f3 as a parent)
        //Now f5 should be ON after calculate
        //String change = FileUtils.fileToString("C:\\develop\\git\\airlock-sdk-android\\sdk\\src\\androidTest\\assets\\test_data\\tree\\moveF5ToF3Parent.json", "UTF-8", false);
        String change = testHelper.getDataFileContent("test_data/tree/moveF5ToF3Parent_4.json");
        testHelper.sdkChange(Constants.SP_RAW_RULES, change, false);
        testHelper.calcSync(null, null);
        //2. verify the tree hierarchy
        //1.1 f1 should be ON and it's parent is root
        Map<String, Feature> features = testHelper.getFeatures();
        Assert.assertTrue("NULL was returned when trying to get f1", features.get("ns.f1") != null);
        Assert.assertTrue("Source = server is expected for f1", features.get("ns.f1").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("Parent = ROOT is expected for f1", features.get("ns.f1").getParent().getName().equals("ROOT"));
        Assert.assertTrue("f1 feature should be ON", features.get("ns.f1").isOn());
        //1.2 f2 should be ON and it's parent is root
        Assert.assertTrue("NULL was returned when trying to get f2", features.get("ns.f2") != null);
        Assert.assertTrue("Source = server is expected for f2", features.get("ns.f2").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("Parent = ROOT is expected for f2", features.get("ns.f2").getParent().getName().equals("ROOT"));
        Assert.assertTrue("f2 feature should be ON", features.get("ns.f2").isOn());
        //1.3 f3 should be ON and it's parent is f2
        Assert.assertTrue("NULL was returned when trying to get f3", features.get("f2.f3") != null);
        Assert.assertTrue("Source = server is expected for f3", features.get("f2.f3").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("Parent = f2 is expected for f3", features.get("f2.f3").getParent().getName().equalsIgnoreCase("ns.f2"));
        Assert.assertTrue("f3 feature should be ON", features.get("f2.f3").isOn());
        //1.4 f4 should be OFF and it's parent is f2
        Assert.assertTrue("NULL was returned when trying to get f4", features.get("f2.f4") != null);
        Assert.assertTrue("Source = server is expected for f4", features.get("f2.f4").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("Parent = f2 is expected for f4", features.get("f2.f4").getParent().getName().equals("ns.f2"));
        Assert.assertTrue("f4 feature should be OFF", !features.get("f2.f4").isOn());
        //1.5 f5 should be OFF (because it's parent is OFF) and it's parent is f4
        Assert.assertTrue("NULL was returned when trying to get f5", features.get("f4.f5") != null);
        Assert.assertTrue("Source = server is expected for f5", features.get("f4.f5").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("Parent = f3 is expected for f5", features.get("f4.f5").getParent().getName().equals("f2.f3"));
        Assert.assertTrue("f5 feature should be ON", features.get("f4.f5").isOn());
    }

    @Test
    public void getFromServerDeleteF5() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        testHelper.customSetUp(m_version, m_ug, null, null, true, true, false);
        verification7_8_version();
        getFromServerNoDFeature();
        //1. change the tree hierarchy (remove f5)
        //Now f5 should be source = cache
        String change = testHelper.getDataFileContent("test_data/tree/removeF5_4.json");
        testHelper.sdkChange(Constants.SP_RAW_RULES, change, false);
        testHelper.calcSync(null, null);
        //2. verify the tree hierarchy
        //1.1 f1 should be ON and it's parent is root
        Map<String, Feature> features = testHelper.getFeatures();
        Assert.assertTrue("NULL was returned when trying to get f1", features.get("ns.f1") != null);
        Assert.assertTrue("Source = server is expected for f1", features.get("ns.f1").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("Parent = ROOT is expected for f1", features.get("ns.f1").getParent().getName().equals("ROOT"));
        Assert.assertTrue("f1 feature should be ON", features.get("ns.f1").isOn());
        //1.2 f2 should be ON and it's parent is root
        Assert.assertTrue("NULL was returned when trying to get f2", features.get("ns.f2") != null);
        Assert.assertTrue("Source = server is expected for f2", features.get("ns.f2").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("Parent = ROOT is expected for f2", features.get("ns.f2").getParent().getName().equals("ROOT"));
        Assert.assertTrue("f2 feature should be ON", features.get("ns.f2").isOn());
        //1.3 f3 should be ON and it's parent is f2
        Assert.assertTrue("NULL was returned when trying to get f3", features.get("f2.f3") != null);
        Assert.assertTrue("Source = server is expected for f3", features.get("f2.f3").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("Parent = f2 is expected for f3", features.get("f2.f3").getParent().getName().equalsIgnoreCase("ns.f2"));
        Assert.assertTrue("f3 feature should be ON", features.get("f2.f3").isOn());
        //1.4 f4 should be OFF and it's parent is f2
        Assert.assertTrue("NULL was returned when trying to get f4", features.get("f2.f4") != null);
        Assert.assertTrue("Source = server is expected for f4", features.get("f2.f4").getSource().equals(Feature.Source.SERVER));
        Assert.assertTrue("Parent = f2 is expected for f4", features.get("f2.f4").getParent().getName().equals("ns.f2"));
        Assert.assertTrue("f4 feature should be OFF", !features.get("f2.f4").isOn());
        //1.5 f5 should be OFF and it's source is cache
        Assert.assertTrue("NULL was returned when trying to get f5", features.get("f4.f5") != null);
        Assert.assertTrue("Source = cache is expected for f5", features.get("f4.f5").getSource().equals(Feature.Source.CACHE));
        Assert.assertTrue("Parent = f4 is expected for f5", features.get("f4.f5").getParent().getName().equals("f2.f4"));
        Assert.assertTrue("f5 feature should be OFF", !features.get("f4.f5").isOn());
    }
}
