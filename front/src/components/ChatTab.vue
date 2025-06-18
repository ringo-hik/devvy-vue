<template>
  <div class="chat-tab">
    <!-- 카테고리 선택 화면 -->
    <div v-if="!selectedCategory" class="category-select">
      <h3>{{ getText('selectCategory') }}</h3>
      <div v-if="loadingCategories" class="loading">
        <div class="spinner"></div>
        <p>{{ getText('loadingCategories') }}</p>
      </div>
      <div v-else class="categories">
        <div v-for="category in categories" :key="category.categoryId" 
             @click="selectCategory(category)" class="category">
          <!-- 개선된 아이콘 처리: iconPath 우선 사용, 없으면 categoryCode 기반 기본 아이콘 사용 -->
          <unicon :name="getCategoryIconName(category)" fill="#2C5AA0" :width="24" :height="24"></unicon>
          <div class="info">
            <div class="name">{{ currentLanguage === 'ko' ? category.description : category.descriptionEn }}</div>
            <div class="desc">{{ currentLanguage === 'ko' ? category.description : category.descriptionEn }}</div>
          </div>
          <unicon name="angle-right" fill="#9AA0A6" :width="16" :height="16"></unicon>
        </div>
      </div>
    </div>

    <!-- 채팅 화면 -->
    <div v-else class="chat-area">
      <!-- 헤더 -->
      <div class="chat-header">
        <div class="category-info">
          <div class="badge">
            <!-- 선택된 카테고리의 아이콘도 동일한 로직 적용 -->
            <unicon :name="getCategoryIconName(selectedCategory)" fill="#2C5AA0" :width="14" :height="14"></unicon>
            {{ currentLanguage === 'ko' ? selectedCategory.description : selectedCategory.descriptionEn }}
          </div>
        </div>
        <div class="controls">
          <button @click="resetCategory" :disabled="isProcessing" class="btn-text">
            <unicon name="redo" fill="#9AA0A6" :width="12" :height="12"></unicon>
            {{ getText('change') }}
          </button>
        </div>
      </div>

      <!-- 메시지 영역 -->
      <div class="messages" ref="messagesContainer" @scroll="handleScroll">
        <div v-if="messages.length === 0 && !isProcessing && !loadingHistory" class="welcome">
          <unicon name="sparkle" fill="#2C5AA0" :width="36" :height="36"></unicon>
          <h4>{{ getText('welcomeTitle').replace('{category}', currentLanguage === 'ko' ? selectedCategory.description : selectedCategory.descriptionEn) }}</h4>
        </div>

        <!-- 히스토리 로딩 -->
        <div v-if="loadingHistory" class="loading-history">
          <div class="spinner"></div>
          <span>{{ getText('loadingHistory') }}</span>
        </div>

        <!-- 메시지들 -->
        <div v-for="message in messages" 
             :key="message.id"
             :class="['message', message.type]"
             ref="messageElements">
          <div class="bubble" :class="{ 'loading': message.isLoading }">
            <!-- 로딩 중인 메시지 -->
            <template v-if="message.isLoading">
              <div class="dots"><span></span><span></span><span></span></div>
              <div class="text">{{ currentLoadingMessage }}</div>
            </template>
            
            <!-- 일반 메시지 -->
            <template v-else>
              <div class="content" v-html="formatMessageContent(message.content)"></div>
              
              <button v-if="message.type === 'ai'" 
                      @click="copyMessage(message)" 
                      class="copy">
                <unicon name="copy" fill="#9AA0A6" :width="12" :height="12"></unicon>
              </button>
            </template>
          </div>
        </div>
      </div>

      <!-- 입력 영역 -->
      <div class="input-area">
        <div v-if="quickQuestions.length > 0 && !isProcessing" class="quick-questions">
          <button v-for="q in quickQuestions" :key="q" @click="sendQuickQuestion(q)" class="quick-btn">
            <unicon name="bolt" fill="#2C5AA0" :width="12" :height="12"></unicon>
            {{ q }}
          </button>
        </div>
        <div class="input-box">
          <textarea v-model="currentMessage" 
                    ref="messageInput" 
                    :placeholder="getText('inputPlaceholder')"
                    @keydown.enter.exact.prevent="sendMessage" 
                    @input="adjustTextareaHeight"
                    :disabled="isProcessing" 
                    maxlength="1000"></textarea>
          <button @click="sendMessage" :disabled="!canSendMessage" class="send">
            <div v-if="isProcessing" class="loading-spinner"></div>
            <unicon v-else name="message" fill="#FFFFFF" :width="18" :height="18"></unicon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import floatChatService from '@/services/floatChatService';

