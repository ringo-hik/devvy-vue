package com.temporary22.aiagent.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * SWP Float Chat 데이터 객체 (시스템 프롬프트 추가)
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FloatChatVo {

    // === API 응답 구조 ===
    private Boolean success;
    private String message;
    private String errorMessage;
    private Object data;
    private LocalDateTime timestamp;

    // === 카테고리 정보 ===
    private Long categoryId;
    private String categoryCode;
    private String description;
    private String descriptionEn;
    private String iconPath;
    private String activeYn;
    private String systemPrompt;  // 새로 추가된 시스템 프롬프트

    // === 대화 정보 ===
    private Long conversationId;
    private String userId;
    private String userQuestion;
    private String aiResponse;
    private LocalDateTime createdDate;

    // === 피드백 ===
    private Integer rating;
    private String feedbackCategory;
    private String comment;

    // === 정적 팩토리 메서드 ===
    public static FloatChatVo createSuccessResponse(Object data, String message) {
        FloatChatVo vo = new FloatChatVo();
        vo.setSuccess(true);
        vo.setData(data);
        vo.setMessage(message);
        vo.setTimestamp(LocalDateTime.now());
        return vo;
    }
    
    public static FloatChatVo createErrorResponse(String errorMessage) {
        FloatChatVo vo = new FloatChatVo();
        vo.setSuccess(false);
        vo.setErrorMessage(errorMessage);
        vo.setTimestamp(LocalDateTime.now());
        return vo;
    }

    public static FloatChatVo createAIResponse(String aiResponse) {
        FloatChatVo vo = new FloatChatVo();
        vo.setSuccess(true);
        vo.setAiResponse(aiResponse);
        vo.setTimestamp(LocalDateTime.now());
        return vo;
    }

    // === 유효성 검증 메서드 ===
    public boolean isValidMessage() {
        return categoryCode != null && !categoryCode.trim().isEmpty() &&
               userQuestion != null && !userQuestion.trim().isEmpty() &&
               userQuestion.length() <= 1000;
    }

    public boolean isValidFeedback() {
        return rating != null && rating >= 1 && rating <= 5 &&
               (comment == null || comment.length() <= 1000);
    }

    // === 프롬프트 관련 검증 메서드 ===
    public boolean isValidPromptUpdate() {
        return categoryCode != null && !categoryCode.trim().isEmpty() &&
               systemPrompt != null && !systemPrompt.trim().isEmpty() &&
               systemPrompt.length() <= 5000; // 프롬프트 최대 길이 제한
    }
}