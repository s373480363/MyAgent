package com.myagent.modelcatalog.secret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 默认模型供应商密钥服务。
 */
@Component
public class DefaultModelProviderSecretService implements ModelProviderSecretService {

    /**
     * Base64 主密钥配置。
     */
    private final String secretKey;

    /**
     * 构造密钥服务。
     *
     * @param secretKey Base64 主密钥配置
     */
    public DefaultModelProviderSecretService(@Value("${AGENT_STUDIO_SECRET_KEY:}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String encrypt(String apiKey) {
        return ModelProviderSecretCodec.encrypt(apiKey, requiredMasterKey());
    }

    @Override
    public String decrypt(String ciphertext) {
        return ModelProviderSecretCodec.decrypt(ciphertext, requiredMasterKey());
    }

    @Override
    public String mask(String apiKey) {
        return ModelProviderSecretCodec.mask(apiKey);
    }

    @Override
    public boolean isConfigured(String ciphertext) {
        return ciphertext != null && !ciphertext.isBlank();
    }

    /**
     * 读取主密钥。
     *
     * @return 主密钥字节
     */
    private byte[] requiredMasterKey() {
        return ModelProviderSecretCodec.decodeMasterKey(secretKey);
    }
}
