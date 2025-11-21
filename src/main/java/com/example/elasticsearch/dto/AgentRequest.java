package com.example.elasticsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 채팅 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {
    /**
     * 사용자 질문
     */
    private String question;
    
    /**
     * 검색할 상품 개수 (기본값: 5)
     */
    private Integer topK = 5;
}

