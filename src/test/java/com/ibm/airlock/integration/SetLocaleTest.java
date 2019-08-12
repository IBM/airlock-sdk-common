package com.ibm.airlock.integration;

import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.BaseTestModel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;


/**
 * @author Denis Voloshin
 */

public class SetLocaleTest extends BaseTestModel {

    public static final String PRODUCT_NAME = "QA.Product";

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "SetLocaleTest";
        return getConfigs();
    }


    public SetLocaleTest(String adminUrl,String serverUrl, String productName, String version,String key) throws Exception {
        super(adminUrl,serverUrl, productName, version,key);
        m_ug = new ArrayList<>();
        testHelper.setup(m_appVersion, m_ug, null, null, false, false, false);
    }


    @Before
    public void init() {
        AirlockProductManager manager = testHelper.getManager();
        try {
            //manager.initSDK(testHelper.getContext(), AbstractBaseTest.slurp(testHelper.getDefaultFile(), 1024), "1.1");
            manager.reset();
           // manager.initSDK(testHelper.getContext(), AbstractBaseTest.slurp(testHelper.getDefaultFile(), 1024), "1.1");
        } catch (Exception e) {
            Assert.fail("An exception was thrown when trying to init sdk. Message: " + e.getMessage());
        }
    }

    @Test
    public void beforeSetLocaleTest() {

    }

    @Test
    public void setLocaleTest() {
        testHelper.setLocale(new Locale("fr_FR"));
        testHelper.getManager().getStringsService().resetLocale();
    }
}

