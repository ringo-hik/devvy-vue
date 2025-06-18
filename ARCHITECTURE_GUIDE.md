# FloatChat 별도 마운트 아키텍처 가이드

## 개요

EventBus 없이 별도 Vue 인스턴스로 FloatChat을 마운트하는 완전 격리형 아키텍처

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                     브라우저 Window                          │
├─────────────────────────────────────────────────────────────┤
│  ┌─ 메인 Vue 앱 (#app) ─┐    ┌─ FloatChat Vue 앱 ─┐      │
│  │                       │    │ (#float-chat-container)   │  │
│  │ App.vue               │    │                           │  │
│  │ ├─ demo-container     │    │ FloatChatMain.vue         │  │
│  │ ├─ toggleFloatChat()  │    │ ├─ ChatTab               │  │
│  │ └─ FloatChatManager   │    │ ├─ FeedbackTab           │  │
│  │                       │    │ └─ 비동기 메시지 처리     │  │
│  └───────────────────────┘    └───────────────────────────┘  │
│             │                              ▲                 │
│             │ 동적 import()                │                 │
│             ▼                              │                 │
│  ┌─────────────────────────────────────────┴─────────────┐   │
│  │            FloatChatManager                           │   │
│  │  ┌─ 메서드 레지스트리 ─┐  ┌─ 인스턴스 관리 ─┐        │   │
│  │  │ getUserInfo()      │  │ init()          │        │   │
│  │  │ showNotification() │  │ destroy()       │        │   │
│  │  │ navigateTo()       │  │ setVisibility() │        │   │
│  │  │ callAPI()          │  │ updateUserInfo()│        │   │
│  │  └────────────────────┘  └─────────────────┘        │   │
│  └───────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 핵심 컴포넌트

### 1. FloatChatManager (floatChatApp.js)

독립적인 Vue 앱을 관리하는 매니저 클래스

```javascript
class FloatChatManager {
  constructor() {
    this.instance = null           // Vue 인스턴스
    this.isInitialized = false     // 초기화 상태
    this.messageHandlers = new Map() // 메인 앱 메서드 레지스트리
    this.config = {
      autoInit: true,
      containerId: 'float-chat-container',
      language: 'ko'
    }
  }
  
  // 핵심 메서드들
  init(options)                    // Vue 앱 초기화
  registerMainMethod(name, handler) // 메인 앱 메서드 등록
  updateUserInfo(userInfo)         // 사용자 정보 업데이트
  setVisibility(visible)           // 가시성 제어
  destroy()                        // 리소스 정리
  getStatus()                      // 상태 조회
}
```

### 2. 메인 앱 (App.vue)

FloatChat을 동적으로 로딩하고 관리하는 메인 애플리케이션

```vue
<template>
  <div id="app">
    <div class="demo-container">
      <!-- 메인 앱 UI -->
      <button @click="toggleFloatChat">FloatChat 토글</button>
      <div class="info">
        <p>FloatChat 상태: {{ floatChatStatus }}</p>
        <p>등록된 메서드: {{ registeredMethods.join(', ') }}</p>
      </div>
    </div>
    
    <!-- FloatChat 별도 마운트 컨테이너 -->
    <div id="float-chat-container" v-show="floatChatVisible"></div>
  </div>
</template>
```

### 3. FloatChat 컴포넌트 (FloatChatMain.vue)

별도 Vue 앱에서 실행되는 채팅 인터페이스

```vue
<template>
  <div class="float-chat">
    <div class="float-chat-button" @click="toggleChat">
      <!-- 플로팅 버튼 -->
    </div>
    <div v-if="isOpen" class="float-chat-window">
      <!-- 채팅 창 UI -->
      <ChatTab />
      <FeedbackTab />
    </div>
  </div>
</template>
```

## 통신 메커니즘

### 1. 메서드 레지스트리 패턴

EventBus 대신 함수 레지스트리를 사용한 통신

```javascript
// 메인 앱에서 메서드 등록
this.floatChatManager.registerMainMethod('getUserInfo', () => {
  console.log('Main App: getUserInfo called')
  return this.userInfo
})

// FloatChat에서 메인 앱 메서드 호출
this.callMainAppMethod('getUserInfo')
```

### 2. 동적 로딩 패턴

```javascript
// App.vue mounted()
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
    
    // 초기 데이터 전달
    this.floatChatManager.updateUserInfo(this.userInfo)
    
  } catch (error) {
    console.error('FloatChat initialization failed:', error)
  }
}
```

### 3. 상태 동기화

```javascript
// 메인 앱 → FloatChat
updateUserInfo(newUserInfo) {
  this.userInfo = { ...this.userInfo, ...newUserInfo }
  if (this.floatChatManager) {
    this.floatChatManager.updateUserInfo(this.userInfo)
  }
}

// FloatChat → 메인 앱 
toggleLanguage() {
  this.currentLanguage = this.currentLanguage === 'ko' ? 'en' : 'ko'
  this.$emit('language-changed', this.currentLanguage)
}
```

## 생명주기 관리

### 1. 초기화 순서

```
1. 메인 Vue 앱 마운트
2. App.vue mounted() 실행
3. floatChatApp.js 동적 import
4. FloatChatManager 인스턴스 생성
5. 메인 앱 메서드들 등록
6. FloatChat Vue 앱 초기화
7. 초기 데이터 동기화
```

### 2. 정리 과정

```
1. App.vue beforeDestroy() 실행
2. FloatChatManager.destroy() 호출
3. FloatChat Vue 인스턴스 제거
4. 메서드 레지스트리 정리
5. 메모리 해제
```

## 비동기 처리 유지

### 1. 메시지 처리 플로우

```javascript
// FloatChatMain.vue
handleMessageSent(data) {
  const requestId = Date.now() + Math.random()
  
  // 1. 즉시 상태 업데이트 (논블로킹)
  this.pendingRequests.set(requestId, {
    categoryCode: data.categoryCode,
    startTime: Date.now()
  })
  this.updateProcessingState()
  
  // 2. 비동기 API 호출 (백그라운드)
  floatChatService.sendMessage(data, true)
    .then(response => this.handleSuccessResponse(data, response))
    .catch(error => this.handleErrorResponse(error))
    .finally(() => {
      this.pendingRequests.delete(requestId)
      this.updateProcessingState()
    })
  
  // 3. 즉시 반환 - 메인 스레드 보호
}
```

### 2. 다중 요청 관리

```javascript
// 동시 다중 메시지 처리 가능
this.pendingRequests = new Map([
  ['req_1672', { categoryCode: 'A', startTime: 1672531200000 }],
  ['req_1673', { categoryCode: 'B', startTime: 1672531201000 }],
  ['req_1674', { categoryCode: 'C', startTime: 1672531202000 }]
])

// UI는 계속 반응적으로 동작
this.chatProcessingCount = this.pendingRequests.size // 3
```

## 격리 수준

### 1. 메모리 격리

```
메인 Vue 앱 메모리 공간    FloatChat Vue 앱 메모리 공간
├─ App.vue 인스턴스       ├─ FloatChatMain.vue 인스턴스
├─ userInfo 데이터        ├─ categories 데이터
├─ floatChatManager       ├─ messages 배열
└─ 메인 앱 상태           └─ 채팅 앱 상태

→ 완전히 분리된 메모리 공간
```

### 2. 상태 격리

```javascript
// 메인 앱 상태
{
  floatChatVisible: true,
  userInfo: { id: 'user123', name: '김개발' },
  floatChatManager: FloatChatManager
}

// FloatChat 앱 상태 (별도 Vue 인스턴스)
{
  isOpen: false,
  activeTab: 'chat',
  categories: [],
  messages: [],
  currentLanguage: 'ko'
}
```

### 3. 오류 격리

```javascript
// FloatChat 에러가 메인 앱에 영향 없음
try {
  floatChatService.sendMessage(data)
} catch (error) {
  // FloatChat 내부에서 처리
  this.handleErrorResponse(error)
  // 메인 앱은 계속 정상 동작
}
```

## 성능 최적화

### 1. 코드 분할

```javascript
// 동적 import로 초기 번들 크기 최소화
const { default: FloatChatManager } = await import('./floatChatApp.js')

// 빌드 결과
main.js         - 36.19 KiB (메인 앱만)
floatChatApp.js - 별도 청크로 분리 (필요시에만 로딩)
```

### 2. 지연 로딩

```javascript
// FloatChat은 메인 앱 로딩 후 별도로 초기화
async mounted() {
  await this.initFloatChat() // 비동기 초기화
}
```

### 3. 메모리 관리

```javascript
beforeDestroy() {
  if (this.floatChatManager) {
    this.floatChatManager.destroy() // 명시적 리소스 정리
  }
}
```

## 장점 분석

### 1. 완전한 격리
- **메모리**: 각각 독립된 Vue 인스턴스
- **상태**: 서로 영향받지 않는 상태 관리
- **오류**: 한쪽 크래시가 다른 쪽에 영향 없음

### 2. 독립적 배포
- **버전**: 각각 다른 Vue 버전 사용 가능
- **업데이트**: FloatChat만 독립적으로 업데이트
- **테스트**: 각각 별도로 테스트 가능

### 3. 확장성
- **팀 분업**: 메인 앱과 FloatChat 팀 독립 개발
- **기술 스택**: 서로 다른 라이브러리 사용 가능
- **성능**: 필요시에만 로딩하여 초기 성능 향상

## 단점 및 고려사항

### 1. 복잡성 증가
- **통신 로직**: 메서드 레지스트리 관리 필요
- **디버깅**: 두 개의 Vue 앱 상태 추적
- **개발 도구**: Vue DevTools에서 별도 인스턴스

### 2. 메모리 사용량
- **Vue 인스턴스**: 2개의 독립된 인스턴스
- **라이브러리**: 중복 로딩 가능성
- **상태 복제**: 일부 데이터의 중복 저장

## 사용 시나리오

### 적합한 경우
- **마이크로 프론트엔드** 아키텍처
- **다른 팀**이 개발하는 독립 모듈
- **완전한 격리**가 필요한 보안 요구사항
- **점진적 마이그레이션** 상황

### 부적합한 경우
- **간단한 컴포넌트** 통합
- **빈번한 상태 공유**가 필요한 경우
- **성능이 매우 중요**한 모바일 환경
- **단순한 프로젝트** 구조

## 결론

EventBus 없는 별도 마운트 방식은 **완전한 격리**와 **독립성**을 제공하는 고급 아키텍처입니다. 

복잡성은 증가하지만, 대규모 프로젝트나 마이크로 프론트엔드 환경에서는 매우 유용한 패턴입니다.

**선택 기준**: 프로젝트 규모, 팀 구조, 격리 요구사항을 종합적으로 고려하여 결정하시기 바랍니다.