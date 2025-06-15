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

/**
 * Devvy 챗봇 비즈니스 로직 서비스
 * - 카테고리 Text 기반 케이스 분기
 * - userQuery 필드명 통일 적용
 * - 미니멀 설계 원칙 준수
 */
@Service
public class DevvyService {

    private static final Logger log = LoggerFactory.getLogger(DevvyService.class);

    @Autowired
    private DevvyMapper devvyMapper;

    // === 카테고리 관리 ===
    public List<DevvyVo> getCategories() {
        log.debug("카테고리 목록 조회 시작");
        List<DevvyVo> categories = devvyMapper.selectCategories();
        log.info("카테고리 조회 완료: {}개", categories.size());
        return categories;
    }

    // === 채팅 처리 핵심 로직 ===
    @Transactional
    public DevvyVo processDevvyChat(DevvyVo request) {
        log.info("Devvy 채팅 처리 시작 - User: {}, Category: {}", 
                request.getUserId(), request.getCategoryCode());

        try {
            String sessionId = manageSession(request);
            request.setSessionId(sessionId);

            saveUserMessage(request);
            String aiResponse = generateCategoryResponse(request);
            saveAiMessage(request, aiResponse);
            updateSessionMetadata(request);

            return createChatResponse(aiResponse, sessionId);

        } catch (Exception e) {
            log.error("채팅 처리 중 오류 발생", e);
            throw new RuntimeException("채팅 처리 실패", e);
        }
    }

    // === 히스토리 관리 ===
    public List<DevvyVo> getChatHistory(String userId) {
        log.debug("사용자 히스토리 조회 시작 - User: {}", userId);
        List<DevvyVo> history = devvyMapper.selectUserSessions(userId);
        log.info("히스토리 조회 완료 - User: {}, 세션 수: {}", userId, history.size());
        return history;
    }

    public List<DevvyVo> getSessionMessages(String sessionId, String userId) {
        log.debug("세션 메시지 조회 시작 - Session: {}", sessionId);
        
        if (devvyMapper.checkSessionExists(sessionId, userId) == 0) {
            throw new IllegalArgumentException("해당 세션에 접근할 수 없습니다.");
        }
        
        List<DevvyVo> messages = devvyMapper.selectSessionMessages(sessionId, userId);
        log.info("세션 메시지 조회 완료 - Session: {}, 메시지 수: {}", sessionId, messages.size());
        return messages;
    }

    // === 피드백 관리 ===
    @Transactional
    public void saveFeedback(DevvyVo feedback) {
        log.info("피드백 저장 시작 - User: {}, Rating: {}", feedback.getUserId(), feedback.getRating());
        feedback.setCreatedAt(LocalDateTime.now());
        devvyMapper.insertFeedback(feedback);
        log.info("피드백 저장 완료");
    }

    // === 시스템 상태 관리 ===
    public DevvyVo performHealthCheck() {
        try {
            int healthStatus = devvyMapper.dummySystemHealthCheck();
            return DevvyVo.createSuccessResponse(
                healthStatus == 1 ? "HEALTHY" : "UNHEALTHY", 
                "시스템 상태 정상"
            );
        } catch (Exception e) {
            log.error("헬스체크 실패", e);
            return DevvyVo.createErrorResponse("시스템 상태 확인 실패");
        }
    }

    // === Private Helper Methods ===

    /**
     * 카테고리별 AI 응답 생성 (케이스 분기)
     */
    private String generateCategoryResponse(DevvyVo request) {
        String categoryCode = request.getCategoryCode();
        String userQuery = request.getUserQuery();
        
        switch (categoryCode) {
            case "swdp_menu":
                return dummySWDPMenu(userQuery);
            case "project":
                return dummyProject(userQuery);
            case "voc":
                return dummyVOC(userQuery);
            default:
                return "지원하지 않는 카테고리입니다.";
        }
    }

    /**
     * SWDP Menu 카테고리 Dummy 응답
     */
    private String dummySWDPMenu(String userQuery) {
        String lowerQuery = userQuery.toLowerCase();
        
        if (lowerQuery.contains("메뉴") || lowerQuery.contains("menu")) {
            return "🗺️ **SWDP 메뉴 안내**\n\n" +
                   "**주요 메뉴 구조:**\n" +
                   "• 📊 **프로젝트 관리** - 프로젝트 등록, 수정, 조회\n" +
                   "• 🚀 **배포 관리** - CI/CD 파이프라인, 배포 이력\n" +
                   "• 📈 **모니터링** - 시스템 현황, 성능 지표\n\n" +
                   "어떤 메뉴에 대해 더 자세히 알고 싶으신가요?";
        }
        
        if (lowerQuery.contains("등록") || lowerQuery.contains("register")) {
            return "📝 **프로젝트 등록 절차**\n\n" +
                   "1. 기본 정보 입력\n" +
                   "2. 기술 스택 선택\n" +
                   "3. 환경 설정\n" +
                   "4. CI/CD 파이프라인 구성\n\n" +
                   "SWDP > 프로젝트 관리 > 신규 등록을 이용해주세요.";
        }
        
        return "🗺️ **SWDP 시스템 안내**\n\n" +
               "Software Development Platform의 핵심 도구입니다.\n" +
               "구체적인 기능이나 메뉴에 대해 문의해주세요.";
    }

