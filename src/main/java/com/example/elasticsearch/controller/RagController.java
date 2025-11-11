package com.example.elasticsearch.controller;

import com.example.elasticsearch.dto.RagRequest;
import com.example.elasticsearch.dto.RagResponse;
import com.example.elasticsearch.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RAG API 컨트롤러
 * MCP 서버에서 호출할 RAG 검색 엔드포인트
 */
@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RagController {
    
    private static final Logger logger = LoggerFactory.getLogger(RagController.class);
    
    private final RagService ragService;
    
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }
    
    /**
     * RAG 검색 API
     * POST /api/rag/search
     * 
     * Request Body:
     * {
     *   "query": "무선 이어폰 추천해줘",
     *   "topK": 5
     * }
     * 
     * Response:
     * {
     *   "query": "무선 이어폰 추천해줘",
     *   "context": "질문: 무선 이어폰 추천해줘\n\n검색된 유사 상품 5개:\n...",
     *   "products": [...],
     *   "count": 5
     * }
     */
    @PostMapping("/search")
    public ResponseEntity<RagResponse> search(@RequestBody RagRequest request) {
        logger.info("RAG 검색 요청: {}", request);
        
        try {
            // 입력 검증
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                logger.warn("검색어가 비어있음");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            // RAG 검색 실행
            RagResponse response = ragService.search(request);
            
            logger.info("RAG 검색 성공: {}개 결과", response.getCount());
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("RAG 검색 실패: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * GET 방식 RAG 검색 (간단한 테스트용)
     * GET /api/rag/search?query=노트북&topK=3
     */
    @GetMapping("/search")
    public ResponseEntity<RagResponse> searchGet(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") Integer topK) {
        
        logger.info("RAG 검색 요청 (GET): query={}, topK={}", query, topK);
        
        RagRequest request = new RagRequest(query, topK);
        return search(request);
    }
    
    /**
     * Health Check
     * GET /api/rag/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RAG API is running");
    }
}

