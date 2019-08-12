package com.ibm.airlock.common.engine;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;


/**
 * The class defines the safe script execution sand-box and guarantees that the script won't run the predefined amount of time. If a limit is reached the
 * timeout exception will be thrown.
 *
 * @author Denis Voloshin
 */
public class SafeContextFactory extends ContextFactory {


    //check if the rhino reached time-out each 10000 bytecode instructions
    private static final int INSTRUCTIONS_OBSERVATION_STEP = 10000;
    private static final int DEFAULT_SCRIPT_TIMEOUT_PERIOD = 1000;

    static {
        // Initialize GlobalFactory with custom factory
        ContextFactory.initGlobal(new SafeContextFactory());
    }

    /**
     * Create safe execution context
     */
    @Override
    public Context makeContext() {
        SafeContext cx = new SafeContext(this);
        // Use pure interpreter mode to allow for
        // observeInstructionCount(Context, int) to work
        cx.setOptimizationLevel(-1);
        // Make Rhino runtime to call observeInstructionCount
        // each 10000 bytecode instructions
        cx.setInstructionObserverThreshold(INSTRUCTIONS_OBSERVATION_STEP);
        return cx;
    }

    // Override hasFeature(Context, int)
    @Override
    public boolean hasFeature(Context cx, int featureIndex) {
        // Turn on maximum compatibility with MSIE scripts
        switch (featureIndex) {
            case Context.FEATURE_NON_ECMA_GET_YEAR:
            case Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME:
            case Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER:
                return true;

            case Context.FEATURE_PARENT_PROTO_PROPERTIES:
                return false;
        }
        return super.hasFeature(cx, featureIndex);
    }

    // Override observeInstructionCount(Context, int)
    @Override
    protected void observeInstructionCount(Context cx, int instructionCount) {
        SafeContext mcx = (SafeContext) cx;
        long currentTime = System.currentTimeMillis();
        if (currentTime - mcx.startTime > DEFAULT_SCRIPT_TIMEOUT_PERIOD) {
            // More then maximum period from Context creation time:
            // it is time to stop the script.
            // Throw Error instance to ensure that script will never
            // get control back through catch or finally.
            throw new ScriptExecutionTimeoutException(DEFAULT_SCRIPT_TIMEOUT_PERIOD);
        }
    }

    @Override
    protected Object doTopCall(Callable callable,
            Context cx, Scriptable scope,
            Scriptable thisObj, Object[] args) {
        SafeContext mcx = (SafeContext) cx;
        mcx.startTime = System.currentTimeMillis();
        return super.doTopCall(callable, cx, scope, thisObj, args);
    }

    // Custom Context to store execution time.
    private static class SafeContext extends Context {
        long startTime;

        SafeContext(ContextFactory factory) {
            super(factory);
        }
    }
}
