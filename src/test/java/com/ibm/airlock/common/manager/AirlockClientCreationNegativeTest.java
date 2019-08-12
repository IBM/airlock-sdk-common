package com.ibm.airlock.common.manager;

import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.DefaultAirlockProductManager;
import com.ibm.airlock.common.model.Feature;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Denis Voloshin
 */
public class AirlockClientCreationNegativeTest {

    private static AirlockProductManager manager;

    @BeforeClass
    public static void setUp() throws AirlockInvalidFileException {
        AirlockProductManager airlockProductManager = DefaultAirlockProductManager.builder().withAirlockDefaults("{}").
                withAppVersion("1.0.0").withProductName("AirlockClientCreationNegativeTest").build();
        manager = airlockProductManager.createClient("111-111-111-111").getAirlockProductManager();
    }


    @Test
    public void noInitGetFeatureTest() {
        Feature feature = manager.getFeaturesService().getFeature("Multi Location Home Screen");
        Assert.assertFalse("isOn = false is expected when calling getFeature without calling initSDK before.", feature.isOn());
        Feature.Source source = feature.getSource();
        Assert.assertEquals("MISSING name is expected when calling getFeature without calling initSDK before.", "MISSING", source.name());
    }


    @Test
    public void noInitGetSessionAndProductIdShouldNotBeNull() {
        Assert.assertNotNull("Session should be null if call AirlockManager.getSessionId before initSDK", manager.getProductInfoService().getSeasonId());
        Assert.assertNotNull("Session should be null if call AirlockManager.getProductId before initSDK", manager.getProductInfoService().getProductId());
    }
}
