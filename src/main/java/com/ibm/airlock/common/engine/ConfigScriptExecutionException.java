package com.ibm.airlock.common.engine;

import org.json.JSONObject;


/**
 * Add description here
 *
 * @author Denis Voloshin
 */
public class ConfigScriptExecutionException extends ScriptExecutionException {
    private JSONObject config;

    ConfigScriptExecutionException(String msg) {
        super(msg);
    }

    public JSONObject getConfig() {
        return config;
    }

    public void setConfig(JSONObject config) {
        this.config = config;
    }
}
