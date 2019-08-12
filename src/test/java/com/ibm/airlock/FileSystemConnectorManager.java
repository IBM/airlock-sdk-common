package com.ibm.airlock;

import com.ibm.airlock.common.net.RemoteConfigurationAsyncFetcher;
import com.ibm.airlock.common.net.ConnectionManager;

import java.util.Map;

import okhttp3.Callback;
import okhttp3.Headers;

/**
 * @author Denis Voloshin
 */
public class FileSystemConnectorManager implements ConnectionManager {


    public  FileSystemConnectorManager(){}
    @Override
    public RemoteConfigurationAsyncFetcher.DataProviderType getDataProviderType() {
        return null;
    }

    @Override
    public void setDataProviderType(RemoteConfigurationAsyncFetcher.DataProviderType dataProviderType) {

    }

    @Override
    public void sendPostRequestAsJson(String url, Callback callbackListener, String json) {

    }

    @Override
    public void sendRequest(String url, Callback callbackListener, Headers headers) {

    }

    @Override
    public void sendRequest(String url, Callback callbackListener) {

    }

    @Override
    public void sendRequest(String url, Map<String, String> headers, Callback callbackListener) {

    }
}
