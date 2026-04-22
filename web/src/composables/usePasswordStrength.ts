import { ref, computed, watch, type Ref } from 'vue'
import { zxcvbnOptions, zxcvbn } from '@zxcvbn-ts/core'
import * as zxcvbnCommonPackage from '@zxcvbn-ts/language-common'
import * as zxcvbnEnPackage from '@zxcvbn-ts/language-en'
import { useI18n } from '@/composables/useI18n'

let optionsInitialized = false

function ensureOptions() {
  if (optionsInitialized) return
  zxcvbnOptions.setOptions({
    dictionary: {
      ...zxcvbnCommonPackage.dictionary,
      ...zxcvbnEnPackage.dictionary,
    },
    graphs: zxcvbnCommonPackage.adjacencyGraphs,
    translations: zxcvbnEnPackage.translations,
  })
  optionsInitialized = true
}

/**
 * 密码强度分数（0-4）
 * 0-1: 弱，阻止提交
 * 2:   一般
 * 3:   良好
 * 4:   强
 */
export function usePasswordStrength(password: Ref<string>) {
  ensureOptions()
  const { t } = useI18n()

  const score = ref(0)

  watch(
    password,
    (val) => {
      if (!val) {
        score.value = 0
        return
      }
      score.value = zxcvbn(val).score
    },
    { immediate: true }
  )

  const label = computed(() => {
    if (!password.value) return ''
    const key = ['tooWeak', 'weak', 'fair', 'good', 'strong'][score.value]
    return t(`validation.passwordStrength.${key}`)
  })

  const color = computed(() => {
    if (!password.value) return 'grey'
    // 0-1 弱：红；2 一般：浅绿；3 良好：主题绿；4 强：深绿
    return (
      ['error', 'error', 'primary-lighten-1', 'primary', 'primary-darken-1'][score.value] ?? 'grey'
    )
  })

  /** 分数是否达标（≥ 2） */
  const isAcceptable = computed(() => !!password.value && score.value >= 2)

  return { score, label, color, isAcceptable }
}
