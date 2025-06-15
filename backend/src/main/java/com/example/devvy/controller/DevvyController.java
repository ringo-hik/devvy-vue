package com.example.devvy.controller;

import com.example.devvy.service.DevvyService;
import com.example.devvy.vo.DevvyVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Devvy 챗봇 API 컨트롤러
 * - 카테고리 Text 기반 매칭
 * - userQuery 필드명 통일
 * - getUserId() 패턴 적용
 * - 미니멀 설계 원칙 준수
 */
@RestController
@RequestMapping("/api/v1/devportal/devvy")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DevvyController {

    private static final Logger log = LoggerFactory.getLogger(DevvyController.class);

    @Autowired
    private DevvyService devvyService;

    // === 시스템 상태 관리 ===
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            DevvyVo healthStatus = devvyService.performHealthCheck();
            
            Map<String, Object> response = Map.of(
                "status", "OK",
                "service", "Devvy Bot API",
                "timestamp", LocalDateTime.now(),
                "systemHealth", healthStatus.getData()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("헬스체크 실패", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "ERROR", "timestamp", LocalDateTime.now()));
        }
    }

    // === 카테고리 관리 ===
    @GetMapping("/categories")
    public ResponseEntity<DevvyVo> getCategories() {
        log.info("카테고리 목록 조회 요청");
        
        try {
            List<DevvyVo> categories = devvyService.getCategories();
            DevvyVo response = DevvyVo.createSuccessResponse(categories, "카테고리 조회 성공");
            
            log.info("카테고리 조회 성공 ({}개)", categories.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카테고리 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DevvyVo.createErrorResponse("카테고리 조회 실패"));
        }
    }

    // === 채팅 처리 ===
    @PostMapping("/chat")
    public ResponseEntity<DevvyVo> chat(@Valid @RequestBody DevvyVo request) {
        String userId = getUserId();
        request.setUserId(userId);
        
        log.info("채팅 요청 - User: {}, Category: {}", userId, request.getCategoryCode());

        try {
            validateChatRequest(request);
            
            DevvyVo aiResponse = devvyService.processDevvyChat(request);
            DevvyVo response = createStandardizedChatResponse(aiResponse);
            
            log.info("AI 응답 성공 - Session: {}", aiResponse.getSessionId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(DevvyVo.createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("채팅 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DevvyVo.createErrorResponse("채팅 처리 중 오류 발생"));
        }
    }

    // === 히스토리 관리 ===
    @GetMapping("/history")
    public ResponseEntity<DevvyVo> getChatHistory() {
        String userId = getUserId();
        log.info("히스토리 조회 요청 - User: {}", userId);
        
        try {
            List<DevvyVo> history = devvyService.getChatHistory(userId);
            DevvyVo response = DevvyVo.createSuccessResponse(history, 
                String.format("히스토리 조회 성공 (%d개)", history.size()));
            
            log.info("히스토리 조회 성공 - {}개 세션", history.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("히스토리 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DevvyVo.createErrorResponse("히스토리 조회 실패"));
        }
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<DevvyVo> getSessionMessages(@PathVariable @NotBlank String sessionId) {
        String userId = getUserId();
        log.info("세션 메시지 조회 - Session: {}, User: {}", sessionId, userId);
        
        try {
            List<DevvyVo> messages = devvyService.getSessionMessages(sessionId, userId);
            DevvyVo response = DevvyVo.createSuccessResponse(messages,
                String.format("메시지 조회 성공 (%d개)", messages.size()));
            
            log.info("세션 메시지 조회 성공 - {}개 메시지", messages.size());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("세션 접근 권한 없음 - User: {}", userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(DevvyVo.createErrorResponse("접근 권한 없음"));
                
        } catch (Exception e) {
            log.error("세션 메시지 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DevvyVo.createErrorResponse("메시지 조회 실패"));
        }
    }

    // === 피드백 관리 ===
    @PostMapping("/feedback")
    public ResponseEntity<DevvyVo> saveFeedback(@Valid @RequestBody DevvyVo request) {
        String userId = getUserId();
        request.setUserId(userId);
        
        log.info("피드백 저장 요청 - User: {}, Rating: {}", userId, request.getRating());

        try {
            validateFeedbackRequest(request);
            devvyService.saveFeedback(request);
            
            DevvyVo response = DevvyVo.createSuccessResponse(null, "피드백 저장 성공");
            
            log.info("피드백 저장 성공");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 피드백: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(DevvyVo.createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("피드백 저장 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DevvyVo.createErrorResponse("피드백 저장 실패"));
        }
    }

    // === Private Helper Methods ===

    /**
     * 사용자 ID 조회 (개별 구현 시스템 연동)
     * 실제 환경에서는 사용자별 인증 시스템과 연동
     */
    private String getUserId() {
        // TODO: 개별 구현된 인증 시스템과 연동
        // SecurityContext, JWT Token, Session 등에서 사용자 ID 추출
        return "devvy-user-01";
    }

    /**
     * 채팅 요청 데이터 검증
     */
    private void validateChatRequest(DevvyVo request) {
        if (request.getCategoryCode() == null || request.getCategoryCode().trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리 코드는 필수입니다.");
        }
        if (request.getUserQuery() == null || request.getUserQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 질의는 필수입니다.");
        }
        if (request.getUserQuery().length() > 1000) {
            throw new IllegalArgumentException("질의는 1000자를 초과할 수 없습니다.");
        }
    }

    /**
     * 피드백 요청 데이터 검증
     */
    private void validateFeedbackRequest(DevvyVo request) {
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("평점은 1-5 사이 값이어야 합니다.");
        }
        if (request.getComment() != null && request.getComment().length() > 1000) {
            throw new IllegalArgumentException("피드백은 1000자를 초과할 수 없습니다.");
        }
    }

    /**
     * 표준화된 채팅 응답 생성
     */
    private DevvyVo createStandardizedChatResponse(DevvyVo serviceResponse) {
        DevvyVo response = new DevvyVo();
        response.setSuccess(true);
        response.setMessage(serviceResponse.getUserQuery());  // AI 응답 내용
        response.setSessionId(serviceResponse.getSessionId());
        response.setTimestamp(serviceResponse.getTimestamp());
        return response;
    }
}
