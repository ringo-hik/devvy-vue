package com.example.devvy.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Devvy 챗봇 통합 데이터 객체
 * - userQuery 필드명 통일 적용
 * - 카테고리 Text 기반 매칭 구조
 * - 미니멀 설계 원칙 적용
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DevvyVo {

    // === API 응답 구조 ===
    private Boolean success;
    private String message;
    private String errorMessage;
    private Object data;
    private LocalDateTime timestamp;

    // === 카테고리 (Text 기반) ===
    private String categoryCode;     // swdp_menu, project, voc
    private String categoryName;
    private String description;
    private String icon;
    private Integer sortOrder;

    // === 채팅 & 세션 ===
    private String sessionId;
    private String userId;
    private String userQuery;        // 통일된 필드명
    private String messageType;      // USER, AI, SYSTEM
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;

    // === 히스토리 ===
    private Integer messageCount;
    private String firstMessage;

    // === 피드백 ===
    private Integer rating;
    private String feedbackCategory;
    private String comment;

    // === 정적 팩토리 메서드 ===
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

    public static DevvyVo createChatResponse(String userQuery, String sessionId) {
        DevvyVo vo = new DevvyVo();
        vo.setSuccess(true);
        vo.setUserQuery(userQuery);
        vo.setSessionId(sessionId);
        vo.setTimestamp(LocalDateTime.now());
        return vo;
    }

    // === Getters and Setters ===
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
    
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }
    
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