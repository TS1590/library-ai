package com.example.library.service.ai;

import com.example.library.config.DeepSeekProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/**
 * DeepSeek API 客户端（兼容 OpenAI Chat Completions 格式）
 * 支持普通对话调用和 Function Calling
 *
 * 当前使用阿里云百炼平台的通义千问模型
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    @Autowired
    private DeepSeekProperties config;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 普通对话调用（非流式）
     */
    public String call(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userMessage);
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getBaseUrl() + "/v1/chat/completions",
                    HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            String msg = e.getMessage();
            log.error("DeepSeek API 调用失败: {}", msg);
            return "抱歉，AI服务暂时不可用 [" + e.getClass().getSimpleName() + "]";
        }
    }

    /**
     * 带工具定义的对话调用
     * @return JsonNode 原始响应（可能包含 tool_calls 或 text content）
     */
    public JsonNode callWithTools(String systemPrompt, List<Map<String, Object>> tools,
                                   List<Map<String, Object>> messages) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModel());
            body.put("max_tokens", config.getMaxTokens());
            body.put("temperature", config.getTemperature());

            // 系统消息 + 消息列表
            List<Map<String, Object>> allMsgs = new ArrayList<>();
            Map<String, Object> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            allMsgs.add(sysMsg);
            allMsgs.addAll(messages);
            body.put("messages", allMsgs);

            // 工具定义
            body.put("tools", tools);
            body.put("tool_choice", "auto");

            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getBaseUrl() + "/v1/chat/completions",
                    HttpMethod.POST, entity, String.class);

            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Function Calling 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String systemPrompt, String userMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("max_tokens", config.getMaxTokens());
        body.put("temperature", config.getTemperature());

        List<Map<String, String>> messages = new ArrayList<>();

        // 系统提示词
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        // 用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        body.put("messages", messages);
        return body;
    }

    /**
     * 构建HTTP请求头
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Ollama 本地服务不需要 Authorization，但保留以兼容云端 API
        String key = config.getApiKey();
        if (key != null && !key.isEmpty() && !key.equals("ollama")) {
            headers.set("Authorization", "Bearer " + key);
        }
        return headers;
    }
}
