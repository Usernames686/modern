package com.jsy.crmeb.modern.common.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public final class LegacyPasswordUtil {
    private LegacyPasswordUtil() {
    }

    public static String encryptPassword(String password, String account) {
        try {
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(buildDesKey(account), "DES"));
            return Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt legacy CRMEB password", e);
        }
    }

    private static byte[] buildDesKey(String key) {
        byte[] result = new byte[8];
        Arrays.fill(result, (byte) 0x01);
        byte[] source = key == null ? new byte[0] : key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
        return result;
    }
}
