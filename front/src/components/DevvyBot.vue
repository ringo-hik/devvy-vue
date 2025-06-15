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
      <img class="bot-icon" :src="icons.bot" alt="bot" />
      <div v-if="hasNotification" class="notification-badge"></div>
    </div>

    <!-- 챗봇 창 -->
    <transition name="devvy-window-transition">
      <div v-if="isOpen" class="devvy-window">
        <!-- 헤더 -->
        <div class="devvy-header">
          <div class="header-content">
            <div class="bot-info">
            <img class="bot-avatar" :src="icons.devvy" alt="avatar" />
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
                <img :src="icons.minimize" alt="minimize" />
              </button>
              <button class="close-btn" @click="closeBot" :title="getText('close')">
                <img :src="icons.close" alt="close" />
              </button>
            </div>
          </div>
        </div>

        <!-- 탭 메뉴 -->
        <div class="tab-menu">
          <button class="tab-button" :class="{ active: activeTab === 'chat' }" @click="switchTab('chat')" :disabled="isProcessing">
            <img :src="icons.chat" alt="chat" />
            {{ getText('chat') }}
            <span v-if="isProcessing" class="processing-indicator"></span>
          </button>
          <button class="tab-button" :class="{ active: activeTab === 'history' }" @click="switchTab('history')" :disabled="isProcessing">
             <img :src="icons.history" alt="history" />
            {{ getText('history') }}
            <span v-if="chatHistory.length > 0" class="history-count">{{ chatHistory.length }}</span>
          </button>
          <button class="tab-button" :class="{ active: activeTab === 'feedback' }" @click="switchTab('feedback')" :disabled="isProcessing">
             <img :src="icons.feedback" alt="feedback" />
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

import BotIcon from '../assets/icons/bot-icon.svg';
import DevvyIcon from '../assets/icons/devvy-icon.svg';
import MinimizeIcon from '../assets/icons/minimize-icon.svg';
import CloseIcon from '../assets/icons/close-icon.svg';
import ChatIcon from '../assets/icons/chat-icon.svg';
import HistoryIcon from '../assets/icons/history-icon.svg';
import FeedbackIcon from '../assets/icons/feedback-icon.svg';

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
      icons: {
        bot: BotIcon,
        devvy: DevvyIcon,
        minimize: MinimizeIcon,
        close: CloseIcon,
        chat: ChatIcon,
        history: HistoryIcon,
        feedback: FeedbackIcon
      }
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
          this.categories = (response.data || []).map(c => ({
            ...c,
            name: c.name || c.categoryName
          }));
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
