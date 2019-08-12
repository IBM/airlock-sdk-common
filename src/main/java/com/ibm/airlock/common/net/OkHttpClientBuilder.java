package com.ibm.airlock.common.net;

import okhttp3.OkHttpClient;


/**
 * Created by Denis Voloshin on 02/11/2017.
 */

public interface OkHttpClientBuilder {
    public OkHttpClient create(String encryptionKey);
    public OkHttpClient create();
}
