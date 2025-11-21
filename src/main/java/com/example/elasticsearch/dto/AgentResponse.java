package com.example.elasticsearch.dto;

import com.example.elasticsearch.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent 채팅 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {
    /**
     * 사용자 질문
     */
    private String question;
    
    /**
     * AI 응답
     */
    private String answer;
    
    /**
     * 검색된 상품 목록
     */
    private List<Product> products;
    
    /**
     * RAG 컨텍스트 (디버그용)
     */
    private String context;
    
    /**
     * 응답 생성 시간 (ms)
     */
    private Long responseTime;
}

