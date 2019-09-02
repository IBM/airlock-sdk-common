package com.ibm.airlock.common.engine.context;

import com.ibm.airlock.common.engine.SafeContextFactory;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StateFullContext {


    public static final String MERGE_FUNCTION_NAME = "mergeDeep";
    private final Scriptable scope;
    private final Script mergeDeepFunction;
    private final String contextVarName;
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock(true);

    // o2 overrides o1
    private static final String jsonMergeMethod = "function mergeDeep (o1, o2) { \n" +
            "               var tempNewObj = o1; \n" +
            "            \n" +
            "               //if o1 is an object - {} \n" +
            "               if (o1.length === undefined && typeof o1 !== \"number\") { \n" +
            "                   for (var key in o2) { \n" +
            "                   // skip loop if the property is from prototype \n" +
            "                     if(!o2.hasOwnProperty(key)) continue; \n" +
            "            \n" +
            "                     if (o1[key] === undefined) { \n" +
            "                         tempNewObj[key] = o2[key]; \n" +
            "                     }else { \n" +
            "                         if(o1[key] != null){ \n" +
            "                            tempNewObj[key] = mergeDeep(o1[key], o2[key]); \n" +
            "                         } \n" +
            "                     } \n" +
            "                   } \n" +
            "               } \n" +
            "            \n" +
            "               //else if o1 is an array - [] \n" +
            "               else if (o1.length > 0 && typeof o1 !== \"string\") { \n" +
            "                   o2.forEach(function(element,index) { \n" +
            "                       if (JSON.stringify(o1).indexOf(JSON.stringify(o2[index])) === -1) { \n" +
            "                        if(o2[index]!==undefined){\n" +
            "                           tempNewObj.push(o2[index]); \n" +
            "                        }\n" +
            "                       } \n" +
            "                   }); \n" +
            "               } \n" +
            "            \n" +
            "               //handling other types like string or number \n" +
            "               else { \n" +
            "                   //taking value from the second object o2 \n" +
            "                   //could be modified to keep o1 value with tempNewObj = o1; \n" +
            "                   tempNewObj = o2; \n" +
            "               } \n" +
            "               return tempNewObj; \n" +
            "            };";

    public StateFullContext(String contextVarName) {
        //create and enter safe execution context
        this.contextVarName = escapeContextVarName(contextVarName);
        SafeContextFactory safeContextFactory = new SafeContextFactory();
        Context rhino = Context.enter();
        mergeDeepFunction = rhino.compileString(jsonMergeMethod + " ; var " + this.contextVarName + " = {};", "mergeDeep", 1, null);
        scope = rhino.initStandardObjects();
        mergeDeepFunction.exec(rhino, scope);
        Context.exit();
    }


    private String escapeContextVarName(String name) {
        //noinspection DynamicRegexReplaceableByCompiledPattern
        return name.replace(".", "_").replace(" ", "_");
    }

    public String getContextVarName() {
        return contextVarName;
    }

    public void update(String context, Boolean clearPreviousContext) {
        if (!clearPreviousContext) {
            update(context);
        } else {
            ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();
            try {
                writeLock.lock();
                SafeContextFactory safeContextFactory = new SafeContextFactory();
                Context rhino = Context.enter();
                rhino.evaluateString(scope, this.contextVarName + " = " + context, "<cmd>", 1, null);
            } finally {
                writeLock.unlock();
            }
        }
    }


    public void update(JSONObject context, Boolean clearPreviousContext) {
        update(context.toString(), clearPreviousContext);
    }

    private void update(String context) {
        try {
            reentrantReadWriteLock.writeLock().lock();
            //create and enter safe execution context
            SafeContextFactory safeContextFactory = new SafeContextFactory();
            Context rhino = Context.enter();
            String contextScript = this.contextVarName + " = mergeDeep(" + this.contextVarName + ',' + context + ')';
            rhino.evaluateString(scope, contextScript, "<cmd>", 1, null);
            Context.exit();
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }


    public void update(JSONObject context) {
        update(context.toString());
    }

    public void removeContextField(String path) {
        try {
            reentrantReadWriteLock.writeLock().lock();
            SafeContextFactory safeContextFactory = new SafeContextFactory();
            Context rhino = Context.enter();
            rhino.evaluateString(scope, "if (" + this.contextVarName + '.' + path + "){ delete " + this.contextVarName + '.' + path + " }", "<cmd>", 1, null);
            Context.exit();
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }

    }


    private Scriptable doScopeClone() {
        SafeContextFactory safeContextFactory = new SafeContextFactory();
        Context rhino = Context.enter();

        Scriptable scope = rhino.initStandardObjects();
        mergeDeepFunction.exec(rhino, scope);

        rhino.evaluateString(scope, this.contextVarName + '=' + this.doToString(), "<cmd>", 1, null);
        return scope;
    }

    public Scriptable getScopeClone() {
        try {
            reentrantReadWriteLock.readLock().lock();
            return doScopeClone();
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    /**
     * This context has high priority upon the given the result is assign to the this context
     *
     * @param stateFullContext
     */
    public void mergeWith(@Nullable StateFullContext stateFullContext) {
        try {
            reentrantReadWriteLock.writeLock().lock();
            if (stateFullContext == null) {
                return;
            }
            scope.setPrototype(stateFullContext.doScopeClone());
            SafeContextFactory safeContextFactory = new SafeContextFactory();
            Context rhino = Context.enter();
            String contextScript = this.contextVarName + " = mergeDeep(" + stateFullContext.contextVarName + ',' + this.contextVarName + ')';
            rhino.evaluateString(scope, contextScript, "<cmd>", 1, null);
            Context.exit();
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }

    }

    private String doToString() {
        SafeContextFactory safeContextFactory = new SafeContextFactory();
        Context rhino = Context.enter();
        Object context = rhino.evaluateString(scope, "JSON.stringify(" + this.contextVarName + ')', "<cmd>", 1, null);
        Context.exit();
        return Context.toString(context);
    }

    public String toString() {
        try {
            reentrantReadWriteLock.readLock().lock();
            return doToString();
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }
}
