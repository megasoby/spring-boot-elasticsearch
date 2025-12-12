package com.example.elasticsearch.service;

import com.example.elasticsearch.dto.ConsultationProperty;
import com.example.elasticsearch.dto.ConsultationRequest;
import com.example.elasticsearch.dto.ConsultationResponse;
import com.example.elasticsearch.dto.OrderInfo;
import com.example.elasticsearch.entity.Consultation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
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
    private final ChatModel chatModel;  // AWS Bedrock Claude
    private final OrderService orderService;  // 주문 정보 조회 (고도화 1차)
    
    @Value("${llm.provider:mock}")
    private String llmProvider;
    
    /**
     * 상담 가이드 RAG 검색 + AI 응답 생성
     * @param request 검색 요청
     * @return 검색 결과 및 AI 응답
     */
    public ConsultationResponse search(ConsultationRequest request) {
        long startTime = System.currentTimeMillis();
        
        log.info("🔍 상담 가이드 RAG 검색 시작: query={}, topK={}, ordNo={}, ordItemSeq={}", 
                request.getQuery(), request.getTopK(), request.getOrdNo(), request.getOrdItemSeq());
        
        // 1. 주문 정보 조회 (고도화 1차: 주문번호가 있는 경우)
        OrderInfo orderInfo = null;
        if (request.getOrdNo() != null && !request.getOrdNo().isEmpty()) {
            Integer ordItemSeq = request.getOrdItemSeq() != null ? request.getOrdItemSeq() : 1;
            orderInfo = orderService.getOrderInfo(request.getOrdNo(), ordItemSeq);
            if (orderInfo != null) {
                log.info("📦 주문 정보 조회 성공: 상태={}, 상품={}", 
                        orderInfo.getOrdItemStatNm(), orderInfo.getItemNm());
            }
        }
        
        // 2. 벡터 검색으로 유사한 상담 가이드 찾기
        List<Consultation> consultations = vectorSearchService.vectorSearch(
            request.getQuery(), 
            request.getTopK()
        );
        
        // 3. 검색 결과를 Claude가 이해할 수 있는 컨텍스트로 변환 (주문 정보 포함)
        String context = buildContext(request.getQuery(), consultations, orderInfo);
        
        // 4. AI 응답 생성 (Bedrock 모드일 때만)
        String aiAnswer = null;
        if ("bedrock".equals(llmProvider) && !consultations.isEmpty()) {
            log.info("🤖 AWS Bedrock Claude AI 응답 생성 중...");
            aiAnswer = generateAiResponse(request.getQuery(), context, consultations, orderInfo);
            log.info("✅ AI 응답 생성 완료");
        } else {
            aiAnswer = context;  // LLM 미사용시 context 그대로 반환
        }
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        // 5. 응답 생성
        ConsultationResponse response = new ConsultationResponse(
            request.getQuery(),
            context,
            consultations,
            aiAnswer,
            responseTime
        );
        
        log.info("✅ 상담 가이드 RAG 검색 완료: {}건 발견, {}ms", consultations.size(), responseTime);
        
        return response;
    }
    
    /**
     * Claude AI 응답 생성 (주문 정보 포함)
     */
    private String generateAiResponse(String query, String context, List<Consultation> consultations, OrderInfo orderInfo) {
        try {
            // 주문 정보가 있을 때와 없을 때 시스템 프롬프트 분기
            String systemPrompt;
            if (orderInfo != null) {
                systemPrompt = """
                    당신은 친절하고 전문적인 고객 상담 AI 어시스턴트입니다.
                    사용자는 '송그랜트'이고, 당신은 '웬즈데이'입니다.
                    
                    역할:
                    - 상담원이 고객 문의에 대응할 수 있도록 상담 가이드를 정리해서 알려주세요.
                    - 주문 정보가 제공되면, 해당 주문의 상태를 고려하여 맞춤형 안내를 해주세요.
                    
                    응답 가이드:
                    1. 먼저 주문 상태를 요약하고, 현재 가능한 처리 방법을 안내해주세요.
                    2. 상담 가이드를 바탕으로 구체적인 처리 절차를 설명해주세요.
                    3. 고객에게 안내할 멘트가 있다면 포함해주세요.
                    4. 유의사항이 있다면 강조해주세요.
                    5. 이모지를 적절히 활용하여 읽기 쉽게 작성해주세요.
                    """;
            } else {
                systemPrompt = """
                    당신은 친절하고 전문적인 고객 상담 AI 어시스턴트입니다.
                    사용자는 '송그랜트'이고, 당신은 '웬즈데이'입니다.
                    
                    역할:
                    - 상담원이 고객 문의에 대응할 수 있도록 상담 가이드를 정리해서 알려주세요.
                    - 검색된 상담 가이드를 바탕으로 명확하고 친절하게 안내해주세요.
                    
                    응답 가이드:
                    1. 핵심 내용을 먼저 요약해주세요.
                    2. 단계별 처리 방법이 있다면 순서대로 정리해주세요.
                    3. 고객에게 안내할 멘트가 있다면 포함해주세요.
                    4. 유의사항이 있다면 강조해주세요.
                    5. 이모지를 적절히 활용하여 읽기 쉽게 작성해주세요.
                    """;
            }
            
            // 사용자 프롬프트 생성
            StringBuilder userPromptBuilder = new StringBuilder();
            userPromptBuilder.append("=== 상담원 질문 ===\n");
            userPromptBuilder.append(query).append("\n\n");
            
            // 주문 정보가 있으면 추가
            if (orderInfo != null) {
                userPromptBuilder.append(orderInfo.toSummary()).append("\n");
                
                // 상태별 가능한 액션 추가
                String availableActions = orderService.getAvailableActions(orderInfo.getOrdItemStatCd());
                if (!availableActions.isEmpty()) {
                    userPromptBuilder.append("=== 현재 상태에서 가능한 처리 ===\n");
                    userPromptBuilder.append(availableActions).append("\n\n");
                }
            }
            
            userPromptBuilder.append("=== 검색된 상담 가이드 ===\n");
            userPromptBuilder.append(context).append("\n\n");
            userPromptBuilder.append("위 정보를 바탕으로 송그랜트에게 도움이 되는 답변을 작성해주세요.");
            
            SystemMessage systemMessage = new SystemMessage(systemPrompt);
            UserMessage userMessage = new UserMessage(userPromptBuilder.toString());
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            
            return chatModel.call(prompt).getResult().getOutput().getContent();
            
        } catch (Exception e) {
            log.error("❌ AI 응답 생성 실패: {}", e.getMessage(), e);
            return "AI 응답 생성 중 오류가 발생했습니다: " + e.getMessage() + "\n\n" + context;
        }
    }
    
    /**
     * Claude API용 컨텍스트 생성 (주문 정보 포함)
     * @param query 사용자 질문
     * @param consultations 검색된 상담 가이드 목록
     * @param orderInfo 주문 정보 (nullable)
     * @return 포맷팅된 컨텍스트
     */
    private String buildContext(String query, List<Consultation> consultations, OrderInfo orderInfo) {
        StringBuilder context = new StringBuilder();
        
        context.append("상담원 문의: ").append(query).append("\n\n");
        
        // 주문 정보가 있으면 추가
        if (orderInfo != null) {
            context.append(orderInfo.toSummary()).append("\n");
        }
        
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
        
        String context = buildContext(request.getQuery(), consultations, null);
        
        ConsultationResponse response = new ConsultationResponse(
            request.getQuery(),
            context,
            consultations
        );
        
        log.info("상담 가이드 텍스트 검색 완료: {}건 발견", consultations.size());
        
        return response;
    }
}

