package com.ibm.airlock.common.engine;

import com.ibm.airlock.common.engine.context.AirlockContextManager;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Map;


public abstract class ScriptInvoker {
    final Map<String, String> scriptObjects;
    int evalCounter = 2;

    // the map contains key/value pairs, where the key is the name of a JavaScript object ("profile", "context", etc)
    // and the value is the JSON string that will construct it.
    ScriptInvoker(Map<String, String> scriptObjects) {
        this.scriptObjects = scriptObjects;
    }

    // construct a JavaScript snippet from the query and the script objects, invoke it and return true/false/error
    public abstract Output evaluate(@Nullable String query);

    public abstract JSONObject evaluateConfiguration(String configString) throws ScriptExecutionException;

    public abstract void exit();

    public enum Result {TRUE, FALSE, ERROR}

    public static class Output {
        public final Result result;
        public final String error;

        Output(Result result, String error) {
            this.result = result;
            this.error = error;
        }

        public Output(Result result) {
            this.result = result;
            this.error = "";
        }
    }

    public static class InvokerException extends Exception {
        private static final long serialVersionUID = 1L;

        InvokerException(String str) {
            super(str);
        }
    }

    public int getEvalsCounter() {
        return this.evalCounter;
    }

    public void setAirlockScriptScope(AirlockContextManager airlockScriptScope) {
    }

}
