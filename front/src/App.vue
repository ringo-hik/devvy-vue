<template>
  <div id="app">
    <div class="demo-container">
      <div class="logo">🏢</div>
      <h1>Main Service</h1>
      <p class="description">
        메인 서비스가 독립적으로 동작합니다. FloatChat은 별도 Vue 앱으로 실행됩니다.
      </p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'App'
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
}
</style>