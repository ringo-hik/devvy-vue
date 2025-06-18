# FloatChat 별도 마운트 구현 가이드

## 구현 개요

EventBus를 사용하지 않고 별도 Vue 인스턴스로 FloatChat을 마운트하는 방법

## 1. 프로젝트 구조

```
front/
├── src/
│   ├── main.js                 # 메인 Vue 앱 진입점
│   ├── App.vue                 # 메인 앱 + FloatChat 컨테이너
│   ├── floatChatApp.js         # FloatChat 별도 앱 매니저
│   ├── components/
│   │   ├── FloatChatMain.vue   # FloatChat 메인 컴포넌트
│   │   ├── ChatTab.vue         # 채팅 탭
│   │   └── FeedbackTab.vue     # 피드백 탭
│   ├── services/
│   │   └── floatChatService.js # API 서비스
│   └── styles/
│       └── FloatChat.css       # FloatChat 전용 스타일
└── public/
    └── index.html              # 기본 HTML 템플릿
```

## 2. 핵심 구현 파일

### 2.1 FloatChatManager (floatChatApp.js)

별도 Vue 앱을 관리하는 매니저 클래스

```javascript
import Vue from 'vue'
import FloatChatMain from './components/FloatChatMain.vue'
import './styles/FloatChat.css'

class FloatChatManager {
  constructor() {
    this.instance = null
    this.isInitialized = false
    this.messageHandlers = new Map()
    this.config = {
      autoInit: true,
      containerId: 'float-chat-container',
      language: localStorage.getItem('float-chat-lang') || 'ko'
    }
  }

  // Vue 앱 초기화
  init(options = {}) {
    this.config = { ...this.config, ...options }
    
    const container = document.getElementById(this.config.containerId)
    if (!container) {
      console.warn(`FloatChat container #${this.config.containerId} not found`)
      return null
    }

    if (this.isInitialized) {
      console.warn('FloatChat already initialized')
      return this.instance
    }

    // 별도 Vue 인스턴스 생성
    this.instance = new Vue({
      el: `#${this.config.containerId}`,
      components: { FloatChatMain },
      data: () => ({
        language: this.config.language,
        userInfo: null,
        isVisible: true
      }),
      methods: {
        updateUserInfo(userInfo) {
          this.userInfo = userInfo
        },
        
        setLanguage(lang) {
          this.language = lang
          localStorage.setItem('float-chat-lang', lang)
        },
        
        setVisibility(visible) {
          this.isVisible = visible
        },
        
        // 메인 앱 메서드 호출
        callMainAppMethod(methodName, ...args) {
          const handler = window.FloatChatManager?.messageHandlers.get(methodName)
          if (handler && typeof handler === 'function') {
            return handler(...args)
          }
          console.warn(`Main app method '${methodName}' not found`)
          return null
        }
      },
      template: `
        <FloatChatMain 
          v-if="isVisible"
          :user-info="userInfo" 
          :language="language"
          @request-user-info="callMainAppMethod('getUserInfo')"
          @show-notification="callMainAppMethod('showNotification', $event)"
          @navigate-to="callMainAppMethod('navigateTo', $event)"
          @call-api="callMainAppMethod('callAPI', $event)"
          @language-changed="setLanguage"
          @visibility-changed="setVisibility" />
      `,
      mounted() {
        console.log('FloatChat app initialized successfully')
      }
    })

    this.isInitialized = true
    return this.instance
  }

  // 메인 앱 메서드 등록
  registerMainMethod(methodName, handler) {
    if (typeof handler !== 'function') {
      console.error(`Handler for '${methodName}' must be a function`)
      return false
    }
    
    this.messageHandlers.set(methodName, handler)
    console.log(`Main app method '${methodName}' registered`)
    return true
  }

  // 사용자 정보 업데이트
  updateUserInfo(userInfo) {
    if (this.instance) {
      this.instance.updateUserInfo(userInfo)
    }
  }

  // 언어 설정
  setLanguage(language) {
    if (this.instance) {
      this.instance.setLanguage(language)
    }
  }

  // 가시성 제어
  setVisibility(visible) {
    if (this.instance) {
      this.instance.setVisibility(visible)
    }
  }

  // 리소스 정리
  destroy() {
    if (this.instance) {
      this.instance.$destroy()
      this.instance = null
    }
    this.isInitialized = false
    this.messageHandlers.clear()
    console.log('FloatChat app destroyed')
  }

  // 상태 조회
  getStatus() {
    return {
      isInitialized: this.isInitialized,
      hasInstance: !!this.instance,
      registeredMethods: Array.from(this.messageHandlers.keys())
    }
  }
}

