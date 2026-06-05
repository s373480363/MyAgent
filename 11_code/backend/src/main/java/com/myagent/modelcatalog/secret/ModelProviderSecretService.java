package com.myagent.modelcatalog.secret;

/**
 * 模型供应商密钥服务。
 */
public interface ModelProviderSecretService {

    /**
     * 加密 API Key。
     *
     * @param apiKey 明文
     * @return 密文
     */
    String encrypt(String apiKey);

    /**
     * 解密 API Key。
     *
     * @param ciphertext 密文
     * @return 明文
     */
    String decrypt(String ciphertext);

    /**
     * 生成展示掩码。
     *
     * @param apiKey 明文
     * @return 掩码
     */
    String mask(String apiKey);

    /**
     * 判断是否已配置密钥。
     *
     * @param ciphertext 密文
     * @return 已配置时返回 true
     */
    boolean isConfigured(String ciphertext);
}
