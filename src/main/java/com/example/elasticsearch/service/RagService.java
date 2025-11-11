package com.example.elasticsearch.service;

import com.example.elasticsearch.dto.RagRequest;
import com.example.elasticsearch.dto.RagResponse;
import com.example.elasticsearch.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG (Retrieval-Augmented Generation) 서비스
 * 벡터 검색 + 컨텍스트 생성
 */
@Service
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    private final VectorSearchService vectorSearchService;
    
    public RagService(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }
    
    /**
     * RAG 검색 실행
     * @param request 검색 요청 (query, topK)
     * @return RAG 응답 (context, products)
     */
    public RagResponse search(RagRequest request) {
        logger.info("RAG 검색 시작: query={}, topK={}", request.getQuery(), request.getTopK());
        
        try {
            // 1. 벡터 검색으로 유사한 상품 찾기
            List<Product> products = vectorSearchService.vectorSearch(
                    request.getQuery(), 
                    request.getTopK()
            );
            
            // 2. 검색 결과를 Claude가 이해할 수 있는 컨텍스트로 변환
            String context = buildContext(request.getQuery(), products);
            
            // 3. 응답 생성
            RagResponse response = new RagResponse(request.getQuery(), context, products);
            
            logger.info("RAG 검색 완료: {}개 상품 발견", products.size());
            return response;
            
        } catch (Exception e) {
            logger.error("RAG 검색 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("RAG 검색 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 검색 결과를 Claude가 사용할 컨텍스트로 변환
     * @param query 사용자 질문
     * @param products 검색된 상품 리스트
     * @return 포맷팅된 컨텍스트 텍스트
     */
    private String buildContext(String query, List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "관련된 상품을 찾지 못했습니다.";
        }
        
        StringBuilder context = new StringBuilder();
        context.append(String.format("질문: %s\n\n", query));
        context.append(String.format("검색된 유사 상품 %d개:\n\n", products.size()));
        
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            context.append(String.format("%d. %s\n", i + 1, product.getName()));
            
            if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                context.append(String.format("   설명: %s\n", product.getDescription()));
            }
            
            if (product.getPrice() != null) {
                context.append(String.format("   가격: ₩%,d\n", product.getPrice().intValue()));
            }
            
            if (product.getCategory() != null) {
                context.append(String.format("   카테고리: %s\n", product.getCategory()));
            }
            
            if (product.getStock() != null) {
                context.append(String.format("   재고: %d개\n", product.getStock()));
            }
            
            // 유사도 점수가 있으면 표시
            if (product.getScore() != null) {
                context.append(String.format("   유사도: %.4f\n", product.getScore()));
            }
            
            context.append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * 간단한 컨텍스트 생성 (상품명만)
     * @param products 검색된 상품 리스트
     * @return 간단한 컨텍스트
     */
    public String buildSimpleContext(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "관련 상품 없음";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("유사 상품: ");
        
        for (int i = 0; i < products.size(); i++) {
            if (i > 0) context.append(", ");
            context.append(products.get(i).getName());
        }
        
        return context.toString();
    }
}

