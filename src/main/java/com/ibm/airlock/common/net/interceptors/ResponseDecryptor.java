package com.ibm.airlock.common.net.interceptors;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Decryptor;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.annotation.Nullable;


/**
 * Created by Denis Voloshin on 30/10/2017.
 */

public class ResponseDecryptor implements Interceptor {

    private static String TAG = "ResponseDecryptor";

    @Nullable
    private String encryptionKey;

    public ResponseDecryptor(@Nullable String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        MediaType contentType = response.body().contentType();
        byte[] body = response.body().bytes();
        if (Decryptor.isDecryptionRequired(body) && encryptionKey != null) {
            try {
                body = Decryptor.decryptAES(body, encryptionKey.getBytes());
            } catch (GeneralSecurityException e) {
                ResponseBody.create(contentType, e.getMessage());
                Logger.log.e(TAG, e.getMessage());
                return response.newBuilder().body(ResponseBody.create(contentType, e.getMessage())).code(430).build();
            }
        }
        return response.newBuilder().body(ResponseBody.create(contentType, body)).build();
    }
}
