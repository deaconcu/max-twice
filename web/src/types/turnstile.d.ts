/**
 * Cloudflare Turnstile 类型声明
 */

interface TurnstileOptions {
  sitekey: string
  theme?: 'light' | 'dark' | 'auto'
  size?: 'normal' | 'compact'
  appearance?: 'always' | 'execute' | 'interaction-only'
  retry?: 'auto' | 'never'
  callback?: (token: string) => void
  'error-callback'?: () => void
  'expired-callback'?: () => void
  'before-interactive-callback'?: () => void
  'after-interactive-callback'?: () => void
}

interface Turnstile {
  render: (container: HTMLElement, options: TurnstileOptions) => string
  reset: (widgetId: string) => void
  remove: (widgetId: string) => void
  getResponse: (widgetId: string) => string | undefined
}

declare global {
  interface Window {
    turnstile?: Turnstile
  }
}

export {}
