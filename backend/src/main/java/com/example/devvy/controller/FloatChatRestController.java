package com.temporary22.aiagent.rest;

import com.temporary22.aiagent.impl.aiAgent.FloatChatService;
import com.temporary22.aiagent.entity.FloatChatVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SWP Float Chat REST API 컨트롤러 (비동기 처리 및 프롬프트 관리 추가)
 */
@RestController
@RequestMapping("/api/v1/devportal/float-chat")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FloatChatRestController {

    private static final Logger log = LoggerFactory.getLogger(FloatChatRestController.class);

    @Autowired
    private FloatChatService floatChatService;

    // === 시스템 상태 ===

    /**
     * 시스템 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            FloatChatVo healthStatus = floatChatService.performHealthCheck();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", "OK");
            response.put("service", "SWP Float Chat API");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("헬스체크 실패", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("status", "ERROR");
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
        }
    }

    // === 카테고리 관리 ===

    /**
     * 활성화된 카테고리 목록을 조회합니다. (시스템 프롬프트 포함)
     */
    @GetMapping("/categories")
    public ResponseEntity<FloatChatVo> getCategories() {
        log.info("카테고리 목록 조회 요청");
        
        try {
            List<FloatChatVo> categories = floatChatService.getCategories();
            FloatChatVo response = FloatChatVo.createSuccessResponse(categories, "카테고리 조회 성공");
            
            log.info("카테고리 조회 성공 ({}개)", categories.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카테고리 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FloatChatVo.createErrorResponse("카테고리 조회 실패"));
        }
    }

    /**
     * 특정 카테고리 정보를 조회합니다.
     */
    @GetMapping("/categories/{categoryCode}")
    public ResponseEntity<FloatChatVo> getCategoryByCode(@PathVariable @NotBlank String categoryCode) {
        log.info("카테고리 상세 조회 요청: {}", categoryCode);
        
        try {
            FloatChatVo category = floatChatService.getCategoryByCode(categoryCode);
            FloatChatVo response = FloatChatVo.createSuccessResponse(category, "카테고리 조회 성공");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(FloatChatVo.createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("카테고리 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FloatChatVo.createErrorResponse("카테고리 조회 실패"));
        }
    }

    // === 프롬프트 관리 ===

    /**
     * 카테고리의 시스템 프롬프트를 조회합니다.
     */
    @GetMapping("/categories/{categoryCode}/prompt")
    public ResponseEntity<FloatChatVo> getSystemPrompt(@PathVariable @NotBlank String categoryCode) {
        log.info("시스템 프롬프트 조회 요청: {}", categoryCode);
        
        try {
            String systemPrompt = floatChatService.getSystemPrompt(categoryCode);
            
            Map<String, Object> data = new HashMap<>();
            data.put("categoryCode", categoryCode);
            data.put("systemPrompt", systemPrompt);
            
            FloatChatVo response = FloatChatVo.createSuccessResponse(data, "시스템 프롬프트 조회 성공");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(FloatChatVo.createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("시스템 프롬프트 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FloatChatVo.createErrorResponse("시스템 프롬프트 조회 실패"));
        }
    }

    /**
     * 카테고리의 시스템 프롬프트를 업데이트합니다.
     */
    @PutMapping("/categories/{categoryCode}/prompt")
    public ResponseEntity<FloatChatVo> updateSystemPrompt(
            @PathVariable @NotBlank String categoryCode,
            @RequestBody @Valid FloatChatVo request) {
        
        String userId = getUserId();
        log.info("시스템 프롬프트 업데이트 요청 - Category: {}, User: {}", categoryCode, userId);
        
        try {
            // 요청에서 시스템 프롬프트 추출
            String systemPrompt = request.getSystemPrompt();
            if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(FloatChatVo.createErrorResponse("시스템 프롬프트는 필수입니다."));
            }
            
            floatChatService.updateSystemPrompt(categoryCode, systemPrompt, userId);
            
            FloatChatVo response = FloatChatVo.createSuccessResponse(null, "시스템 프롬프트 업데이트 성공");
            
            log.info("시스템 프롬프트 업데이트 성공");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(FloatChatVo.createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("시스템 프롬프트 업데이트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FloatChatVo.createErrorResponse("시스템 프롬프트 업데이트 실패"));
        }
    }

    // === 메시지 처리 (비동기 지원) ===

    /**
     * 사용자 질문을 처리하고 AI 응답을 반환합니다. (비동기 처리)
     */
    @PostMapping("/message/async")
    public DeferredResult<ResponseEntity<FloatChatVo>> sendMessageAsync(@Valid @RequestBody FloatChatVo request) {
        String userId = getUserId();
        request.setUserId(userId);
        
        log.info("비동기 메시지 요청 - User: {}, Category: {}", userId, request.getCategoryCode());

        DeferredResult<ResponseEntity<FloatChatVo>> deferredResult = new DeferredResult<>(30000L); // 30초 타임아웃
        
        try {
            validateMessageRequest(request);
            
            CompletableFuture<FloatChatVo> future = floatChatService.processMessageAsync(request);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("비동기 메시지 처리 실패", ex);
                    deferredResult.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(FloatChatVo.createErrorResponse("메시지 처리 중 오류 발생")));
                } else {
                    log.info("비동기 메시지 응답 성공 - ConversationId: {}", result.getConversationId());
                    deferredResult.setResult(ResponseEntity.ok(result));
                }
            });
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            deferredResult.setResult(ResponseEntity.badRequest()
                .body(FloatChatVo.createErrorResponse(e.getMessage())));
                
        } catch (Exception e) {
            log.error("비동기 메시지 처리 초기화 실패", e);
            deferredResult.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FloatChatVo.createErrorResponse("메시지 처리 초기화 실패")));
        }
        
        // 타임아웃 처리
        deferredResult.onTimeout(() -> {
            log.warn("비동기 메시지 처리 타임아웃 - User: {}", userId);
            deferredResult.setResult(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(FloatChatVo.createErrorResponse("응답 생성 시간이 초과되었습니다. 다시 시도해주세요.")));
        });
        
        return deferredResult;
    }

    /**
     * 사용자 질문을 처리하고 AI 응답을 반환합니다. (기존 동기식 - 호환성 유지)
     */
    @PostMapping("/message")
    public ResponseEntity<FloatChatVo> sendMessage(@Valid @RequestBody FloatChatVo request) {
        String userId = getUserId();
        request.setUserId(userId);
        
        log.info("동기 메시지 요청 - User: {}, Category: {}", userId, request.getCategoryCode());

        try {
            validateMessageRequest(request);
            
            FloatChatVo response = floatChatService.processMessage(request);
            
            log.info("동기 메시지 응답 성공 - ConversationId: {}", response.getConversationId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(FloatChatVo.createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("동기 메시지 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FloatChatVo.createErrorResponse("메시지 처리 중 오류 발생"));
        }
    }

    // === 대화 기록 ===

    /**
     * 사용자별 카테고리별 최신 대화 목록을 조회합니다.
     */
    @GetMapping("/conversations/{categoryCode}")
    public ResponseEntity<FloatChatVo> getConversations(
            @PathVariable @NotBlank String categoryCode) {
        String userId = getUserId();
        log.info("대화 목록 조회 - User: {}, Category: {}", userId, categoryCode);
        
        try {
            List<FloatChatVo> conversations = floatChatService.getRecentConversations(userId, categoryCode);
            FloatChatVo response = FloatChatVo.createSuccessResponse(conversations, 
                String.format("대화 목록 조회 성공 (%d개)", conversations.size()));
            
            log.info("대화 목록 조회 성공 - {}개 대화", conversations.size());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(FloatChatVo.createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("대화 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FloatChatVo.createErrorResponse("대화 목록 조회 실패"));
        }
    }

    // === 피드백 ===

    /**
     * 사용자 피드백을 저장합니다.
     */
    @PostMapping("/feedback")
    public ResponseEntity<FloatChatVo> saveFeedback(@Valid @RequestBody FloatChatVo request) {
        String userId = getUserId();
        request.setUserId(userId);
        
        log.info("피드백 저장 요청 - User: {}, Rating: {}", userId, request.getRating());

        try {
            validateFeedbackRequest(request);
            
            floatChatService.saveFeedback(request);
            
            FloatChatVo response = FloatChatVo.createSuccessResponse(null, "피드백 저장 성공");
            
            log.info("피드백 저장 성공");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 피드백: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(FloatChatVo.createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("피드백 저장 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FloatChatVo.createErrorResponse("피드백 저장 실패"));
        }
    }

    // === Private Helper Methods ===

    /**
     * 현재 사용자 ID를 반환합니다.
     */
    private String getUserId() {
        // TODO: Spring Security 연동 시 SecurityContextHolder에서 사용자 정보 가져오기
        // return SecurityContextHolder.getContext().getAuthentication().getName();
        return "dev-user";
    }

    /**
     * 메시지 요청의 유효성을 검증합니다.
     */
    private void validateMessageRequest(FloatChatVo request) {
        if (!request.isValidMessage()) {
            if (request.getCategoryCode() == null || request.getCategoryCode().trim().isEmpty()) {
                throw new IllegalArgumentException("카테고리 코드는 필수입니다.");
            }
            if (request.getUserQuestion() == null || request.getUserQuestion().trim().isEmpty()) {
                throw new IllegalArgumentException("질문 내용은 필수입니다.");
            }
            if (request.getUserQuestion().length() > 1000) {
                throw new IllegalArgumentException("질문은 1000자를 초과할 수 없습니다.");
            }
        }
    }

    /**
     * 피드백 요청의 유효성을 검증합니다.
     */
    private void validateFeedbackRequest(FloatChatVo request) {
        if (!request.isValidFeedback()) {
            if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
                throw new IllegalArgumentException("평점은 1-5 사이 값이어야 합니다.");
            }
            if (request.getComment() != null && request.getComment().length() > 1000) {
                throw new IllegalArgumentException("피드백은 1000자를 초과할 수 없습니다.");
            }
        }
    }
}