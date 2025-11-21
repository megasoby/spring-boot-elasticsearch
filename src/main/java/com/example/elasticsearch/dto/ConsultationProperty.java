package com.example.elasticsearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상담 가이드 속성 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationProperty {
    @JsonProperty("prop_id")
    private String propId;
    
    @JsonProperty("prop_type_cd")
    private String propTypeCd;
    
    @JsonProperty("prop_seq")
    private Integer propSeq;
    
    private String content;
}

