package com.ibm.airlock.common.engine;

import com.ibm.airlock.common.engine.context.StateFullContext;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StateFullContextTest {


    private StateFullContext stateFullContext;
    private String DUMMY_PRODUCT_CONEXT_VAR_NAME = "dummyProductContext";
    private String MULTI_PRODUCTS_CONEXT_VAR_NAME = "mutliProductsContext";

    @Before
    public void setUp() {
        stateFullContext = new StateFullContext(DUMMY_PRODUCT_CONEXT_VAR_NAME);
    }

    @Test
    public void addContextField() {
        stateFullContext.update(new JSONObject("{\"a\":{\"b\":1,\"c\":3}}"));
        stateFullContext.update(new JSONObject("{\"a\":{\"b\":2}}"));
        Assert.assertEquals(stateFullContext.toString(), "{\"a\":{\"b\":2,\"c\":3}}");
        stateFullContext.update(new JSONObject("{\"a\":{\"b\":1,\"c\":3}}"));
        Assert.assertEquals(stateFullContext.toString(), "{\"a\":{\"b\":1,\"c\":3}}");

    }

    @Test
    public void removeContextField() {
        stateFullContext.update(new JSONObject("{\"a\":{\"b\":1,\"c\":3}}"));
        stateFullContext.update(new JSONObject("{\"a\":{\"b\":2}}"));
        stateFullContext.removeContextField("a.c");
        Assert.assertEquals(stateFullContext.toString(), "{\"a\":{\"b\":2}}");
        stateFullContext.removeContextField("a.b");
        Assert.assertEquals(stateFullContext.toString(), "{\"a\":{}}");
    }

    @Test
    public void mergeShareContextIntoRuntimeContext() {

    }

    @Test
    public void mergeInnerArray() {
        stateFullContext.update(new JSONObject("{\"a\":[\"b\",\"c\"]}"));


        StateFullContext mutipProductSharedScope = new StateFullContext(MULTI_PRODUCTS_CONEXT_VAR_NAME);
        mutipProductSharedScope.update(new JSONObject("{\"a\":[\"d\",\"f\"]}"));

        stateFullContext.mergeWith(mutipProductSharedScope);
        Assert.assertEquals(stateFullContext.toString(), "{\"a\":[\"d\",\"f\",\"b\",\"c\"]}");
    }


    @Test
    public void mergeTheSameInnerArray() {
        stateFullContext.update(new JSONObject("{\"a\":[\"b\",\"c\"]}"));


        StateFullContext mutipProductSharedScope = new StateFullContext(MULTI_PRODUCTS_CONEXT_VAR_NAME);
        mutipProductSharedScope.update(new JSONObject("{\"a\":[\"b\",\"c\"]}"));

        stateFullContext.mergeWith(mutipProductSharedScope);
        Assert.assertEquals(stateFullContext.toString(), "{\"a\":[\"b\",\"c\"]}");
    }

    @Test
    public void mergeTwoShareContexts() {
        stateFullContext.update(new JSONObject("{\"a\":{\"b\":1,\"c\":3}}"));
        stateFullContext.update(new JSONObject("{\"a\":{\"b\":2}}"));

        StateFullContext mutipProductSharedScope = new StateFullContext(MULTI_PRODUCTS_CONEXT_VAR_NAME);
        mutipProductSharedScope.update(new JSONObject("{\"a\":{\"b\":1,\"c\":4,\"d\":5}}"));

        stateFullContext.mergeWith(mutipProductSharedScope);
        Assert.assertEquals(stateFullContext.toString(), "{\"a\":{\"b\":2,\"c\":3,\"d\":5}}");
    }

    @Test
    public void mergeEmptyShareContexts() {
        stateFullContext.update(new JSONObject("{\"a\":{\"b\":1,\"c\":3}}"));
        stateFullContext.update(new JSONObject("{\"a\":{\"b\":2}}"));

        StateFullContext mutipProductSharedScope = new StateFullContext(MULTI_PRODUCTS_CONEXT_VAR_NAME);

        stateFullContext.mergeWith(mutipProductSharedScope);
        Assert.assertEquals(stateFullContext.toString(), "{\"a\":{\"b\":2,\"c\":3}}");
    }
}