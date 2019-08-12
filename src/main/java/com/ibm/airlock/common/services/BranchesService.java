package com.ibm.airlock.common.services;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.RemoteConfigurationAsyncFetcher;
import com.ibm.airlock.common.util.AirlockMessages;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;

@SuppressWarnings({"WeakerAccess", "unused"})
public class BranchesService {

    private static final String TAG = "BranchesService";

    @Inject
    InfraAirlockService infraAirlockService;

    @Inject
    PersistenceHandler persistenceHandler;


    public void init(ProductDiComponent productDiComponent) {
        productDiComponent.inject(this);
    }

    /**
     * Asynchronously returns the list of branches defined on the Airlock server.
     *
     * @param callback Callback to be called when the function returns.
     */

    public void getProductBranches(final AirlockCallback callback) {

        RemoteConfigurationAsyncFetcher.pullBranches(infraAirlockService, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.log.e(TAG, String.format(AirlockMessages.ERROR_FETCH_BRANCHES_FORMATTED, call.request().url().toString()));
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call request, Response response) throws IOException {
                String productBranches = response.body() == null ? "[]" : response.body().string();
                try {
                    JSONObject productBranchesJson = new JSONObject(productBranches);
                    if (productBranchesJson.has("branches")) {
                        callback.onSuccess(productBranchesJson.getJSONArray("branches").toString());
                    }
                } catch (Exception e) {
                    callback.onSuccess("[]");
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }


    /**
     * Asynchronously returns the branch by ID.
     *
     * @param callback Callback to be called when the function returns.
     * @param branchId branch Id.
     */

    public void getProductBranchById(String branchId, final AirlockCallback callback) {

        RemoteConfigurationAsyncFetcher.pullBranchById(infraAirlockService, branchId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.log.e(TAG, String.format(AirlockMessages.ERROR_FETCH_BRANCHES_FORMATTED, call.request().url().toString()));
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call request, Response response) throws IOException {
                String branch = response.body() == null ? "{}" : response.body().string();
                try {
                    callback.onSuccess(branch);
                } catch (Exception e) {
                    callback.onSuccess("{}");
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }


    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    public String getDevelopBranchName() {
        return persistenceHandler.getLastBranchName();
    }

    /**
     * Stores a selected develop branch name to the local store
     */
    public void setDevelopBranchName(String selectedDevelopBranchName) {
        persistenceHandler.setDevelopBranchName(selectedDevelopBranchName);
    }

    /**
     * returns the last branch name from the local store
     *
     * @return the name of the last branch name used
     */
    public String getLastBranchName() {
        return persistenceHandler.getLastBranchName();
    }

    /**
     * Stores the current branch name to the local store
     */
    public void setLastBranchName(String selectedDevelopBranchName) {
        persistenceHandler.setLastBranchName(selectedDevelopBranchName);
    }

    /**
     * Return a selected develop branch id from the local store
     *
     * @return the identification of a selected develop branch
     */
    public String getDevelopBranchId() {
        return persistenceHandler.getDevelopBranchId();
    }

    /**
     * Stores a selected develop branch id to the local store
     */
    public void setDevelopBranchId(String selectedDevelopBranchId) {
        persistenceHandler.setDevelopBranchId(selectedDevelopBranchId);
    }

    /**
     * Return a selected develop branch from the local store
     *
     * @return the name of a selected develop branch
     */
    public String getDevelopBranch() {
        return persistenceHandler.getDevelopBranch();
    }

    /**
     * Stores a selected develop branch config (in JSON) to the local store
     */
    public void setDevelopBranch(String selectedDevelopBranch) {
        persistenceHandler.setDevelopBranch(selectedDevelopBranch);
    }
}
