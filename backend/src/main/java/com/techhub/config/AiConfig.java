package com.techhub.config;

import lombok.Data;
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
}
