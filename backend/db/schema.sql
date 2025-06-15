-- Devvy Bot Database Schema

CREATE TABLE devvy_category (
    category_id BIGINT PRIMARY KEY,
    category_name VARCHAR(100),
    description TEXT,
    icon VARCHAR(50),
    sort_order INT,
    is_active BOOLEAN
);

CREATE TABLE devvy_session (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(50),
    category_id BIGINT,
    message_count INT,
    created_at TIMESTAMP,
    last_message_at TIMESTAMP
);

CREATE TABLE devvy_message (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64),
    user_id VARCHAR(50),
    category_id BIGINT,
    message_type VARCHAR(10),
    user_query TEXT,
    timestamp TIMESTAMP
);

CREATE TABLE devvy_feedback (
    feedback_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50),
    session_id VARCHAR(64),
    rating INT,
    comment TEXT,
    feedback_category VARCHAR(50),
    created_at TIMESTAMP
);

CREATE TABLE devvy_context (
    context_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    context_data TEXT,
    related_info TEXT,
    is_active BOOLEAN DEFAULT TRUE
);
