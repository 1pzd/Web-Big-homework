package com.iot.qa.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.qa.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 大模型回答策略：调用 OpenAI 兼容的 /chat/completions 接口。
 *
 * <p>接口地址、密钥、模型名都来自 sys_config，管理员可在后台随时切换。
 * 未配置密钥时返回 null，由 {@link AnswerEngine} 自动降级到本地检索策略，
 * 保证「没有网络或没有密钥时网站依然可用」。</p>
 */
@Slf4j
@Component
public class LlmStrategy implements AnswerStrategy {

    private static final String SYSTEM_PROMPT =
            "你是「智问」智能问答平台的助手。请用简洁、准确、友好的中文回答用户问题，"
                    + "回答控制在 300 字以内；如果问题超出你的知识范围，请如实说明并建议用户换个问法。";

    private final ConfigService configService;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    public LlmStrategy(ConfigService configService, RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String mode() {
        return "llm";
    }

    @Override
    public AnswerResult answer(String question) {
        String apiKey = configService.get("ai.llm.apiKey", "");
        if (apiKey.isBlank()) {
            log.debug("未配置大模型 API Key，跳过 LLM 策略");
            return null;
        }

        String baseUrl = configService.get("ai.llm.baseUrl", "https://api.openai.com/v1");
        String model = configService.get("ai.llm.model", "gpt-4o-mini");
        String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", question)
                ),
                "temperature", 0.7,
                "max_tokens", 800
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);
            String answer = extractContent(response.getBody());
            return answer == null ? null : AnswerResult.ofLlm(answer);
        } catch (Exception e) {
            log.warn("调用大模型接口失败: {}", e.getMessage());
            return null;
        }
    }

    /** 解析 choices[0].message.content */
    private String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }
            String content = choices.get(0).path("message").path("content").asText("");
            return content.isBlank() ? null : content.trim();
        } catch (Exception e) {
            log.warn("解析大模型响应失败: {}", e.getMessage());
            return null;
        }
    }
}