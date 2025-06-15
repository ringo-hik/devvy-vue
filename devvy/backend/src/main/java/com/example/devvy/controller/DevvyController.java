package com.example.devvy.controller;

import com.example.devvy.service.DevvyService;
import com.example.devvy.vo.DevvyVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Devvy 챗봇 API 컨트롤러
 * @Refactored: SmartSearchController -> DevvyController
 */
@RestController
@RequestMapping("/api/v1/devportal/devvy")
@CrossOrigin(origins = "*") // 개발 환경을 위한 CORS 설정
public class DevvyController {

    private static final Logger log = LoggerFactory.getLogger(DevvyController.class);

    @Autowired
    private DevvyService devvyService;

    /**
     * 서비스 상태를 확인하는 Health Check API
     * @return 서비스 상태 응답
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "Devvy Bot API", "timestamp", LocalDateTime.now()));
    }

    /**
     * 챗봇 카테고리 목록을 조회합니다.
     * @return 카테고리 목록 응답
     */
    @GetMapping("/categories")
    public ResponseEntity<DevvyVo> getCategories() {
        log.info("▶️ GET /categories - 카테고리 목록 조회 요청");
        try {
            List<DevvyVo> categories = devvyService.getCategories();
            DevvyVo response = DevvyVo.createSuccessResponse(categories, "카테고리 조회 성공");
            log.info("✅ GET /categories - 조회 성공 ({}개)", categories.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ GET /categories - 조회 실패", e);
            return ResponseEntity.badRequest().body(DevvyVo.createErrorResponse("카테고리 조회에 실패했습니다."));
        }
    }

    /**
     * 챗봇과 대화를 처리합니다.
     * @param request 채팅 요청 데이터 (categoryId, message, sessionId 등)
     * @return AI 응답 데이터
     */
    @PostMapping("/chat")
    public ResponseEntity<DevvyVo> chat(@RequestBody DevvyVo request) {
        String userId = getUserIdFromContext(); // 실제 환경에서는 인증 컨텍스트에서 사용자 ID를 가져와야 합니다.
        request.setUserId(userId);
        log.info("▶️ POST /chat - 채팅 요청 (User: {}, Category: {})", userId, request.getCategoryId());

        try {
            DevvyVo aiResponse = devvyService.processDevvyChat(request);
            log.info("✅ POST /chat - AI 응답 성공 (Session: {})", aiResponse.getSessionId());
            return ResponseEntity.ok(DevvyVo.createSuccessResponse(aiResponse, "AI 응답 생성 성공"));
        } catch (Exception e) {
            log.error("❌ POST /chat - 채팅 처리 실패", e);
            return ResponseEntity.internalServerError().body(DevvyVo.createErrorResponse("채팅 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 사용자의 전체 대화 히스토리 목록을 조회합니다.
     * @return 대화 히스토리 목록
     */
    @GetMapping("/history")
    public ResponseEntity<DevvyVo> getChatHistory() {
        String userId = getUserIdFromContext();
        log.info("▶️ GET /history - 히스토리 조회 요청 (User: {})", userId);
        try {
            List<DevvyVo> history = devvyService.getChatHistory(userId);
            log.info("✅ GET /history - 조회 성공 ({}개 세션)", history.size());
            return ResponseEntity.ok(DevvyVo.createSuccessResponse(history, "히스토리 조회 성공"));
        } catch (Exception e) {
            log.error("❌ GET /history - 조회 실패", e);
            return ResponseEntity.badRequest().body(DevvyVo.createErrorResponse("히스토리 조회에 실패했습니다."));
        }
    }

    /**
     * 특정 세션의 상세 메시지 목록을 조회합니다.
     * @param sessionId 조회할 세션 ID
     * @return 메시지 목록
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<DevvyVo> getSessionMessages(@PathVariable String sessionId) {
        String userId = getUserIdFromContext();
        log.info("▶️ GET /sessions/{}/messages - 세션 메시지 조회 요청 (User: {})", sessionId, userId);
        try {
            List<DevvyVo> messages = devvyService.getSessionMessages(sessionId, userId);
            log.info("✅ GET /sessions/{}/messages - 조회 성공 ({}개 메시지)", sessionId, messages.size());
            return ResponseEntity.ok(DevvyVo.createSuccessResponse(messages, "세션 메시지 조회 성공"));
        } catch (Exception e) {
            log.error("❌ GET /sessions/{}/messages - 조회 실패", sessionId, e);
            return ResponseEntity.badRequest().body(DevvyVo.createErrorResponse("세션 메시지 조회에 실패했습니다."));
        }
    }

    /**
     * 사용자의 피드백을 저장합니다.
     * @param request 피드백 데이터 (rating, content, category 등)
     * @return 처리 결과
     */
    @PostMapping("/feedback")
    public ResponseEntity<DevvyVo> saveFeedback(@RequestBody DevvyVo request) {
        String userId = getUserIdFromContext();
        request.setUserId(userId);
        log.info("▶️ POST /feedback - 피드백 저장 요청 (User: {}, Rating: {})", userId, request.getRating());

        try {
            devvyService.saveFeedback(request);
            log.info("✅ POST /feedback - 저장 성공");
            return ResponseEntity.ok(DevvyVo.createSuccessResponse(null, "피드백이 성공적으로 저장되었습니다."));
        } catch (Exception e) {
            log.error("❌ POST /feedback - 저장 실패", e);
            return ResponseEntity.internalServerError().body(DevvyVo.createErrorResponse("피드백 저장 중 오류가 발생했습니다."));
        }
    }

    /**
     * Spring Security 등 인증 컨텍스트에서 사용자 ID를 가져오는 메서드 (현재는 더미)
     * @return 사용자 ID
     */
    private String getUserIdFromContext() {
        // TODO: 실제 환경에서는 SecurityContextHolder.getContext().getAuthentication().getName(); 등으로 구현
        return "devvy-user-01";
    }
}
