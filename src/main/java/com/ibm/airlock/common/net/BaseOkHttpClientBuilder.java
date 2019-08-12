package com.ibm.airlock.common.net;

import java.util.concurrent.TimeUnit;

import com.ibm.airlock.common.net.interceptors.ResponseDecryptor;
import com.ibm.airlock.common.net.interceptors.ResponseExtractor;
import okhttp3.OkHttpClient;

import javax.annotation.Nullable;


/**
 * Created by Denis Voloshin on 05/11/2017.
 */

public class BaseOkHttpClientBuilder implements OkHttpClientBuilder {
    @Override
    public OkHttpClient create(@Nullable String encryptionKey) {

        if(encryptionKey == null || encryptionKey.isEmpty()){
            return create();
        }

        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .cache(null)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(new ResponseExtractor())
                .addInterceptor(new ResponseDecryptor(encryptionKey));
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
