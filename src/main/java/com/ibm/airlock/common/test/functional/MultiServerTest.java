package com.ibm.airlock.common.test.functional;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Servers;
import com.ibm.airlock.common.test.common.BaseTestModel;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import edu.umd.cs.findbugs.annotations.NonNull;


/**
 * Created by Denis Voloshin on 25/12/2017.
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
        testHelper.customSetUp(m_version, null, null, null, true, true, true);
    }

    @Test
    public void doPullDefaultFileTest() {
        AirlockProductManager manager = testHelper.getManager();

        Servers.Server server = manager.getCacheManager().getServers().getCurrentServer();
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            manager.getCacheManager().pullProductList(server, new AirlockCallback() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    failMessage = e.getClass() + ": " + e.getMessage();
                    latch.countDown();
                }

                @Override
                public void onSuccess(@NonNull String msg) {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (Exception e) {
            Assert.fail(e.getClass() + ": " + e.getMessage());
        }
        if (failMessage != null) {
            String tmp = failMessage;
            failMessage = null;
            Assert.fail(tmp);
        }
        Servers.Product my_product = null;
        Iterator it = server.getProducts().iterator();
        while (it.hasNext()) {
            my_product = (Servers.Product) it.next();
            if (my_product.getName().equals(PRODUCT_NAME)) break;
        }

        final CountDownLatch latch2 = new CountDownLatch(1);
        try {
            manager.getCacheManager().pullDefaultFile(server, my_product, new AirlockCallback() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    failMessage = e.getClass() + ": " + e.getMessage();
                    latch2.countDown();
                }

                @Override
                public void onSuccess(@NonNull String msg) {
                    successMessage = msg;
                    latch2.countDown();
                }
            });
            latch2.await();
        } catch (Exception e) {
            Assert.fail(e.getClass() + ": " + e.getMessage());
        }
        try {
            String d_file = manager.getCacheManager().getPersistenceHandler().read(Constants.SP_UPDATED_DEFAULT_FILE, "").trim();
            JSONObject read = new JSONObject(d_file);
            Assert.assertTrue("Unexpected product name was loaded.", read.get("productName").equals(PRODUCT_NAME));
        } catch (JSONException e) {
            Assert.fail("JSONException when trying to create a JSON from the updated default file. " + e.getMessage());
        }
        if (failMessage != null) {
            String tmp = failMessage;
            failMessage = null;
            Assert.fail(tmp);
        }
    }
}
