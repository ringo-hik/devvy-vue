<template>
  <div class="chat-tab">
    <!-- 카테고리 선택 화면 -->
    <div v-if="!selectedCategory" class="category-selection">
      <h3 class="section-title">{{ getText('selectCategory') }}</h3>
      <div v-if="loadingCategories" class="loading-state">
        <div class="loading-spinner"></div>
        <p>{{ getText('loadingCategories') }}</p>
      </div>
      <div v-else class="category-list">
        <div v-for="category in categories" :key="category.categoryCode" class="category-item" @click="selectCategory(category)">
          <div class="category-icon-wrapper">
            <img class="category-icon" :src="getCategoryIcon(category.icon)" alt="icon" />
          </div>
          <div class="category-details">
            <div class="category-name">{{ category.categoryName }}</div>
            <div class="category-desc">{{ category.description }}</div>
          </div>
          <div class="category-arrow">
            <img :src="arrowIcon" alt="arrow" />
          </div>
        </div>
      </div>
    </div>

    <!-- 채팅 화면 -->
    <div v-else class="chat-section">
      <div class="selected-category-header">
        <div class="category-info">
          <div class="category-badge">{{ selectedCategory.icon || '✨' }} {{ selectedCategory.categoryName }}</div>
          <div v-if="isHistorySession" class="history-session-info">📖 {{ getText('pastConversation') }}</div>
        </div>
        <div class="category-actions">
           <button v-if="isHistorySession" class="action-btn new-chat" @click="startNewChat" :title="getText('startNewChat')">+</button>
           <button class="action-btn change-category" @click="resetCategory" :disabled="isProcessing">{{ getText('change') }}</button>
        </div>
      </div>

      <div class="messages-container" ref="messagesContainer">
        <div v-if="messages.length === 0 && !isProcessing" class="welcome-message">
            <div class="welcome-icon">{{ selectedCategory.icon || '✨' }}</div>
            <h4>{{ getText('welcomeTitle').replace('{category}', selectedCategory.categoryName) }}</h4>
        </div>

        <div v-for="(message, index) in messages" :key="index" class="message" :class="[message.type === 'user' ? 'user' : 'ai']">
            <div class="message-bubble">
              <div class="message-content" v-html="formatMessage(message.userQuery)"></div>
              <button v-if="message.type === 'ai'" class="copy-btn" @click="copyMessage(message.userQuery)" :title="getText('copy')">
                <img :src="copyIcon" alt="copy" />
              </button>
            </div>
        </div>

        <div v-if="isProcessing" class="message ai">
            <div class="message-bubble loading">
              <div class="loading-dots"><span></span><span></span><span></span></div>
              <div class="loading-text">{{ currentLoadingMessage }}</div>
            </div>
        </div>
      </div>

      <!-- 입력 영역 -->
      <div class="input-section">
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
            <img :src="sendIcon" alt="send" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import devvyService, { getLoadingMessage } from '../services/devvyService';
import { marked } from 'marked';
import ArrowIcon from '../assets/icons/chevron-right.svg';
import SendIcon from '../assets/icons/send-icon.svg';
import CopyIcon from '../assets/icons/thumb-icon.svg';
import CatIcon from '../assets/icons/cat-icon.svg';

export default {
  name: 'ChatTab',
  props: ['categories', 'loadingCategories', 'isProcessing', 'currentLanguage'],
  data() {
    return {
      selectedCategory: null,
      currentMessage: '',
      messages: [],
      currentSessionId: null,
      isHistorySession: false,
      currentLoadingMessage: '',
      loadingInterval: null,
      arrowIcon: ArrowIcon,
      sendIcon: SendIcon,
      copyIcon: CopyIcon,
    };
  },
  computed: {
    texts() {
      const allTexts = {
        ko: {
          selectCategory: '어떤 도움이 필요하신가요?', 
          loadingCategories: '카테고리 로딩 중...', 
          pastConversation: '과거 대화',
          startNewChat: '새 대화 시작', 
          change: '변경', 
          welcomeTitle: '{category} 전문가, Devvy입니다!', 
          copy: '복사',
          inputPlaceholder: '질문을 입력하세요...', 
          quickQuestions: {
              'swdp_menu': ['SWDP 메뉴 찾아줘', '프로젝트 등록 방법 알려줘'], 
              'project': ['진행중인 프로젝트 목록 보여줘', '배포 상태 확인해줘'], 
              'voc': ['최근 장애 현황 알려줘', '로그인 문제 해결 방법은?']
          }
        },
        en: {
          selectCategory: 'How can I help you?', 
          loadingCategories: 'Loading categories...', 
          pastConversation: 'Past Conversation',
          startNewChat: 'Start New Chat', 
          change: 'Change', 
          welcomeTitle: 'I am Devvy, an expert in {category}!', 
          copy: 'Copy',
          inputPlaceholder: 'Enter your question...', 
          quickQuestions: {
              'swdp_menu': ['Find SWDP menu', 'How to register a project?'], 
              'project': ['Show project list', 'Check deployment status'], 
              'voc': ['Recent issue status?', 'Login problem solutions?']
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
      return this.texts.quickQuestions[this.selectedCategory.categoryCode] || [];
    }
  },
  methods: {
    getText(key) { return this.texts[key] || key; },
    
    getCategoryIcon(name) {
      if (!name) return CatIcon;
      try {
        return require(`../assets/icons/${name}`);
      } catch (e) {
        return CatIcon;
      }
    },
    
    formatMessage(userQuery) {
      return marked(userQuery || '');
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
      this.messages.push({ type: 'user', userQuery: messageContent });
      this.currentMessage = '';
      this.adjustTextareaHeight();
      this.scrollToBottom();

      this.$emit('processing-state-changed', true);
      this.startLoadingMessages();

      // userQuery 필드로 통일된 메시지 전송
      this.$emit('message-sent', {
        categoryCode: this.selectedCategory.categoryCode,
        userQuery: messageContent,
        sessionId: this.currentSessionId,
      });
    },
    
    sendQuickQuestion(question) {
      this.currentMessage = question;
      this.sendMessage();
    },
    
    /**
     * AI 응답 처리 (userQuery 기반)
     */
    addAiResponse(response) {
      this.stopLoadingMessages();
      this.$emit('processing-state-changed', false);
      
      let userQuery;
      if (response.success) {
        userQuery = response.message || '응답을 받지 못했습니다.';
        if (response.sessionId) this.currentSessionId = response.sessionId;
      } else {
        userQuery = response.errorMessage || (this.currentLanguage === 'ko' ? '오류가 발생했습니다.' : 'An error occurred.');
      }
      
      this.messages.push({ type: 'ai', userQuery });
      this.scrollToBottom();
    },
    
    /**
     * 히스토리 세션 로드
     */
    loadHistorySession(historyItem) {
      const category = this.categories.find(c => c.categoryCode === historyItem.categoryCode);
      if (!category) return;

      this.selectedCategory = category;
      this.currentSessionId = historyItem.sessionId;
      this.isHistorySession = true;
      this.messages = [];
      this.$emit('processing-state-changed', true);

      devvyService.getSessionMessages(historyItem.sessionId).then(res => {
        if(res.success && res.data) {
          this.messages = res.data.map(m => ({
            type: m.messageType?.toLowerCase() || 'ai',
            userQuery: m.userQuery || ''
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
    
    copyMessage(userQuery) {
      navigator.clipboard.writeText(userQuery);
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
  
  beforeDestroy() {
    this.stopLoadingMessages();
  },
};
</script>