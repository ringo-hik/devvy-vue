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

  registerMainMethod(methodName, handler) {
    if (typeof handler !== 'function') {
      console.error(`Handler for '${methodName}' must be a function`)
      return false
    }
    
    this.messageHandlers.set(methodName, handler)
    console.log(`Main app method '${methodName}' registered`)
    return true
  }

  unregisterMainMethod(methodName) {
    return this.messageHandlers.delete(methodName)
  }

  updateUserInfo(userInfo) {
    if (this.instance) {
      this.instance.updateUserInfo(userInfo)
    }
  }

  setLanguage(language) {
    if (this.instance) {
      this.instance.setLanguage(language)
    }
  }

  setVisibility(visible) {
    if (this.instance) {
      this.instance.setVisibility(visible)
    }
  }

  destroy() {
    if (this.instance) {
      this.instance.$destroy()
      this.instance = null
    }
    this.isInitialized = false
    this.messageHandlers.clear()
    console.log('FloatChat app destroyed')
  }

  getStatus() {
    return {
      isInitialized: this.isInitialized,
      hasInstance: !!this.instance,
      registeredMethods: Array.from(this.messageHandlers.keys())
    }
  }
}

const floatChatManager = new FloatChatManager()

if (typeof window !== 'undefined') {
  window.FloatChatManager = floatChatManager
}

export default floatChatManager