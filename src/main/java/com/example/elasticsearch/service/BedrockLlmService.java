package com.example.elasticsearch.service;

import com.example.elasticsearch.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AWS Bedrock Claude 연동 LLM 서비스
 * Spring AI Bedrock Converse API를 통해 Claude 모델 사용
 */
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "bedrock")
public class BedrockLlmService implements LlmService {
    private static final Logger logger = LoggerFactory.getLogger(BedrockLlmService.class);
    
    private final ChatModel chatModel;
    
    @Autowired
    public BedrockLlmService(ChatModel chatModel) {
        this.chatModel = chatModel;
        logger.info("🚀 AWS Bedrock LLM Service initialized (Converse API)");
    }
    
    @Override
    public String generateResponse(String question, String context, List<Product> products) {
        logger.info("🤖 AWS Bedrock Claude API 호출 중 (Converse API)...");
        
        try {
            // 시스템 메시지
            String systemPrompt = """
                당신은 친절하고 전문적인 상담 AI Agent입니다.
                사용자는 '송그랜트'이고, 당신은 '웬즈데이'입니다.
                상품 추천 및 검색을 도와주는 역할을 합니다.
                
                응답 가이드:
                1. 검색된 상품 정보를 바탕으로 친절하게 답변해주세요.
                2. 상품의 주요 특징과 장점을 강조해주세요.
                3. 사용자에게 도움이 되는 추가 정보를 제공해주세요.
                4. 자연스럽고 대화하는 듯한 톤으로 작성해주세요.
                5. 이모지를 적절히 활용하여 친근하게 작성해주세요.
                """;
            
            // 사용자 메시지 구성
            String userPrompt = buildUserPrompt(question, context, products);
            
            // Spring AI Converse API를 통한 Bedrock 호출
            SystemMessage systemMessage = new SystemMessage(systemPrompt);
            UserMessage userMessage = new UserMessage(userPrompt);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            
            String response = chatModel.call(prompt).getResult().getOutput().getContent();
            
            logger.info("✅ AWS Bedrock Claude 응답 성공 (Converse API)");
            return response;
            
        } catch (Exception e) {
            logger.error("❌ AWS Bedrock API 호출 실패: {}", e.getMessage(), e);
            return "죄송합니다. AI 응답 생성 중 오류가 발생했습니다: " + e.getMessage() + 
                   "\n\nMock 모드로 전환하시려면 application.properties에서 llm.provider=mock으로 설정해주세요.";
        }
    }
    
    /**
     * 사용자 프롬프트 생성
     */
    private String buildUserPrompt(String question, String context, List<Product> products) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("=== 사용자 질문 ===\n");
        prompt.append(question).append("\n\n");
        
        if (products != null && !products.isEmpty()) {
            prompt.append("=== 검색된 상품 정보 ===\n");
            prompt.append(context).append("\n\n");
            
            prompt.append("=== 상품 상세 ===\n");
            for (int i = 0; i < Math.min(5, products.size()); i++) {
                Product p = products.get(i);
                prompt.append(String.format("%d. %s\n", i + 1, p.getName()));
                prompt.append(String.format("   - 카테고리: %s\n", p.getCategory()));
                if (p.getPrice() != null) {
                    prompt.append(String.format("   - 가격: %,.0f원\n", p.getPrice()));
                }
                if (p.getDescription() != null) {
                    prompt.append(String.format("   - 설명: %s\n", p.getDescription()));
                }
                prompt.append("\n");
            }
        } else {
            prompt.append("검색된 상품이 없습니다. 다른 키워드를 추천해주세요.\n");
        }
        
        prompt.append("\n송그랜트에게 도움이 되는 답변을 작성해주세요:");
        
        return prompt.toString();
    }
}
