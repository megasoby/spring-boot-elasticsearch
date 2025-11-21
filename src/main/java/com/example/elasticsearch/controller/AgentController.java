package com.example.elasticsearch.controller;

import com.example.elasticsearch.dto.AgentRequest;
import com.example.elasticsearch.dto.AgentResponse;
import com.example.elasticsearch.entity.ChatHistory;
import com.example.elasticsearch.service.AgentService;
import com.example.elasticsearch.service.ChatHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI Agent REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    
    private final AgentService agentService;
    private final ChatHistoryService chatHistoryService;
    
    public AgentController(AgentService agentService, 
                          ChatHistoryService chatHistoryService) {
        this.agentService = agentService;
        this.chatHistoryService = chatHistoryService;
    }
    
    /**
     * 채팅 요청 처리
     */
    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(@RequestBody AgentRequest request) {
        logger.info("💬 채팅 요청: question='{}', topK={}", 
            request.getQuestion(), request.getTopK());
        
        try {
            // 입력 검증
            if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
                logger.warn("⚠️ 빈 질문 요청");
                return ResponseEntity.badRequest().build();
            }
            
            // topK 기본값 설정
            if (request.getTopK() == null || request.getTopK() < 1) {
                request.setTopK(5);
            }
            
            // Agent 처리
            AgentResponse response = agentService.chat(request);
            
            logger.info("✅ 채팅 응답 성공 ({}ms)", response.getResponseTime());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ 채팅 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
    
    /**
     * 대화 히스토리 조회
     */
    @GetMapping("/history")
    public ResponseEntity<List<ChatHistory>> getHistory(
            @RequestParam(defaultValue = "default") String userId,
            @RequestParam(required = false) Integer limit) {
        
        logger.info("📋 대화 히스토리 조회: userId={}, limit={}", userId, limit);
        
        try {
            List<ChatHistory> history = limit != null 
                ? chatHistoryService.getRecentHistory(userId, limit)
                : chatHistoryService.getHistory(userId);
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("❌ 히스토리 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 대화 히스토리 삭제
     */
    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(
            @RequestParam(defaultValue = "default") String userId) {
        
        logger.info("🗑️ 대화 히스토리 삭제: userId={}", userId);
        
        try {
            chatHistoryService.clearHistory(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("❌ 히스토리 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        logger.info("📊 Agent 통계 조회");
        
        try {
            Map<String, Object> stats = chatHistoryService.getStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("❌ 통계 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Agent 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        logger.info("💚 Agent 상태 확인");
        return ResponseEntity.ok("Agent is running");
    }
}

