package com.example.elasticsearch.entity;

import com.example.elasticsearch.dto.ConsultationProperty;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상담 가이드 엔티티
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "csasi_consultation")
public class Consultation {
    
    @Id
    @JsonProperty("csasi_id")
    private String csasiId;
    
    @Field(type = FieldType.Text, analyzer = "my_nori_analyzer")
    @JsonProperty("csasi_name")
    private String csasiName;
    
    @Field(type = FieldType.Integer)
    @JsonProperty("browse_count")
    private Integer browseCount;
    
    @Field(type = FieldType.Nested)
    private List<ConsultationProperty> properties;
    
    @Field(type = FieldType.Text, analyzer = "my_nori_analyzer")
    @JsonProperty("full_content")
    private String fullContent;
    
    @Field(type = FieldType.Dense_Vector, dims = 768)
    @JsonProperty("content_vector")
    private float[] contentVector;
    
    @Field(type = FieldType.Keyword)
    @JsonProperty("use_yn")
    private String useYn;
    
    @Field(type = FieldType.Date)
    @JsonProperty("reg_dts")
    private LocalDateTime regDts;
    
    @Field(type = FieldType.Date)
    @JsonProperty("indexed_at")
    private LocalDateTime indexedAt;
    
    // 유사도 점수 (검색 결과용, 저장되지 않음)
    private Double score;
}