export default {
  name: 'ChatTab',
  props: {
    categories: {
      type: Array,
      default: () => []
    },
    loadingCategories: {
      type: Boolean,
      default: false
    },
    isProcessing: {
      type: Boolean,
      default: false
    },
    currentLanguage: {
      type: String,
      default: 'ko'
    },
    windowSize: {
      type: Object,
      default: () => ({ width: 455, height: 676 })
    }
  },
  data() {
    return {
      selectedCategory: null,
      currentMessage: '',
      messages: [],
      loadingHistory: false,
      currentLoadingMessage: '',
      loadingInterval: null,
      loadingMessageId: null,
      floatChatService: floatChatService
    };
  },
  computed: {
    texts() {
      return {
        ko: {
          selectCategory: '어떤 도움이 필요하신가요?',
          loadingCategories: '카테고리 로딩 중...',
          loadingHistory: '대화 기록 불러오는 중...',
          change: '변경',
          welcomeTitle: '{category} 전문가, SWP Float Chat입니다!',
          inputPlaceholder: '질문을 입력하세요...'
        },
        en: {
          selectCategory: 'How can I help you?',
          loadingCategories: 'Loading categories...',
          loadingHistory: 'Loading conversation history...',
          change: 'Change',
          welcomeTitle: 'I am SWP Float Chat, an expert in {category}!',
          inputPlaceholder: 'Enter your question...'
        }
      }[this.currentLanguage];
    },
    
    canSendMessage() {
      return this.currentMessage.trim().length > 0 && !this.isProcessing;
    },
    
    quickQuestions() {
      if (!this.selectedCategory) return [];
      return floatChatService.getQuickQuestions(this.selectedCategory.categoryCode, this.currentLanguage);
    }
  },
  methods: {
    getText(key) { 
      return this.texts[key] || key; 
    },
    
    // ===== 아이콘 처리 메서드 (개선됨) =====
    
    /**
     * 카테고리의 아이콘명을 반환합니다.
     * iconPath가 있으면 우선 사용, 없으면 categoryCode 기반 기본 아이콘 사용
     */
    getCategoryIconName(category) {
      if (!category) return 'comment';
      
      // floatChatService의 개선된 getCategoryIcon 메서드 사용
      return this.floatChatService.getCategoryIcon(category.categoryCode, category.iconPath);
    },
    
    // ===== 카테고리 관리 =====
    
    selectCategory(category) {
      this.selectedCategory = category;
      this.messages = [];
      this.loadingHistory = true;
      
      // 대화 히스토리 로드 (비동기)
      floatChatService.getConversations(category.categoryCode)
        .then(response => {
          if (response.success && response.data) {
            // 대화 데이터를 채팅 메시지 형태로 변환
            this.messages = floatChatService.convertConversationsToMessages(response.data);
          }
        })
        .catch(error => {
          console.error('대화 기록 로드 오류:', error);
        })
        .finally(() => {
          this.loadingHistory = false;
          this.$nextTick(() => {
            this.scrollToBottom();
            this.$refs.messageInput?.focus();
          });
        });
    },
    
    resetCategory() {
      this.selectedCategory = null;
      this.messages = [];
      this.currentMessage = '';
      this.stopLoadingMessages();
    },
    
    resetToInitialState() {
      this.selectedCategory = null;
      this.messages = [];
      this.currentMessage = '';
      this.stopLoadingMessages();
    },
    
    // ===== 메시지 송수신 =====
    
    sendMessage() {
      if (!this.canSendMessage) return;
      
      const messageContent = this.currentMessage.trim();
      const timestamp = Date.now();
      
      // 사용자 메시지를 즉시 화면에 표시
      const userMessage = {
        id: `user-${timestamp}`,
        type: 'user', 
        content: messageContent,
        timestamp,
        isLoading: false
      };
      
      this.messages.push(userMessage);
      
      // 로딩 메시지 추가
      this.loadingMessageId = `ai-${timestamp}`;
      const loadingMessage = {
        id: this.loadingMessageId,
        type: 'ai',
        content: '',
        timestamp: timestamp + 1,
        isLoading: true
      };
      
      this.messages.push(loadingMessage);
      
      // 입력 필드 초기화 및 UI 업데이트
      this.currentMessage = '';
      this.adjustTextareaHeight();
      this.scrollToBottom();
      this.startLoadingMessages();
      
      // ⭐ 메시지 전송 이벤트 발생 (부모 컴포넌트에서 실제 API 호출)
      this.$emit('message-sent', {
        categoryCode: this.selectedCategory.categoryCode,
        userQuestion: messageContent
      });
    },
    
    sendQuickQuestion(question) {
      this.currentMessage = question;
      this.sendMessage();
    },
    
    // ===== AI 응답 처리 =====
    
    addAiResponse(response) {
      // 로딩 메시지 제거
      if (this.loadingMessageId) {
        const loadingIndex = this.messages.findIndex(msg => msg.id === this.loadingMessageId);
        if (loadingIndex !== -1) {
          this.messages.splice(loadingIndex, 1);
        }
        this.loadingMessageId = null;
      }
      
      this.stopLoadingMessages();
      
      // AI 응답 메시지 추가
      if (response.success) {
        const aiMessage = {
          id: `ai-${Date.now()}`,
          type: 'ai',
          content: response.aiResponse || response.message || '응답을 받았습니다.',
          timestamp: Date.now(),
          isLoading: false,
          conversationId: response.conversationId
        };
        
        this.messages.push(aiMessage);
      } else {
        // 에러 메시지 추가
        const errorMessage = {
          id: `error-${Date.now()}`,
          type: 'ai',
          content: response.message || '응답 생성 중 오류가 발생했습니다.',
          timestamp: Date.now(),
          isLoading: false,
          isError: true
        };
        
        this.messages.push(errorMessage);
      }
      
      this.scrollToBottom();
    },
    
    // ===== 메시지 포맷팅 =====
    
    formatMessageContent(content) {
      if (!content) return '';
      
      // 마크다운 테이블이 포함된 경우 HTML로 변환
      return floatChatService.convertMarkdownTableToHtml(content);
    },
    
    // ===== 로딩 메시지 관리 =====
    
    startLoadingMessages() {
      const loadingMessages = {
        ko: [
          '분석 중입니다',
          '정보를 수집하고 있습니다',
          '응답을 준비하고 있습니다'
        ],
        en: [
          'Analyzing...',
          'Gathering information...',
          'Preparing response...'
        ]
      };
      
      const messages = loadingMessages[this.currentLanguage];
      let index = 0;
      
      this.currentLoadingMessage = messages[index];
      
      this.loadingInterval = setInterval(() => {
        index = (index + 1) % messages.length;
        this.currentLoadingMessage = messages[index];
      }, 2000);
    },
    
    stopLoadingMessages() {
      if (this.loadingInterval) {
        clearInterval(this.loadingInterval);
        this.loadingInterval = null;
      }
    },
    
    // ===== 유틸리티 메서드 =====
    
    copyMessage(message) {
      const textToCopy = floatChatService.convertMessageToText(message);
      
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(textToCopy)
          .then(() => {
            console.log('메시지가 복사되었습니다.');
          })
          .catch(err => {
            console.error('복사 실패:', err);
            this.fallbackCopyTextToClipboard(textToCopy);
          });
      } else {
        this.fallbackCopyTextToClipboard(textToCopy);
      }
    },
    
    fallbackCopyTextToClipboard(text) {
      const textArea = document.createElement("textarea");
      textArea.value = text;
      textArea.style.top = "0";
      textArea.style.left = "0";
      textArea.style.position = "fixed";
      
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      
      try {
        const successful = document.execCommand('copy');
        if (successful) {
          console.log('메시지가 복사되었습니다.');
        } else {
          console.error('복사 실패');
        }
      } catch (err) {
        console.error('복사 실패:', err);
      }
      
      document.body.removeChild(textArea);
    },
    
    adjustTextareaHeight() {
      this.$nextTick(() => {
        const textarea = this.$refs.messageInput;
        if (textarea) {
          textarea.style.height = 'auto';
          const newHeight = Math.min(textarea.scrollHeight, 100);
          textarea.style.height = `${newHeight}px`;
        }
      });
    },
    
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer;
        if (container) {
          container.scrollTop = container.scrollHeight;
        }
      });
    },
    
    handleScroll() {
      // 필요시 스크롤 이벤트 처리 (예: 이전 메시지 로드)
    }
  },
  
  beforeDestroy() {
    this.stopLoadingMessages();
  },
  
  watch: {
    currentLanguage() {
      this.$nextTick(() => {
        this.adjustTextareaHeight();
      });
    },
    
    isProcessing(newVal) {
      if (!newVal) {
        // 처리 완료 시 로딩 메시지 정리
        this.stopLoadingMessages();
      }
    }
  }
};
</script>

