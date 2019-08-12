package com.ibm.airlock;

import com.ibm.airlock.common.server.TestAirlockConfigurationServer;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite;
import com.ibm.airlock.integration.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;

import java.io.IOException;


/**
 * @author Denis Voloshin
 */
@RunWith(ParameterizedSuite.class)
@Suite.SuiteClasses({
        //InMemoryCacheTest.class,
        //EncryptedServerMinMaxVersionTest.class,
        //SecuredConnectionTest.class,
        //BasicEntitlementsTest.class,
        MultipleAirlockClientTest.class,
        BasicEntitlementsTest.class,
        MinMaxVersionTest.class,
        PercentageUpgradeRegTest.class,
        GoldsTest.class,
        FeatureOrderingTest.class,
        SetLocaleTest.class,
        ManagerBasicTest.class,
        StreamsDevTest.class,
        RePullProdFeaturesTest.class,
        DevProdSeparationTest.class,
        FeaturesListTreeTest.class,
        StreamsQATest.class,
        UserGroupsTest.class,
        AnalyticsTest.class,
        PercentageManagerTest.class,
        BranchesDiffBugRegTest.class
})
public class CommonTestsSuite {

    private static TestAirlockConfigurationServer testAirlockConfigurationServer;

    @Parameterized.Parameters(name = "Create test helper")
    public static Object[] params() {
        return new Object[][]{{new CommonSdkBaseTest()}};
    }

    @Parameterized.Parameter
    public AbstractBaseTest baseTest;

    @BeforeClass
    public static void startLocalAirlockConfigurationServer() {
        try {
            testAirlockConfigurationServer = new TestAirlockConfigurationServer();
            testAirlockConfigurationServer.start();
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public static void stopLocalAirlockConfigurationServer() {
        testAirlockConfigurationServer.stop();
    }
}

