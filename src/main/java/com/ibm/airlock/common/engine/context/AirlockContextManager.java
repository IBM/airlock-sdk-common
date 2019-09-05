package com.ibm.airlock.common.engine.context;

import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.Strings;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;

public class AirlockContextManager {
    private static final String JS_UTILS_SCRIPT = "jsUtilsScript";
    private static final String JS_TRANSLATIONS_SCRIPT = "jsTranslationsScript";

    @Nullable
    private Script jsUtilsScript;
    @Nullable
    private Script jsTranslations;

    private final StateFullContext currentContext;

    private final StateFullContext runtimeContext;


    public StateFullContext getRuntimeContext() {
            return runtimeContext;
    }

    private final Hashtable<String, String> preCompiledScriptsMD5 = new Hashtable<>();

    public AirlockContextManager(String name) {
        currentContext = new StateFullContext(name);
        runtimeContext = new StateFullContext(name);
    }

    public synchronized void overwriteRuntimeWithCurrentContext(){
        runtimeContext.update(currentContext.toString(),true);
    }

    public synchronized void mergeSharedContext(@Nullable  StateFullContext sharedContext) {
        this.runtimeContext.mergeWith(sharedContext);
    }

    public void updateCurrentContext(String context) {
        synchronized (this.currentContext) {
            this.currentContext.update(new JSONObject(context));
        }
    }

    public void removeCurrentContextField(String fieldPath) {
        synchronized (this.currentContext) {
            this.currentContext.removeContextField(fieldPath);
        }
    }

    @CheckForNull
    public StateFullContext getCurrentContext() {
        return this.currentContext;
    }

    public void setJsUtilsScript(String script) {

        //create and enter safe execution context
        Context rhino = Context.enter();

        try {
            String md5 = generateMD5(script);
            if (!md5.equals(preCompiledScriptsMD5.get(JS_UTILS_SCRIPT))) {
                jsUtilsScript = rhino.compileString(script, "jsFunctions", 1, null);
                preCompiledScriptsMD5.put(JS_UTILS_SCRIPT, md5);
            }
        } catch (NoSuchAlgorithmException e) {
            jsUtilsScript = rhino.compileString(script, "jsFunctions", 1, null);
        }
        if (jsUtilsScript == null || !preCompiledScriptsMD5.containsKey(JS_UTILS_SCRIPT)) {
            jsUtilsScript = rhino.compileString(script, "jsFunctions", 1, null);
        }
        Context.exit();
        // a method after proguard equals to 'exec'JS_UTILS_SCRIPT
    }

    public void setJsTranslationsScript(String script) {

        //create and enter safe execution context
        Context rhino = Context.enter();


        String translations = "\nvar " +
                Constants.JS_TRANSLATIONS_VAR_NAME +
                " = JSON.parse(\"" +
                Strings.escapeCharactersForJSON(script) +
                "\")";

        try {
            String md5 = generateMD5(translations);
            if (!md5.equals(preCompiledScriptsMD5.get(JS_TRANSLATIONS_SCRIPT))) {
                jsTranslations = rhino.compileString(translations, "jsTranslations", 1, null);
                preCompiledScriptsMD5.put(JS_TRANSLATIONS_SCRIPT, md5);
            }
        } catch (NoSuchAlgorithmException e) {
            jsTranslations = rhino.compileString(translations, "jsTranslations", 1, null);
        }

        if (jsTranslations == null || !preCompiledScriptsMD5.containsKey(JS_TRANSLATIONS_SCRIPT)) {
            jsTranslations = rhino.compileString(translations, "jsTranslations", 1, null);
        }
        Context.exit();
    }


    @CheckForNull
    public Script getJsUtilsScript() {
        return jsUtilsScript;
    }

    @CheckForNull
    public Script getJsTranslationsScript() {
        return jsTranslations;
    }

    private String generateMD5(String script) throws NoSuchAlgorithmException {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(script.getBytes(), 0, script.length());
        return new BigInteger(1, m.digest()).toString(16);
    }
}
