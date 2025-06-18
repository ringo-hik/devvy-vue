<template>
  <div id="app">
    <div class="demo-container">
      <div class="logo">🏢</div>
      <h1>Main Service</h1>
      <p class="description">
        메인 서비스가 독립적으로 동작합니다. FloatChat은 통합 컴포넌트로 실행됩니다.
      </p>
      <div class="status">
        <button @click="toggleFloatChat" class="toggle-btn">
          FloatChat {{ floatChatVisible ? '숨기기' : '보이기' }}
        </button>
      </div>
    </div>

    <FloatChatMain 
      v-if="floatChatVisible" 
      :user-info="userInfo"
      @request-user-info="handleRequestUserInfo"
      @show-notification="handleShowNotification"
      @navigate-to="handleNavigateTo"
      @update-state="handleUpdateState"
      @call-api="handleCallAPI"
      @toggle-visibility="handleToggleVisibility" />
  </div>
</template>

<script>
import FloatChatMain from './components/FloatChatMain.vue';

export default {
  name: 'App',
  components: {
    FloatChatMain
  },
  data() {
    return {
      floatChatVisible: true,
      userInfo: {
        id: 'user123',
        name: '김개발',
        role: 'developer'
      }
    };
  },
  methods: {
    toggleFloatChat() {
      this.floatChatVisible = !this.floatChatVisible;
    },

    handleRequestUserInfo() {
      return this.userInfo;
    },

    handleShowNotification(message) {
      alert(`📢 ${message}`);
    },

    handleNavigateTo(page) {
      window.location.hash = `#/${page}`;
    },

    handleUpdateState(key, value) {
      if (key in this.$data) {
        this[key] = value;
      }
    },

    handleCallAPI(endpoint, data) {
      return fetch(endpoint, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.userInfo.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
      }).then(res => res.json());
    },

    handleToggleVisibility(visible) {
      this.floatChatVisible = visible;
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

    &:hover {
      background-color: #1C3959;
    }
  }
}
</style>