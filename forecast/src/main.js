import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { i18n } from './i18n'
import en from 'element-plus/es/locale/lang/en'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

const authStore = useAuthStore()
try {
  await authStore.initialize()
} catch (error) {
  console.error('Failed to initialize authentication', error)
}

app.use(router)
app.use(ElementPlus, { locale: i18n.global.locale.value === 'en' ? en : zhCn })
app.use(i18n)
app.mount('#app')
