package com.ibm.airlock.common.net;

import java.util.Map;
import okhttp3.Callback;
import okhttp3.Headers;


/**
 * @author Denis Voloshin
 */
public interface ConnectionManager {

    RemoteConfigurationAsyncFetcher.DataProviderType getDataProviderType();

    void setDataProviderType(RemoteConfigurationAsyncFetcher.DataProviderType dataProviderType);

    void sendPostRequestAsJson(String url, Callback callbackListener, String json);

    void sendRequest(String url, Callback callbackListener, Headers headers);

    void sendRequest(String url, Callback callbackListener);

    void sendRequest(String url, Map<String, String> headers, Callback callbackListener);

}
