package com.iot.qa.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 密码加密工具：为每个用户生成随机盐，存储格式为 {@code 盐值:哈希值}。
 *
 * <p>相比明文或固定盐 MD5，随机盐可以抵御彩虹表攻击；校验时用同一盐值重新计算哈希再比较。</p>
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PasswordUtil() {
    }

    /** 生成 16 位十六进制随机盐 */
    public static String randomSalt() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    /** 加密，返回 {@code salt:hash} */
    public static String encode(String rawPassword) {
        String salt = randomSalt();
        return salt + ":" + sha256(salt + rawPassword);
    }

    /** 校验原始密码是否匹配已存储的密文 */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        int idx = encodedPassword.indexOf(':');
        if (idx <= 0) {
            return false;
        }
        String salt = encodedPassword.substring(0, idx);
        String hash = encodedPassword.substring(idx + 1);
        return sha256(salt + rawPassword).equals(hash);
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            chars[i * 2] = HEX[(bytes[i] >> 4) & 0x0F];
            chars[i * 2 + 1] = HEX[bytes[i] & 0x0F];
        }
        return new String(chars);
    }
}