// Vuetify 3 配置
// 注意：使用 vite-plugin-vuetify 的 autoImport 后，
// 样式和组件都会自动按需引入，无需手动导入
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify, type ThemeDefinition } from 'vuetify'

// 自定义亮色主题
const lightTheme: ThemeDefinition = {
  dark: false,
  colors: {
    primary: '#1976D2',
    secondary: '#424242',
    accent: '#82B1FF',
    error: '#FF5252',
    info: '#2196F3',
    success: '#4CAF50',
    warning: '#FB8C00',
    background: '#FFFFFF',
    surface: '#FFFFFF',
  },
}

// 自定义暗色主题
const darkTheme: ThemeDefinition = {
  dark: true,
  colors: {
    primary: '#2196F3',
    secondary: '#424242',
    accent: '#FF4081',
    error: '#FF5252',
    info: '#2196F3',
    success: '#4CAF50',
    warning: '#FB8C00',
    background: '#121212',
    surface: '#212121',
  },
}

export default createVuetify({
  // 图标配置
  icons: {
    defaultSet: 'mdi',
  },

  // 主题配置
  theme: {
    defaultTheme: 'light',
    themes: {
      light: lightTheme,
      dark: darkTheme,
    },
    variations: {
      colors: ['primary', 'secondary'],
      lighten: 2,
      darken: 2,
    },
  },

  // 默认配置
  defaults: {
    VBtn: {
      color: 'primary',
      variant: 'flat',
    },
    VCard: {
      elevation: 2,
    },
    VTextField: {
      variant: 'outlined',
      density: 'comfortable',
    },
    VSelect: {
      variant: 'outlined',
      density: 'comfortable',
    },
  },
})
