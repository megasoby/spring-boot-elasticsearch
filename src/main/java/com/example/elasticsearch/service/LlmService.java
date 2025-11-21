package com.example.elasticsearch.service;

import com.example.elasticsearch.entity.Product;

import java.util.List;

/**
 * LLM (Large Language Model) 서비스 인터페이스
 * Claude, GPT 등 다양한 LLM을 쉽게 교체할 수 있도록 추상화
 */
public interface LlmService {
    
    /**
     * 사용자 질문에 대한 AI 응답 생성
     * 
     * @param question 사용자 질문
     * @param context RAG 검색 결과 컨텍스트
     * @param products 검색된 상품 목록
     * @return AI 응답
     */
    String generateResponse(String question, String context, List<Product> products);
}

