package com.ufs.core.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 签名（服务端全能力鉴权，对齐契约 §3）。
 * 规范串 canonical = METHOD + "\n" + PATH + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY_SHA256。
 */
public final class HmacSigner {

    private HmacSigner() {
    }

    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sign(String appSecret, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("hmac sign failed", e);
        }
    }

    public static String canonical(String method, String path, long timestamp, String nonce, String body) {
        String bodyHash = body == null ? "" : sha256Hex(body);
        return method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + bodyHash;
    }

    public static boolean verify(String appSecret, String canonical, String signature) {
        return sign(appSecret, canonical).equalsIgnoreCase(signature);
    }
}
