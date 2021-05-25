package com.ibm.airlock.common.net;


import com.ibm.airlock.common.log.Logger;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


/**
 * Represents http client
 *
 * @author Denis Voloshin
 */
public class ConnectionManager {

    private final static String TAG = "ConnectionManager";
    private OkHttpClient okHttpClient;
    private AirlockDAO.DataProviderType dataProviderType = AirlockDAO.DataProviderType.CACHED_MODE;
    private static Map<String, OkHttpClient> clientsMap;

    static {
        clientsMap = new HashMap<>();
    }


    public ConnectionManager(OkHttpClientBuilder builder, boolean newHttpClient) {
        if (newHttpClient){
            clientsMap.remove("");
        }
        getAndSetClient(builder, "");
    }

    public ConnectionManager(OkHttpClientBuilder builder, @Nullable String encryptionKey, boolean newHttpClient) {
        if (newHttpClient && encryptionKey != null){
            clientsMap.remove(encryptionKey);
        }
        getAndSetClient(builder, encryptionKey);
    }

    public ConnectionManager(OkHttpClientBuilder builder, @Nullable String encryptionKey) {
        getAndSetClient(builder, encryptionKey);
    }

    public ConnectionManager(OkHttpClientBuilder builder) {
        getAndSetClient(builder, "");
    }

    private void getAndSetClient(OkHttpClientBuilder builder, String key) {
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

    public AirlockDAO.DataProviderType getDataProviderType() {
        return dataProviderType;
    }

    public void setDataProviderType(AirlockDAO.DataProviderType dataProviderType) {
        this.dataProviderType = dataProviderType;
    }


    public void sendPostRequestAsJson(String url, Callback callbackListener, String json) {
        Logger.log.d(TAG, "Send request, url = " + url);
        RequestBody body = RequestBody.create(MediaType
                .parse("application/json"), json);
        Request.Builder builder = new Request.Builder();
        builder.url(url).post(body);
        Request request = builder.build();
        Call call = okHttpClient.newCall(request);//.execute();
        call.enqueue(callbackListener);
    }


    public void sendRequest(String url, Callback callbackListener, Headers headers) {
        Logger.log.d(TAG, "Send request, url = " + url);
        Request.Builder builder = new Request.Builder();
        builder.url(url).headers(headers);
        Request request = builder.build();
        Call call = okHttpClient.newCall(request);//.execute();
        call.enqueue(callbackListener);
    }


    public void sendRequest(String url, Callback callbackListener) {
        Logger.log.d(TAG, "Send request, url = " + url);
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        Request request = builder.build();
        Call call = okHttpClient.newCall(request);//.execute();
        call.enqueue(callbackListener);
    }

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
