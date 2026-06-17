package com.example.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AI 智能体配置类
 */
@Configuration
public class AIConfig {

    /**
     * HTTP 客户端（用于调用 DeepSeek API）
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