const floatChatManager = new FloatChatManager()

// 전역 접근 가능하도록 설정
if (typeof window !== 'undefined') {
  window.FloatChatManager = floatChatManager
}

export default floatChatManager
```

### 2.2 메인 앱 (App.vue)

```vue
<template>
  <div id="app">
    <div class="demo-container">
      <div class="logo">🏢</div>
      <h1>Main Service</h1>
      <p class="description">
        메인 서비스가 독립적으로 동작합니다. FloatChat은 별도 Vue 앱으로 실행됩니다.
      </p>
      <div class="status">
        <button @click="toggleFloatChat" class="toggle-btn">
          FloatChat {{ floatChatVisible ? '숨기기' : '보이기' }}
        </button>
        <div class="info">
          <p>FloatChat 상태: {{ floatChatStatus }}</p>
          <p>등록된 메서드: {{ registeredMethods.join(', ') }}</p>
        </div>
      </div>
    </div>

    <!-- FloatChat 별도 마운트 컨테이너 -->
    <div id="float-chat-container" v-show="floatChatVisible"></div>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      floatChatVisible: true,
      floatChatManager: null,
      userInfo: {
        id: 'user123',
        name: '김개발',
        role: 'developer',
        token: 'sample-jwt-token'
      }
    }
  },
  computed: {
    floatChatStatus() {
      if (!this.floatChatManager) return 'Not Loaded'
      const status = this.floatChatManager.getStatus()
      return status.isInitialized ? 'Initialized' : 'Loading'
    },
    
    registeredMethods() {
      if (!this.floatChatManager) return []
      const status = this.floatChatManager.getStatus()
      return status.registeredMethods
    }
  },
  methods: {
    // FloatChat 초기화
    async initFloatChat() {
      try {
        // 동적 import로 코드 분할
        const { default: FloatChatManager } = await import('./floatChatApp.js')
        this.floatChatManager = FloatChatManager
        
        // 메인 앱 메서드들 등록
        this.registerMainAppMethods()
        
        // FloatChat 앱 초기화
        this.floatChatManager.init({
          containerId: 'float-chat-container',
          language: 'ko'
        })
        
        // 초기 사용자 정보 전달
        this.floatChatManager.updateUserInfo(this.userInfo)
        
        console.log('FloatChat initialization completed')
        
      } catch (error) {
        console.error('FloatChat initialization failed:', error)
      }
    },
    
    // 메인 앱 메서드들을 FloatChat에서 호출 가능하도록 등록
    registerMainAppMethods() {
      this.floatChatManager.registerMainMethod('getUserInfo', () => {
        console.log('Main App: getUserInfo called')
        return this.userInfo
      })
      
      this.floatChatManager.registerMainMethod('showNotification', (message) => {
        console.log('Main App: showNotification called with:', message)
        this.showNotification(message)
      })
      
      this.floatChatManager.registerMainMethod('navigateTo', (page) => {
        console.log('Main App: navigateTo called with:', page)
        this.navigateToPage(page)
      })
      
      this.floatChatManager.registerMainMethod('callAPI', (endpoint, data) => {
        console.log('Main App: callAPI called with:', endpoint, data)
        return this.callMainAPI(endpoint, data)
      })
    },
    
    // FloatChat 토글
    toggleFloatChat() {
      this.floatChatVisible = !this.floatChatVisible
      if (this.floatChatManager) {
        this.floatChatManager.setVisibility(this.floatChatVisible)
      }
    },
    
    // 알림 표시
    showNotification(message) {
      alert(`📢 ${message}`)
    },
    
    // 페이지 이동
    navigateToPage(page) {
      console.log(`Navigating to: ${page}`)
      window.location.hash = `#/${page}`
    },
    
    // API 호출
    callMainAPI(endpoint, data) {
      return fetch(endpoint, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.userInfo.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
      }).then(res => res.json())
    },
    
    // 사용자 정보 업데이트
    updateUserInfo(newUserInfo) {
      this.userInfo = { ...this.userInfo, ...newUserInfo }
      if (this.floatChatManager) {
        this.floatChatManager.updateUserInfo(this.userInfo)
      }
    }
  },
  
  async mounted() {
    // 컴포넌트 마운트 후 FloatChat 초기화
    await this.initFloatChat()
  },
  
  beforeDestroy() {
    // 컴포넌트 제거 시 FloatChat 리소스 정리
    if (this.floatChatManager) {
      this.floatChatManager.destroy()
    }
  }
}
</script>

