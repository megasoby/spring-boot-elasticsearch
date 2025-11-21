package com.example.elasticsearch.service;

import com.example.elasticsearch.entity.ChatHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 채팅 히스토리 관리 서비스
 * 인메모리로 대화 히스토리를 저장하고 관리
 * 추후 DB 연동 시 Repository 패턴으로 확장 가능
 */
@Service
public class ChatHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryService.class);
    
    // 사용자별 대화 히스토리 저장 (userId -> List<ChatHistory>)
    private final Map<String, List<ChatHistory>> historyStore = new ConcurrentHashMap<>();
    
    // 최대 저장 개수 (메모리 관리)
    private static final int MAX_HISTORY_SIZE = 100;
    
    /**
     * 대화 히스토리 저장
     */
    public void save(ChatHistory history) {
        String userId = history.getUserId();
        
        historyStore.computeIfAbsent(userId, k -> new ArrayList<>());
        List<ChatHistory> userHistory = historyStore.get(userId);
        
        // 최대 개수 초과 시 오래된 것부터 삭제
        if (userHistory.size() >= MAX_HISTORY_SIZE) {
            userHistory.remove(0);
            logger.info("📝 최대 히스토리 개수 초과, 오래된 히스토리 삭제");
        }
        
        userHistory.add(history);
        logger.info("💾 대화 히스토리 저장: id={}, userId={}", history.getId(), userId);
    }
    
    /**
     * 사용자의 전체 대화 히스토리 조회
     */
    public List<ChatHistory> getHistory(String userId) {
        return historyStore.getOrDefault(userId, new ArrayList<>());
    }
    
    /**
     * 최근 N개 대화 히스토리 조회
     */
    public List<ChatHistory> getRecentHistory(String userId, int limit) {
        List<ChatHistory> history = getHistory(userId);
        int size = history.size();
        
        if (size <= limit) {
            return new ArrayList<>(history);
        }
        
        return history.subList(size - limit, size);
    }
    
    /**
     * 사용자의 모든 히스토리 삭제
     */
    public void clearHistory(String userId) {
        historyStore.remove(userId);
        logger.info("🗑️ 사용자 히스토리 삭제: userId={}", userId);
    }
    
    /**
     * 전체 통계 조회
     */
    public Map<String, Object> getStats() {
        int totalUsers = historyStore.size();
        int totalChats = historyStore.values().stream()
            .mapToInt(List::size)
            .sum();
        
        return Map.of(
            "totalUsers", totalUsers,
            "totalChats", totalChats,
            "activeUsers", historyStore.keySet()
        );
    }
}

