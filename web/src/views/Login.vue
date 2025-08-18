<script setup>
import TheWelcome from '../components/TheWelcome.vue'
//import { mdiAlphaMBox, mdiClose, mdiEmailOutline } from '@mdi/js'
import { ref } from 'vue';
import { useRoute, useRouter} from 'vue-router';

import Footer from '../components/Footer.vue';
import { userService} from '@/services/learnService';
import { useUserStore } from "@/stores/user";

const router = useRouter();
const user = useUserStore();

/* start other */
const togglePasswordVisibility = (repeat) => {
  if (!repeat) showPassword.value = !showPassword.value;
  else showPasswordRepeat.value = !showPasswordRepeat.value;
};

const showPassword = ref(false);
const showPasswordRepeat = ref(false);
/* end other */


/* start login */

const registerForm = ref({
  email: '',
  name: '',
  password: '',
  passwordRepeat: '',
  validateCode: ''
});

const loginForm = ref({
  email: 'deaconcc@126.com',
  password: ''
})

const registerFirstDialog = ref(false);
const registerSecondDialog = ref(false);
const loginDialog = ref(false);

const submitRegisterFirstForm = async () => {
  try {
    console.log("begin post");
    const formData = new FormData();
    formData.append('email', registerForm.value.email);
    formData.append('userName', registerForm.value.name);
    formData.append('password', registerForm.value.password);

    const response = await userService.register(formData);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      registerFirstDialog.value = false;
      registerSecondDialog.value = true;
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
};

const submitRegisterSecondForm = async () => {
  try {
    console.log("begin post");
    const formData = new FormData();
    formData.append('email', registerForm.value.email);
    formData.append('code', registerForm.value.validateCode);

    const response = await userService.validateMail(formData);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      registerSecondDialog.value = false;
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  } 
}

const submitLogin = async() => {
  try {
    console.log("begin post");
    const formData = new FormData();
    formData.append('email', loginForm.value.email);
    formData.append('password', loginForm.value.password);

    const response = await userService.login(formData);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      loginDialog.value = false;
      user.setUserId(response.data.id);
      user.setSubscription(response.data.subscriptions);
      router.push({ name: 'courseList', params: { } });
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  } 
}
/* end login */

</script>

<template>
  <v-app>
    <!-- 顶部标题栏 -->
    <v-app-bar app flat>
      <v-container class="ma-0 pa-0" fluid>
        <div class="d-flex align-center">
          <v-avatar color="primary" size="40" class="mr-3">
            <v-icon size="24" color="white">mdi-alpha-m-box</v-icon>
          </v-avatar>
          <span class="text-h6 font-weight-bold text-grey-darken-4">最多两次，最好一次</span>
          <v-spacer></v-spacer>
          <router-link to="/about" class="text-primary font-weight-medium text-decoration-none">
            <v-btn variant="text" color="primary" prepend-icon="mdi-help-circle">
              为什么有这个网站？
            </v-btn>
          </router-link>
        </div>
      </v-container>
    </v-app-bar>

    <!-- 页面内容 -->
    <v-main class="main-background">
      <v-container class="fill-height ma-0" fluid>
        <v-row class="align-center justify-center" no-gutters>

          <!-- 左侧图片区域 -->
          <v-col cols="6" class="text-center pa-8">
            <div class="mb-6">
              <h1 class="text-h3 font-weight-bold text-grey-darken-4 mb-4">欢迎来到学习平台</h1>
              <p class="text-h6 text-grey-darken-2 mb-8">高效学习，一次掌握 • 智能互动，共同进步</p>
            </div>
            
            <v-card rounded="xl" elevation="0" color="grey-lighten-5" class="pa-6 mx-auto" style="max-width: 600px; border: 1px solid rgba(0, 0, 0, 0.06);">
              <v-img 
                src="/images/big2.png" 
                width="0" 
                class="mx-auto rounded-lg mb-4">
              </v-img>
              
              <!-- 特色展示 -->
              <div class="d-flex justify-space-around mt-6">
                <div class="text-center">
                  <v-avatar color="primary" size="48" class="mb-2">
                    <v-icon icon="mdi-lightning-bolt" color="white" size="24"></v-icon>
                  </v-avatar>
                  <h4 class="text-body-1 font-weight-bold text-grey-darken-4">高效学习</h4>
                  <p class="text-body-2 text-grey-darken-2 mb-0">最多两次掌握</p>
                </div>
                
                <div class="text-center">
                  <v-avatar color="success" size="48" class="mb-2">
                    <v-icon icon="mdi-account-group" color="white" size="24"></v-icon>
                  </v-avatar>
                  <h4 class="text-body-1 font-weight-bold text-grey-darken-4">互动社区</h4>
                  <p class="text-body-2 text-grey-darken-2 mb-0">共同学习进步</p>
                </div>
                
                <div class="text-center">
                  <v-avatar color="warning" size="48" class="mb-2">
                    <v-icon icon="mdi-chart-line" color="white" size="24"></v-icon>
                  </v-avatar>
                  <h4 class="text-body-1 font-weight-bold text-grey-darken-4">进度跟踪</h4>
                  <p class="text-body-2 text-grey-darken-2 mb-0">实时掌握成果</p>
                </div>
              </div>
              
              <!-- 添加这里的网站简介内容 -->
              <div class="mt-6 pa-4 bg-blue-lighten-5 rounded-lg">
                <h3 class="text-h6 font-weight-bold text-primary mb-2">为什么选择我们？</h3>
                <p class="text-body-2 text-grey-darken-2 mb-2">• 个性化学习路径，根据您的进度调整</p>
                <p class="text-body-2 text-grey-darken-2 mb-2">• 专业导师在线指导，随时答疑解惑</p>
                <p class="text-body-2 text-grey-darken-2 mb-2">• 学习社区互动，与同学共同进步</p>
                <p class="text-body-2 text-grey-darken-2 mb-0">• 多媒体教学资源，让学习更生动</p>
              </div>
            </v-card>
          </v-col>

          <!-- 右侧登录和注册部分 -->
          <v-col cols="6" class="pa-8">
            <v-card rounded="xl" elevation="0" color="white" class="pa-8" style="border: 1px solid rgba(0, 0, 0, 0.06); box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08); max-width: 480px; margin: 0 auto;">
              <div class="text-center mb-6">
                <h2 class="text-h4 font-weight-bold text-grey-darken-4 mb-2">开始您的学习之旅</h2>
                <p class="text-body-1 text-grey-darken-2">我不懂的问题，能一次教会我吗？</p>
                <p class="text-body-1 text-grey-darken-2">最多 <span class="text-primary font-weight-bold">两次</span>，不能再多了 ^_^</p>
              </div>

              <div class="mb-6">
                <p class="text-h6 font-weight-medium text-grey-darken-3 mb-4">现在就加入，或者先逛逛</p>
                
                <v-dialog max-width="600" v-model="registerFirstDialog" persistent>
                  <template v-slot:activator="{ props: activatorProps }">
                    <v-btn 
                      block 
                      size="large" 
                      color="primary" 
                      rounded="lg" 
                      v-bind="activatorProps"
                      class="font-weight-bold mb-4">
                      <v-icon icon="mdi-account-plus" class="mr-2"></v-icon>
                      注 册
                    </v-btn>
                  </template>
                  <template v-slot:default="{ isActive }">
                    <v-card rounded="xl" elevation="0">
                      <v-card-title class="pa-6 pb-4">
                        <div class="d-flex align-center">
                          <v-avatar color="primary" size="40" class="mr-3">
                            <v-icon icon="mdi-account-plus" color="white" size="20"></v-icon>
                          </v-avatar>
                          <div>
                            <h3 class="text-h6 font-weight-bold">欢迎加入</h3>
                            <p class="text-body-2 text-grey-darken-2 mb-0">创建您的学习账户</p>
                          </div>
                        </div>
                        <v-btn 
                          icon="mdi-close" 
                          variant="text" 
                          size="small" 
                          class="position-absolute" 
                          style="top: 16px; right: 16px;"
                          @click="registerFirstDialog = false">
                        </v-btn>
                      </v-card-title>

                      <v-card-text class="pa-6 pt-0">
                        <div class="mb-4 pa-3 bg-primary-lighten-5 rounded text-primary d-flex align-center">
                          <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
                          <span class="text-body-2">请填写基本信息完成注册</span>
                        </div>

                        <v-text-field 
                          v-model="registerForm.email" 
                          label="邮箱地址" 
                          variant="outlined" 
                          class="mb-4"
                          prepend-inner-icon="mdi-email-outline" 
                          hint="请输入常用邮箱，用于接收验证码" 
                          persistent-hint 
                          maxlength="30"
                          rounded="lg"
                          density="comfortable"
                          clearable>
                        </v-text-field>

                        <v-text-field 
                          v-model="registerForm.name" 
                          label="用户名" 
                          variant="outlined" 
                          class="mb-4"
                          prepend-inner-icon="mdi-account-outline" 
                          hint="用户名长度2-20个字符" 
                          persistent-hint
                          maxlength="20" 
                          rounded="lg"
                          density="comfortable"
                          clearable>
                        </v-text-field>

                        <v-text-field 
                          v-model="registerForm.password" 
                          :type="showPassword ? 'text' : 'password'" 
                          label="密码" 
                          variant="outlined" 
                          class="mb-4"
                          prepend-inner-icon="mdi-lock-outline"
                          :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'" 
                          hint="密码需包含大小写字母和特殊字符" 
                          maxlength="20" 
                          persistent-hint
                          rounded="lg"
                          density="comfortable"
                          @click:append-inner="togglePasswordVisibility(false)"
                          clearable>
                        </v-text-field>

                        <v-text-field 
                          v-model="registerForm.passwordRepeat" 
                          :type="showPasswordRepeat ? 'text' : 'password'"
                          label="确认密码" 
                          variant="outlined" 
                          class="mb-6"
                          prepend-inner-icon="mdi-lock-check-outline"
                          :append-inner-icon="showPasswordRepeat ? 'mdi-eye' : 'mdi-eye-off'" 
                          maxlength="20"
                          rounded="lg"
                          density="comfortable"
                          @click:append-inner="togglePasswordVisibility(true)"
                          clearable>
                        </v-text-field>

                        <v-btn 
                          block 
                          size="large" 
                          @click="submitRegisterFirstForm" 
                          color="primary"
                          rounded="lg" 
                          class="font-weight-bold">
                          <v-icon icon="mdi-rocket-launch" class="mr-2"></v-icon>
                          创建账户
                        </v-btn>
                      </v-card-text>
                    </v-card>
                  </template>
                </v-dialog>
              </div>
            <v-dialog max-width="600" v-model="registerSecondDialog" persistent>
              <template v-slot:default>
                <v-card rounded="xl" elevation="0">
                  <v-card-title class="pa-6 pb-4">
                    <div class="d-flex align-center">
                      <v-avatar color="success" size="40" class="mr-3">
                        <v-icon icon="mdi-email-check" color="white" size="20"></v-icon>
                      </v-avatar>
                      <div>
                        <h3 class="text-h6 font-weight-bold">邮箱验证</h3>
                        <p class="text-body-2 text-grey-darken-2 mb-0">请检查您的邮箱</p>
                      </div>
                    </div>
                    <v-btn 
                      icon="mdi-close" 
                      variant="text" 
                      size="small" 
                      class="position-absolute" 
                      style="top: 16px; right: 16px;"
                      @click="registerSecondDialog = false">
                    </v-btn>
                  </v-card-title>

                  <v-card-text class="pa-6 pt-0">
                    <div class="mb-4 pa-3 bg-warning-lighten-5 rounded text-warning d-flex align-center">
                      <v-icon icon="mdi-email-outline" start size="16" class="mr-2"></v-icon>
                      <span class="text-body-2">我们向您的邮箱发送了验证码，请查收并输入。</span>
                    </div>

                    <v-form @submit.prevent="submitRegisterSecondForm">
                      <v-text-field 
                        v-model="registerForm.validateCode" 
                        label="验证码" 
                        variant="outlined"
                        class="mb-4" 
                        prepend-inner-icon="mdi-key"
                        rounded="lg"
                        density="comfortable"
                        hint="请输入6位数字验证码"
                        persistent-hint
                        clearable>
                      </v-text-field>

                      <v-btn 
                        block 
                        size="large" 
                        @click="submitRegisterSecondForm" 
                        color="success"
                        rounded="lg" 
                        class="font-weight-bold mb-3">
                        <v-icon icon="mdi-check-circle" class="mr-2"></v-icon>
                        验证邮箱
                      </v-btn>
                    </v-form>

                    <div class="text-center">
                      <v-btn variant="text" color="primary" class="text-body-2">
                        重新发送验证码
                      </v-btn>
                    </div>
                  </v-card-text>
                </v-card>
              </template>
            </v-dialog>

            <div class="mb-4">
              <p class="text-body-2 text-grey-darken-2 text-center mb-4">
                注册即表示同意
                <a href="#" class="text-primary text-decoration-none">服务条款</a>
                及
                <a href="#" class="text-primary text-decoration-none">隐私政策</a>
              </p>
            </div>

            <div class="text-center">
              <p class="text-body-1 font-weight-medium text-grey-darken-3 mb-3">已有账号？</p>
              
              <v-dialog max-width="600" v-model="loginDialog" persistent>
                <template v-slot:activator="{ props: activatorProps }">
                  <v-btn 
                    block 
                    size="large" 
                    variant="outlined" 
                    color="primary"
                    rounded="lg"
                    v-bind="activatorProps"
                    class="font-weight-bold">
                    <v-icon icon="mdi-login" class="mr-2"></v-icon>
                    登 录
                  </v-btn>
                </template>
                <template v-slot:default="{ isActive }">
                  <v-card rounded="xl" elevation="0">
                    <v-card-title class="pa-6 pb-4">
                      <div class="d-flex align-center">
                        <v-avatar color="primary" size="40" class="mr-3">
                          <v-icon icon="mdi-emoticon-wink" color="white" size="20"></v-icon>
                        </v-avatar>
                        <div>
                          <h3 class="text-h6 font-weight-bold">欢迎回来</h3>
                          <p class="text-body-2 text-grey-darken-2 mb-0">登录您的学习账户</p>
                        </div>
                      </div>
                      <v-btn 
                        icon="mdi-close" 
                        variant="text" 
                        size="small" 
                        class="position-absolute" 
                        style="top: 16px; right: 16px;"
                        @click="loginDialog = false">
                      </v-btn>
                    </v-card-title>

                    <v-card-text class="pa-6 pt-0">
                      <div class="mb-4 pa-3 bg-blue-lighten-5 rounded text-info d-flex align-center">
                        <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
                        <span class="text-body-2">使用邮箱和密码登录您的账户</span>
                      </div>

                      <v-text-field 
                        v-model="loginForm.email" 
                        label="邮箱地址" 
                        variant="outlined" 
                        class="mb-4"
                        prepend-inner-icon="mdi-email-outline"
                        rounded="lg"
                        density="comfortable"
                        clearable>
                      </v-text-field>

                      <v-text-field 
                        v-model="loginForm.password" 
                        :type="showPassword ? 'text' : 'password'" 
                        label="密码" 
                        variant="outlined" 
                        class="mb-6"
                        prepend-inner-icon="mdi-lock-outline"
                        :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'" 
                        @click:append-inner="togglePasswordVisibility"
                        rounded="lg"
                        density="comfortable"
                        clearable>
                      </v-text-field>

                      <v-btn 
                        block 
                        size="large" 
                        @click="submitLogin" 
                        color="primary"
                        rounded="lg" 
                        class="font-weight-bold">
                        <v-icon icon="mdi-login" class="mr-2"></v-icon>
                        登录
                      </v-btn>
                    </v-card-text>
                  </v-card>
                </template>
              </v-dialog>
            </div>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>

    <!-- 页脚 -->
    <v-footer app>
      <v-container class="ma-0 pa-0" fluid>
        <Footer />
      </v-container>
    </v-footer>
  </v-app>
</template>
  
  <style>
  .main-background {
    min-width: 1440px;
  }

  </style>