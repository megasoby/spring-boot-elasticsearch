package com.example.elasticsearch.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 히스토리 엔티티
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory {
    /**
     * 대화 ID
     */
    private String id;
    
    /**
     * 사용자 ID (추후 인증 시스템 연동 시 사용)
     */
    private String userId;
    
    /**
     * 사용자 질문
     */
    private String question;
    
    /**
     * AI 응답
     */
    private String answer;
    
    /**
     * 검색된 상품 개수
     */
    private Integer productCount;
    
    /**
     * 응답 생성 시간 (ms)
     */
    private Long responseTime;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 생성자
     */
    public ChatHistory(String question, String answer, Integer productCount, Long responseTime) {
        this.id = UUID.randomUUID().toString();
        this.userId = "default"; // 추후 인증 시스템 연동
        this.question = question;
        this.answer = answer;
        this.productCount = productCount;
        this.responseTime = responseTime;
        this.createdAt = LocalDateTime.now();
    }
}