<style lang="scss" scoped>
#app {
  min-height: 100vh;
  background-color: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.demo-container {
  text-align: center;
  padding: 50px;
  background-color: #ffffff;
  border-radius: 20px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.05);
  max-width: 500px;

  .logo {
    font-size: 5rem;
    margin-bottom: 20px;
  }

  h1 {
    font-size: 2rem;
    color: #1C3959;
    margin-bottom: 15px;
  }

  .description {
    font-size: 1rem;
    color: #6B7886;
    line-height: 1.6;
  }

  .toggle-btn {
    padding: 12px 24px;
    background-color: #2C5AA0;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 14px;
    transition: background-color 0.3s;
    margin-bottom: 20px;

    &:hover {
      background-color: #1C3959;
    }
  }
  
  .info {
    text-align: left;
    background-color: #f8f9fa;
    padding: 15px;
    border-radius: 8px;
    border: 1px solid #e9ecef;
    
    p {
      margin: 5px 0;
      font-size: 12px;
      color: #6c757d;
    }
  }
}
</style>
```

### 2.3 FloatChat 컴포넌트 (FloatChatMain.vue)

```vue
<script>
import floatChatService from '@/services/floatChatService';
import ChatTab from './ChatTab.vue';
import FeedbackTab from './FeedbackTab.vue';