<style scoped>
/* 로딩 히스토리 스타일 */
.loading-history {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  color: var(--text-light);
  font-size: 14px;
}

.loading-history .spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #eee;
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 로딩 스피너 (전송 버튼용) */
.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 마크다운 테이블 스타일 */
.content {
  line-height: 1.6;
  word-wrap: break-word;
}

.content ::v-deep .text-content {
  margin-bottom: 12px;
}

.content ::v-deep .table-container {
  margin: 12px 0;
  overflow-x: auto;
}

.content ::v-deep .markdown-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  background: white;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.content ::v-deep .markdown-table thead th {
  background: var(--primary);
  color: white;
  padding: 10px 8px;
  text-align: left;
  font-weight: 600;
  font-size: 11px;
}

.content ::v-deep .markdown-table tbody td {
  padding: 8px;
  border-bottom: 1px solid #e9ecef;
  color: var(--text-dark);
  vertical-align: top;
}

.content ::v-deep .markdown-table tbody tr:hover {
  background-color: #f1f3f4;
}

.content ::v-deep .markdown-table tbody tr:last-child td {
  border-bottom: none;
}

/* AI 메시지 스타일 조정 */
.message.ai .bubble {
  max-width: 95%;
}

/* 로딩 상태 메시지 스타일 */
.message .bubble.loading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
}

.message .bubble.loading .dots {
  display: flex;
  gap: 4px;
}

.message .bubble.loading .dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
  animation: bounce 1.4s ease-in-out infinite both;
}

.message .bubble.loading .dots span:nth-child(2) {
  animation-delay: 0.16s;
}

.message .bubble.loading .dots span:nth-child(3) {
  animation-delay: 0.32s;
}

.message .bubble.loading .text {
  font-size: 12px;
  color: var(--text-light);
}

/* 반응형 테이블 */
@media (max-width: 480px) {
  .content ::v-deep .markdown-table {
    font-size: 10px;
  }
  
  .content ::v-deep .markdown-table thead th,
  .content ::v-deep .markdown-table tbody td {
    padding: 6px 4px;
  }
}

/* 애니메이션 */
@keyframes spin { 
  100% { transform: rotate(360deg); } 
}

@keyframes bounce { 
  0%, 80%, 100% { transform: translateY(0); } 
  40% { transform: translateY(-4px); } 
}
</style>