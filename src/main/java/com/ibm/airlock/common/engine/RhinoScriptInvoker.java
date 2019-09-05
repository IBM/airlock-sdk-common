package com.ibm.airlock.common.engine;

import com.ibm.airlock.common.engine.context.AirlockContextManager;
import com.ibm.airlock.common.engine.context.StateFullContext;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.Strings;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;


/**
 * Java script engine wrapper, the class is based in Rhino library to evaluate JS code. Created by DenisV on 8/22/16.
 */
public class RhinoScriptInvoker extends ScriptInvoker {
    private static final String TAG = "airlock.RhinoInvoker";

    // function definitions and device context objects are added into the shared scope.
    // all queries will use a new scope that refers to the shared scope
    private Scriptable scope;
    private Context rhino;
    @Nullable
    private final AirlockContextManager airlockContextManager;

    public RhinoScriptInvoker(JSONObject runtimeContext, @Nullable AirlockContextManager airlockContextManager) throws JSONException, ScriptInitException {
        super(new TreeMap<String, String>()); // scriptObjects
        long start = System.currentTimeMillis();
        this.airlockContextManager = airlockContextManager;
        String functions = null;
        String translations = null;
        Iterator<String> keys = runtimeContext.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            switch (key) {
                case Constants.JSON_JS_FUNCTIONS_FIELD_NAME:
                    functions = runtimeContext.optString(key, "");
                    break;
                case Constants.JS_GROUPS_VAR_NAME:
                    String groups = runtimeContext.getJSONArray(key).toString();
                    scriptObjects.put(key, groups);
                    break;
                case Constants.JS_TRANSLATIONS_VAR_NAME:
                    translations = runtimeContext.optString(key, "");
                    scriptObjects.put(key, translations);
                    break;
                default:
                    JSONObject item = runtimeContext.getJSONObject(key);
                    scriptObjects.put(key, item.toString());
            }
        }
        if (functions == null || translations == null) {
            throw new ScriptInitException("Any context function was found");
        }
        init(functions, translations);
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().report(AirlockEnginePerformanceMetric.RHINO_INIT, start);
    }


    private void init(String functions, String translations) throws ScriptInitException {

        String translateScript = translations;
        StringBuilder ruleEngineContextBuffer = new StringBuilder();
        // add items such as profile, context etc as strings to the JS binding
        for (Map.Entry<String, String> e : scriptObjects.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            if (key.equals(Constants.JS_CONTEXT_VAR_NAME)) {
                ruleEngineContextBuffer.append("var ");
                ruleEngineContextBuffer.append(key);
                ruleEngineContextBuffer.append(" = JSON.parse(\"");
                ruleEngineContextBuffer.append(Strings.escapeCharactersForJSON(value));
                ruleEngineContextBuffer.append("\")");
            }
            if (key.equals(Constants.JS_TRANSLATIONS_VAR_NAME)) {
                translateScript = "\nvar " +
                        key +
                        " = JSON.parse(\"" +
                        Strings.escapeCharactersForJSON(value) +
                        "\")";
            }
        }

        //create and enter safe execution context
        SafeContextFactory safeContextFactory = new SafeContextFactory();
        rhino = safeContextFactory.makeContext().enter();

        try {
            long start;
            scope = rhino.initStandardObjects();
            start = System.currentTimeMillis();

            initJSfunctions(scope, functions, rhino);
            AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().report(AirlockEnginePerformanceMetric.JS_UTILS_LOADING, start);

            start = System.currentTimeMillis();
            initTranslations(scope, translateScript, rhino);
            AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().report(AirlockEnginePerformanceMetric.TRANSLATION_LOADING, start);

            start = System.currentTimeMillis();
            rhino.evaluateString(scope, ruleEngineContextBuffer.toString(), "<init1>", 1, null);
            AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().report(AirlockEnginePerformanceMetric.CONTEXT_INIT, start);

            start = System.currentTimeMillis();
            if (this.airlockContextManager != null) {
                scope.setPrototype(this.airlockContextManager.getRuntimeContext().getScopeClone());
                rhino.evaluateString(scope, Constants.JS_CONTEXT_VAR_NAME + " = " + StateFullContext.MERGE_FUNCTION_NAME +
                        "(JSON.parse(JSON.stringify(" + this.airlockContextManager.getRuntimeContext().getContextVarName() + "))" + ','
                        + Constants.JS_CONTEXT_VAR_NAME + ')'
                        , "<init2>", 1, null);
            }
            AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().report(AirlockEnginePerformanceMetric.MERGE_SHARED_CONTEXT, start);
        } catch (Throwable e) {
            throw new ScriptInitException("Javascript shared scope initialization error: " + e.getMessage());
        }
    }


    private synchronized void initTranslations(Scriptable sharedScope, String translations, Context rhino) {
        if (this.airlockContextManager != null) {
            Script jsTranslations = this.airlockContextManager.getJsTranslationsScript();
            if (jsTranslations == null) {
                jsTranslations = rhino.compileString(translations, "jsTranslations", 1, null);
            }
            jsTranslations.exec(rhino, sharedScope);
        }
    }

    private synchronized void initJSfunctions(Scriptable sharedScope, String funtions, Context rhino) {
        if (this.airlockContextManager != null) {
            Script jsUtilsScript = this.airlockContextManager.getJsUtilsScript();
            if (jsUtilsScript == null) {
                jsUtilsScript = rhino.compileString(funtions, "jsFunctions", 1, null);
            }
            // a method after proguard equals to 'exec'
            jsUtilsScript.exec(rhino, sharedScope);
        }
    }

    @Override
    public Output evaluate(@Nullable String query) {

        if (query == null || query.isEmpty() || query.equals("null") || query.trim().toLowerCase(Locale.getDefault()).equals("true")) {
            return new Output(Result.TRUE);
        } else if (query.trim().toLowerCase(Locale.getDefault()).equals("false")) {
            return new Output(Result.FALSE);
        }

        try {
            Object result = rhino.evaluateString(this.scope, query, "JavaScript", 1, null);
            evalCounter++;
            // read execution result
            int out = -1;
            if (result instanceof Boolean) {
                out = Context.toBoolean(result) ? 1 : 0;
            }

            if (out == 0) {
                return new Output(Result.FALSE);
            }
            if (out == 1) {
                return new Output(Result.TRUE);
            }

            return new Output(Result.ERROR, "Script result is not boolean");
        } catch (ScriptExecutionTimeoutException e) {
            return new Output(Result.ERROR, "Javascript timeout: " + e.getMessage());
        } catch (Throwable e) {
            return new Output(Result.ERROR, "Javascript error: " + e.getMessage());
        }
    }

    @Override
    public JSONObject evaluateConfiguration(@Nullable String configString) throws ScriptExecutionException {
        JSONObject out = new JSONObject();
        if (configString == null || configString.isEmpty() || configString.equals("null")) {
            return out;
        }

        try {
            String exec = "var result_ = " + configString + "; JSON.stringify(result_); ";
            Object result = rhino.evaluateString(this.scope, exec, "JavaScript", 1, null);
            evalCounter++;
            // TODO: fewer conversions
            String str = Context.toString(result);

            return new JSONObject(str);
            // Catch also ScriptExecutionTimeoutException
        } catch (Throwable e) {
            throw new ScriptExecutionException(e.getMessage());
        }
    }

    /**
     * Destroys current context
     */
    @Override
    public void exit() {
        Context.exit();
    }
}
