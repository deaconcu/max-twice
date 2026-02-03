/**
 * Vuetify 插件配置
 */
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
    grey: '#757575',
    background: '#FFFFFF',
    surface: '#FFFFFF',
    'surface-variant': '#F8F9FA',
    'on-surface': '#1A1A1B',
    'on-surface-variant': '#666666',
    'on-background': '#1A1A1B',
    'on-primary': '#FFFFFF',
    border: '#EDEFF1',
    outline: '#E5E5E5',
  },
}

export default createVuetify({
  icons: {
    defaultSet: 'mdi',
  },
  theme: {
    defaultTheme: 'light',
    themes: {
      light: lightTheme,
    },
    variations: {
      colors: ['primary', 'secondary', 'grey'],
      lighten: 5,
      darken: 4,
    },
  },
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
