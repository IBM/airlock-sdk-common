package com.ibm.airlock.common.services;

import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.engine.context.StateFullContext;
import com.ibm.airlock.common.util.Constants;
import org.json.JSONObject;

import javax.inject.Inject;

public class ContextService {
    private static final String TAG = "ContextService";

    @Inject
    InfraAirlockService infraAirlockService;

    @Inject
    PersistenceHandler persistenceHandler;

    public void init(ProductDiComponent productDiComponent) {
        productDiComponent.inject(this);
    }

    public void updateProductContext(String context){

    }

    public void updateProductContext(String context, boolean clearPreviousContext){

    }

    public void removeProductContextField(String fieldPath){

    }

    public void updateContext(String context, boolean clearPreviousContext) {
        if (infraAirlockService.getAirlockContextManager().getCurrentContext() != null) {
            infraAirlockService.getAirlockContextManager().getCurrentContext().update(new JSONObject(context), clearPreviousContext);
        }


        new Thread() {
            @Override
            @SuppressWarnings("MethodDoesntCallSuperMethod")
            public void run() {
                infraAirlockService.getPersistenceHandler().write(Constants.SP_CURRENT_CONTEXT,
                        infraAirlockService.getAirlockContextManager().getCurrentContext().toString());
            }
        }.start();
    }

    public void removeContextField(String fieldPath) {
        if (infraAirlockService.getAirlockContextManager().getCurrentContext() != null) {
            infraAirlockService.getAirlockContextManager().getCurrentContext().removeContextField(fieldPath);
        }
    }

    public void setSharedContext(StateFullContext sharedContext) {
        infraAirlockService.setAirlockSharedContext(sharedContext);
    }

}
