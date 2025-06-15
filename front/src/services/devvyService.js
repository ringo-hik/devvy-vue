/**
 * Devvy Bot API 더미 서비스 (리팩토링 버전)
 * - 백엔드 API 연동 전 프론트엔드 테스트를 위한 Mock Service 입니다.
 * - 요청에 따라 새로운 기능(SWDP, 프로젝트, VOC)과 다국어 로딩 메시지를 추가했습니다.
 */

// 다국어 지원, 재미있는 로딩 메시지 목록
const LOADING_MESSAGES = {
  ko: [
    "✅ Devvy가 최적의 답변을 찾고 있어요.",
    "🧠 AI의 뉴런이 활발하게 움직이는 중...",
    "데이터의 바다에서 정보를 탐색 중입니다 🌊",
    "잠시만요, 거의 다 됐어요! 🚀",
    "마법 같은 답변을 생성하고 있습니다 ✨"
  ],
  en: [
    "✅ Devvy is finding the optimal answer.",
    "🧠 AI neurons are firing up...",
    "Navigating the sea of data 🌊",
    "Just a moment, almost there! 🚀",
    "Generating a magical response ✨"
  ]
};

// 새로운 기능에 맞춘 더미 카테고리 데이터
const DUMMY_CATEGORIES = [
  { categoryId: 1, name: 'SWDP Menu', description: 'SWDP 메뉴와 기능을 안내합니다.', icon: '🗺️' },
  { categoryId: 2, name: '프로젝트', description: '프로젝트 현황을 요약, 정리합니다.', icon: '📊' },
  { categoryId: 3, name: 'VOC', description: 'VOC 장애 및 문의에 답변합니다.', icon: '🔔' }
];

// 더미 히스토리 데이터
const DUMMY_CHAT_HISTORY = [
    { sessionId: 'session-001', categoryName: '프로젝트', messageCount: 4, createdAt: new Date(Date.now() - 2 * 3600 * 1000).toISOString(), firstMessage: '현재 진행중인 프로젝트 목록 보여줘' },
    { sessionId: 'session-002', categoryName: 'VOC', messageCount: 2, createdAt: new Date(Date.now() - 25 * 3600 * 1000).toISOString(), firstMessage: '로그인 실패 VOC 현황 알려줘' },
];

// 카테고리별 더미 AI 응답
const AI_RESPONSES = {
  ko: {
    1: { // SWDP Menu
      default: "🗺️ **SWDP 메뉴**에 대해 무엇이 궁금하신가요? \n\n 자주 찾는 메뉴는 다음과 같아요: \n - `프로젝트 등록` \n - `배포 파이프라인` \n - `권한 관리`"
    },
    2: { // 프로젝트
      default: "📊 **DummyProjectInfo**에서 조회한 프로젝트 현황입니다. \n\n- **활성 프로젝트**: 15개\n- **오늘 배포 수**: 28회 (성공률 96%)\n- **주의 필요한 서비스**: `payment-gateway`"
    },
    3: { // VOC
      default: "🔔 **DummyVOCData** 기준, 오늘 등록된 VOC 현황입니다.\n\n- **전체**: 8건\n- **긴급**: 2건 (로그인 실패 관련)\n- **평균 처리 시간**: 45분"
    }
  },
  en: {
    1: { // SWDP Menu
      default: "🗺️ What would you like to know about the **SWDP Menu**? \n\n Common menus include: \n - `Project Registration` \n - `Deployment Pipeline` \n - `Permission Management`"
    },
    2: { // 프로젝트
      default: "📊 Here is the project status from **DummyProjectInfo**. \n\n- **Active Projects**: 15\n- **Deploys Today**: 28 (96% success)\n- **Service needing attention**: `payment-gateway`"
    },
    3: { // VOC
      default: "🔔 Today's VOC status based on **DummyVOCData**.\n\n- **Total**: 8 cases\n- **Urgent**: 2 cases (related to login failures)\n- **Average resolution time**: 45 minutes"
    }
  }
};

// 네트워크 지연 시뮬레이션
const simulateDelay = (ms = 1500) => new Promise(resolve => setTimeout(resolve, ms + Math.random() * 1000));

const devvyService = {
  getLoadingMessage(language = 'ko') {
    const messages = LOADING_MESSAGES[language] || LOADING_MESSAGES.en;
    return messages[Math.floor(Math.random() * messages.length)];
  },

  async getCategories() {
    await simulateDelay(500);
    return { success: true, data: DUMMY_CATEGORIES };
  },

  async sendMessage(request) {
    await simulateDelay(3000); // 3~5초 지연
    const lang = request.language || 'ko';
    const responseTemplate = AI_RESPONSES[lang][request.categoryId] || { default: "Sorry, I can't help with that topic." };
    
    // 키워드 기반 응답 (간단한 예시)
    let message = responseTemplate.default;
    if (request.message.includes("등록")) {
        message = "프로젝트 등록은 `프로젝트 관리` 메뉴에서 가능합니다. \n\n 가이드 문서를 링크해드릴까요?";
    }

    return {
      success: true,
      data: {
        sessionId: request.sessionId || `session-${Date.now()}`,
        message: message,
      }
    };
  },

  async getChatHistory() {
      await simulateDelay(800);
      return { success: true, data: DUMMY_CHAT_HISTORY };
  },

  async getSessionMessages(sessionId) {
      await simulateDelay(400);
      // 실제로는 sessionId를 기반으로 메시지를 가져와야 합니다.
      return { success: true, data: [
          { messageType: 'USER', content: DUMMY_CHAT_HISTORY.find(h => h.sessionId === sessionId)?.firstMessage || 'History Question' },
          { messageType: 'AI', content: 'This is a response from the past conversation.'}
      ]};
  },

  async sendFeedback(feedbackData) {
    await simulateDelay(600);
    console.log('✅ Feedback received:', feedbackData);
    if (!feedbackData.content || feedbackData.rating === 0) {
        return { success: false, error: 'Rating and content are required.' };
    }
    return { success: true, message: '소중한 의견 감사합니다! 더 나은 Devvy Bot으로 보답하겠습니다.' };
  },
  
  async healthCheck() {
      await simulateDelay(200);
      return { success: Math.random() > 0.1 }; // 10% 확률로 실패
  }
};

export default devvyService;
