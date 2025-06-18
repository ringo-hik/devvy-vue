package com.temporary22.aiagent.impl.aiAgent;

import com.temporary22.devportal.mapper.aiagent.FloatChatMapper;
import com.temporary22.aiagent.entity.FloatChatVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SWP Float Chat 비즈니스 로직 서비스 (비동기 처리 및 프롬프트 관리 추가)
 */
@Service
@EnableAsync
public class FloatChatService {

    private static final Logger log = LoggerFactory.getLogger(FloatChatService.class);
    private static final int RECENT_CONVERSATION_LIMIT = 20;

    @Autowired
    private FloatChatMapper floatChatMapper;

    // === 카테고리 관리 ===

    /**
     * 활성화된 카테고리 목록을 반환합니다. (시스템 프롬프트 포함)
     */
    public List<FloatChatVo> getCategories() {
        log.debug("카테고리 목록 조회 시작");
        List<FloatChatVo> categories = floatChatMapper.selectCategories();
        log.info("카테고리 조회 완료: {}개", categories.size());
        return categories;
    }

    /**
     * 특정 카테고리 정보를 조회합니다.
     */
    public FloatChatVo getCategoryByCode(String categoryCode) {
        log.debug("카테고리 조회 시작: {}", categoryCode);
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리 코드가 필요합니다.");
        }
        
        FloatChatVo category = floatChatMapper.selectCategoryByCode(categoryCode);
        if (category == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다: " + categoryCode);
        }
        
