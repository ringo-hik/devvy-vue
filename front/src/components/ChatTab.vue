<template>
  <div class="chat-tab">
    <!-- 1. 카테고리 선택 화면 -->
    <!-- `selectedCategory`가 null일 때, 가로 리스트 형태로 카테고리를 보여줍니다. (요청사항 반영) -->
    <div v-if="!selectedCategory" class="category-selection">
      <h3 class="section-title">{{ getText('selectCategory') }}</h3>
      <div v-if="loadingCategories" class="loading-state">
        <div class="loading-spinner"></div>
        <p>{{ getText('loadingCategories') }}</p>
      </div>
      <!-- [수정] category-grid -> category-list 로 변경 -->
      <div v-else class="category-list">
        <div v-for="category in categories" :key="category.categoryId" class="category-item" @click="selectCategory(category)">
          <span class="category-icon">{{ category.icon || '✨' }}</span>
          <span class="category-name">{{ category.name }}</span>
          <div class="category-arrow">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 채팅 화면 -->
    <!-- 카테고리가 선택되면 이 부분이 표시됩니다. (원본 구조 유지) -->
    <div v-else class="chat-section">
      <div class="selected-category-header">
        <div class="category-info">
          <div class="category-badge">{{ selectedCategory.icon || '✨' }} {{ selectedCategory.name }}</div>
          <!-- [복원] 히스토리 대화 보기 기능 -->
          <div v-if="isHistorySession" class="history-session-info">📖 {{ getText('pastConversation') }}</div>
        </div>
        <div class="category-actions">
           <!-- [복원] 히스토리 대화 시 '새 대화' 버튼 -->
           <button v-if="isHistorySession" class="action-btn new-chat" @click="startNewChat" :title="getText('startNewChat')">+</button>
           <button class="action-btn change-category" @click="resetCategory" :disabled="isProcessing">{{ getText('change') }}</button>
        </div>
      </div>

      <div class="messages-container" ref="messagesContainer">
        <!-- [복원] 웰컴 메시지 -->
        <div v-if="messages.length === 0 && !isProcessing" class="welcome-message">
            <div class="welcome-icon">{{ selectedCategory.icon || '✨' }}</div>
            <h4>{{ getText('welcomeTitle').replace('{category}', selectedCategory.name) }}</h4>
        </div>

        <div v-for="(message, index) in messages" :key="index" class="message" :class="[message.type === 'user' ? 'user' : 'ai']">
            <div class="message-bubble">
              <!-- [복원] Markdown 렌더링을 위해 v-html 사용 -->
              <div class="message-content" v-html="formatMessage(message.content)"></div>
              <!-- [복원] AI 메시지 복사 버튼 -->
              <button v-if="message.type === 'ai'" class="copy-btn" @click="copyMessage(message.content)" :title="getText('copy')">
                <svg width="14" height="14" viewBox="0 0 24 24"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2m4-2h-4a2 2 0 0 0-2 2v2h8V4a2 2 0 0 0-2-2z" fill="none" stroke="currentColor" stroke-width="2"/></svg>
              </button>
            </div>
        </div>

        <!-- [복원] 다이나믹 로딩 메시지 -->
        <div v-if="isProcessing" class="message ai">
            <div class="message-bubble loading">
              <div class="loading-dots"><span></span><span></span><span></span></div>
              <div class="loading-text">{{ currentLoadingMessage }}</div>
            </div>
        </div>
      </div>

      <!-- 입력 영역 -->
      <div class="input-section">
         <!-- [복원] 빠른 질문 기능 -->
         <div v-if="!isHistorySession" class="quick-questions">
            <button v-for="q in quickQuestions" :key="q" @click="sendQuickQuestion(q)">{{ q }}</button>
        </div>
        <div class="input-container">
          <textarea
            v-model="currentMessage"
            ref="messageInput"
            :placeholder="getText('inputPlaceholder')"
            @keydown.enter.exact.prevent="sendMessage"
            @input="adjustTextareaHeight"
            :disabled="isProcessing"
            maxlength="1000"
          ></textarea>
          <button class="send-button" @click="sendMessage" :disabled="!canSendMessage">
            <svg width="18" height="18" viewBox="0 0 24 24"><path d="M2 21l21-9L2 3v7l15 2-15 2v7z" fill="currentColor"/></svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 원본과 동일하게 devvyService와 marked를 import합니다.
import devvyService, { getLoadingMessage } from '../services/devvyService';
import { marked } from 'marked';

