package com.example.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * DeepSeek API 配置属性
 */
@Component
@ConfigurationProperties(prefix = "ai.deepseek")
public class DeepSeekProperties {

    /** DeepSeek API Key */
    private String apiKey;
    /** API 基础URL */
    private String baseUrl = "https://api.deepseek.com";
    /** 模型名称 */
    private String model = "deepseek-chat";
    /** 最大输出Token数 */
    private Integer maxTokens = 2048;
    /** 随机性参数 0-2 */
    private Double temperature = 0.7;

    @PostConstruct
    public void init() {
        System.out.println("===== DeepSeek Config =====");
        System.out.println("apiKey: " + (apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "NULL"));
        System.out.println("baseUrl: " + baseUrl);
        System.out.println("model: " + model);
        System.out.println("==========================");
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
}
