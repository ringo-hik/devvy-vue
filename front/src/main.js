import Vue from 'vue'
import App from './App.vue'
import FloatChatMain from './components/FloatChatMain.vue'

Vue.config.productionTip = false

new Vue({
  render: h => h(App),
}).$mount('#app')

new Vue({
  render: h => h(FloatChatMain),
}).$mount('#float-chat-app')