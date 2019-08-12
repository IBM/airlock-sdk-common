package com.ibm.airlock.common.engine;

import java.util.Locale;

import com.ibm.airlock.common.util.Constants;


/**
 * The exception is thrown when a script time happens.
 *
 * @author Denis Voloshin
 */
class ScriptExecutionTimeoutException extends Error {
    ScriptExecutionTimeoutException(int timeout) {
        super(String.format(Locale.getDefault(),Constants.SCRIPT_EXECUTION_TIMEOUT_EXCEPTION, timeout));
    }
}
