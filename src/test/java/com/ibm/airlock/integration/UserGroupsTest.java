package com.ibm.airlock.integration;

import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.BaseTestModel;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Denis Voloshin
 */

public class UserGroupsTest extends BaseTestModel {


    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "UserGroupsTest";
        return getConfigs();
    }


    public UserGroupsTest(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException {
        super(adminUrl, serverUrl, productName, version, key);
    }

    public List<String> m_userGroupList = null;


    private void verifyNotOnAndServer() {
        Map<String, Feature> features = testHelper.getFeatures();
        //Verify that all of the development features are off (and from server and have a reevant trace info)
        //Feature is in development and the device is not associated with any of the feature's internal user groups
        Feature f = features.get("ns.parent");
        assertTrue("ns.parent feature should return from the server", f.getSource().equals(Feature.Source.SERVER));
        assertTrue("ns.parent feature should be off", !f.isOn());
        assertTrue("ns.parent feature should contain a trace info indicating the device is not associated with the relevant user group", f.getTraceInfo().contains("device is not associated"));
        f = features.get("ns.f1");
        assertTrue("ns.f1 feature should return from the server", f.getSource().equals(Feature.Source.SERVER));
        assertTrue("ns.f1 feature should be off", !f.isOn());
        assertTrue("ns.f1 feature should contain a trace info indicating the device is not associated with the relevant user group", f.getTraceInfo().contains("device is not associated"));
        //Verify that production features are in
        f = features.get("ns.onfeature");
        assertTrue("ns.onFeature feature should return from the server but is missing in the synced list", f != null);
        assertTrue("ns.onFeature feature should return from the server", f.getSource().equals(Feature.Source.SERVER));
        assertTrue("ns.onFeature feature should be on", f.isOn());
    }


    private void verifyDoNotExist() {
        Map<String, Feature> features = testHelper.getFeatures();
        Feature f = features.get("ns.parent");
        assertTrue(f == null);
        f = features.get("ns.f1");
        assertTrue(f == null);
        //Verify that production features are in
        f = features.get("ns.onfeature");
        assertTrue("ns.onFeature feature should return from the server but is missing in the synced list", f != null);
        assertTrue("ns.onFeature feature should return from the server", f.getSource().equals(Feature.Source.SERVER));
        assertTrue("ns.onFeature feature should be on", f.isOn());
    }


    private void verifyOnAndServer() {
        Map<String, Feature> features = testHelper.getFeatures();
        //Verify that all of the development features are on (and from server and have a relevant trace info)
        //Feature is in development and the device is not associated with any of the feature's internal user groups
        Feature f = features.get("ns.parent");
        assertTrue("ns.parent feature should return from the server", f.getSource().equals(Feature.Source.SERVER));
        assertTrue("ns.parent feature should be on", f.isOn());
        assertTrue("ns.parent feature should not contain a trace info indicating the device is not associated with the relevant user group", !f.getTraceInfo().contains("device is not associated"));
        f = features.get("ns.f1");
        assertTrue("ns.f1 feature should return from the server", f.getSource().equals(Feature.Source.SERVER));
        assertTrue("ns.f1 feature should be on", f.isOn());
        assertTrue("ns.f1 feature should not contain a trace info indicating the device is not associated with the relevant user group", !f.getTraceInfo().contains("device is not associated"));
        //Verify that production features are in
        f = features.get("ns.onfeature");
        assertTrue("ns.onFeature feature should return from the server but is missing in the synced list", f != null);
        assertTrue("ns.onFeature feature should return from the server", f.getSource().equals(Feature.Source.SERVER));
        assertTrue("ns.onFeature feature should be on", f.isOn());
    }


    @Test
    public void featureNotInUGTest() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {

        //Add irrelevant user group
        ArrayList<String> ug = new ArrayList<String>();
        ug.add("NONO");
        testHelper.setup(m_appVersion, ug, null, null, false, true, false);

        testHelper.pull();
        testHelper.calcSync(null, null);
        verifyNotOnAndServer();
    }

    @Test
    public void nullUserGroupTest() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        ArrayList<String> ug = new ArrayList<String>();
        ug.add("QA");
        testHelper.setup(m_appVersion, ug, null, null, false, true, false);
        testHelper.pull();
        testHelper.calcSync(null, null);
        verifyOnAndServer();
        //setting the groups to null on the customSetup do not affect the user groups....
        testHelper.setup(m_appVersion, null, null, null, false, false, false);
        testHelper.pull();
        testHelper.calcSync(null, null);
        verifyOnAndServer();
    }

    @Test
    public void emptyUserGroupTest() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        ArrayList<String> ug = new ArrayList<String>();
        ug.add("QA");
        testHelper.setup(m_appVersion, ug, null, null, false, true, false);
        testHelper.pull();
        testHelper.calcSync(null, null);
        verifyOnAndServer();
        ug = new ArrayList<>();
        testHelper.setup(m_appVersion, ug, null, null, false, false, false);
        testHelper.pull();
        testHelper.calcSync(null, null);
        verifyDoNotExist();
    }

}
