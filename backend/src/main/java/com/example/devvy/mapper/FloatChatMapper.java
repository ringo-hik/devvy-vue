package com.temporary22.devportal.mapper.aiagent;

import com.temporary22.aiagent.entity.FloatChatVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * SWP Float Chat 데이터베이스 접근 인터페이스 (프롬프트 관리 추가)
 */
@Mapper
public interface FloatChatMapper {

    // === 카테고리 관리 ===
    
    /**
     * 활성화된 카테고리 목록을 조회합니다. (시스템 프롬프트 포함)
     */
    List<FloatChatVo> selectCategories();

    /**
     * 특정 카테고리 정보를 조회합니다. (시스템 프롬프트 포함)
     */
    FloatChatVo selectCategoryByCode(@Param("categoryCode") String categoryCode);

    /**
     * 카테고리 존재 여부를 확인합니다.
     */
    int checkCategoryExists(@Param("categoryCode") String categoryCode);

    // === 프롬프트 관리 ===
    
    /**
     * 카테고리의 시스템 프롬프트를 업데이트합니다.
     */
    int updateSystemPrompt(@Param("categoryCode") String categoryCode, 
                          @Param("systemPrompt") String systemPrompt,
                          @Param("userId") String userId);

    /**
     * 특정 카테고리의 시스템 프롬프트만 조회합니다.
     */
    String selectSystemPrompt(@Param("categoryCode") String categoryCode);

    // === 대화 관리 ===
    
    /**
     * 질문-답변 대화를 저장합니다.
     */
    int insertConversation(FloatChatVo conversationVo);

    /**
     * 사용자별 카테고리별 최신 대화 목록을 조회합니다.
     */
    List<FloatChatVo> selectRecentConversations(@Param("userId") String userId, 
                                               @Param("categoryCode") String categoryCode,
                                               @Param("limit") int limit);

    /**
     * 사용자별 전체 카테고리 대화 수를 조회합니다.
     */
    int getTotalConversationCount(@Param("userId") String userId, @Param("categoryCode") String categoryCode);

    // === 피드백 관리 ===
    
    /**
     * 피드백을 저장합니다.
     */
    int insertFeedback(FloatChatVo feedbackVo);

    // === 시스템 관리 ===
    
    /**
     * 시스템 상태를 확인합니다.
     */
    int healthCheck();

    /**
     * 오래된 대화 데이터를 정리합니다.
     */
    int deleteOldConversations(@Param("days") int days);
}