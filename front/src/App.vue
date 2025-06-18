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
    async initFloatChat() {
      try {
        const { default: FloatChatManager } = await import('./floatChatApp.js')
        this.floatChatManager = FloatChatManager
        
        this.registerMainAppMethods()
        
        this.floatChatManager.init({
          containerId: 'float-chat-container',
          language: 'ko'
        })
        
        this.floatChatManager.updateUserInfo(this.userInfo)
        
        console.log('FloatChat initialization completed')
        
      } catch (error) {
        console.error('FloatChat initialization failed:', error)
      }
    },
    
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
    
    toggleFloatChat() {
      this.floatChatVisible = !this.floatChatVisible
      if (this.floatChatManager) {
        this.floatChatManager.setVisibility(this.floatChatVisible)
      }
    },
    
    showNotification(message) {
      alert(`📢 ${message}`)
    },
    
    navigateToPage(page) {
      console.log(`Navigating to: ${page}`)
      window.location.hash = `#/${page}`
    },
    
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
    
    updateUserInfo(newUserInfo) {
      this.userInfo = { ...this.userInfo, ...newUserInfo }
      if (this.floatChatManager) {
        this.floatChatManager.updateUserInfo(this.userInfo)
      }
    }
  },
  
  async mounted() {
    await this.initFloatChat()
  },
  
  beforeDestroy() {
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