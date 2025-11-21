package com.example.elasticsearch.service;

import com.example.elasticsearch.dto.AgentRequest;
import com.example.elasticsearch.dto.AgentResponse;
import com.example.elasticsearch.entity.ChatHistory;
import com.example.elasticsearch.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI Agent 서비스
 * RAG 검색과 AI 응답 생성을 통합 관리
 */
@Service
public class AgentService {
    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    
    private final VectorSearchService vectorSearchService;
    private final LlmService llmService;
    private final ChatHistoryService chatHistoryService;
    
    @Value("${agent.rag.enabled:true}")
    private boolean ragEnabled;
    
    public AgentService(VectorSearchService vectorSearchService, 
                       LlmService llmService,
                       ChatHistoryService chatHistoryService) {
        this.vectorSearchService = vectorSearchService;
        this.llmService = llmService;
        this.chatHistoryService = chatHistoryService;
    }
    
    /**
     * 사용자 질문에 대한 AI 응답 생성
     */
    public AgentResponse chat(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        
        logger.info("🤖 Agent 요청: question='{}', topK={}", 
            request.getQuestion(), request.getTopK());
        
        try {
            // 1. RAG 검색
            List<Product> products = null;
            String context = null;
            
            if (ragEnabled) {
                logger.info("📚 RAG 검색 시작...");
                products = vectorSearchService.vectorSearch(
                    request.getQuestion(), 
                    request.getTopK()
                );
                
                // 2. 컨텍스트 생성
                context = buildContext(request.getQuestion(), products);
                logger.info("✅ RAG 검색 완료: {}개 상품 발견", products.size());
            }
            
            // 3. AI 응답 생성
            logger.info("🧠 AI 응답 생성 중...");
            String answer = llmService.generateResponse(
                request.getQuestion(), 
                context, 
                products
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("✅ Agent 응답 완료 ({}ms)", responseTime);
            
            // 대화 히스토리 저장
            ChatHistory history = new ChatHistory(
                request.getQuestion(),
                answer,
                products != null ? products.size() : 0,
                responseTime
            );
            chatHistoryService.save(history);
            
            return new AgentResponse(
                request.getQuestion(),
                answer,
                products,
                context,
                responseTime
            );
            
        } catch (Exception e) {
            logger.error("❌ Agent 처리 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Agent 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    /**
     * RAG 컨텍스트 생성
     */
    private String buildContext(String query, List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "검색 결과가 없습니다.";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("=== 검색 결과 ===\n\n");
        context.append(String.format("질문: %s\n", query));
        context.append(String.format("검색된 상품: %d개\n\n", products.size()));
        
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            context.append(String.format("[상품 %d]\n", i + 1));
            context.append(String.format("- 이름: %s\n", product.getName()));
            context.append(String.format("- 카테고리: %s\n", product.getCategory()));
            
            if (product.getPrice() != null) {
                context.append(String.format("- 가격: %,.0f원\n", product.getPrice()));
            }
            
            if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                context.append(String.format("- 설명: %s\n", product.getDescription()));
            }
            
            if (product.getScore() != null) {
                context.append(String.format("- 유사도: %.4f\n", product.getScore()));
            }
            
            context.append("\n");
        }
        
        return context.toString();
    }
}

