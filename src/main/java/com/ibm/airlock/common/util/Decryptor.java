package com.ibm.airlock.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;


/**
 * Created by Denis Voloshin on 31/10/2017.
 */

public class Decryptor {

    private static final byte[] magic = new byte[] { 0x54, 0x39, 0x71, 0x12 };
    private static final byte[] version = new byte[] { 0x00, 0x01 }; // for future use

    private static final int blockSize = 16;
    private static final int headerSize = magic.length + version.length + blockSize;

    public static byte[] decryptAES(byte[] encrypted,byte[] key) throws GeneralSecurityException {
        if (encrypted.length <= headerSize) {
            throw new GeneralSecurityException("input size is too short");
        }

        // extract initialization vector and encrypted model buffer
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
