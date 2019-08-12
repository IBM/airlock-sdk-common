package com.ibm.airlock.integration;

import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.BaseTestModel;

import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;


/**
 * @author Denis Voloshin
 */

public class MultiServerTest extends BaseTestModel {

    private final static String PRODUCT_NAME = "QA.Product";
    private String failMessage = null;
    private String successMessage = null;
    private static String VERSION = "7.10";

    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "MultiServerTest";
        return getConfigs();
    }

    public MultiServerTest(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        super(adminUrl, serverUrl, productName, version, key);
        testHelper.setup(m_appVersion, null, null, null, true, true, true);
    }

}
