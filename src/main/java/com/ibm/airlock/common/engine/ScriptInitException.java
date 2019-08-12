package com.ibm.airlock.common.engine;

import com.ibm.airlock.common.util.Constants;


/**
 * The exception is thrown when the JS script couldn't be initialized
 *
 * @author Denis Voloshin
 */
public class ScriptInitException extends Exception {
    ScriptInitException(String msg) {
        super(Constants.SCRIPT_CONTEXT_INIT_EXCEPTION + ",Error:" + msg);
    }
}
