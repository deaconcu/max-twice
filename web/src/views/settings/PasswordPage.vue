<script lang="ts">
export default {
  name: 'SettingsPasswordPage',
}
</script>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation } from '@tanstack/vue-query'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import { useI18n } from '@/composables/useI18n'
import { getGlobalSnackbar } from '@/composables/config'
import { usePasswordStrength } from '@/composables/usePasswordStrength'
import { useUserStore } from '@/stores/modules/user'
import { accountApi } from '@/api'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

// 已设置密码 → 直接跳回首页
onMounted(() => {
  if (userStore.currentUser?.hasPassword === true) {
    void router.replace('/')
  }
})

const email = computed(() => userStore.currentUser?.email ?? '')

// 流程状态
const codeSent = ref(false)

// 表单数据
const code = ref('')
const newPassword = ref('')
const showPassword = ref(false)

// 密码强度
const { score, label, color, isAcceptable } = usePasswordStrength(newPassword)

// 重发倒计时
const resendSeconds = ref(0)
let resendTimer: ReturnType<typeof setInterval> | null = null

function startResendCountdown(seconds: number) {
  resendSeconds.value = Math.max(0, Math.floor(seconds))
  if (resendTimer) clearInterval(resendTimer)
  if (resendSeconds.value <= 0) return
  resendTimer = setInterval(() => {
    resendSeconds.value -= 1
    if (resendSeconds.value <= 0 && resendTimer) {
      clearInterval(resendTimer)
      resendTimer = null
    }
  }, 1000)
}

onUnmounted(() => {
  if (resendTimer) clearInterval(resendTimer)
})

const { mutate: sendCodeMutate, isPending: sending } = useMutation({
  mutationFn: () => accountApi.sendSetPasswordCode(),
  onSuccess: (data) => {
    codeSent.value = true
    startResendCountdown(data.resendAvailableIn)
    getGlobalSnackbar()?.(t('settings.password.codeSent'), 'success')
  },
})

function handleSendCode() {
  if (sending.value || resendSeconds.value > 0) return
  sendCodeMutate()
}

const { mutate: confirmMutate, isPending: confirming } = useMutation({
  mutationFn: () => accountApi.confirmSetPassword(code.value.trim(), newPassword.value),
  onSuccess: () => {
    getGlobalSnackbar()?.(t('settings.password.success'), 'success')
    userStore.updateUser({ hasPassword: true })
    void router.replace('/')
  },
})

const canConfirm = computed(() => {
  return code.value.trim().length > 0 && isAcceptable.value && !confirming.value
})

function handleConfirm() {
  if (!canConfirm.value) return
  confirmMutate()
}
</script>

<template>
  <DefaultLayout>
    <v-container class="py-8" style="max-width: 560px">
      <v-card class="pa-6" variant="flat" rounded="lg">
        <div class="d-flex align-center ga-3 mb-4">
          <v-icon icon="mdi-lock-outline" size="28" color="primary" />
          <h1 class="text-h5 font-weight-bold">{{ t('settings.password.title') }}</h1>
        </div>

        <p class="text-body-2 mb-4" :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }">
          {{ t('settings.password.description') }}
        </p>

        <!-- 邮箱展示 + 发送按钮 -->
        <div>
          <div
            class="text-caption mb-1"
            :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
          >
            {{
              codeSent ? t('settings.password.emailLabelSent') : t('settings.password.emailLabel')
            }}
          </div>
          <div class="d-flex align-center ga-3 flex-wrap">
            <div class="text-body-1 font-weight-medium">{{ email }}</div>
            <v-btn
              :loading="sending"
              :disabled="resendSeconds > 0"
              color="primary"
              variant="tonal"
              size="small"
              @click="handleSendCode"
            >
              <template v-if="resendSeconds > 0">
                {{ t('settings.password.resendIn', { seconds: resendSeconds }) }}
              </template>
              <template v-else-if="codeSent">
                {{ t('settings.password.resend') }}
              </template>
              <template v-else>
                {{ t('settings.password.sendCode') }}
              </template>
            </v-btn>
          </div>
        </div>

        <!-- Step 2: 验证码 + 新密码（仅在发过码后显示） -->
        <template v-if="codeSent">
          <v-text-field
            v-model="code"
            :label="t('settings.password.codeLabel')"
            :placeholder="t('settings.password.codePlaceholder')"
            variant="outlined"
            density="comfortable"
            maxlength="6"
            autocomplete="one-time-code"
            class="mt-6 mb-3"
          />

          <v-text-field
            v-model="newPassword"
            :label="t('settings.password.newPasswordLabel')"
            :placeholder="t('settings.password.newPasswordPlaceholder')"
            :type="showPassword ? 'text' : 'password'"
            variant="outlined"
            density="comfortable"
            autocomplete="new-password"
            class="mb-4"
          >
            <template #append-inner>
              <div v-if="newPassword" class="strength-segments mr-2">
                <span class="text-caption strength-label" :class="`text-${color}`">
                  {{ label }}
                </span>
                <span
                  v-for="i in 5"
                  :key="i"
                  class="seg"
                  :class="i <= score + 1 ? `bg-${color}` : ''"
                />
              </div>
              <v-icon tabindex="-1" style="cursor: pointer" @click="showPassword = !showPassword">
                {{ showPassword ? 'mdi-eye-off' : 'mdi-eye' }}
              </v-icon>
            </template>
          </v-text-field>

          <v-btn
            :loading="confirming"
            :disabled="!canConfirm"
            color="primary"
            block
            size="large"
            @click="handleConfirm"
          >
            {{ t('settings.password.confirm') }}
          </v-btn>
        </template>
      </v-card>
    </v-container>
  </DefaultLayout>
</template>

<style scoped>
.strength-segments {
  display: flex;
  align-items: center;
  gap: 3px;
  line-height: 1;
  flex: none;
  white-space: nowrap;
}
.strength-label {
  flex: none;
  min-width: 56px;
  text-align: right;
  margin-right: 6px;
}
.strength-segments .seg {
  flex: none;
  width: 6px;
  height: 0.7em;
  border-radius: 2px;
  background-color: rgba(var(--v-theme-on-surface), 0.12);
  transition: background-color 0.2s ease;
}
</style>
