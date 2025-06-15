package com.example.devvy.service;

import com.example.devvy.mapper.DevvyMapper;
import com.example.devvy.vo.DevvyVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Devvy 챗봇 비즈니스 로직 서비스
 * @Refactored: SmartSearchService -> DevvyService
 */
@Service
public class DevvyService {

    private static final Logger log = LoggerFactory.getLogger(DevvyService.class);

    @Autowired
    private DevvyMapper devvyMapper;

    // @Autowired
    // private LlmService llmService; // 실제 LLM 연동 서비스 (현재는 주석 처리)

    /**
     * 활성화된 모든 카테고리 목록을 조회합니다.
     */
    public List<DevvyVo> getCategories() {
        return devvyMapper.selectCategories();
    }

    /**
     * 채팅 요청을 처리하는 메인 로직입니다.
     */
    @Transactional
    public DevvyVo processDevvyChat(DevvyVo request) {
        log.info("Devvy 채팅 처리 시작 (User: {}, Category: {})", request.getUserId(), request.getCategoryId());

        // 1. 세션 관리
        boolean isNewSession = (request.getSessionId() == null || request.getSessionId().isEmpty());
        String sessionId = isNewSession ? generateSessionId() : request.getSessionId();
        request.setSessionId(sessionId);

        // 2. 사용자 메시지 저장
        saveMessage(request, "USER", request.getMessage());

        // 3. 컨텍스트 데이터 조회 (RAG를 위한 정보)
        // String contextInfo = getContextData(request.getCategoryId());
        
        // 4. LLM 호출하여 AI 응답 생성 (현재는 Dummy 응답)
        // String prompt = buildPrompt(contextInfo, request.getMessage());
        // String aiContent = llmService.generateResponse(prompt);
        String aiContent = generateDummyAiResponse(request.getCategoryId()); // Dummy 로직으로 대체
        
        // 5. AI 응답 메시지 저장
        saveMessage(request, "AI", aiContent);

        // 6. 세션 정보 업데이트 또는 생성
        if (isNewSession) {
            devvyMapper.insertChatSession(request);
        } else {
            devvyMapper.updateChatSession(request);
        }
        
        // 7. 프론트엔드로 전달할 응답 객체 생성
        DevvyVo response = new DevvyVo();
        response.setSessionId(sessionId);
        response.setMessage(aiContent);
        response.setTimestamp(LocalDateTime.now());
        
        return response;
    }

    /**
     * 사용자의 전체 대화 히스토리 목록을 조회합니다.
     */
    public List<DevvyVo> getChatHistory(String userId) {
        return devvyMapper.selectChatHistory(userId);
    }

    /**
     * 특정 세션의 모든 메시지를 조회합니다.
     */
    public List<DevvyVo> getSessionMessages(String sessionId, String userId) {
        return devvyMapper.selectSessionMessages(sessionId, userId);
    }

    /**
     * 사용자 피드백을 저장합니다.
     */
    @Transactional
    public void saveFeedback(DevvyVo feedback) {
        feedback.setCreatedAt(LocalDateTime.now());
        devvyMapper.insertFeedback(feedback);
    }

    /**
     * 메시지를 DB에 저장합니다.
     */
    private void saveMessage(DevvyVo request, String messageType, String content) {
        DevvyVo messageVo = new DevvyVo();
        messageVo.setSessionId(request.getSessionId());
        messageVo.setUserId(request.getUserId());
        messageVo.setCategoryId(request.getCategoryId());
        messageVo.setMessageType(messageType);
        messageVo.setContent(content);
        messageVo.setTimestamp(LocalDateTime.now());
        devvyMapper.insertChatMessage(messageVo);
    }

    /**
     * 카테고리별 컨텍스트 데이터를 조회하고 문자열로 만듭니다. (Dummy)
     * 실제로는 이 부분에 DB 쿼리나 외부 API 호출 로직이 들어갑니다.
     */
    private String getContextData(Long categoryId) {
        // Dummy: 실제로는 devvyMapper.selectContextDataByCategory(categoryId) 등을 사용
        if (categoryId == 2) { // '프로젝트' 카테고리
            return "Context: DummyProjectInfo - 현재 활성 프로젝트는 15개, 주요 프로젝트는 'Phoenix'입니다.";
        }
        return "No specific context available.";
    }
    
    /**
     * LLM에 전달할 프롬프트를 구성합니다.
     */
    private String buildPrompt(String context, String userQuestion) {
        return "Context: " + context + "\n\nQuestion: " + userQuestion + "\n\nAnswer:";
    }

    /**
     * LLM 연동 전, 테스트를 위한 더미 AI 응답을 생성합니다.
     */
    private String generateDummyAiResponse(Long categoryId) {
        switch (categoryId.intValue()) {
            case 1: // SWDP Menu
                return "🗺️ **SWDP 메뉴**에 대한 더미 답변입니다. `프로젝트 등록` 메뉴로 이동하여 새 프로젝트를 시작할 수 있습니다.";
            case 2: // 프로젝트
                return "📊 **DummyProjectInfo**에서 조회한 프로젝트 현황 더미 답변입니다. 현재 `payment-gateway` 서비스의 응답 시간이 지연되고 있어 확인이 필요합니다.";
            case 3: // VOC
                return "🔔 **DummyVOCData** 기준, VOC 관련 더미 답변입니다. 오늘 긴급으로 접수된 `로그인 실패` 관련 VOC는 현재 조치 중입니다.";
            default:
                return "해당 카테고리에 대한 정보를 찾을 수 없습니다.";
        }
    }

    /**
     * 고유한 세션 ID를 생성합니다.
     */
    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
