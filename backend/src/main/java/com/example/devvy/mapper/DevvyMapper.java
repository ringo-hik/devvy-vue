package com.example.devvy.mapper;

import com.example.devvy.vo.DevvyVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * Devvy 챗봇 데이터베이스 접근 인터페이스
 * - 카테고리 Text 매칭 기반 설계
 * - userQuery 필드명 통일 적용
 * - 미니멀 설계 원칙 준수
 */
@Mapper
public interface DevvyMapper {

    // === 카테고리 관리 ===
    List<DevvyVo> selectCategories();

    // === 메시지 관리 ===
    int insertMessage(DevvyVo messageVo);
    List<DevvyVo> selectSessionMessages(@Param("sessionId") String sessionId, @Param("userId") String userId);

    // === 세션 관리 ===
    int insertSession(DevvyVo sessionVo);
    int updateSessionMetadata(DevvyVo sessionVo);
    List<DevvyVo> selectUserSessions(@Param("userId") String userId);
    int checkSessionExists(@Param("sessionId") String sessionId, @Param("userId") String userId);

    // === 피드백 관리 ===
    int insertFeedback(DevvyVo feedbackVo);

    // === 통계 관리 ===
    int getUserMessageCount(@Param("userId") String userId);
    int getUserActiveSessionCount(@Param("userId") String userId);
    int deleteOldData(@Param("days") int days);

    // === Dummy 시스템 연동 ===
    int dummySystemHealthCheck();
}
