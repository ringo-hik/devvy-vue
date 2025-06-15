package com.example.devvy.mapper;

import com.example.devvy.vo.DevvyVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Devvy 챗봇 데이터베이스 접근을 위한 MyBatis Mapper 인터페이스
 * @Refactored: SmartSearchMapper -> DevvyMapper
 */
@Mapper
public interface DevvyMapper {

    /**
     * 모든 활성화된 카테고리 목록을 조회합니다.
     */
    List<DevvyVo> selectCategories();

    /**
     * 사용자 질문에 대한 컨텍스트를 제공하기 위해 카테고리별 데이터를 조회합니다. (Dummy)
     * 실제로는 RAG에 사용될 관련 문서를 검색하는 쿼리가 됩니다.
     */
    List<DevvyVo> selectContextDataByCategory(@Param("categoryId") Long categoryId);

    /**
     * 채팅 메시지 (사용자 또는 AI)를 DB에 저장합니다.
     */
    int insertChatMessage(DevvyVo messageVo);

    /**
     * 사용자의 전체 대화 세션 목록 (히스토리)을 조회합니다.
     */
    List<DevvyVo> selectChatHistory(@Param("userId") String userId);

    /**
     * 특정 세션에 속한 모든 메시지를 시간순으로 조회합니다.
     */
    List<DevvyVo> selectSessionMessages(@Param("sessionId") String sessionId, @Param("userId") String userId);

    /**
     * 새로운 대화 세션을 생성합니다.
     */
    int insertChatSession(DevvyVo sessionVo);
    
    /**
     * 기존 대화 세션의 정보를 (마지막 메시지 시간, 메시지 수) 업데이트합니다.
     */
    int updateChatSession(DevvyVo sessionVo);

    /**

     * 사용자 피드백을 DB에 저장합니다.
     */
    int insertFeedback(DevvyVo feedbackVo);
}
