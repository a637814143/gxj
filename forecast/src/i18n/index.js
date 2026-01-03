import { createI18n } from 'vue-i18n'

const messages = {
  zh: {
    app: {
      title: '农作物产量预测平台',
      subtitle: '请登录以获取最新预测与数据洞察'
    },
    auth: {
      admin: '管理员登录',
      user: '用户登录',
      username: '用户名',
      password: '密码',
      captcha: '验证码',
      refresh: '点击刷新',
      remember: '记住我',
      login: '登录',
      register: '立即注册',
      switchUser: '切换至用户登录',
      switchAdmin: '切换至管理员登录',
      forgot: '忘记密码？',
      noAccount: '还没有账号？',
      userEntrance: '普通用户入口？'
    }
  },
  en: {
    app: {
      title: 'Crop Yield Forecast Platform',
      subtitle: 'Sign in to access forecasts and insights'
    },
    auth: {
      admin: 'Admin Login',
      user: 'User Login',
      username: 'Username',
      password: 'Password',
      captcha: 'Captcha',
      refresh: 'Click to refresh',
      remember: 'Remember me',
      login: 'Sign In',
      register: 'Sign Up',
      switchUser: 'Switch to user login',
      switchAdmin: 'Switch to admin login',
      forgot: 'Forgot password?',
      noAccount: "Don't have an account?",
      userEntrance: 'User entrance?'
    }
  }
}

const storedLocale = localStorage.getItem('app-locale')
const browserLocale = storedLocale || (navigator.language?.toLowerCase().startsWith('en') ? 'en' : 'zh')

export const i18n = createI18n({
  legacy: false,
  locale: browserLocale,
  fallbackLocale: 'zh',
  messages
})
