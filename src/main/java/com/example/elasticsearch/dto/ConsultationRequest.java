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
}

