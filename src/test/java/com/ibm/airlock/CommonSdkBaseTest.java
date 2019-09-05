package com.ibm.airlock;

import com.ibm.airlock.common.AirlockClient;
import com.ibm.airlock.common.AirlockProductSeasonManager;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.DefaultAirlockProductManager;
import com.ibm.airlock.common.net.ConnectionManager;

import com.ibm.airlock.common.util.Constants;
import org.json.JSONException;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * @author Denis Voloshin
 */
public class CommonSdkBaseTest extends AbstractBaseTest {

    private static final String DEFAULT_FILES_ROOT_LOCATION = "test_data/defaults";
    private CommonSdkTestDataManager commonSdkTestDataManager;

    public CommonSdkBaseTest() {
        commonSdkTestDataManager = new CommonSdkTestDataManager();
    }

    @Override
    public void setUpMockUps() throws JSONException {

    }

    public void createClientInstance() throws AirlockInvalidFileException{
        airlockProductSeasonManager = new AirlockProductSeasonManager(m_productName, slurp(getDefaultFile(), 1024), "", m_appVersion);
        airlockClient = airlockProductSeasonManager.createClient("111-111-111-111");
        manager = airlockClient.getAirlockProductManager();
    }

    @Override
    public void setup(String version, String key, ArrayList<String> groups, String locale, String randoms, boolean setUpMockUps, boolean reset, boolean cleanStreams) throws IOException, AirlockInvalidFileException, JSONException {
        //set up mocks
        createClientInstance();
        m_appVersion = version;
        //reset
        if (reset && manager != null) {
            //manager.reset();
            //createClientInstance();
        }
        //set user groups
        if (groups != null) {
            airlockProductSeasonManager.setDeviceUserGroups(groups);
            airlockClient.getAirlockProductManager().getUserGroupsService().setDeviceUserGroups(groups);
        }
        //set locale
        if (locale != null) {
            Locale.setDefault(new Locale(locale));
        }
        //write required randoms
        if (randoms != null) {
            manager.getInfraAirlockService().getPersistenceHandler().write(Constants.SP_RANDOMS, randoms);
        }
        //clean streams
        try {
            if (cleanStreams && manager.getStreamsService() != null) {
                manager.getStreamsService().clearAllStreams();
                if (reset) {
                    manager.getStreamsService().clearStreams();
                }
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    public void recreateClient(String version) throws IOException, AirlockInvalidFileException {
        m_appVersion = version;
        setup(version, m_key, null, null, null, true, true, true);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public InputStream getDefaultFile() throws JSONException {
        try {
            String defaultsAsString = getDataFileContent(DEFAULT_FILES_ROOT_LOCATION + File.separator +
                    m_productName + "_" + normalizeVersion(m_appVersion) + ".json");
            if (defaultsAsString.isEmpty()) {
                Assert.fail("[" + DEFAULT_FILES_ROOT_LOCATION + File.separator +
                        m_productName + "_" + normalizeVersion(m_appVersion) + ".json] not found");
            }
            return new ByteArrayInputStream(defaultsAsString.getBytes());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }

    private String normalizeVersion(String version) {
        return version;
    }

    @Override
    public String getDataFileContent(String pathInDataFolder) throws IOException {
        return commonSdkTestDataManager.getFileContent(pathInDataFolder);
    }

    @Override
    public String[] getDataFileNames(String directoryPathInDataFolder) throws IOException {
        return commonSdkTestDataManager.getFileNamesListFromDirectory(directoryPathInDataFolder);
    }

    @Override
    protected ConnectionManager getConnectionManager() {
        return new FileSystemConnectorManager();
    }

    @Override
    protected ConnectionManager getConnectionManager(String m_key) {
        return new FileSystemConnectorManager();
    }

    @Override
    public void setLocale(Locale locale) {
        Locale.setDefault(locale);
    }
}