export default {
  name: 'ChatTab',
  // 원본 props를 그대로 유지합니다.
  props: ['categories', 'loadingCategories', 'isProcessing', 'currentLanguage'],
  // 원본 data를 그대로 유지합니다. (히스토리, 로딩 메시지 관련 상태 포함)
  data() {
    return {
      selectedCategory: null,
      currentMessage: '',
      messages: [],
      currentSessionId: null,
      isHistorySession: false,
      currentLoadingMessage: '',
      loadingInterval: null,
    };
  },
  // 원본 computed 속성을 그대로 유지합니다.
  computed: {
    texts() {
      const allTexts = {
        ko: {
          selectCategory: '어떤 도움이 필요하신가요?', loadingCategories: '카테고리 로딩 중...', pastConversation: '과거 대화',
          startNewChat: '새 대화 시작', change: '변경', welcomeTitle: '{category} 전문가, Devvy입니다!', copy: '복사',
          inputPlaceholder: '질문을 입력하세요...', quickQuestions: {
              1: ['SWDP 메뉴 찾아줘', '프로젝트 등록 방법 알려줘'], 2: ['진행중인 프로젝트 목록 보여줘', '배포 상태 확인해줘'], 3: ['최근 장애 현황 알려줘', '로그인 문제 해결 방법은?']
          }
        },
        en: {
          selectCategory: 'How can I help you?', loadingCategories: 'Loading categories...', pastConversation: 'Past Conversation',
          startNewChat: 'Start New Chat', change: 'Change', welcomeTitle: 'I am Devvy, an expert in {category}!', copy: 'Copy',
          inputPlaceholder: 'Enter your question...', quickQuestions: {
              1: ['Find SWDP menu', 'How to register a project?'], 2: ['Show project list', 'Check deployment status'], 3: ['Recent issue status?', 'Login problem solutions?']
          }
        },
      };
      return allTexts[this.currentLanguage];
    },
    canSendMessage() {
      return this.currentMessage.trim().length > 0 && !this.isProcessing;
    },
    quickQuestions() {
      if (!this.selectedCategory) return [];
      return this.texts.quickQuestions[this.selectedCategory.categoryId] || [];
    }
  },
  // 원본 methods를 그대로 유지합니다.
  methods: {
    getText(key) { return this.texts[key] || key; },
    formatMessage(content) {
      // AI 응답에 Markdown이 포함되어 있으면 HTML로 변환합니다.
      return marked(content || '');
    },
    selectCategory(category) {
      this.selectedCategory = category;
      this.messages = [];
      this.currentSessionId = null;
      this.isHistorySession = false;
      this.$nextTick(() => this.$refs.messageInput.focus());
    },
    resetCategory() {
      this.selectedCategory = null;
      this.$emit('processing-state-changed', false);
    },
    resetToInitialState() {
      this.selectedCategory = null;
      this.messages = [];
      this.currentMessage = '';
      this.currentSessionId = null;
      this.isHistorySession = false;
      this.stopLoadingMessages();
    },
    sendMessage() {
      if (!this.canSendMessage) return;
      const messageContent = this.currentMessage;
      this.messages.push({ type: 'user', content: messageContent });
      this.currentMessage = '';
      this.adjustTextareaHeight();
      this.scrollToBottom();

      this.$emit('processing-state-changed', true);
      this.startLoadingMessages();

      this.$emit('message-sent', {
        categoryId: this.selectedCategory.categoryId,
        message: messageContent,
        sessionId: this.currentSessionId,
      });
    },
    sendQuickQuestion(question) {
      this.currentMessage = question;
      this.sendMessage();
    },
    addAiResponse(response) {
      this.stopLoadingMessages();
      this.$emit('processing-state-changed', false);
      const content = response.message || (this.currentLanguage === 'ko' ? '오류가 발생했습니다.' : 'An error occurred.');
      this.messages.push({ type: 'ai', content });
      if (response.sessionId) this.currentSessionId = response.sessionId;
      this.scrollToBottom();
    },
    loadHistorySession(historyItem) {
      const category = this.categories.find(c => c.name === historyItem.categoryName);
      if (!category) return;

      this.selectedCategory = category;
      this.currentSessionId = historyItem.sessionId;
      this.isHistorySession = true;
      this.messages = [];
      this.$emit('processing-state-changed', true);

      devvyService.getSessionMessages(historyItem.sessionId).then(res => {
        if(res.success) {
          this.messages = res.data.map(m => ({
            type: m.messageType.toLowerCase(),
            content: m.content
          }));
        }
      }).finally(() => {
        this.$emit('processing-state-changed', false);
        this.scrollToBottom();
      });
    },
    startNewChat() {
      this.isHistorySession = false;
      this.messages = [];
      this.currentSessionId = null;
    },
    startLoadingMessages() {
      this.currentLoadingMessage = getLoadingMessage(this.currentLanguage);
      this.loadingInterval = setInterval(() => {
        this.currentLoadingMessage = getLoadingMessage(this.currentLanguage);
      }, 2000);
    },
    stopLoadingMessages() {
      clearInterval(this.loadingInterval);
      this.loadingInterval = null;
    },
    copyMessage(content) {
      navigator.clipboard.writeText(content).then(() => {
        // 복사 성공 시 간단한 UI 피드백을 줄 수 있습니다.
      });
    },
    adjustTextareaHeight() {
      const textarea = this.$refs.messageInput;
      if (textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = `${textarea.scrollHeight}px`;
      }
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer;
        if (container) container.scrollTop = container.scrollHeight;
      });
    }
  },
  // 원본 lifecycle hook을 그대로 유지합니다.
  beforeDestroy() {
    this.stopLoadingMessages();
  },
};
</script>