export default {
  name: 'FloatChatMain',
  components: { ChatTab, FeedbackTab },
  props: {
    userInfo: {
      type: Object,
      default: () => ({})
    },
    language: {
      type: String,
      default: 'ko'
    }
  },
  data() {
    return {
      isOpen: false,
      activeTab: 'chat',
      categories: [],
      loadingCategories: false,
      isConnected: true,
      currentLanguage: this.language,
      categorySessionMap: {},
      windowState: 'normal',
      windowSize: {
        width: 455,
        height: 676
      },
      chatProcessingCount: 0,
      pendingRequests: new Map(),
      healthCheckInterval: null,
      currentUser: null
    };
  },
  computed: {
    texts() {
      return {
        ko: {
          online: '온라인', offline: '오프라인', chat: '채팅', 
          feedback: '피드백', aiError: '응답 생성 중 오류가 발생했어요.', 
          networkError: '일시적인 오류가 발생했어요. 다시 시도해주세요.'
        },
        en: {
          online: 'Online', offline: 'Offline', chat: 'Chat', 
          feedback: 'Feedback', aiError: 'An error occurred while generating the response.', 
          networkError: 'A temporary error occurred. Please try again.'
        }
      }[this.currentLanguage];
    }
  },
  methods: {
    getText(key) { 
      return this.texts[key] || key; 
    },
    
    toggleChat() {
      this.isOpen = !this.isOpen;
      if (this.isOpen) {
        this.initializeChatData();
        this.windowState = 'normal';
      }
    },
    
    closeChat() {
      this.isOpen = false;
      this.activeTab = 'chat';
      this.windowState = 'normal';
      this.cancelAllPendingRequests();
      if (this.$refs.chatTab) this.$refs.chatTab.resetToInitialState();
      this.$emit('visibility-changed', false);
    },
    
    minimizeWindow() {
      this.windowState = this.windowState === 'minimized' ? 'normal' : 'minimized';
    },
    
    toggleMaximizeWindow() {
      this.windowState = this.windowState === 'maximized' ? 'normal' : 'maximized';
    },
    
    setActiveTab(tab) {
      if (this.chatProcessingCount > 0 && tab !== 'chat') return;
      this.activeTab = tab;
    },
    
    toggleLanguage() {
      this.currentLanguage = this.currentLanguage === 'ko' ? 'en' : 'ko';
      try {
        localStorage.setItem('float-chat-lang', this.currentLanguage);
      } catch (error) {
        console.error('언어 설정 저장 오류:', error);
      }
      this.$emit('language-changed', this.currentLanguage);
    },
    
    initializeChatData() {
      if (this.categories.length === 0) this.loadCategories();
    },
    
    loadCategories() {
      if (this.loadingCategories) return;
      this.loadingCategories = true;
      
      floatChatService.getCategories()
        .then(response => {
          if (response.success) {
            this.categories = response.data || [];
          }
        })
        .catch(error => {
          console.error('카테고리 로드 오류:', error);
        })
        .finally(() => {
          this.loadingCategories = false;
        });
    },
    
    handleMessageSent(data) {
      const requestId = Date.now() + Math.random();
      this.pendingRequests.set(requestId, {
        categoryCode: data.categoryCode,
        startTime: Date.now()
      });
      this.updateProcessingState();
      
      if (!data.sessionId && this.categorySessionMap[data.categoryCode]) {
        data.sessionId = this.categorySessionMap[data.categoryCode];
      }
      
      floatChatService.sendMessage(data, true)
        .then(response => {
          if (response.success) {
            this.handleSuccessResponse(data, response);
          } else {
            this.handleErrorResponse(response.errorMessage || this.getText('aiError'));
          }
        })
        .catch(error => {
          console.error('메시지 전송 오류:', error);
          this.handleErrorResponse(this.getText('networkError'));
          this.isConnected = false;
        })
        .finally(() => {
          this.pendingRequests.delete(requestId);
          this.updateProcessingState();
        });
    },
    
    handleSuccessResponse(data, response) {
      if (response.sessionId) {
        this.categorySessionMap[data.categoryCode] = response.sessionId;
        try {
          localStorage.setItem('float-chat-sessions', JSON.stringify(this.categorySessionMap));
        } catch (error) {
          console.error('세션 저장 오류:', error);
        }
      }
      
      if (this.$refs.chatTab) {
        this.$refs.chatTab.addAiResponse(response);
      }
      this.isConnected = true;
    },
    
    handleErrorResponse(errorMessage) {
      if (this.$refs.chatTab) {
        this.$refs.chatTab.addAiResponse({ 
          success: false,
          message: errorMessage
        });
      }
    },
    
    updateProcessingState() {
      const newCount = this.pendingRequests.size;
      const oldCount = this.chatProcessingCount;
      this.chatProcessingCount = newCount;
      
      if (oldCount !== newCount) {
        this.$emit('processing-state-changed', newCount > 0);
      }
    },
    
    handleProcessingStateChanged() {
    },
    
    cancelAllPendingRequests() {
      this.pendingRequests.clear();
      this.updateProcessingState();
    },
    
    handleFeedbackSent(feedbackData) {
      floatChatService.sendFeedback(feedbackData)
        .then(response => {
          if (response.success) {
            if (this.$refs.feedbackTab) {
              this.$refs.feedbackTab.showSuccess(response.message);
            }
          } else {
            if (this.$refs.feedbackTab) {
              this.$refs.feedbackTab.showError(response.errorMessage || 'An error occurred while sending feedback.');
            }
          }
        })
        .catch(error => {
          console.error('피드백 전송 오류:', error);
          if (this.$refs.feedbackTab) {
            this.$refs.feedbackTab.showError('An error occurred while sending feedback.');
          }
        });
    },
    
    loadStoredSessions() {
      try {
        const sessionData = localStorage.getItem('float-chat-sessions');
        if (sessionData) {
          this.categorySessionMap = JSON.parse(sessionData);
        }
      } catch (error) {
        console.error('세션 로드 오류:', error);
        this.categorySessionMap = {};
      }
    },
    
    startHealthCheck() {
      this.healthCheckInterval = setInterval(() => {
        floatChatService.healthCheck()
          .then(response => {
            this.isConnected = response.success;
          })
          .catch(() => {
            this.isConnected = false;
          });
      }, 30000);
    },
    
    stopHealthCheck() {
      if (this.healthCheckInterval) {
        clearInterval(this.healthCheckInterval);
        this.healthCheckInterval = null;
      }
    },

    // 메인 앱과 통신하는 메서드들
    requestMainNotification(message, type = 'info') {
      this.$emit('show-notification', message, type);
    },

    requestMainNavigation(page) {
      this.$emit('navigate-to', page);
    },

    callMainAppAPI(endpoint, data) {
      return this.$emit('call-api', endpoint, data);
    },

    requestUserInfo() {
      return this.$emit('request-user-info');
    },

    applyTheme(theme) {
      document.documentElement.setAttribute('data-float-chat-theme', theme);
    }
  },
  
  mounted() {
    this.loadStoredSessions();
    this.startHealthCheck();
    this.currentUser = this.userInfo;
  },
  
  beforeDestroy() {
    this.stopHealthCheck();
    this.cancelAllPendingRequests();
  }
};
</script>
```

## 3. 통신 플로우

### 3.1 초기화 플로우

```
1. 메인 Vue 앱 마운트
   ↓
2. App.vue mounted() 실행
   ↓
3. initFloatChat() 호출
   ↓
4. import('./floatChatApp.js') 동적 로딩
   ↓
5. FloatChatManager 인스턴스 획득
   ↓
6. registerMainAppMethods() 실행
   ↓
7. floatChatManager.init() 호출
   ↓
