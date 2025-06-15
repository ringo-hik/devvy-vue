/**
 * Devvy Bot 프론트엔드 API 서비스
 * - userQuery 필드명 통일 적용
 * - 카테고리 Text 기반 매칭
 * - 미니멀 설계 원칙 준수
 */

import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api/v1/devportal/devvy',
  headers: {
    'Content-Type': 'application/json',
  }
});

/**
 * API 응답 표준화 처리
 */
const handleRequest = async (request) => {
  try {
    const response = await request;
    
    if (response.data && response.data.success) {
      return {
        success: true,
        data: response.data.data,
        message: response.data.message,
        sessionId: response.data.sessionId,
        timestamp: response.data.timestamp
      };
    }
    
    if (response.status === 200 && !response.data.hasOwnProperty('success')) {
      return { success: true, data: response.data };
    }
    
    return {
      success: false,
      error: response.data.errorMessage || 'Unknown error occurred.'
    };
    
  } catch (error) {
    console.error('API Request Failed:', error);
    
    const errorMessage = error.response?.data?.errorMessage || 
                        error.response?.data?.message || 
                        error.message || 
                        'Network error or server not responding.';
                        
    return {
      success: false,
      error: errorMessage
    };
  }
};

/**
 * 카테고리 데이터 정규화
 */
const normalizeCategoryData = (categories) => {
  if (!Array.isArray(categories)) return [];
  
  return categories.map(category => ({
    categoryCode: category.categoryCode,
    categoryName: category.categoryName,
    description: category.description,
    icon: category.icon,
    sortOrder: category.sortOrder
  }));
};

/**
 * 메시지 데이터 정규화 (userQuery 기반)
 */
const normalizeMessageData = (messages) => {
  if (!Array.isArray(messages)) return [];
  
  return messages.map(message => ({
    messageType: message.messageType,
    userQuery: message.userQuery || '',
    createdAt: message.createdAt,
    type: message.messageType?.toLowerCase()
  }));
};

const devvyService = {
  /**
   * 서비스 상태 확인
   */
  healthCheck() {
    return handleRequest(apiClient.get('/health'));
  },

  /**
   * 카테고리 목록 조회
   */
  async getCategories() {
    const response = await handleRequest(apiClient.get('/categories'));
    
    if (response.success && response.data) {
      const normalizedCategories = normalizeCategoryData(response.data);
      return {
        ...response,
        data: normalizedCategories
      };
    }
    
    return response;
  },

  /**
   * 채팅 메시지 전송 (userQuery 기반)
   */
  async sendMessage(requestData) {
    const normalizedRequest = {
      categoryCode: requestData.categoryCode,
      userQuery: requestData.userQuery,
      sessionId: requestData.sessionId
    };
    
    const response = await handleRequest(apiClient.post('/chat', normalizedRequest));
    
    if (response.success) {
      return {
        success: true,
        message: response.message,
        sessionId: response.sessionId,
        timestamp: response.timestamp
      };
    }
    
    return response;
  },

  /**
   * 대화 히스토리 조회
   */
  async getChatHistory() {
    const response = await handleRequest(apiClient.get('/history'));
    
    if (response.success && response.data) {
      const normalizedHistory = response.data.map(session => ({
        sessionId: session.sessionId,
        categoryCode: session.categoryCode,
        categoryName: session.categoryName,
        messageCount: session.messageCount,
        createdAt: session.createdAt,
        lastMessageAt: session.lastMessageAt,
        firstMessage: session.firstMessage
      }));
      
      return {
        ...response,
        data: normalizedHistory
      };
    }
    
    return response;
  },

  /**
   * 세션 메시지 조회
   */
  async getSessionMessages(sessionId) {
    const response = await handleRequest(apiClient.get(`/sessions/${sessionId}/messages`));
    
    if (response.success && response.data) {
      const normalizedMessages = normalizeMessageData(response.data);
      return {
        ...response,
        data: normalizedMessages
      };
    }
    
    return response;
  },

  /**
   * 피드백 전송
   */
  sendFeedback(feedbackData) {
    return handleRequest(apiClient.post('/feedback', feedbackData));
  }
};

// 로딩 메시지 시스템
const LOADING_MESSAGES = {
  ko: [
    "✅ Devvy가 최적의 답변을 찾고 있어요.",
    "🧠 AI의 뉴런이 활발하게 움직이는 중...",
    "📊 데이터를 분석하고 있습니다 🌊",
    "⚡ 잠시만요, 거의 다 됐어요! 🚀",
    "✨ 전문적인 답변을 생성하고 있습니다"
  ],
  en: [
    "✅ Devvy is finding the optimal answer.",
    "🧠 AI neurons are firing up...",
    "📊 Analyzing data 🌊",
    "⚡ Just a moment, almost there! 🚀",
    "✨ Generating professional response"
  ]
};

export function getLoadingMessage(language = 'ko') {
  const messages = LOADING_MESSAGES[language] || LOADING_MESSAGES.en;
  return messages[Math.floor(Math.random() * messages.length)];
}

export default devvyService;