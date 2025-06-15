-- Devvy Bot 최종 데이터베이스 스키마
-- 카테고리 Text 매칭 기반 단일 테이블 설계
-- userQuery 필드명 통일 및 미니멀 구조

CREATE TABLE devvy_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(100) NOT NULL COMMENT '사용자 식별자',
    session_id VARCHAR(50) NOT NULL COMMENT '세션 고유 식별자',
    
    -- 메시지 관련 (userQuery 통일)
    message_type ENUM('USER', 'AI', 'SYSTEM') NOT NULL COMMENT '메시지 타입',
    user_query TEXT COMMENT '사용자 질의 및 AI 응답 내용',
    
    -- 카테고리 정보 (Text 기반 매칭)
    category_code VARCHAR(50) COMMENT '카테고리 코드 (swdp_menu, project, voc)',
    category_info JSON COMMENT '카테고리 상세 정보',
    
    -- 세션 메타데이터
    session_metadata JSON COMMENT '세션 정보 (메시지 수, 첫 메시지 등)',
    
    -- 피드백 정보
    feedback_data JSON COMMENT '피드백 정보 (rating, comment, category)',
    
    -- 타임스탬프
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 데이터 타입 구분
    data_type ENUM('MESSAGE', 'SESSION', 'FEEDBACK') NOT NULL DEFAULT 'MESSAGE',
    
    -- 최적화된 인덱스
    INDEX idx_user_session (user_id, session_id),
    INDEX idx_category_code (category_code),
    INDEX idx_data_type_created (data_type, created_at DESC),
    INDEX idx_user_created (user_id, created_at DESC)
) COMMENT='Devvy Bot 통합 데이터 테이블 - userQuery 필드명 통일';

-- 카테고리 기본 데이터 (Text 코드 기반)
INSERT INTO devvy_data (
    user_id, 
    session_id, 
    message_type, 
    user_query, 
    category_info, 
    data_type
) VALUES 
('SYSTEM', 'CATEGORY_INIT', 'SYSTEM', 'Category initialization', 
 JSON_OBJECT(
     'categories', JSON_ARRAY(
         JSON_OBJECT('categoryCode', 'swdp_menu', 'categoryName', 'SWDP Menu', 'description', 'SWDP 시스템 메뉴 및 기능 안내', 'icon', 'menu-icon.svg', 'sortOrder', 1),
         JSON_OBJECT('categoryCode', 'project', 'categoryName', '프로젝트', 'description', '프로젝트 관리 및 현황 조회', 'icon', 'project-icon.svg', 'sortOrder', 2),
         JSON_OBJECT('categoryCode', 'voc', 'categoryName', 'VOC', 'description', 'VOC 및 이슈 관리 지원', 'icon', 'voc-icon.svg', 'sortOrder', 3)
     )
 ), 
 'SESSION'
);

-- 히스토리 제한 트리거 (최대 10개)
DELIMITER //
CREATE TRIGGER devvy_history_limit 
AFTER INSERT ON devvy_data
FOR EACH ROW
BEGIN
    DECLARE session_count INT;
    
    IF NEW.data_type = 'SESSION' AND NEW.user_id != 'SYSTEM' THEN
        SELECT COUNT(DISTINCT session_id) INTO session_count
        FROM devvy_data 
        WHERE user_id = NEW.user_id 
        AND data_type = 'SESSION'
        AND user_id != 'SYSTEM';
        
        IF session_count > 10 THEN
            DELETE FROM devvy_data 
            WHERE session_id IN (
                SELECT session_id FROM (
                    SELECT session_id 
                    FROM devvy_data 
                    WHERE user_id = NEW.user_id 
                    AND data_type = 'SESSION'
                    AND user_id != 'SYSTEM'
                    ORDER BY created_at ASC 
                    LIMIT 1
                ) AS oldest_session
            );
        END IF;
    END IF;
END//
DELIMITER ;

-- 성능 최적화 인덱스
CREATE INDEX idx_category_code_type ON devvy_data (category_code, data_type);
CREATE INDEX idx_session_type_created ON devvy_data (session_id, data_type, created_at);