8. 별도 Vue 인스턴스 생성 (#float-chat-container에 마운트)
   ↓
9. updateUserInfo() 초기 데이터 전달
   ↓
10. FloatChat 앱 준비 완료
```

### 3.2 메시지 전송 플로우

```
1. 사용자가 FloatChat에서 메시지 입력
   ↓
2. FloatChatMain.handleMessageSent() 호출
   ↓
3. pendingRequests에 요청 등록 (비동기 상태 관리)
   ↓
4. floatChatService.sendMessage() API 호출
   ↓
5. 백엔드에서 비동기 처리 (Spring @Async)
   ↓
6. 응답 수신 후 handleSuccessResponse() 실행
   ↓
7. UI 업데이트 및 pendingRequests에서 제거
   ↓
8. 메인 스레드는 전 과정에서 블로킹되지 않음
```

### 3.3 메인 앱 메서드 호출 플로우

```
1. FloatChat에서 메인 앱 기능 필요
   ↓
2. callMainAppMethod('methodName', args) 호출
   ↓
3. FloatChatManager.messageHandlers에서 핸들러 검색
   ↓
4. 등록된 메인 앱 메서드 실행
   ↓
5. 결과 반환 또는 사이드 이펙트 발생
```

## 4. 빌드 및 배포

### 4.1 개발 환경

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run serve

# 빌드
npm run build
```

### 4.2 빌드 결과

```
dist/
├── js/
│   ├── app.[hash].js          # 메인 앱 번들
│   ├── floatChatApp.[hash].js # FloatChat 앱 번들 (코드 분할)
│   └── chunk-vendors.[hash].js # 공통 라이브러리
├── css/
│   └── app.[hash].css         # 통합 스타일
└── index.html                 # 메인 HTML
```

### 4.3 성능 최적화

1. **코드 분할**: FloatChat은 별도 청크로 분리
2. **지연 로딩**: 필요시에만 FloatChat 로딩
3. **메모리 관리**: beforeDestroy에서 명시적 정리

## 5. 테스트 방법

### 5.1 수동 테스트

```bash
# 개발 서버 실행
npm run serve

# 브라우저에서 확인할 항목:
# 1. FloatChat 토글 버튼 동작
# 2. 상태 정보 표시 (Initialized, 등록된 메서드)
# 3. 메시지 전송 및 응답
# 4. 언어 전환 기능
# 5. 창 최소화/최대화
# 6. 메인 앱과 독립적 동작
```

### 5.2 콘솔 로그 확인

```javascript
// 다음 로그들이 순차적으로 나타나야 함:
// 1. "FloatChat initialization completed"
// 2. "Main app method 'getUserInfo' registered"
// 3. "FloatChat app initialized successfully"
// 4. 메시지 전송 시: "Main App: callAPI called with: ..."
```

## 6. 트러블슈팅

### 6.1 일반적인 문제

**Q: FloatChat이 표시되지 않음**
```javascript
// 해결방법:
// 1. #float-chat-container 엘리먼트 존재 확인
// 2. 브라우저 콘솔에서 오류 메시지 확인
// 3. floatChatManager.getStatus() 호출하여 상태 확인
```

**Q: 메인 앱 메서드 호출이 안됨**
```javascript
// 해결방법:
// 1. registerMainAppMethods() 실행 확인
// 2. messageHandlers Map에 메서드 등록 확인
// 3. 메서드명 오타 확인
```

**Q: 메모리 누수 발생**
```javascript
// 해결방법:
// 1. beforeDestroy에서 floatChatManager.destroy() 호출 확인
// 2. healthCheckInterval 정리 확인
// 3. pendingRequests Map 정리 확인
```

### 6.2 디버깅 도구

```javascript
// FloatChat 상태 확인
window.FloatChatManager.getStatus()

// 등록된 메서드 확인
window.FloatChatManager.messageHandlers

// Vue 인스턴스 직접 접근
window.FloatChatManager.instance
```

## 7. 확장 가이드

### 7.1 새로운 메인 앱 메서드 추가

```javascript
// App.vue의 registerMainAppMethods()에 추가
this.floatChatManager.registerMainMethod('newMethod', (param1, param2) => {
  console.log('New method called with:', param1, param2)
  return this.handleNewMethod(param1, param2)
})

// FloatChat에서 호출
this.callMainAppMethod('newMethod', 'arg1', 'arg2')
```

### 7.2 추가 이벤트 처리

```vue
<!-- FloatChatMain.vue 템플릿 -->
<FloatChatMain 
  @new-event="callMainAppMethod('handleNewEvent', $event)"
  ... />

<!-- App.vue 메서드 등록 -->
this.floatChatManager.registerMainMethod('handleNewEvent', (eventData) => {
  this.handleNewEvent(eventData)
})
```

이 구현 가이드를 따라하면 EventBus 없이도 완전히 격리된 별도 마운트 아키텍처를 구현할 수 있습니다.