package com.ibm.airlock.common.test.functional;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.test.common.BaseTestModel;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;


/**
 * Created by Denis Voloshin on 25/12/2017.
 */

public class RePullProdFeaturesTest extends BaseTestModel {
    private final static String PRODUCT_NAME = "QA.Product";
    private final static String VERSION = "7.10";

    private String[] prods = {"no.cache.f","no.cache.f2","f7","f8","f9","f10","override.def.val","onFeature"};
    private String[] devs = {"parent","f1","f2","TestRandomNumbers"};

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "RePullProdFeaturesTest";
        return getConfigs();
    }

    public RePullProdFeaturesTest(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        super(adminUrl,serverUrl,productName,version,key);
        testHelper.customSetUp(m_version, null, null, null, true, true, true);
    }


    private void verifyAllProdsReturnedFromServer(){
        for (int i=0;i<prods.length;i++)
            Assert.assertTrue(prods[i]+" feature source is not SERVER",testHelper.getManager().getFeature("ns."+prods[i]).getSource().equals(Feature.Source.SERVER));
    }

    private void verifyAllDevsNotReturnedFromServer(){
        for (int i=0;i<devs.length;i++)
            Assert.assertTrue(devs[i]+" feature source is SERVER",!testHelper.getManager().getFeature("ns."+devs[i]).getSource().equals(Feature.Source.SERVER));
    }

    private void verifyAllDevsMissing(){
        for (int i=0;i<devs.length;i++)
            Assert.assertTrue(devs[i]+" feature source is SERVER", testHelper.getManager().getFeature("ns."+devs[i]).getSource().equals(Feature.Source.MISSING));
    }

    private void verifyAllDevsReturnedFromServer(){
        for (int i=0;i<devs.length;i++)
            Assert.assertTrue(devs[i]+" feature source is SERVER",testHelper.getManager().getFeature("ns."+devs[i]).getSource().equals(Feature.Source.SERVER));
    }

    private void pullCalcSync() throws AirlockNotInitializedException, InterruptedException {
        testHelper.pull();
        testHelper.calcSync(null,null);
    }

    //@Test
    public void rePullTest() throws AirlockNotInitializedException, InterruptedException {
        Date before = testHelper.getManager().getLastPullTime();
        pullCalcSync();
        Date after = testHelper.getManager().getLastPullTime();
        Assert.assertTrue(after.after(before));
        //Verify that only production features returned from the server
        verifyAllProdsReturnedFromServer();
        verifyAllDevsNotReturnedFromServer();
        pullCalcSync();
        Assert.assertTrue(testHelper.getManager().getLastPullTime().after(after));
        //Verify that only production features returned from the server
        verifyAllProdsReturnedFromServer();
        verifyAllDevsNotReturnedFromServer();
    }



    @Test
    public void rePullProdAfterDevTest() throws AirlockNotInitializedException, InterruptedException, IOException, AirlockInvalidFileException {
        ArrayList<String> groups = new ArrayList<>();
        groups.add("QA");
        testHelper.customSetUp(m_version, groups, null, null, true, true, true);

        Date before = testHelper.getManager().getLastPullTime();
        pullCalcSync();
        Date after = testHelper.getManager().getLastPullTime();
        Assert.assertTrue(after.after(before));
        //Verify that only production features returned from the server
        verifyAllProdsReturnedFromServer();
        verifyAllDevsReturnedFromServer();
        groups.clear();
        testHelper.getManager().setDeviceUserGroups(groups);
        pullCalcSync();
        Assert.assertTrue(testHelper.getManager().getLastPullTime().after(after));
        //Verify that only production features returned from the server
        verifyAllProdsReturnedFromServer();
        verifyAllDevsMissing();
    }
}
