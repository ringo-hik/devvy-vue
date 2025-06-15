package com.example.devvy.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Devvy 챗봇 API를 위한 VO (Value Object) / DTO (Data Transfer Object)
 * - 프론트엔드와 백엔드 간 데이터 교환에 사용되는 모든 필드를 포함합니다.
 * - Null 값은 JSON 변환 시 제외하여 응답을 간결하게 유지합니다.
 * @Refactored: SmartSearchVo -> DevvyVo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DevvyVo {

    // --- 공통 & API 응답 ---
    private Boolean success;
    private String message;
    private String errorMessage;
    private Object data; // List<DevvyVo> 등 다양한 데이터를 담기 위함
    private LocalDateTime timestamp;

    // --- 카테고리 ---
    private Long categoryId;
    private String categoryName;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Boolean isActive;
    
    // --- 채팅 & 세션 ---
    private String sessionId;
    private String userId;
    private String content;      // message 필드 대신 content로 통일
    private String messageType;  // "USER" or "AI"
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;

    // --- 히스토리 ---
    private Integer messageCount;
    private String firstMessage;

    // --- 피드백 ---
    private Integer rating;
    private String feedbackCategory; // 피드백 카테고리
    private String comment;          // 피드백 내용

    // --- 정적 팩토리 메서드 (응답 객체 생성을 위함) ---
    public static DevvyVo createSuccessResponse(Object data, String message) {
        DevvyVo vo = new DevvyVo();
        vo.setSuccess(true);
        vo.setData(data);
        vo.setMessage(message);
        vo.setTimestamp(LocalDateTime.now());
        return vo;
    }
    
    public static DevvyVo createErrorResponse(String errorMessage) {
        DevvyVo vo = new DevvyVo();
        vo.setSuccess(false);
        vo.setErrorMessage(errorMessage);
        vo.setTimestamp(LocalDateTime.now());
        return vo;
    }

    // --- Getters and Setters ---
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    public String getFirstMessage() { return firstMessage; }
    public void setFirstMessage(String firstMessage) { this.firstMessage = firstMessage; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getFeedbackCategory() { return feedbackCategory; }
    public void setFeedbackCategory(String feedbackCategory) { this.feedbackCategory = feedbackCategory; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
