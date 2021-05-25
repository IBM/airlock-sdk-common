package com.ibm.airlock.common.net.interceptors;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import com.ibm.airlock.common.log.Logger;
import com.weather.airlock.sdk.util.Gzip;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


/**
 * Created by Denis Voloshin on 30/10/2017.
 */

public class ResponseExtractor implements Interceptor {

    private static String TAG = "ResponseExtractor";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        MediaType contentType = response.body().contentType();
        byte[] body = response.body().bytes();
        if(isGZipped(body)) {
            try {
                body = Gzip.decompress(body);
            } catch (IOException e) {
                ResponseBody.create(contentType, e.getMessage());
                Logger.log.e(TAG, e.getMessage());
                return response.newBuilder().body(ResponseBody.create(contentType, e.getMessage())).code(430).build();
            }
        }
        return response.newBuilder().body(ResponseBody.create(contentType, body)).build();
    }

    /**
     * Checks if an input stream is gzipped.
     *
     * @param in
     * @return
     */
    public static boolean isGZipped(byte[] in) {
        byte[] twoBytes = Arrays.copyOfRange(in, 0, 2);
        int magic = 0;
        magic = twoBytes[0] & 0xff | ((twoBytes[1] << 8) & 0xff00);
        return magic == GZIPInputStream.GZIP_MAGIC;
    }
}
