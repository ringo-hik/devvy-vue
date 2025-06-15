<template>
  <div id="app">
    <!-- 
      데모 페이지 콘텐츠를 최소화하여 
      Devvy Bot 자체에 집중할 수 있도록 합니다.
    -->
    <div class="demo-container">
      <div class="logo">🤖</div>
      <h1>Welcome to Devvy Bot</h1>
      <p class="description">
        Click the icon at the bottom right to start a conversation with your AI assistant.
      </p>
    </div>

    <!-- Devvy Bot 플로팅 챗봇 컴포넌트 -->
    <DevvyBot />
  </div>
</template>

<script>
// DevvyBot 컴포넌트를 임포트합니다.
import DevvyBot from './components/DevvyBot.vue'

export default {
  name: 'App',
  components: {
    DevvyBot
  },
  mounted() {
    // App.vue가 마운트되었을 때 콘솔에 로그를 출력합니다.
    console.log('✅ App.vue mounted, DevvyBot is ready.');
  }
}
</script>

<style lang="scss" scoped>
// App.vue에만 적용되는 최소한의 스타일
#app {
  min-height: 100vh;
  background-color: #f0f2f5; // 차분한 배경색
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
    color: #1C3959; // Primary Navy 색상 적용
    margin-bottom: 15px;
  }

  .description {
    font-size: 1rem;
    color: #6B7886; // Gray 500 색상
    line-height: 1.6;
  }
}
</style>
