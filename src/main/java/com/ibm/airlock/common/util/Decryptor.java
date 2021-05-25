package com.ibm.airlock.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by Denis Voloshin on 31/10/2017.
 */

public class Decryptor {

    static byte[] magic = new byte[] { 0x54, 0x39, 0x71, 0x12 };
    static byte[] version = new byte[] { 0x00, 0x01 }; // for future use

    static final int blockSize = 16;
    static final int headerSize = magic.length + version.length + blockSize;

    private static byte[] getMagicNumber() {
        return magic;
    }


    private static String getPassphraseSize16(String key) {
        char controlChar = '\u0014';
        String key16 = key + controlChar;
        if (key16.length() < 16) {
            while (key16.length() < 16) {
                key16 += key + controlChar;
            }
        }
        if (key16.length() > 16) {
            key16 = key16.substring(key16.length() - 16, key16.length());
        }
        return key16;
    }


    public static byte[] decryptAES(byte[] encrypted,byte[] key) throws GeneralSecurityException {
        if (encrypted.length <= headerSize) {
            throw new GeneralSecurityException("input size is too short");
        }

        // extract initialization vector and encrypted data buffer
        byte[] ivBytes = Arrays.copyOfRange(encrypted, magic.length + version.length, headerSize);
        byte[] data = Arrays.copyOfRange(encrypted, headerSize, encrypted.length);

        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        return cipher.doFinal(data);
    }



    public static byte[] encryptAES(byte[] plain,byte[] key) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        SecureRandom randomSecureRandom = SecureRandom.getInstance("SHA1PRNG");

        byte[] ivBytes = new byte[cipher.getBlockSize()];
        randomSecureRandom.nextBytes(ivBytes);
        if (ivBytes.length != blockSize) {
            throw new GeneralSecurityException("unexpected cypher block size");
        }

        SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
        byte[] encrypted = cipher.doFinal(plain);

        // prefix the encrypted bytes with the magic, version, and initialization vector
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(magic);
        out.write(version);
        out.write(ivBytes);
        out.write(encrypted);
        return out.toByteArray();
    }


    public static boolean isDecryptionRequired(byte[] message) {
        return Arrays.equals(Arrays.copyOfRange(message, 0, magic.length), magic);
    }

}
