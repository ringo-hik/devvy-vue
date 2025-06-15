<template>
  <div class="history-tab">
    <div class="history-header">
      <h3 class="section-title">{{ getText('title') }}</h3>
      <button class="refresh-btn" @click="$emit('refresh-history')" :disabled="loading" :title="getText('refresh')">
        <img :src="refreshIcon" alt="refresh" />
      </button>
    </div>

    <div class="history-content">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
      </div>
      <div v-else-if="error" class="error-state">
        <p>⚠️ {{ error }}</p>
        <button @click="$emit('refresh-history')">{{ getText('retry') }}</button>
      </div>
      <div v-else-if="groupedHistory.length > 0" class="history-list">
        <div v-for="group in groupedHistory" :key="group.date" class="date-group">
          <div class="date-header">{{ formatDateHeader(group.date) }}</div>
          <div class="chat-item" v-for="item in group.chats" :key="item.sessionId" @click="viewDetail(item)">
            <div class="item-header">
              <span class="category-badge">{{ getCategoryDisplayName(item.categoryCode) }}</span>
              <span class="time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <p class="item-preview">{{ item.firstMessage }}</p>
            <div class="item-footer">
              <span>{{ item.messageCount }} {{ getText('messages') }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-history">
        <div class="empty-icon">📂</div>
        <h3>{{ getText('noHistory') }}</h3>
        <p>{{ getText('startChatting') }}</p>
        <button @click="$emit('go-to-chat')">{{ getText('startChat') }}</button>
      </div>
    </div>
  </div>
</template>

<script>
import RefreshIcon from '../assets/icons/refresh.svg';

export default {
  name: 'HistoryTab',
  props: ['history', 'loading', 'currentLanguage'],
  data() {
    return {
      error: null,
      refreshIcon: RefreshIcon,
    };
  },
  computed: {
    texts() {
      const allTexts = {
        ko: {
          title: '대화 히스토리', 
          refresh: '새로고침', 
          retry: '재시도', 
          messages: '개 메시지',
          noHistory: '대화 기록이 없습니다.', 
          startChatting: 'Devvy Bot과 새로운 대화를 시작해보세요.',
          startChat: '채팅 시작하기', 
          today: '오늘', 
          yesterday: '어제'
        },
        en: {
          title: 'Chat History', 
          refresh: 'Refresh', 
          retry: 'Retry', 
          messages: 'messages',
          noHistory: 'No chat history found.', 
          startChatting: 'Start a new conversation with Devvy Bot.',
          startChat: 'Start Chat', 
          today: 'Today', 
          yesterday: 'Yesterday'
        }
      };
      return allTexts[this.currentLanguage];
    },
    groupedHistory() {
      if (!this.history || this.history.length === 0) return [];

      const groups = this.history.reduce((acc, chat) => {
        const date = new Date(chat.createdAt).toISOString().split('T')[0];
        if (!acc[date]) {
          acc[date] = [];
        }
        acc[date].push(chat);
        return acc;
      }, {});

      return Object.keys(groups)
        .sort().reverse()
        .map(date => ({ date, chats: groups[date] }));
    }
  },
  methods: {
    getText(key) { 
      return this.texts[key] || key; 
    },
    
    setError(msg) { 
      this.error = msg; 
    },
    
    viewDetail(item) { 
      this.$emit('view-detail', item); 
    },
    
    /**
     * 카테고리 코드를 표시명으로 변환
     */
    getCategoryDisplayName(categoryCode) {
      const categoryMap = {
        'swdp_menu': 'SWDP Menu',
        'project': '프로젝트',
        'voc': 'VOC'
      };
      return categoryMap[categoryCode] || categoryCode;
    },
    
    formatDateHeader(dateStr) {
      const date = new Date(dateStr);
      const today = new Date();
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);

      if (date.toDateString() === today.toDateString()) return this.getText('today');
      if (date.toDateString() === yesterday.toDateString()) return this.getText('yesterday');
      
      const options = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' };
      return date.toLocaleDateString(this.currentLanguage === 'ko' ? 'ko-KR' : 'en-US', options);
    },
    
    formatTime(dateStr) {
      const date = new Date(dateStr);
      return date.toLocaleTimeString(this.currentLanguage === 'ko' ? 'ko-KR' : 'en-US', { 
        hour: '2-digit', 
        minute: '2-digit' 
      });
    }
  },
  watch: {
    loading(newVal) {
      if (!newVal) this.error = null;
    }
  }
};
</script>