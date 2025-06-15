import Vue from 'vue'
import App from './App.vue'

// Devvy Bot 통합 스타일시트 임포트
import './styles/devvy.scss'

Vue.config.productionTip = false

/**
 * Devvy Bot 전역 설정을 위한 간단한 플러그인
 * - 다국어 설정을 localStorage에 저장하고 관리합니다.
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

// 개발 환경에만 표시되는 시작 로그
if (process.env.NODE_ENV === 'development') {
  console.log('🤖 Devvy Bot v2.0 Initialized (Refactored)');
  console.log('🎨 Minimal Tech theme applied.');
  console.log('🌐 Language support: Korean, English');
}
