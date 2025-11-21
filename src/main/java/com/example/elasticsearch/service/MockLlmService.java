package com.example.elasticsearch.service;

import com.example.elasticsearch.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mock LLM 서비스 (테스트용)
 * Claude API 없이도 Agent를 테스트할 수 있도록 간단한 응답 생성
 */
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmService implements LlmService {
    private static final Logger logger = LoggerFactory.getLogger(MockLlmService.class);
    
    @Override
    public String generateResponse(String question, String context, List<Product> products) {
        logger.info("🎭 Mock LLM 응답 생성 중...");
        
        // 간단한 규칙 기반 응답
        if (products == null || products.isEmpty()) {
            return generateNoResultResponse(question);
        }
        
        return generateProductResponse(question, products);
    }
    
    /**
     * 검색 결과가 없을 때 응답
     */
    private String generateNoResultResponse(String question) {
        return String.format(
            "안녕하세요! 송그랜트님의 질문 '%s'에 대한 상품을 찾지 못했습니다.\n\n" +
            "다른 키워드로 검색해보시거나, 구체적인 제품명을 알려주시면 더 정확한 결과를 찾아드리겠습니다.\n\n" +
            "💡 예시:\n" +
            "- '삼성 노트북 추천해줘'\n" +
            "- 'LG 제품 찾아줘'\n" +
            "- '10만원대 상품 보여줘'",
            question
        );
    }
    
    /**
     * 검색 결과가 있을 때 응답
     */
    private String generateProductResponse(String question, List<Product> products) {
        StringBuilder response = new StringBuilder();
        
        // 인사말
        response.append("안녕하세요, 송그랜트! 웬즈데이가 찾은 상품을 알려드립니다.\n\n");
        
        // 검색 결과 요약
        response.append(String.format("'%s' 관련하여 총 %d개의 상품을 찾았습니다:\n\n", 
            question, products.size()));
        
        // 상품 목록 (상위 3개만 상세 설명)
        int displayCount = Math.min(3, products.size());
        for (int i = 0; i < displayCount; i++) {
            Product product = products.get(i);
            response.append(String.format("%d. **%s**\n", i + 1, product.getName()));
            response.append(String.format("   - 카테고리: %s\n", product.getCategory()));
            if (product.getPrice() != null) {
                response.append(String.format("   - 가격: %,.0f원\n", product.getPrice()));
            }
            
            if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                response.append(String.format("   - 설명: %s\n", product.getDescription()));
            }
            
            if (product.getScore() != null) {
                response.append(String.format("   - 매칭도: %.1f%%\n", product.getScore() * 100));
            }
            
            response.append("\n");
        }
        
        // 추가 상품이 있으면 언급
        if (products.size() > displayCount) {
            response.append(String.format("그 외 %d개의 관련 상품이 더 있습니다.\n\n", 
                products.size() - displayCount));
        }
        
        // 추천 멘트 추가 (카테고리 기반)
        String category = products.get(0).getCategory();
        if (category != null) {
            response.append(generateRecommendation(category, products.get(0).getName()));
        }
        
        response.append("\n추가로 궁금하신 점이 있으시면 언제든 물어보세요! 😊");
        
        return response.toString();
    }
    
    /**
     * 카테고리별 추천 멘트 생성
     */
    private String generateRecommendation(String category, String productName) {
        switch (category.toLowerCase()) {
            case "노트북":
            case "laptop":
                return "💻 노트북을 찾고 계시는군요! " + productName + "은(는) 성능과 휴대성이 뛰어난 제품입니다.\n";
            
            case "스마트폰":
            case "smartphone":
                return "📱 스마트폰을 찾고 계시는군요! " + productName + "은(는) 최신 기능을 갖춘 인기 제품입니다.\n";
            
            case "태블릿":
            case "tablet":
                return "📱 태블릿을 찾고 계시는군요! " + productName + "은(는) 멀티미디어와 업무에 적합한 제품입니다.\n";
            
            case "이어폰":
            case "earbuds":
                return "🎧 이어폰을 찾고 계시는군요! " + productName + "은(는) 음질과 착용감이 우수한 제품입니다.\n";
            
            default:
                return "✨ " + productName + "은(는) 고객 만족도가 높은 추천 제품입니다.\n";
        }
    }
}

