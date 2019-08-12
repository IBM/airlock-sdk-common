package com.ibm.airlock.common.engine;

import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * @author Denis Voloshin
 */
public class RhinoScriptInvokerTest {

    @Test
    public void testEvaluate() {

        JSONObject context = new JSONObject();
        context.put("javascriptUtilities","");
        context.put("translations","{}");

        try {
            RhinoScriptInvoker rhinoScriptInvoker = new RhinoScriptInvoker(context, null);
            ScriptInvoker.Output out = rhinoScriptInvoker.evaluate("var arr = [ 3,3,3]; arr.includes(4)");
            Assert.assertEquals(out.result, ScriptInvoker.Result.ERROR);
            Assert.assertEquals("Javascript error: TypeError: Cannot find function includes in object 3,3,3. (JavaScript#1)",
                    out.error);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEvaluateConfiguration() {
    }

    @Test
    public void testExit() {
    }
}