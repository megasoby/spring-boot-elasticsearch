package com.example.elasticsearch.service;

import com.example.elasticsearch.entity.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Claude API 연동 LLM 서비스
 * Anthropic Claude API를 사용하여 실제 AI 응답 생성
 */
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "claude")
public class ClaudeLlmService implements LlmService {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeLlmService.class);
    
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    
    @Value("${claude.api.key:}")
    private String apiKey;
    
    @Value("${claude.model:claude-3-sonnet-20240229}")
    private String model;
    
    @Value("${claude.max-tokens:1024}")
    private int maxTokens;
    
    @Value("${claude.temperature:0.7}")
    private double temperature;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public ClaudeLlmService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String generateResponse(String question, String context, List<Product> products) {
        logger.info("🤖 Claude API 호출 중...");
        
        try {
            // API 키 검증
            if (apiKey == null || apiKey.isEmpty()) {
                logger.error("❌ Claude API 키가 설정되지 않았습니다.");
                return "죄송합니다. Claude API 키가 설정되지 않았습니다. application.properties에서 claude.api.key를 설정해주세요.";
            }
            
            // 프롬프트 생성
            String prompt = buildPrompt(question, context, products);
            
            // API 요청 생성
            String requestBody = buildRequestBody(prompt);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CLAUDE_API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            // API 호출
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            // 응답 처리
            if (response.statusCode() == 200) {
                String answer = parseResponse(response.body());
                logger.info("✅ Claude API 응답 성공");
                return answer;
            } else {
                logger.error("❌ Claude API 오류: status={}, body={}", 
                    response.statusCode(), response.body());
                return String.format(
                    "죄송합니다. Claude API 호출 중 오류가 발생했습니다. (Status: %d)\n\n" +
                    "Mock 모드로 전환하시려면 application.properties에서 llm.provider=mock으로 설정해주세요.",
                    response.statusCode()
                );
            }
            
        } catch (Exception e) {
            logger.error("❌ Claude API 호출 실패: {}", e.getMessage(), e);
            return "죄송합니다. AI 응답 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
    
    /**
     * Claude용 프롬프트 생성
     */
    private String buildPrompt(String question, String context, List<Product> products) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 친절하고 전문적인 상담 AI Agent입니다.\n");
        prompt.append("사용자는 '송그랜트'이고, 당신은 '웬즈데이'입니다.\n");
        prompt.append("상품 추천 및 검색을 도와주는 역할을 합니다.\n\n");
        
        prompt.append("=== 사용자 질문 ===\n");
        prompt.append(question).append("\n\n");
        
        if (products != null && !products.isEmpty()) {
            prompt.append("=== 검색된 상품 정보 ===\n");
            prompt.append(context).append("\n\n");
        }
        
        prompt.append("=== 응답 가이드 ===\n");
        prompt.append("1. 검색된 상품 정보를 바탕으로 친절하게 답변해주세요.\n");
        prompt.append("2. 상품의 주요 특징과 장점을 강조해주세요.\n");
        prompt.append("3. 사용자에게 도움이 되는 추가 정보를 제공해주세요.\n");
        prompt.append("4. 자연스럽고 대화하는 듯한 톤으로 작성해주세요.\n");
        prompt.append("5. 이모지를 적절히 활용하여 친근하게 작성해주세요.\n\n");
        
        prompt.append("송그랜트에게 도움이 되는 답변을 작성해주세요:");
        
        return prompt.toString();
    }
    
    /**
     * Claude API 요청 바디 생성
     */
    private String buildRequestBody(String prompt) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("max_tokens", maxTokens);
            root.put("temperature", temperature);
            
            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            
            root.set("messages", messages);
            
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            logger.error("❌ 요청 바디 생성 실패: {}", e.getMessage());
            throw new RuntimeException("요청 바디 생성 실패", e);
        }
    }
    
    /**
     * Claude API 응답 파싱
     */
    private String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("content").get(0);
            return content.path("text").asText();
        } catch (Exception e) {
            logger.error("❌ 응답 파싱 실패: {}", e.getMessage());
            return "응답 파싱 중 오류가 발생했습니다.";
        }
    }
}

