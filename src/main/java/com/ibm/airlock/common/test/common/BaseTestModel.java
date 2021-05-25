package com.ibm.airlock.common.test.common;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterContext;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.test.AbstractBaseTest;
import com.ibm.airlock.common.test.util.TestConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Created by Denis Voloshin on 07/12/2017.
 */
@RunWith(Parameterized.class)
public abstract class BaseTestModel {

    public static String testClassName;

    protected static AbstractBaseTest testHelper;
    protected String m_adminUrl;
    protected String m_serverUrl;
    protected String m_productName;
    protected String m_version;
    protected String m_key;
    protected ArrayList<String> m_ug;

    public BaseTestModel(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException {
        m_adminUrl = adminUrl;
        m_serverUrl = serverUrl;
        m_productName = productName;
        m_version = version;
        m_key = key;
        testHelper.setUp(m_adminUrl, m_serverUrl, m_productName, m_version, m_key);
    }

    public BaseTestModel(String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException {
        m_serverUrl = serverUrl;
        m_productName = productName;
        m_version = version;
        m_key = key;
        testHelper.setUp(m_serverUrl, m_productName, m_version, m_key);
    }

    public BaseTestModel(String serverUrl, String productName, String version) throws IOException, AirlockInvalidFileException {
        m_serverUrl = serverUrl;
        m_productName = productName;
        m_version = version;
        testHelper.setUp(m_serverUrl, m_productName, m_version);
    }

    public static Collection getConfigs() throws IOException {

        //Get relevant test helper
        Object[] param = ParameterContext.getParameter();
        testHelper = (AbstractBaseTest) param[0];
        //Get test configuration
        JSONObject testConfig = new JSONObject(testHelper.getDataFileContent(TestConstants.TEST_TO_SERVER_FILE_NAME));
        JSONArray configs = testConfig.getJSONObject(testClassName).getJSONArray(TestConstants.CONFIGS_ELEMENT);
        ArrayList<Object[]> params = new ArrayList<>();
        for (int i = 0; i < configs.length(); i++) {
            JSONArray jarr = configs.getJSONArray(i);
            Object[] paramsList = new Object[jarr.length()];
            for (int j = 0; j < jarr.length(); j++) {
                paramsList[j] = jarr.get(j);
            }
            params.add(paramsList);
        }
        return params;
    }
}
