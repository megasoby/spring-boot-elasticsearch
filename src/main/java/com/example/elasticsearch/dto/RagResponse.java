package com.example.elasticsearch.dto;

import com.example.elasticsearch.entity.Product;
import java.util.List;

/**
 * RAG 검색 응답 DTO
 * MCP 서버에서 Claude에게 전달할 컨텍스트 포함
 */
public class RagResponse {
    
    private String query;           // 원본 질문
    private String context;         // Claude가 사용할 컨텍스트 (포맷팅된 텍스트)
    private List<Product> products; // 검색된 상품 리스트
    private Integer count;          // 검색 결과 수
    
    public RagResponse() {}
    
    public RagResponse(String query, String context, List<Product> products) {
        this.query = query;
        this.context = context;
        this.products = products;
        this.count = products != null ? products.size() : 0;
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public List<Product> getProducts() {
        return products;
    }
    
    public void setProducts(List<Product> products) {
        this.products = products;
        this.count = products != null ? products.size() : 0;
    }
    
    public Integer getCount() {
        return count;
    }
    
    public void setCount(Integer count) {
        this.count = count;
    }
    
    @Override
    public String toString() {
        return "RagResponse{" +
                "query='" + query + '\'' +
                ", count=" + count +
                ", contextLength=" + (context != null ? context.length() : 0) +
                '}';
    }
}

