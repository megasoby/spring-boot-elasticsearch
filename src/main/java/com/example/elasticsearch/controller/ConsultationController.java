package com.example.elasticsearch.controller;

import com.example.elasticsearch.dto.ConsultationRequest;
import com.example.elasticsearch.dto.ConsultationResponse;
import com.example.elasticsearch.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 상담 가이드 검색 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/consultation")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConsultationController {
    
    private final ConsultationService consultationService;
    
    /**
     * 상담 가이드 RAG 검색 (POST)
     * @param request 검색 요청
     * @return 검색 결과 및 컨텍스트
     */
    @PostMapping("/search")
    public ResponseEntity<ConsultationResponse> searchPost(@RequestBody ConsultationRequest request) {
        log.info("POST /api/consultation/search - query: {}, topK: {}", 
                request.getQuery(), request.getTopK());
        
        ConsultationResponse response = consultationService.search(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 상담 가이드 RAG 검색 (GET)
     * @param query 검색 질문
     * @param topK 검색 개수 (기본 5)
     * @param ordNo 주문번호 (선택)
     * @param ordItemSeq 상품순번 (선택)
     * @return 검색 결과 및 컨텍스트
     */
    @GetMapping("/search")
    public ResponseEntity<ConsultationResponse> searchGet(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") Integer topK,
            @RequestParam(required = false) String ordNo,
            @RequestParam(required = false) Integer ordItemSeq) {
        log.info("GET /api/consultation/search - query: {}, topK: {}, ordNo: {}, ordItemSeq: {}", 
                query, topK, ordNo, ordItemSeq);
        
        ConsultationRequest request = new ConsultationRequest(query, topK, ordNo, ordItemSeq);
        ConsultationResponse response = consultationService.search(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 상담 가이드 텍스트 검색 (키워드)
     * @param query 검색 질문
     * @param topK 검색 개수 (기본 5)
     * @return 검색 결과
     */
    @GetMapping("/search/text")
    public ResponseEntity<ConsultationResponse> textSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") Integer topK) {
        log.info("GET /api/consultation/search/text - query: {}, topK: {}", query, topK);
        
        ConsultationRequest request = new ConsultationRequest(query, topK, null, null);
        ConsultationResponse response = consultationService.textSearch(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 헬스 체크
     * @return OK
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Consultation API is running");
    }
}

