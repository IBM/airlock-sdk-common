package com.ibm.airlock.common.net;

import java.util.Map;
import okhttp3.Callback;
import okhttp3.Headers;


/**
 * @author Denis Voloshin
 */
public interface ConnectionManager {

    public RemoteConfigurationAsyncFetcher.DataProviderType getDataProviderType();

    public void setDataProviderType(RemoteConfigurationAsyncFetcher.DataProviderType dataProviderType);

    public void sendPostRequestAsJson(String url, Callback callbackListener, String json);

    public void sendRequest(String url, Callback callbackListener, Headers headers);

    public void sendRequest(String url, Callback callbackListener);

    public void sendRequest(String url, Map<String, String> headers, Callback callbackListener);

}
