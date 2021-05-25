package com.ibm.airlock.common.engine;

import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.Strings;

import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class AirlockContextManager {
    private static final String JS_UTILS_SCRIPT = "jsUtilsScript";
    private static final String JS_TRANSLATIONS_SCRIPT = "jsTranslationsScript";

    @Nullable
    private Script jsUtilsScript;
    @Nullable
    private Script jsTranslations;

    private String name;

    private SafeContextFactory safeContextFactory;


    private StateFullContext currentContext;

    private StateFullContext runtimeContext;


    public StateFullContext getRuntimeContext() {
            return runtimeContext;
    }

    private Hashtable<String, String> preCompiledScriptsMD5 = new Hashtable<>();

    public AirlockContextManager(String name) {
        this.name = name;
        this.currentContext = new StateFullContext(this.name);
        this.runtimeContext = new StateFullContext(this.name);
        this.safeContextFactory = new SafeContextFactory();
    }

    public synchronized void overideRuntimeWithCurrentContext(){
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
        Context rhino = safeContextFactory.makeContext().enter();
        try {
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
        } catch (Throwable ex) {
            //"swallow" any throwable to avoid crash
            //worst case no JS utils - better than crash
        }
        Context.exit();
        // a method after proguard equals to 'exec'JS_UTILS_SCRIPT
    }

    public void setJsTranslationsScript(String script) {

        //create and enter safe execution context
        Context rhino = safeContextFactory.makeContext().enter();


        StringBuilder translateScript = new StringBuilder();
        translateScript.append("\nvar ");
        translateScript.append(Constants.JS_TRANSLATIONS_VAR_NAME);
        translateScript.append(" = JSON.parse(\"");
        translateScript.append(Strings.escapeCharactersForJSON(script));
        translateScript.append("\")");
        String translations = translateScript.toString();

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