    /**
     * Project 카테고리 Dummy 응답
     */
    private String dummyProject(String userQuery) {
        String lowerQuery = userQuery.toLowerCase();
        
        if (lowerQuery.contains("현황") || lowerQuery.contains("목록")) {
            return "📊 **프로젝트 현황**\n\n" +
                   "**전체 프로젝트:**\n" +
                   "• 🟢 활성: 15개\n" +
                   "• 🔵 개발 중: 8개\n" +
                   "• ✅ 운영 중: 3개\n\n" +
                   "**주요 프로젝트:**\n" +
                   "• Phoenix (85%)\n" +
                   "• Gateway-Service (70%)\n" +
                   "• Auth-Module (95%)";
        }
        
        if (lowerQuery.contains("배포")) {
            return "🚀 **배포 현황**\n\n" +
                   "**최근 배포:**\n" +
                   "• Phoenix v2.1.0 - 운영 완료 ✅\n" +
                   "• Gateway v1.3.2 - 테스트 중 🔄\n" +
                   "• Auth-Module v1.0.5 - 대기 중 ⏳";
        }
        
        return "📊 **프로젝트 관리 시스템**\n\n" +
               "15개 프로젝트가 진행 중입니다.\n" +
               "현황이나 배포 상태를 확인해보세요.";
    }

    /**
     * VOC 카테고리 Dummy 응답
     */
    private String dummyVOC(String userQuery) {
        String lowerQuery = userQuery.toLowerCase();
        
        if (lowerQuery.contains("현황") || lowerQuery.contains("장애")) {
            return "🔔 **VOC 현황**\n\n" +
                   "**이번 주 현황:**\n" +
                   "• 🔴 긴급: 3건 (로그인 이슈)\n" +
                   "• 🟡 일반: 7건 (성능 개선)\n" +
                   "• 🟢 문의: 12건\n\n" +
                   "**처리 현황:**\n" +
                   "• 해결: 18건\n" +
                   "• 처리 중: 4건";
        }
        
        if (lowerQuery.contains("로그인")) {
            return "🔑 **로그인 이슈 해결**\n\n" +
                   "**해결 방법:**\n" +
                   "1. 브라우저 캐시 삭제\n" +
                   "2. 시크릿 모드 접속\n" +
                   "3. 다른 브라우저 시도\n\n" +
                   "문제 지속 시 IT 헬프데스크(1234)로 연락주세요.";
        }
        
        return "🔔 **VOC 관리 시스템**\n\n" +
               "시스템 이슈와 개선사항을 관리합니다.\n" +
               "구체적인 문제를 알려주세요.";
    }

    // === 유틸리티 메서드 ===
    
    private String manageSession(DevvyVo request) {
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            String newSessionId = UUID.randomUUID().toString().replace("-", "");
            createNewSession(request, newSessionId);
            return newSessionId;
        }
        return request.getSessionId();
    }

    private void createNewSession(DevvyVo request, String sessionId) {
        DevvyVo sessionVo = new DevvyVo();
        sessionVo.setSessionId(sessionId);
        sessionVo.setUserId(request.getUserId());
        sessionVo.setCategoryCode(request.getCategoryCode());
        sessionVo.setFirstMessage(request.getUserQuery());
        devvyMapper.insertSession(sessionVo);
    }

    private void saveUserMessage(DevvyVo request) {
        DevvyVo messageVo = createMessageVo(request, "USER", request.getUserQuery());
        devvyMapper.insertMessage(messageVo);
    }

    private void saveAiMessage(DevvyVo request, String aiResponse) {
        DevvyVo messageVo = createMessageVo(request, "AI", aiResponse);
        devvyMapper.insertMessage(messageVo);
    }

    private DevvyVo createMessageVo(DevvyVo request, String messageType, String userQuery) {
        DevvyVo messageVo = new DevvyVo();
        messageVo.setSessionId(request.getSessionId());
        messageVo.setUserId(request.getUserId());
        messageVo.setCategoryCode(request.getCategoryCode());
        messageVo.setMessageType(messageType);
        messageVo.setUserQuery(userQuery);
        return messageVo;
    }

    private void updateSessionMetadata(DevvyVo request) {
        devvyMapper.updateSessionMetadata(request);
    }

    private DevvyVo createChatResponse(String userQuery, String sessionId) {
        return DevvyVo.createChatResponse(userQuery, sessionId);
    }

    // === 통계 기능 ===
    public DevvyVo getUserStatistics(String userId) {
        DevvyVo stats = new DevvyVo();
        stats.setMessageCount(devvyMapper.getUserMessageCount(userId));
        stats.setUserId(userId);
        return stats;
    }

    @Transactional
    public int cleanupOldData(int retentionDays) {
        return devvyMapper.deleteOldData(retentionDays);
    }
}