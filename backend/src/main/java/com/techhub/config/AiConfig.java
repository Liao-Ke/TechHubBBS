package com.techhub.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * AI 服务配置
 * 读取 application.yml 中 ai.* 配置项
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    /** AI 服务提供商（openai / custom） */
    private String provider;

    /** API 密钥 */
    private String apiKey;

    /** API 基础地址 */
    private String apiUrl;

    /** 模型名称 */
    private String model;

    /** 请求超时时间（秒） */
    private int timeout;

    /** 最大重试次数 */
    private int maxRetries;

    /**
     * 创建 AI 调用的 RestTemplate，10s 连接与读取超时。
     */
    @Bean
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout * 1000);
        factory.setReadTimeout(timeout * 1000);
        return new RestTemplate(factory);
    }

    /**
     * 启动时校验 AI 配置是否完整。
     * API 密钥缺失时仅记录警告，不会阻止应用启动。
     */
    @PostConstruct
    public void validateConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI_API_KEY 未配置！AI 摘要生成和问答功能将不可用。" +
                     "请设置环境变量 AI_API_KEY 或修改 application.yml 中的 ai.api-key 配置。");
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            log.warn("AI_API_URL 未配置，使用默认值: https://api.openai.com/v1");
        }
    }
}
