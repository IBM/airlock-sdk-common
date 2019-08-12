package com.ibm.airlock;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterContext;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.util.TestConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Denis Voloshin
 */
@RunWith(Parameterized.class)
public abstract class BaseTestModel {

    public static String testClassName;

    protected static AbstractBaseTest testHelper;
    protected String m_adminUrl;
    protected String m_serverUrl;
    protected String m_productName;
    protected String m_appVersion;
    protected String m_key;
    protected ArrayList<String> m_ug;

    public BaseTestModel(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException {
        m_adminUrl = adminUrl;
        m_serverUrl = serverUrl;
        m_productName = productName;
        m_appVersion = version;
        m_key = key;
        testHelper.setup(m_adminUrl, m_serverUrl, m_productName, m_appVersion, m_key);
    }

    public BaseTestModel(String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException {
        m_serverUrl = serverUrl;
        m_productName = productName;
        m_appVersion = version;
        m_key = key;
        testHelper.setup(m_serverUrl, m_productName, m_appVersion, m_key);
    }

    public BaseTestModel(String serverUrl, String productName, String version) throws IOException, AirlockInvalidFileException {
        m_serverUrl = serverUrl;
        m_productName = productName;
        m_appVersion = version;
        testHelper.setup(m_serverUrl, m_productName, m_appVersion);
    }

    public static Collection<Object[]> getConfigs() throws IOException {

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
