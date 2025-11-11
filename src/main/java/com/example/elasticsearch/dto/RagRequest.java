package com.example.elasticsearch.dto;

/**
 * RAG 검색 요청 DTO
 */
public class RagRequest {
    
    private String query;  // 사용자 질문
    private Integer topK;  // 반환할 상품 수 (기본값: 5)
    
    public RagRequest() {
        this.topK = 5;  // 기본값
    }
    
    public RagRequest(String query, Integer topK) {
        this.query = query;
        this.topK = topK != null ? topK : 5;
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public Integer getTopK() {
        return topK;
    }
    
    public void setTopK(Integer topK) {
        this.topK = topK != null ? topK : 5;
    }
    
    @Override
    public String toString() {
        return "RagRequest{" +
                "query='" + query + '\'' +
                ", topK=" + topK +
                '}';
    }
}

