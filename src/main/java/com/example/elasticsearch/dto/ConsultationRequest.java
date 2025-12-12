package com.example.elasticsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상담 가이드 검색 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationRequest {
    private String query;
    private Integer topK = 5;
    
    // 주문 정보 (고도화 1차)
    private String ordNo;           // 주문번호
    private Integer ordItemSeq;     // 주문상품순번
}

