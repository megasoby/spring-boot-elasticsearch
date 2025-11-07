package com.example.elasticsearch.service;

import com.example.elasticsearch.dto.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {
    
    private static final String EMBEDDING_API_URL = "http://localhost:5001/embed";
    private final RestTemplate restTemplate;

    public EmbeddingService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 텍스트를 벡터로 변환
     * @param text 변환할 텍스트
     * @return 벡터 (768차원)
     */
    public List<Float> getVector(String text) {
        try {
            // 요청 생성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", text);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            // Python API 호출
            EmbeddingResponse response = restTemplate.postForObject(
                    EMBEDDING_API_URL,
                    request,
                    EmbeddingResponse.class
            );

            if (response == null || response.getVector() == null) {
                throw new RuntimeException("벡터 생성 실패");
            }

            return response.getVector();
            
        } catch (Exception e) {
            throw new RuntimeException("벡터 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }
}

