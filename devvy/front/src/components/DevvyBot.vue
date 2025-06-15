<template>
  <div class="devvy-bot">
    <!-- 챗봇 플로팅 버튼 -->
    <div
      v-if="!isOpen"
      class="devvy-button"
      @click="toggleBot"
      :class="{ 'has-notification': hasNotification }"
      :title="getText('openChatbot')"
    >
      <div class="bot-icon">🤖</div>
      <div v-if="hasNotification" class="notification-badge"></div>
    </div>

    <!-- 챗봇 창 -->
    <transition name="devvy-window-fade">
      <div v-if="isOpen" class="devvy-window">
        <!-- 헤더 -->
        <div class="devvy-header">
          <div class="header-content">
            <div class="bot-info">
              <div class="bot-avatar">🤖</div>
              <div class="bot-details">
                <span class="bot-name">Devvy Bot</span>
                <div class="status-indicator" :class="{ 'offline': !isConnected }">
                  <span class="status-dot"></span>
                  <span class="status-text">{{ isConnected ? getText('online') : getText('offline') }}</span>
                </div>
              </div>
            </div>

            <div class="header-actions">
              <button class="language-btn" @click="toggleLanguage" :title="getText('toggleLanguage')">
                {{ currentLanguage === 'ko' ? 'EN' : 'KR' }}
              </button>
              <button class="minimize-btn" @click="minimizeBot" :title="getText('minimize')">
                <svg width="16" height="16" viewBox="0 0 24 24"><path d="M5 12h14" stroke="currentColor" stroke-width="2"/></svg>
              </button>
              <button class="close-btn" @click="closeBot" :title="getText('close')">
                <svg width="16" height="16" viewBox="0 0 24 24"><path d="M6 18L18 6M6 6l12 12" stroke="currentColor" stroke-width="2"/></svg>
              </button>
            </div>
          </div>
        </div>

        <!-- 탭 메뉴 -->
        <div class="tab-menu">
          <button class="tab-button" :class="{ active: activeTab === 'chat' }" @click="switchTab('chat')" :disabled="isProcessing">
            <svg width="16" height="16" viewBox="0 0 24 24"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" fill="none" stroke="currentColor" stroke-width="2"/></svg>
            {{ getText('chat') }}
            <span v-if="isProcessing" class="processing-indicator"></span>
          </button>
          <button class="tab-button" :class="{ active: activeTab === 'history' }" @click="switchTab('history')" :disabled="isProcessing">
             <svg width="16" height="16" viewBox="0 0 24 24"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8m0-5v5h5m5-5v5l4 2" fill="none" stroke="currentColor" stroke-width="2"/></svg>
            {{ getText('history') }}
            <span v-if="chatHistory.length > 0" class="history-count">{{ chatHistory.length }}</span>
          </button>
          <button class="tab-button" :class="{ active: activeTab === 'feedback' }" @click="switchTab('feedback')" :disabled="isProcessing">
             <svg width="16" height="16" viewBox="0 0 24 24"><path d="M14 9V5a3 3 0 0 0-6 0v4m-2 4h12a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z" fill="none" stroke="currentColor" stroke-width="2"/></svg>
            {{ getText('feedback') }}
          </button>
        </div>

        <!-- 탭 콘텐츠 -->
        <div class="tab-content">
          <ChatTab
            v-if="activeTab === 'chat'"
            ref="chatTab"
            :categories="categories"
            :loading-categories="loadingCategories"
            :is-processing="isProcessing"
            :current-language="currentLanguage"
            @message-sent="handleMessageSent"
            @processing-state-changed="handleProcessingStateChanged"
          />
          <HistoryTab
            v-if="activeTab === 'history'"
            ref="historyTab"
            :history="chatHistory"
            :loading="loadingHistory"
            :current-language="currentLanguage"
            @view-detail="handleHistoryDetail"
            @refresh-history="loadHistory(true)"
            @go-to-chat="switchTab('chat')"
          />
          <FeedbackTab
            v-if="activeTab === 'feedback'"
            ref="feedbackTab"
            :current-language="currentLanguage"
            @feedback-sent="handleFeedbackSent"
          />
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import devvyService from '../services/devvyService';
import ChatTab from './ChatTab.vue';
import HistoryTab from './HistoryTab.vue';
import FeedbackTab from './FeedbackTab.vue';

