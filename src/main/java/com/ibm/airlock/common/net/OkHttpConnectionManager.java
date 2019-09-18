package com.ibm.airlock.common.net;


import com.ibm.airlock.common.log.Logger;
import okhttp3.*;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;


/**
 * Represents http connector
 *
 * @author Denis Voloshin
 */
public class OkHttpConnectionManager implements ConnectionManager {

    private static final String TAG = "OkHttpConnectionManager";
    private OkHttpClient okHttpClient;
    private RemoteConfigurationAsyncFetcher.DataProviderType dataProviderType = RemoteConfigurationAsyncFetcher.DataProviderType.CACHED_MODE;
    private static final Map<String, OkHttpClient> clientsMap;

    static {
        clientsMap = new HashMap<>();
    }


    public OkHttpConnectionManager(OkHttpClientBuilder builder, boolean newHttpClient) {
        if (newHttpClient) {
            clientsMap.remove("");
        }
        getAndSetClient(builder, "");
    }

    public OkHttpConnectionManager(OkHttpClientBuilder builder, @Nullable String encryptionKey, boolean newHttpClient) {
        if (newHttpClient && encryptionKey != null) {
            clientsMap.remove(encryptionKey);
        }
        getAndSetClient(builder, encryptionKey);
    }

    public OkHttpConnectionManager(OkHttpClientBuilder builder, @Nullable String encryptionKey) {
        getAndSetClient(builder, encryptionKey);
    }

    public OkHttpConnectionManager(OkHttpClientBuilder builder) {
        getAndSetClient(builder, "");
    }

    private void getAndSetClient(OkHttpClientBuilder builder,@Nullable String key) {
        if (key == null) {
            key = "";
        }
        OkHttpClient client = clientsMap.get(key);
        if (client == null) {
            if (key.isEmpty()) {
                client = builder.create();
            } else {
                client = builder.create(key);
            }
            clientsMap.put(key, client);
        }
        okHttpClient = client;
    }

    @Override
    public RemoteConfigurationAsyncFetcher.DataProviderType getDataProviderType() {
        return dataProviderType;
    }

    @Override
    public void setDataProviderType(RemoteConfigurationAsyncFetcher.DataProviderType dataProviderType) {
        this.dataProviderType = dataProviderType;
    }


    @Override
    public void sendPostRequestAsJson(String url, Callback callbackListener, String json) {
        Logger.log.d(TAG, "Send request, url = " + url);
        RequestBody body = RequestBody.create(MediaType
                .parse("application/json"), json);
        Request.Builder builder = new Request.Builder();
        builder.url(url).post(body);
        Request request = builder.build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(callbackListener);
    }


    @Override
    public void sendRequest(String url, Callback callbackListener, Headers headers) {
        Logger.log.d(TAG, "Send request, url = " + url);
        Request.Builder builder = new Request.Builder();
        builder.url(url).headers(headers);
        Request request = builder.build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(callbackListener);
    }


    @Override
    public void sendRequest(String url, Callback callbackListener) {
        Logger.log.d(TAG, "Send request, url = " + url);
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        Request request = builder.build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(callbackListener);
    }

    @Override
    public void sendRequest(String url, Map<String, String> headers, Callback callbackListener) {
        Logger.log.d(TAG, "Send request, url = " + url);
        Request.Builder builder = new Request.Builder();
        builder.url(url);

        //add headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.addHeader(header.getKey(), header.getValue());
        }

        Request request = builder.build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(callbackListener);
    }


}
