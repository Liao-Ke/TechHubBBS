package com.techhub.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AiConfig.validateConfig() 单元测试。
 * 验证 @PostConstruct 方法非阻塞行为：apiKey 为空时仅记录 WARN，不抛出异常。
 */
class AiConfigTest {

    private AiConfig aiConfig;

    @BeforeEach
    void setUp() {
        aiConfig = new AiConfig();
    }

    @Test
    @DisplayName("validateConfig should NOT throw when apiKey is null")
    void validateConfigShouldNotThrowOnNullApiKey() {
        aiConfig.setApiKey(null);
        aiConfig.setApiUrl("https://api.openai.com/v1");
        assertDoesNotThrow(() -> aiConfig.validateConfig());
    }

    @Test
    @DisplayName("validateConfig should NOT throw when apiKey is blank")
    void validateConfigShouldNotThrowOnBlankApiKey() {
        aiConfig.setApiKey("");
        aiConfig.setApiUrl("https://api.openai.com/v1");
        assertDoesNotThrow(() -> aiConfig.validateConfig());
    }

    @Test
    @DisplayName("validateConfig should NOT throw when apiKey and apiUrl are both empty")
    void validateConfigShouldNotThrowOnEmptyConfig() {
        aiConfig.setApiKey("");
        aiConfig.setApiUrl("");
        assertDoesNotThrow(() -> aiConfig.validateConfig());
    }

    @Test
    @DisplayName("validateConfig should NOT throw when all config is valid")
    void validateConfigShouldNotThrowOnValidConfig() {
        aiConfig.setApiKey("sk-valid-key");
        aiConfig.setApiUrl("https://api.openai.com/v1");
        assertDoesNotThrow(() -> aiConfig.validateConfig());
    }
}
