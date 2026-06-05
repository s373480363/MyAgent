package com.myagent.modelcatalog.secret;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 模型供应商密钥编解码工具。
 */
public final class ModelProviderSecretCodec {

    /**
     * AES 密钥字节长度。
     */
    private static final int KEY_LENGTH_BYTES = 32;

    /**
     * GCM 随机向量字节长度。
     */
    private static final int IV_LENGTH_BYTES = 12;

    /**
     * GCM 认证标签位数。
     */
    private static final int TAG_LENGTH_BITS = 128;

    /**
     * 随机数生成器。
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 私有构造器。
     */
    private ModelProviderSecretCodec() {
    }

    /**
     * 校验并解析 Base64 主密钥。
     *
     * @param base64SecretKey Base64 主密钥
     * @return 原始密钥字节
     */
    public static byte[] decodeMasterKey(String base64SecretKey) {
        if (base64SecretKey == null || base64SecretKey.isBlank()) {
            throw new IllegalStateException("AGENT_STUDIO_SECRET_KEY 未配置。");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64SecretKey.trim());
            if (keyBytes.length != KEY_LENGTH_BYTES) {
                throw new IllegalStateException("AGENT_STUDIO_SECRET_KEY 必须是 Base64 编码的 32 字节随机值。");
            }
            return keyBytes;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("AGENT_STUDIO_SECRET_KEY 必须是合法 Base64 字符串。", exception);
        }
    }

    /**
     * 使用主密钥加密 API Key。
     *
     * @param apiKey API Key 明文
     * @param keyBytes 主密钥字节
     * @return Base64 密文
     */
    public static String encrypt(String apiKey, byte[] keyBytes) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("模型供应商 API Key 加密失败。", exception);
        }
    }

    /**
     * 使用主密钥解密 API Key。
     *
     * @param ciphertext Base64 密文
     * @param keyBytes 主密钥字节
     * @return API Key 明文
     */
    public static String decrypt(String ciphertext, byte[] keyBytes) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return "";
        }
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext.trim());
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("模型供应商 API Key 密文格式不正确。");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] encrypted = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(payload, IV_LENGTH_BYTES, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("模型供应商 API Key 解密失败。", exception);
        }
    }

    /**
     * 生成展示用密钥掩码。
     *
     * @param apiKey API Key 明文
     * @return 掩码
     */
    public static String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 4) {
            return "已配置";
        }
        return trimmed.substring(0, Math.min(3, trimmed.length())) + "-..." + trimmed.substring(trimmed.length() - 4);
    }
}
