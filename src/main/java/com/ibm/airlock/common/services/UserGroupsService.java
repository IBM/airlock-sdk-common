package com.ibm.airlock.common.services;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.RemoteConfigurationAsyncFetcher;
import com.ibm.airlock.common.util.AirlockMessages;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class UserGroupsService {

    private static final String TAG = "UserGroupsService";

    @Inject
    InfraAirlockService infraAirlockService;


    @Inject
    StreamsService streamsService;


    public void init(ProductDiComponent productDiComponent){
        productDiComponent.inject(this);
    }

    /**
     * Returns a list of user groups selected for the device.
     *
     * @return a list of user groups selected for the device.
     * @throws JSONException if the provided value breaks the JSON serialization process.
     */
    public List<String> getDeviceUserGroups() {
        return infraAirlockService.getPersistenceHandler().getDeviceUserGroups();
    }

    /**
     * Specifies a list of user groups selected for the device.
     *
     * @param userGroups List of the selected user groups.
     * @throws JSONException if the provided value breaks the JSON serialization process.
     */
    public void setDeviceUserGroups(@Nullable List<String> userGroups) {
        if (userGroups == null || userGroups.isEmpty()) {
            infraAirlockService.clearPreSyncRuntimeData();
        }
        if (userGroups == null) {
            return;
        }
        infraAirlockService.getPersistenceHandler().storeDeviceUserGroups(userGroups, streamsService);
    }

    /**
     * Asynchronously returns the list of user groups defined on the Airlock server.
     *
     * @param callback Callback to be called when the function returns.
     */
    public void getServerUserGroups(final AirlockCallback callback) {

        RemoteConfigurationAsyncFetcher.pullUserGroups(infraAirlockService, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.log.e(TAG, String.format(AirlockMessages.ERROR_FETCH_USER_GROUP_FORMATTED, call.request().url().toString()));
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call request, Response response) throws IOException {
                String userGroupsResponse = response.body() == null ? "[]" : response.body().string();
                try {
                    JSONObject userGroupsJson = new JSONObject(userGroupsResponse);
                    if (userGroupsJson.has("internalUserGroups")) {
                        callback.onSuccess(userGroupsJson.getJSONArray("internalUserGroups").toString());
                    }
                } catch (Exception e) {
                    callback.onSuccess("[]");
                } finally {
                    response.body().close();
                }
            }
        });
    }
}
