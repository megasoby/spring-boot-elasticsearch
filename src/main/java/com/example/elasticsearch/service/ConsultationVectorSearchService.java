package com.example.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.elasticsearch.entity.Consultation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상담 가이드 벡터 검색 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationVectorSearchService {
    
    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingService embeddingService;
    
    private static final String INDEX_NAME = "csasi_consultation";
    private static final String VECTOR_FIELD = "content_vector";
    
    /**
     * 벡터 검색 (기본 k=5)
     */
    public List<Consultation> vectorSearch(String queryText) {
        return vectorSearch(queryText, 5);
    }
    
    /**
     * 벡터 검색
     * @param queryText 검색 텍스트
     * @param topK 상위 k개 결과
     * @return 유사한 상담 가이드 목록
     */
    public List<Consultation> vectorSearch(String queryText, int topK) {
        try {
            log.info("상담 가이드 벡터 검색 시작: query={}, topK={}", queryText, topK);
            
            // 1. 검색어를 벡터로 변환
            List<Float> queryVector = embeddingService.getVector(queryText);
            
            // 2. k-NN 검색 요청 생성
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .knn(k -> k
                    .field(VECTOR_FIELD)
                    .queryVector(queryVector)
                    .k(topK)
                    .numCandidates(100)
                )
                .source(src -> src
                    .filter(f -> f
                        .includes("csasi_id", "csasi_name", "browse_count", 
                                  "properties", "full_content", "use_yn")
                    )
                )
            );
            
            // 3. 검색 실행
            SearchResponse<Consultation> response = elasticsearchClient.search(
                searchRequest, 
                Consultation.class
            );
            
            // 4. 결과 변환 (유사도 점수 포함)
            List<Consultation> consultations = response.hits().hits().stream()
                .map(hit -> {
                    Consultation consultation = hit.source();
                    if (consultation != null && hit.score() != null) {
                        consultation.setScore(hit.score());
                    }
                    return consultation;
                })
                .collect(Collectors.toList());
            
            log.info("상담 가이드 벡터 검색 완료: 총 {}건 발견", consultations.size());
            
            return consultations;
            
        } catch (Exception e) {
            log.error("상담 가이드 벡터 검색 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 가이드 벡터 검색 실패", e);
        }
    }
    
    /**
     * 텍스트 검색 (키워드 기반)
     * @param queryText 검색 텍스트
     * @param topK 상위 k개 결과
     * @return 검색된 상담 가이드 목록
     */
    public List<Consultation> textSearch(String queryText, int topK) {
        try {
            log.info("상담 가이드 텍스트 검색 시작: query={}, topK={}", queryText, topK);
            
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                    .multiMatch(m -> m
                        .query(queryText)
                        .fields("csasiName^3", "fullContent^1")
                    )
                )
                .size(topK)
            );
            
            SearchResponse<Consultation> response = elasticsearchClient.search(
                searchRequest,
                Consultation.class
            );
            
            List<Consultation> consultations = response.hits().hits().stream()
                .map(hit -> {
                    Consultation consultation = hit.source();
                    if (consultation != null && hit.score() != null) {
                        consultation.setScore(hit.score());
                    }
                    return consultation;
                })
                .collect(Collectors.toList());
            
            log.info("상담 가이드 텍스트 검색 완료: 총 {}건 발견", consultations.size());
            
            return consultations;
            
        } catch (Exception e) {
            log.error("상담 가이드 텍스트 검색 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 가이드 텍스트 검색 실패", e);
        }
    }
}

