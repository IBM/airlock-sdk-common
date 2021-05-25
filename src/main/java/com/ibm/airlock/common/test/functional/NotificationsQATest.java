package com.ibm.airlock.common.test.functional;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.engine.ScriptInitException;
import com.ibm.airlock.common.notifications.AirlockNotification;
import com.ibm.airlock.common.notifications.NotificationsManager;
import com.ibm.airlock.common.test.common.BaseTestModel;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by iditb on 10/12/17.
 */

public class NotificationsQATest extends BaseTestModel {

    private static NotificationsManager m_notificationsManager;


    @Parameterized.Parameters
    public static Collection params() throws IOException {
        testClassName = "NotificationsQATest";
        return getConfigs();
    }

    public NotificationsQATest(String adminUrl, String serverUrl, String productName, String version, String key) throws IOException, AirlockInvalidFileException {
        super(adminUrl, serverUrl, productName, version, key);
        m_notificationsManager = testHelper.getManager().getNotificationsManager();
    }

    @Test
    public void setNotificationsFalseTrueTest() throws AirlockNotInitializedException, InterruptedException, ScriptInitException {
        m_notificationsManager.setSupported(false);
        Assert.assertTrue("Notifications should not be supported", !m_notificationsManager.isSupported());
        testHelper.pull();
        Assert.assertTrue("Notifications should not return from the server", m_notificationsManager.getNotifications() == null);
        m_notificationsManager.setSupported(true);
        Assert.assertTrue("Notifications should be supported", m_notificationsManager.isSupported());
        testHelper.pull();
        Assert.assertTrue("Notifications should return from the server", m_notificationsManager.getNotifications() != null);
        Assert.assertTrue("Notifications list size should be greater than 0", m_notificationsManager.getNotifications().size() > 0);
    }

    @Test
    public void prodNotificationTest() throws IOException, AirlockInvalidFileException, AirlockNotInitializedException, InterruptedException {
        testHelper.customSetUp(m_version, new ArrayList<String>(), null, null, false, true, false);
        testHelper.getManager().getNotificationsManager().setSupported(true);
        testHelper.pull();
        Map<String, AirlockNotification> notifications = testHelper.getManager().getNotificationsManager().getNotifications();
        Assert.assertTrue("A single notification in production is expected", notifications.size() == 1);
        Assert.assertTrue("NotifyTestInProd name is expected", notifications.get("NotifyTestInProd") != null);
    }

    //todo
    @Test
    public void calculateNotificationsTest() throws AirlockNotInitializedException, InterruptedException, ScriptInitException {
        m_notificationsManager.setSupported(true);
        testHelper.pull();
        m_notificationsManager.calculateAndSaveNotifications(new JSONObject());
        m_notificationsManager.getPendingNotifications();
    }

}
