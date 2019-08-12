package com.ibm.airlock;

import com.ibm.airlock.common.net.OkHttpClientBuilder;
import com.ibm.airlock.common.net.interceptors.ResponseDecryptor;
import com.ibm.airlock.common.net.interceptors.ResponseExtractor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;


/**
 * @author Denis Voloshin
 */

public class DefaultHttpClientBuilder implements OkHttpClientBuilder {

    public DefaultHttpClientBuilder() {
        super();
    }

    @Override
    public OkHttpClient create(String encryptionKey) {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .cache(null)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS).
                        addInterceptor(new ResponseExtractor()).
                        addInterceptor(new ResponseDecryptor(encryptionKey));
        return client.build();
    }

    @Override
    public OkHttpClient create() {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .cache(null)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS);
        return client.build();
    }
}