export default {
  name: 'DevvyBot',
  components: { ChatTab, HistoryTab, FeedbackTab },
  data() {
    return {
      isOpen: false,
      activeTab: 'chat',
      categories: [],
      loadingCategories: false,
      chatHistory: [],
      loadingHistory: false,
      isProcessing: false,
      isConnected: true,
      hasNotification: false,
      lastActiveTab: 'chat',
      currentLanguage: this.$devvy.getCurrentLanguage() || 'ko',
    };
  },
  computed: {
    // 다국어 텍스트를 반환하는 computed 속성
    texts() {
      const allTexts = {
        ko: {
          online: '온라인', offline: '오프라인', chat: '채팅', history: '히스토리', feedback: '피드백',
          openChatbot: '챗봇 열기', toggleLanguage: 'Switch to English', minimize: '최소화', close: '닫기',
          aiError: '죄송해요, 응답 생성 중 오류가 발생했어요.', networkError: '일시적인 오류가 발생했어요. 다시 시도해주세요.',
        },
        en: {
          online: 'Online', offline: 'Offline', chat: 'Chat', history: 'History', feedback: 'Feedback',
          openChatbot: 'Open Chatbot', toggleLanguage: '한국어로 변경', minimize: 'Minimize', close: 'Close',
          aiError: 'Sorry, an error occurred while generating the response.', networkError: 'A temporary error occurred. Please try again.',
        }
      };
      return allTexts[this.currentLanguage];
    }
  },
  methods: {
    getText(key) {
      return this.texts[key] || key;
    },
    toggleBot() {
      this.isOpen = !this.isOpen;
      if (this.isOpen) {
        this.hasNotification = false;
        this.initializeBotData();
        this.activeTab = this.lastActiveTab;
      }
    },
    minimizeBot() {
      this.lastActiveTab = this.activeTab;
      this.isOpen = false;
    },
    closeBot() {
      this.isOpen = false;
      this.activeTab = 'chat';
      if (this.$refs.chatTab) this.$refs.chatTab.resetToInitialState();
    },
    switchTab(tab) {
      if (this.isProcessing) return;
      this.activeTab = tab;
      if (tab === 'history' && this.chatHistory.length === 0) {
        this.loadHistory();
      }
    },
    toggleLanguage() {
      this.currentLanguage = this.currentLanguage === 'ko' ? 'en' : 'ko';
      this.$devvy.setLanguage(this.currentLanguage);
    },
    initializeBotData() {
      if (this.categories.length === 0) this.loadCategories();
    },
    async loadCategories() {
      if (this.loadingCategories) return;
      this.loadingCategories = true;
      try {
        const response = await devvyService.getCategories();
        if (response.success) {
          this.categories = response.data || [];
        } else {
          console.error('⚠️ Failed to load categories:', response.error);
        }
      } catch (error) {
        console.error('💥 Error loading categories:', error);
      } finally {
        this.loadingCategories = false;
      }
    },
    async loadHistory(forceRefresh = false) {
      if (this.loadingHistory) return;
      this.loadingHistory = true;
      if(forceRefresh) this.chatHistory = [];

      try {
        const response = await devvyService.getChatHistory();
        if (response.success) {
          this.chatHistory = response.data || [];
        } else {
           if(this.$refs.historyTab) this.$refs.historyTab.setError(response.error);
        }
      } catch (error) {
        if(this.$refs.historyTab) this.$refs.historyTab.setError('Failed to fetch history.');
      } finally {
        this.loadingHistory = false;
      }
    },
    async handleMessageSent(data) {
      this.isProcessing = true;
      try {
        const response = await devvyService.sendMessage(data);
        if (response.success) {
          this.$refs.chatTab.addAiResponse(response.data);
          this.isConnected = true;
        } else {
          this.$refs.chatTab.addAiResponse({ message: this.getText('aiError') });
        }
      } catch (error) {
        this.$refs.chatTab.addAiResponse({ message: this.getText('networkError') });
        this.isConnected = false;
      } finally {
        this.isProcessing = false;
      }
    },
    handleProcessingStateChanged(isProcessing) {
      this.isProcessing = isProcessing;
    },
    handleHistoryDetail(item) {
      this.activeTab = 'chat';
      this.$nextTick(() => {
        if (this.$refs.chatTab) {
          this.$refs.chatTab.loadHistorySession(item);
        }
      });
    },
    async handleFeedbackSent(feedbackData) {
      try {
        const response = await devvyService.sendFeedback(feedbackData);
        if (response.success) {
          if (this.$refs.feedbackTab) this.$refs.feedbackTab.showSuccess(response.message);
        } else {
          if (this.$refs.feedbackTab) this.$refs.feedbackTab.showError(response.error);
        }
      } catch (error) {
        if (this.$refs.feedbackTab) this.$refs.feedbackTab.showError('An error occurred while sending feedback.');
      }
    }
  },
  mounted() {
    // 주기적으로 연결 상태를 확인하는 로직 (예: 30초마다)
    setInterval(async () => {
        try {
            const response = await devvyService.healthCheck();
            this.isConnected = response.success;
        } catch {
            this.isConnected = false;
        }
    }, 30000);
  },
  watch: {
      activeTab(newTab) {
          this.lastActiveTab = newTab;
      }
  }
};
</script>
