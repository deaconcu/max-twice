// Vuetify 3 配置
import 'vuetify/styles/main.css'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify, type ThemeDefinition } from 'vuetify'

// 自定义亮色主题
const lightTheme: ThemeDefinition = {
  dark: false,
  colors: {
    primary: '#1867C0',
    secondary: '#5CBBF6',
    accent: '#82B1FF',
    error: '#FF5252',
    info: '#2196F3',
    success: '#4CAF50',
    warning: '#FB8C00',
    background: '#FFFFFF',
    surface: '#FFFFFF',
    'surface-variant': '#F8F9FA',
    'on-surface': '#1A1A1B',
    'on-surface-variant': '#666666',
    'on-background': '#1A1A1B',
    'on-primary': '#FFFFFF',
    border: '#EDEFF1',
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
    surface: '#1E1E1E',
    'surface-variant': '#2C2C2C',
    'on-surface': '#E0E0E0',
    'on-surface-variant': '#B0B0B0',
    'on-background': '#E0E0E0',
    'on-primary': '#FFFFFF',
    border: '#333333',
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
      elevation: 0,
      flat: true,
    },
    VCard: {
      elevation: 0,
      flat: true,
      variant: 'outlined',
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
