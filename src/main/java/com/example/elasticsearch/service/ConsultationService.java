package com.example.elasticsearch.service;

import com.example.elasticsearch.dto.ConsultationProperty;
import com.example.elasticsearch.dto.ConsultationRequest;
import com.example.elasticsearch.dto.ConsultationResponse;
import com.example.elasticsearch.entity.Consultation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 상담 가이드 RAG 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {
    
    private final ConsultationVectorSearchService vectorSearchService;
    
    /**
     * 상담 가이드 RAG 검색
     * @param request 검색 요청
     * @return 검색 결과 및 컨텍스트
     */
    public ConsultationResponse search(ConsultationRequest request) {
        log.info("상담 가이드 RAG 검색 시작: query={}, topK={}", 
                request.getQuery(), request.getTopK());
        
        // 1. 벡터 검색으로 유사한 상담 가이드 찾기
        List<Consultation> consultations = vectorSearchService.vectorSearch(
            request.getQuery(), 
            request.getTopK()
        );
        
        // 2. 검색 결과를 Claude가 이해할 수 있는 컨텍스트로 변환
        String context = buildContext(request.getQuery(), consultations);
        
        // 3. 응답 생성
        ConsultationResponse response = new ConsultationResponse(
            request.getQuery(),
            context,
            consultations
        );
        
        log.info("상담 가이드 RAG 검색 완료: {}건 발견", consultations.size());
        
        return response;
    }
    
    /**
     * Claude API용 컨텍스트 생성
     * @param query 사용자 질문
     * @param consultations 검색된 상담 가이드 목록
     * @return 포맷팅된 컨텍스트
     */
    private String buildContext(String query, List<Consultation> consultations) {
        StringBuilder context = new StringBuilder();
        
        context.append("상담원 문의: ").append(query).append("\n\n");
        context.append("검색된 유사 상담 가이드 ").append(consultations.size()).append("개:\n\n");
        
        for (int i = 0; i < consultations.size(); i++) {
            Consultation consultation = consultations.get(i);
            
            context.append(String.format("%d. %s (ID: %s)\n", 
                i + 1, 
                consultation.getCsasiName(),
                consultation.getCsasiId()
            ));
            
            // 조회수 정보
            if (consultation.getBrowseCount() != null) {
                context.append(String.format("   조회수: %,d회\n", consultation.getBrowseCount()));
            }
            
            // 유사도 점수
            if (consultation.getScore() != null) {
                context.append(String.format("   유사도: %.2f%%\n", consultation.getScore() * 100));
            }
            
            // 가이드 내용
            if (consultation.getProperties() != null && !consultation.getProperties().isEmpty()) {
                context.append("   \n");
                context.append("   [상담 가이드 내용]\n");
                
                for (ConsultationProperty prop : consultation.getProperties()) {
                    if (prop.getContent() != null && !prop.getContent().trim().isEmpty() 
                        && !prop.getContent().equals(".")) {
                        
                        // 타입별 라벨
                        String label = getPropertyTypeLabel(prop.getPropTypeCd());
                        context.append(String.format("   %s:\n", label));
                        
                        // 내용 (줄바꿈 처리)
                        String content = prop.getContent()
                            .replace("\n", "\n   ")
                            .trim();
                        context.append("   ").append(content).append("\n");
                        context.append("   \n");
                    }
                }
            }
            
            context.append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * 속성 타입 코드를 라벨로 변환
     */
    private String getPropertyTypeLabel(String typeCd) {
        if (typeCd == null) {
            return "내용";
        }
        
        switch (typeCd) {
            case "001":
                return "처리 방법";
            case "002":
                return "유의사항";
            case "003":
                return "고객 안내 멘트";
            case "004":
                return "추가 정보";
            default:
                return "내용";
        }
    }
    
    /**
     * 텍스트 검색 (키워드 기반)
     * @param request 검색 요청
     * @return 검색 결과
     */
    public ConsultationResponse textSearch(ConsultationRequest request) {
        log.info("상담 가이드 텍스트 검색 시작: query={}, topK={}", 
                request.getQuery(), request.getTopK());
        
        List<Consultation> consultations = vectorSearchService.textSearch(
            request.getQuery(), 
            request.getTopK()
        );
        
        String context = buildContext(request.getQuery(), consultations);
        
        ConsultationResponse response = new ConsultationResponse(
            request.getQuery(),
            context,
            consultations
        );
        
        log.info("상담 가이드 텍스트 검색 완료: {}건 발견", consultations.size());
        
        return response;
    }
}

