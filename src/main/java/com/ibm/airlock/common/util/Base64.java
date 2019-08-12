package com.ibm.airlock.common.util;

/**
 * Created by Denis Voloshin on 02/11/2017.
 */

public class Base64 {

    private static Base64Decoder decoder;

    public static void init(Base64Decoder decoder) {
        Base64.decoder = decoder;
    }

    public static byte[] decode(String str) {
        return decoder.decode(str);
    }
}
