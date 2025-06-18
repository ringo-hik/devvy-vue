/**
 * SWP Float Chat API 서비스 (별도 Vue 인스턴스용)
 * - requestIdleCallback을 통한 메인 스레드 보호
 * - 간단하고 안정적인 비동기 처리
 * - 레거시 환경 완벽 호환
 */

import axios from 'axios';

const baseUrl = '/api/v1/devportal/float-chat';

// ⭐ FloatChat 전용 axios 인스턴스 생성 (전역 interceptor와 완전 분리)
const chatAxios = axios.create({
  baseURL: baseUrl,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// ⭐ FloatChat 전용 interceptor만 적용 (전역 로딩에 영향 없음)
chatAxios.interceptors.request.use(
  (config) => {
    console.log(`[FloatChat] 요청: ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('[FloatChat] 요청 오류:', error);
    return Promise.reject(error);
  }
);

chatAxios.interceptors.response.use(
  (response) => {
    console.log(`[FloatChat] 응답: ${response.status} ${response.config.url}`);
    return response;
  },
  (error) => {
    console.error('[FloatChat] 응답 오류:', error.response?.status, error.response?.data || error.message);
    return Promise.reject(error);
  }
);

const floatChatService = {

  // ===== API 호출 메서드 (간단한 axios 방식) =====

  healthCheck() {
    return chatAxios.get('/health')
      .then(response => response.data)
      .catch(error => {
        console.error('Health check 실패:', error);
        throw error;
      });
  },

  getCategories() {
    return chatAxios.get('/categories')
      .then(response => response.data)
      .catch(error => {
        console.error('카테고리 조회 실패:', error);
        throw error;
      });
  },

  getCategoryByCode(categoryCode) {
    return chatAxios.get(`/categories/${categoryCode}`)
      .then(response => response.data)
      .catch(error => {
        console.error('카테고리 상세 조회 실패:', error);
        throw error;
      });
  },

  // === 프롬프트 관리 API ===

  getSystemPrompt(categoryCode) {
    return chatAxios.get(`/categories/${categoryCode}/prompt`)
      .then(response => response.data)
      .catch(error => {
        console.error('시스템 프롬프트 조회 실패:', error);
        throw error;
      });
  },

  updateSystemPrompt(categoryCode, systemPrompt) {
    return chatAxios.put(`/categories/${categoryCode}/prompt`, {
        systemPrompt: systemPrompt
      })
      .then(response => response.data)
      .catch(error => {
        console.error('시스템 프롬프트 업데이트 실패:', error);
        if (error.response && error.response.data) {
          return error.response.data;
        }
        throw error;
      });
  },

  // === 메시지 처리 (requestIdleCallback 사용) ===

  sendMessage(data, useAsync = false) {
    // 비동기 요청 시 IdleCallback 사용하여 메인 스레드 보호
    if (useAsync) {
      return this.sendMessageWithIdleCallback(data);
    }
    
    return chatAxios.post('/message', data)
      .then(response => response.data)
      .catch(error => {
        console.error('메시지 전송 실패:', error);
        if (error.response && error.response.data) {
          return error.response.data;
        }
        throw error;
      });
  },

  /**
   * requestIdleCallback을 사용한 메인 스레드 보호
   * Web Worker 없이도 UI 블로킹 최소화
   */
  sendMessageWithIdleCallback(data) {
    return new Promise((resolve, reject) => {
      const performRequest = () => {
        console.log('[FloatChat] IdleCallback으로 비동기 메시지 처리:', data);
        
        chatAxios.post('/message/async', data)
          .then(response => {
            resolve(response.data);
          })
          .catch(error => {
            console.error('IdleCallback 메시지 전송 실패:', error);
            if (error.response && error.response.data) {
              resolve(error.response.data);
            } else {
              reject(error);
            }
          });
      };

      // 브라우저가 유휴 상태일 때 요청 실행
      if (window.requestIdleCallback) {
        window.requestIdleCallback(performRequest, { timeout: 1000 });
      } else {
        // requestIdleCallback 미지원 브라우저용 fallback
        setTimeout(performRequest, 0);
      }
    });
  },

  sendMessageAsync(data) {
    return this.sendMessage(data, true);
  },

  getConversations(categoryCode) {
    return chatAxios.get(`/conversations/${categoryCode}`)
      .then(response => response.data)
      .catch(error => {
        console.error('대화 히스토리 조회 실패:', error);
        throw error;
      });
  },

  sendFeedback(data) {
    return chatAxios.post('/feedback', data)
      .then(response => response.data)
      .catch(error => {
        console.error('피드백 전송 실패:', error);
        throw error;
      });
  },

  // ===== 데이터 변환 및 처리 메서드 =====

  /**
   * AI 응답을 채팅 메시지 형태로 변환합니다.
   */
  formatAIResponse(response) {
    const timestamp = Date.now();
    
    if (!response.success) {
      return {
        type: 'ai',
        content: response.errorMessage || '응답 생성 중 오류가 발생했습니다.',
        timestamp,
        isError: true
      };
    }

    return {
      type: 'ai',
      content: response.aiResponse || response.message || '응답을 받았습니다.',
      timestamp,
      isError: false,
      conversationId: response.conversationId
    };
  },

  /**
   * 대화 히스토리를 채팅 메시지 배열로 변환합니다.
   */
  convertConversationsToMessages(conversations) {
    const messages = [];
    
    if (!Array.isArray(conversations)) {
      return messages;
    }

    // 최신 순으로 정렬 (오래된 것부터 표시)
    const sortedConversations = conversations.sort((a, b) => 
      new Date(a.createdDate) - new Date(b.createdDate)
    );

    sortedConversations.forEach((conv) => {
      const baseTimestamp = new Date(conv.createdDate).getTime();
      
      // 사용자 메시지 추가
      if (conv.userQuestion) {
        messages.push({
          id: `history-user-${conv.conversationId}`,
          type: 'user',
          content: conv.userQuestion,
          timestamp: baseTimestamp,
          conversationId: conv.conversationId
        });
      }

      // AI 응답 추가
      if (conv.aiResponse) {
        messages.push({
          id: `history-ai-${conv.conversationId}`,
          type: 'ai',
          content: conv.aiResponse,
          timestamp: baseTimestamp + 1000, // 1초 후로 설정
          conversationId: conv.conversationId,
          isMarkdown: this.hasMarkdownContent(conv.aiResponse)
        });
      }
    });

    return messages;
  },

  // ===== 마크다운 처리 메서드 =====

  /**
   * 콘텐츠에 마크다운이 포함되어 있는지 확인합니다.
   */
  hasMarkdownContent(text) {
    if (!text) return false;
    
    // 마크다운 패턴들 확인
    const markdownPatterns = [
      /\|.*\|.*\n\|[-\s:|]+\|.*\n(\|.*\|.*\n)*/, // 테이블
      /^#{1,6}\s+/, // 헤딩
      /\*\*(.*?)\*\*/, // 볼드
      /\*(.*?)\*/, // 이탤릭
      /`(.*?)`/, // 인라인 코드
      /```[\s\S]*?```/, // 코드 블록
      /^\* /, // 리스트
      /^\d+\. /, // 순서 리스트
      /\[.*?\]\(.*?\)/ // 링크
    ];
    
    return markdownPatterns.some(pattern => pattern.test(text));
  },

  /**
   * 마크다운 테이블이 포함된 텍스트인지 확인합니다.
   */
  hasMarkdownTable(text) {
    if (!text) return false;
    
    // 마크다운 테이블 패턴 확인
    const tablePattern = /\|.*\|.*\n\|[-\s:|]+\|.*\n(\|.*\|.*\n)*/;
    return tablePattern.test(text);
  },

  /**
   * 마크다운 테이블을 HTML 테이블로 변환합니다.
   */
  convertMarkdownTableToHtml(text) {
    if (!this.hasMarkdownTable(text)) {
      return this.formatPlainText(text);
    }

    // 테이블 부분과 일반 텍스트 부분 분리
    const parts = text.split(/(\n\|.*\|.*\n\|[-\s:|]+\|.*\n(?:\|.*\|.*\n)*)/);
    
    let result = '';
    
    parts.forEach(part => {
      if (this.hasMarkdownTable(part)) {
        result += this.parseMarkdownTable(part);
      } else if (part.trim()) {
        result += `<div class="text-content">${this.formatPlainText(part)}</div>`;
      }
    });

    return result;
  },

  /**
   * 마크다운 테이블을 파싱하여 HTML로 변환합니다.
   */
  parseMarkdownTable(tableText) {
    const lines = tableText.trim().split('\n').filter(line => line.includes('|'));
    
    if (lines.length < 2) return this.formatPlainText(tableText);

    const headers = this.parseTableRow(lines[0]);
    // lines[1]은 구분선이므로 스킵
    const rows = lines.slice(2).map(line => this.parseTableRow(line));

    let html = '<div class="table-container">';
    html += '<table class="markdown-table">';
    
    // 헤더
    html += '<thead><tr>';
    headers.forEach(header => {
      html += `<th>${this.escapeHtml(header)}</th>`;
    });
    html += '</tr></thead>';
    
    // 바디
    html += '<tbody>';
    rows.forEach(row => {
      html += '<tr>';
      row.forEach(cell => {
        html += `<td>${this.escapeHtml(cell)}</td>`;
      });
      html += '</tr>';
    });
    html += '</tbody>';
    
    html += '</table></div>';
    
    return html;
  },

  /**
   * 테이블 행을 파싱합니다.
   */
  parseTableRow(line) {
    return line.split('|')
      .map(cell => cell.trim())
      .filter((cell, index, array) => 
        // 첫 번째와 마지막 빈 셀 제거 (| 로 시작/끝나는 경우)
        !(index === 0 && cell === '') && !(index === array.length - 1 && cell === '')
      );
  },

  /**
   * 일반 텍스트를 포맷팅합니다.
   */
  formatPlainText(text) {
    if (!text) return '';
    
    return text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>') // 볼드
      .replace(/\*(.*?)\*/g, '<em>$1</em>') // 이탤릭
      .replace(/`(.*?)`/g, '<code>$1</code>') // 인라인 코드
      .replace(/\n/g, '<br>') // 줄바꿈
      .trim();
  },

  /**
   * HTML 이스케이프 처리
   */
  escapeHtml(text) {
    if (!text) return '';
    
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  },

  // ===== UI 지원 메서드 =====

  /**
   * 카테고리별 아이콘을 반환합니다.
   */
  getCategoryIcon(categoryCode, iconPath = null) {
    // 1. iconPath가 유효한 값이 있으면 그것을 사용
    if (iconPath && iconPath.trim() !== '') {
      return iconPath.trim();
    }
    
    // 2. categoryCode 기반 기본 아이콘 매핑
    const categoryIconMap = {
      'swdp_menu': 'apps',
      'project': 'folder',
      'voc': 'headphones',
      'project_info': 'info-circle',
      'swdp_api': 'code',
      'admin': 'setting',
      'support': 'life-buoy',
      'api': 'terminal',
      'documentation': 'book',
      'analytics': 'chart-bar',
      'security': 'shield',
      'default': 'comment'
    };
    
    if (categoryIconMap[categoryCode]) {
      return categoryIconMap[categoryCode];
    }
    
    // 3. 매칭되지 않으면 deterministic하게 아이콘 선택
    const randomIcons = [
      'comment', 'message', 'chat', 'question-circle', 'info-circle',
      'lightbulb', 'star', 'heart', 'bookmark', 'flag',
      'folder', 'file', 'apps', 'grid', 'layers',
      'tool', 'setting', 'cog', 'bolt', 'rocket',
      'globe', 'map', 'location', 'compass', 'search',
      'user', 'users', 'team', 'organization', 'building',
      'chart', 'graph', 'analytics', 'trending-up', 'dashboard',
      'code', 'terminal', 'database', 'server', 'cloud'
    ];
    
    const hashCode = this.generateHashCode(categoryCode || 'unknown');
    const iconIndex = Math.abs(hashCode) % randomIcons.length;
    
    console.warn(`카테고리 '${categoryCode}'에 대한 아이콘이 정의되지 않아 임의 아이콘 '${randomIcons[iconIndex]}'을 사용합니다.`);
    return randomIcons[iconIndex];
  },

  /**
   * 문자열로부터 해시코드를 생성합니다.
   */
  generateHashCode(str) {
    let hash = 0;
    if (str.length === 0) return hash;
    
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // 32bit 정수로 변환
    }
    return hash;
  },

  /**
   * 카테고리별 빠른 질문 목록을 반환합니다.
   */
  getQuickQuestions(categoryCode, language = 'ko') {
    const quickQuestions = {
      ko: {
        'swdp_menu': [
          '메뉴 구조 안내',
          '주요 기능 소개',
          '사용법 가이드',
          '시스템 개요'
        ],
        'project': [
          '프로젝트 현황',
          '진행 상태 확인',
          '목록 조회',
          '팀 정보'
        ],
        'voc': [
          'VOC 현황',
          '최근 이슈',
          '장애 상황',
          '해결 상태'
        ],
        'project_info': [
          '기술 스택',
          '프로젝트 정보',
          '팀 구성',
          '개발 현황'
        ],
        'swdp_api': [
          'API 목록 조회',
          '인증 방법 안내',
          '요청/응답 예제',
          '에러 코드 설명'
        ]
      },
      en: {
        'swdp_menu': [
          'Menu Structure',
          'Main Features',
          'Usage Guide',
          'System Overview'
        ],
        'project': [
          'Project Status',
          'Progress Check',
          'Project List',
          'Team Info'
        ],
        'voc': [
          'VOC Status',
          'Recent Issues',
          'System Alerts',
          'Resolution Status'
        ],
        'project_info': [
          'Tech Stack',
          'Project Details',
          'Team Structure',
          'Development Status'
        ],
        'swdp_api': [
          'API List',
          'Authentication',
          'Request/Response',
          'Error Codes'
        ]
      }
    };

    return quickQuestions[language]?.[categoryCode] || [];
  },

  // ===== 유틸리티 메서드 =====

  /**
   * 메시지를 클립보드에 복사 가능한 텍스트로 변환합니다.
   */
  convertMessageToText(message) {
    if (!message || !message.content) return '';

    // 마크다운 형태 그대로 반환 (표 포함)
    return message.content.trim();
  },

  /**
   * 응답 검증
   */
  validateResponse(response) {
    if (!response) {
      return { isValid: false, error: '응답이 없습니다.' };
    }

    if (response.success === false) {
      return { 
        isValid: false, 
        error: response.errorMessage || '서버에서 오류가 발생했습니다.' 
      };
    }

    return { isValid: true };
  },

  /**
   * 에러 메시지를 사용자 친화적으로 변환합니다.
   */
  formatErrorMessage(error, language = 'ko') {
    const errorMessages = {
      ko: {
        'NETWORK_ERROR': '네트워크 연결을 확인해주세요.',
        'SERVER_ERROR': '서버에 일시적인 문제가 발생했습니다.',
        'VALIDATION_ERROR': '입력 정보를 확인해주세요.',
        'UNAUTHORIZED': '로그인이 필요합니다.',
        'FORBIDDEN': '접근 권한이 없습니다.',
        'NOT_FOUND': '요청한 리소스를 찾을 수 없습니다.',
        'TIMEOUT': '응답 시간이 초과되었습니다.',
        'DEFAULT': '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
      },
      en: {
        'NETWORK_ERROR': 'Please check your network connection.',
        'SERVER_ERROR': 'A temporary server problem occurred.',
        'VALIDATION_ERROR': 'Please check your input information.',
        'UNAUTHORIZED': 'Login is required.',
        'FORBIDDEN': 'Access denied.',
        'NOT_FOUND': 'The requested resource was not found.',
        'TIMEOUT': 'Response timeout occurred.',
        'DEFAULT': 'A temporary error occurred. Please try again later.'
      }
    };

    if (error.response) {
      const status = error.response.status;
      if (status === 401) return errorMessages[language].UNAUTHORIZED;
      if (status === 403) return errorMessages[language].FORBIDDEN;
      if (status === 404) return errorMessages[language].NOT_FOUND;
      if (status === 408) return errorMessages[language].TIMEOUT;
      if (status >= 500) return errorMessages[language].SERVER_ERROR;
      if (status >= 400) return errorMessages[language].VALIDATION_ERROR;
    }

    if (error.code === 'NETWORK_ERROR' || !error.response) {
      return errorMessages[language].NETWORK_ERROR;
    }

    if (error.code === 'ECONNABORTED') {
      return errorMessages[language].TIMEOUT;
    }

    return errorMessages[language].DEFAULT;
  },

  // ===== 프롬프트 관리 유틸리티 =====

  /**
   * 프롬프트 검증
   */
  validatePrompt(prompt) {
    if (!prompt || prompt.trim().length === 0) {
      return { isValid: false, error: '프롬프트는 필수입니다.' };
    }

    if (prompt.length > 5000) {
      return { isValid: false, error: '프롬프트는 5000자를 초과할 수 없습니다.' };
    }

    return { isValid: true };
  },

  /**
   * 기본 프롬프트 템플릿 생성
   */
  getDefaultPromptTemplate(categoryCode) {
    const templates = {
      'swdp_menu': `당신은 SWDP 메뉴 구조 전문 AI 어시스턴트입니다.
사용자가 SWDP 시스템의 메뉴와 기능을 이해할 수 있도록 도와주세요.

응답 규칙:
1. 한국어로 답변하세요.
2. 메뉴 구조나 기능 목록은 표 형태로 정리해주세요.
3. 단계별 사용법이 필요한 경우 순서대로 설명해주세요.`,

      'project': `당신은 프로젝트 관리 전문 AI 어시스턴트입니다.
프로젝트 상태, 진행률, 팀원 정보 등을 제공해주세요.

응답 규칙:
1. 한국어로 답변하세요.
2. 프로젝트 현황은 반드시 표 형태로 정리해주세요.
3. 진행률은 퍼센트로 표시해주세요.`,

      'voc': `당신은 VOC(고객의 소리) 관리 전문 AI 어시스턴트입니다.
사용자 문의, 장애 현황, 해결 상태를 분석하고 보고해주세요.

응답 규칙:
1. 한국어로 답변하세요.
2. 이슈나 장애 현황은 표 형태로 정리해주세요.
3. 우선순위와 상태를 명확히 표시해주세요.`,

      'project_info': `당신은 프로젝트 정보 관리 전문 AI 어시스턴트입니다.
프로젝트의 기술적 세부사항과 구성원 정보를 제공해주세요.

응답 규칙:
1. 한국어로 답변하세요.
2. 기술 스택은 표 형태로 정리해주세요.
3. 팀 구성원 정보도 표로 보여주세요.`,

      'swdp_api': `당신은 SWDP API 문서 전문 AI 어시스턴트입니다.
API 명세서, 인증 방법, 요청/응답 예제를 제공해주세요.

응답 규칙:
1. 한국어로 답변하세요.
2. API 목록은 표 형태로 정리해주세요.
3. 요청/응답 예제는 코드 블록으로 보여주세요.`,

      'default': `당신은 소프트웨어 개발 플랫폼 전문 AI 어시스턴트입니다.
개발 관련 질문에 대한 종합적인 답변을 제공해주세요.

응답 규칙:
1. 한국어로 답변하세요.
2. 구조화된 정보는 표 형태로 정리해주세요.
3. 정확하고 도움이 되는 정보를 제공해주세요.`
    };

    return templates[categoryCode] || templates.default;
  },

  // ===== 유틸리티 메서드 =====

  /**
   * 디버그 모드 토글
   */
  toggleDebugMode(enabled) {
    this.debugMode = enabled;
    console.log(`[FloatChat] 디버그 모드 ${enabled ? '활성화' : '비활성화'}`);
  }
};

export default floatChatService;