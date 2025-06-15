<template>
  <div id="app">
    <!-- 미니멀 데모 페이지 -->
    <div class="demo-container">
      <div class="logo">🤖</div>
      <h1>Devvy Bot</h1>
      <p class="description">
        AI-powered assistant for your development workflow
      </p>
      <div class="demo-note">
        Click the floating button to start chatting
      </div>
    </div>

    <!-- Devvy Bot 플로팅 챗봇 -->
    <DevvyBot />
  </div>
</template>

<script>
import DevvyBot from './components/DevvyBot.vue'

export default {
  name: 'App',
  components: {
    DevvyBot
  },
  mounted() {
    console.log('🤖 Devvy Bot initialized');
  }
}
</script>

<style lang="scss" scoped>
#app {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.demo-container {
  text-align: center;
  padding: 60px 40px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  max-width: 480px;
  border: 1px solid rgba(255, 255, 255, 0.2);

  .logo {
    font-size: 4rem;
    margin-bottom: 24px;
    filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.1));
  }

  h1 {
    font-size: 2.5rem;
    color: #2A52BE;
    margin-bottom: 16px;
    font-weight: 600;
    letter-spacing: -0.02em;
  }

  .description {
    font-size: 1.1rem;
    color: #6C757D;
    line-height: 1.6;
    margin-bottom: 24px;
  }
  
  .demo-note {
    font-size: 0.9rem;
    color: #ADB5BD;
    padding: 12px 20px;
    background: rgba(42, 82, 190, 0.05);
    border-radius: 12px;
    border: 1px solid rgba(42, 82, 190, 0.1);
  }
}
</style>