        log.info("카테고리 조회 완료: {}", categoryCode);
        return category;
    }

    /**
     * 지정된 카테고리가 존재하고 활성화되어 있는지 확인합니다.
     */
    public boolean isCategoryValid(String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            return false;
        }
        return floatChatMapper.checkCategoryExists(categoryCode) > 0;
    }

    // === 프롬프트 관리 ===

    /**
     * 카테고리의 시스템 프롬프트를 업데이트합니다.
     */
    @Transactional
    public void updateSystemPrompt(String categoryCode, String systemPrompt, String userId) {
        log.info("시스템 프롬프트 업데이트 시작 - Category: {}, User: {}", categoryCode, userId);
        
        if (!isCategoryValid(categoryCode)) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다: " + categoryCode);
        }
        
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("시스템 프롬프트는 필수입니다.");
        }
        
        if (systemPrompt.length() > 5000) {
            throw new IllegalArgumentException("시스템 프롬프트는 5000자를 초과할 수 없습니다.");
        }
        
        int updatedRows = floatChatMapper.updateSystemPrompt(categoryCode, systemPrompt.trim(), userId);
        
        if (updatedRows == 0) {
            throw new RuntimeException("시스템 프롬프트 업데이트에 실패했습니다.");
        }
        
        log.info("시스템 프롬프트 업데이트 완료 - Category: {}", categoryCode);
    }

    /**
     * 특정 카테고리의 시스템 프롬프트를 조회합니다.
     */
    public String getSystemPrompt(String categoryCode) {
        log.debug("시스템 프롬프트 조회 시작: {}", categoryCode);
        
        if (!isCategoryValid(categoryCode)) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다: " + categoryCode);
        }
        
        String systemPrompt = floatChatMapper.selectSystemPrompt(categoryCode);
        
        // DB에 저장된 프롬프트가 없으면 기본 프롬프트 사용
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            systemPrompt = buildDefaultSystemPrompt(categoryCode);
            log.info("기본 시스템 프롬프트 사용: {}", categoryCode);
        }
        
        return systemPrompt;
    }

    // === 메시지 처리 (비동기 개선) ===

    /**
     * 사용자 질문을 처리하고 AI 응답을 생성하여 저장합니다. (비동기 처리)
     */
    @Async
    public CompletableFuture<FloatChatVo> processMessageAsync(FloatChatVo request) {
        log.info("비동기 메시지 처리 시작 - User: {}, Category: {}", 
                request.getUserId(), request.getCategoryCode());

        try {
            validateMessageRequest(request);
            
            FloatChatVo aiResponse = generateAIResponse(request);
            saveConversation(request, aiResponse);

            log.info("비동기 메시지 처리 완료 - ConversationId: {}", aiResponse.getConversationId());
            return CompletableFuture.completedFuture(aiResponse);

        } catch (Exception e) {
            log.error("비동기 메시지 처리 중 오류 발생", e);
            FloatChatVo errorResponse = FloatChatVo.createErrorResponse("메시지 처리 실패: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResponse);
        }
    }

    /**
     * 동기식 메시지 처리 (기존 호환성 유지)
     */
    @Transactional
    public FloatChatVo processMessage(FloatChatVo request) {
        log.info("동기 메시지 처리 시작 - User: {}, Category: {}", 
                request.getUserId(), request.getCategoryCode());

        try {
            validateMessageRequest(request);
            
            FloatChatVo aiResponse = generateAIResponse(request);
            saveConversation(request, aiResponse);

            log.info("동기 메시지 처리 완료 - ConversationId: {}", aiResponse.getConversationId());
            return aiResponse;

        } catch (Exception e) {
            log.error("동기 메시지 처리 중 오류 발생", e);
            throw new RuntimeException("메시지 처리 실패", e);
        }
    }

    /**
     * 사용자별 카테고리별 최신 대화 목록을 조회합니다.
     */
    public List<FloatChatVo> getRecentConversations(String userId, String categoryCode) {
        log.debug("최신 대화 조회 시작 - User: {}, Category: {}", userId, categoryCode);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다.");
        }
        
        if (!isCategoryValid(categoryCode)) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + categoryCode);
        }
        
        List<FloatChatVo> conversations = floatChatMapper.selectRecentConversations(
            userId, categoryCode, RECENT_CONVERSATION_LIMIT);
        
        log.info("최신 대화 조회 완료 - User: {}, Category: {}, 대화 수: {}", 
                userId, categoryCode, conversations.size());
        
        return conversations;
    }

    /**
     * 피드백을 저장합니다.
     */
    @Transactional
    public void saveFeedback(FloatChatVo feedback) {
        log.info("피드백 저장 시작 - User: {}, Rating: {}", 
                feedback.getUserId(), feedback.getRating());
        
        if (!feedback.isValidFeedback()) {
            throw new IllegalArgumentException("피드백 데이터가 유효하지 않습니다.");
        }
        
        floatChatMapper.insertFeedback(feedback);
        log.info("피드백 저장 완료");
    }

    /**
     * 시스템 상태를 확인합니다.
     */
    public FloatChatVo performHealthCheck() {
        try {
            int healthStatus = floatChatMapper.healthCheck();
            return FloatChatVo.createSuccessResponse(
                healthStatus == 1 ? "HEALTHY" : "UNHEALTHY", 
                "시스템 상태 정상"
            );
        } catch (Exception e) {
            log.error("헬스체크 실패", e);
            return FloatChatVo.createErrorResponse("시스템 상태 확인 실패");
        }
    }

    // === Private 메서드들 ===

    /**
     * LLMCall을 이용하여 AI 응답을 생성합니다. (시스템 프롬프트 DB에서 조회)
     */
    private FloatChatVo generateAIResponse(FloatChatVo request) {
        String categoryCode = request.getCategoryCode();
        String userQuestion = request.getUserQuestion();
        
        log.info("AI 응답 생성 시작 - Category: {}", categoryCode);
        
        try {
            // DB에서 시스템 프롬프트 조회
            String systemPrompt = getSystemPrompt(categoryCode);
            
            // 사용자 프롬프트 생성
            String userPrompt = buildUserPrompt(userQuestion);
            
            // LLMCall 호출
            String llmResponse = LLMCall(systemPrompt, userPrompt);
            
            // 응답을 그대로 사용 (복잡한 파싱 없음)
            return FloatChatVo.createAIResponse(llmResponse);
            
        } catch (Exception e) {
            log.error("AI 응답 생성 실패", e);
            return FloatChatVo.createAIResponse(
                "죄송합니다. 일시적인 오류로 응답을 생성할 수 없습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    /**
     * 기본 시스템 프롬프트를 생성합니다. (DB에 프롬프트가 없을 때 사용)
     */
    private String buildDefaultSystemPrompt(String categoryCode) {
        StringBuilder systemPrompt = new StringBuilder();
        
        systemPrompt.append("당신은 SWP(Software Platform) 전문 AI 어시스턴트입니다.\n");
        systemPrompt.append("사용자의 질문에 정확하고 도움이 되는 답변을 제공해야 합니다.\n\n");
        
        switch (categoryCode) {
            case "swdp_menu":
                systemPrompt.append("전문 분야: SWDP 메뉴 구조 및 기능 안내\n");
                systemPrompt.append("주요 역할: 사용자가 SWDP 시스템의 메뉴와 기능을 이해할 수 있도록 도와줍니다.\n");
                break;
                
            case "project":
                systemPrompt.append("전문 분야: 프로젝트 관리 및 현황 분석\n");
                systemPrompt.append("주요 역할: 프로젝트 상태, 진행률, 팀원 정보 등을 제공합니다.\n");
                break;
                
            case "voc":
                systemPrompt.append("전문 분야: VOC(고객의 소리) 관리 및 이슈 분석\n");
                systemPrompt.append("주요 역할: 사용자 문의, 장애 현황, 해결 상태를 분석하고 보고합니다.\n");
                break;
                
            case "project_info":
                systemPrompt.append("전문 분야: 프로젝트 상세 정보 및 기술 스택\n");
                systemPrompt.append("주요 역할: 프로젝트의 기술적 세부사항과 구성원 정보를 제공합니다.\n");
                break;
                
            case "swdp_api":
                systemPrompt.append("전문 분야: SWDP API 문서 및 사용법 안내\n");
                systemPrompt.append("주요 역할: API 명세서, 인증 방법, 요청/응답 예제를 제공합니다.\n");
                break;
                
            default:
                systemPrompt.append("전문 분야: 일반 소프트웨어 개발 플랫폼 지원\n");
                systemPrompt.append("주요 역할: 개발 관련 질문에 대한 종합적인 답변을 제공합니다.\n");
        }
        
        systemPrompt.append("\n응답 규칙:\n");
        systemPrompt.append("1. 한국어로 답변하세요.\n");
        systemPrompt.append("2. 정확하고 구체적인 정보를 제공하세요.\n");
        systemPrompt.append("3. 데이터나 정보가 표 형태로 보여주는 것이 적절한 경우, 마크다운 표 형식을 사용하세요.\n");
        systemPrompt.append("4. 표가 적절하지 않은 일반적인 질문은 자연스러운 텍스트로 답변하세요.\n");
        systemPrompt.append("5. 목록, 현황, 비교, 통계 등의 정보는 반드시 표 형태로 제공하세요.\n");
        
        return systemPrompt.toString();
    }

    /**
     * 사용자 프롬프트를 생성합니다.
     */
    private String buildUserPrompt(String userQuestion) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("사용자 질문: ").append(userQuestion).append("\n\n");
        userPrompt.append("위 질문에 대해 답변해주세요. ");
        userPrompt.append("만약 답변 내용이 목록, 현황, 비교, 통계, 데이터 등의 구조화된 정보라면 ");
        userPrompt.append("반드시 마크다운 표 형태로 깔끔하게 정리해서 제공해주세요.");
        
        return userPrompt.toString();
    }

    /**
     * LLMCall 함수 (이미 구현되어 있다고 가정)
     */
    private String LLMCall(String systemPrompt, String userPrompt) {
        // TODO: 실제 LLM API 호출 구현
        log.debug("LLM 호출 - System: {}, User: {}", 
                systemPrompt.substring(0, Math.min(100, systemPrompt.length())),
                userPrompt.substring(0, Math.min(100, userPrompt.length())));
        
        // 임시 구현 (실제 환경에서는 제거하고 실제 LLM 호출 코드로 대체)
        return "LLMCall 함수가 아직 구현되지 않았습니다. 실제 LLM API 연동이 필요합니다.";
    }

    // ===== 헬퍼 메서드들 =====
    
    private void validateMessageRequest(FloatChatVo request) {
        if (!request.isValidMessage()) {
            throw new IllegalArgumentException("메시지 요청이 유효하지 않습니다.");
        }
        
        if (!isCategoryValid(request.getCategoryCode())) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다: " + request.getCategoryCode());
        }
    }

    private void saveConversation(FloatChatVo request, FloatChatVo aiResponse) {
        FloatChatVo conversationVo = new FloatChatVo();
        conversationVo.setCategoryCode(request.getCategoryCode());
        conversationVo.setUserId(request.getUserId());
        conversationVo.setUserQuestion(request.getUserQuestion());
        conversationVo.setAiResponse(aiResponse.getAiResponse());
        
        floatChatMapper.insertConversation(conversationVo);
        aiResponse.setConversationId(conversationVo.getConversationId());
    }

    @Transactional
    public int cleanupOldData(int retentionDays) {
        log.info("오래된 데이터 정리 시작 - 보존 기간: {}일", retentionDays);
        
        if (retentionDays < 1) {
            throw new IllegalArgumentException("보존 기간은 1일 이상이어야 합니다.");
        }
        
        int deletedCount = floatChatMapper.deleteOldConversations(retentionDays);
        log.info("오래된 데이터 정리 완료 - 삭제된 레코드 수: {}", deletedCount);
        
        return deletedCount;
    }
}