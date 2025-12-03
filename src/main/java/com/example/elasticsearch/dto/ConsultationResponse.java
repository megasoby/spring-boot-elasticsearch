package com.example.elasticsearch.dto;

import com.example.elasticsearch.entity.Consultation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 상담 가이드 검색 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponse {
    private String query;
    private String context;
    private List<Consultation> consultations;
    private Integer count;
    private String aiAnswer;  // Claude AI 응답
    private Long responseTime; // 응답 시간 (ms)
    
    public ConsultationResponse(String query, String context, List<Consultation> consultations) {
        this.query = query;
        this.context = context;
        this.consultations = consultations;
        this.count = consultations != null ? consultations.size() : 0;
    }
    
    public ConsultationResponse(String query, String context, List<Consultation> consultations, 
                                 String aiAnswer, Long responseTime) {
        this.query = query;
        this.context = context;
        this.consultations = consultations;
        this.count = consultations != null ? consultations.size() : 0;
        this.aiAnswer = aiAnswer;
        this.responseTime = responseTime;
    }
}

