package com.ibm.airlock.integration;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterContext;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.exceptions.AirlockNotInitializedException;
import com.ibm.airlock.common.model.Feature;
import com.ibm.airlock.AbstractBaseTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author Denis Voloshin
 */
@RunWith(Parameterized.class)
public class GoldsTest {

    private static AbstractBaseTest testHelper;

    private static final String dataPath = "test_data/golds_test/";

    private String m_analyticsGoldFileName = null;
    private JSONObject m_context;
    private String m_locale;
    private String m_minAppVersion;
    private String m_randomMap = null;
    private String m_stage ;
    private ArrayList<String> m_userGroups;
    private String m_testName;
    private String m_goldFileName;

    private String m_serverUrl;
    private String m_adminUrl;
    private String m_productName;


    public GoldsTest(String adminUrl, String serverUrl, String productName, JSONObject config) throws JSONException, IOException, AirlockInvalidFileException {

        m_adminUrl = adminUrl;
        m_serverUrl = serverUrl;
        m_productName = productName;

        //If no analytics just continue
        try {
            m_analyticsGoldFileName = config.getString("analytics");
        } catch (JSONException e) {
        }

        m_context = new JSONObject(testHelper.getDataFileContent(dataPath + productName + "/" + config.getString("context")));
        if (m_context.has("context")) {
            m_context = m_context.getJSONObject("context");
        }

        //If no locale then English is the default
        try {
            m_locale = config.getString("locale");
        } catch (JSONException e) {
            m_locale = "en";
        }

        m_minAppVersion = config.getString("minAppVer");

        m_goldFileName = config.getString("output");
        // m_gold = dataPathInAssets + productName + "/" + m_goldFileName;

        String randomMapFileName = config.getString("randomMap");
        if (randomMapFileName != null) {
            if (!randomMapFileName.isEmpty()) {
                //        m_randomMap = AndroidTestUtils.readFromAssets(dataPathInAssets + productName + "/" + randomMapFileName);
            }
        }

        m_stage = config.getString("stage");

        if (m_stage != null) {
            if (m_stage.toLowerCase().contains("prod")) {
                m_userGroups = null;
            }
        }

        m_userGroups = new ArrayList(Arrays.asList(config.getString("usergroups").split(",")));

        m_testName = config.getString("testName");

        testHelper.setup(m_adminUrl, m_serverUrl, m_productName, m_minAppVersion,"");
    }

    @Parameterized.Parameters(name = "{index}:{1}")
    public static Collection configs() throws IOException, JSONException {

        Object[] param = ParameterContext.getParameter();
        testHelper = (AbstractBaseTest) param[0];

        ArrayList<Object[]> configs = new ArrayList<>();

        String configString = testHelper.getDataFileContent(dataPath + "config.json");
        JSONArray configJsonArray = new JSONArray(configString);
        for (int j = 0; j < configJsonArray.length(); j++) {

            JSONObject configJson = configJsonArray.getJSONObject(j);

            String serverUrl = configJson.getString("serverUrl");
            String adminUrl = configJson.getString("adminUrl");
            JSONArray products = configJson.getJSONArray("products");
            for (int i = 0; i < products.length(); i++) {
                String productName = products.getString(i);
                String[] list = testHelper.getDataFileNames(dataPath + productName);
                for (String testProductProfile : list) {
                    //check if filename is what you need
                    if (testProductProfile.contains("ToRun")) {
                        JSONArray allTests = new JSONArray(testHelper.getDataFileContent(dataPath + productName + "/" + testProductProfile));
                        for (int k = 0; k < allTests.length(); k++) {
                            JSONObject testCaseConfig = allTests.getJSONObject(k);
                            if (testCaseConfig.length() < 3) break;
                            Object[] params = new Object[4];
                            params[0] = adminUrl;
                            params[1] = serverUrl;
                            params[2] = productName;
                            params[3] = testCaseConfig;
                            configs.add(params);
                        }
                    }
                }
            }
        }

        return configs;
    }


    @Test
    public void test() throws AirlockNotInitializedException, JSONException, IOException, AirlockInvalidFileException, InterruptedException {

        testHelper.setup(m_minAppVersion, m_userGroups, m_locale, m_randomMap, true, true, false);
        testHelper.pull();
        testHelper.calcSync(null, m_context);
        //gold file processing
        GoldToOutputComparator comparator = new GoldToOutputComparator();
        Map<String, Feature> features = testHelper.getFeatures();
        JSONObject goldFileContent = new JSONObject(testHelper.getDataFileContent(dataPath + m_productName + "/" + m_goldFileName));
        JSONObject analyticsGoldFileContent = null;
        if (m_analyticsGoldFileName != null) {
            analyticsGoldFileContent = new JSONObject(testHelper.getDataFileContent(dataPath + m_productName + "/" + m_analyticsGoldFileName));
        }
        comparator.compare(m_productName, m_goldFileName, goldFileContent, analyticsGoldFileContent, features, m_testName);
    }
}
