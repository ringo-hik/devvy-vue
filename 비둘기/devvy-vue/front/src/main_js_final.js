import Vue from 'vue'
import App from './App.vue'

// Devvy Bot 통합 스타일시트
import './styles/devvy.scss'

Vue.config.productionTip = false

/**
 * Devvy Bot 전역 설정 플러그인
 * 다국어 및 사용자 설정 관리
 */
const DevvyPlugin = {
  install(Vue) {
    const devvy = new Vue({
      data: {
        language: localStorage.getItem('devvy-lang') || 'ko',
      },
      methods: {
        setLanguage(lang) {
          this.language = lang;
          localStorage.setItem('devvy-lang', lang);
        },
        getCurrentLanguage() {
          return this.language;
        }
      }
    });
    Vue.prototype.$devvy = devvy;
  }
};

Vue.use(DevvyPlugin);

// Vue 앱 생성 및 마운트
new Vue({
  render: h => h(App),
}).$mount('#app')

// 개발 환경 로그
if (process.env.NODE_ENV === 'development') {
  console.log('🤖 Devvy Bot v3.0 Initialized');
  console.log('🎨 Minimal Tech Design Applied');
  console.log('📱 Responsive Layout Enabled');
}