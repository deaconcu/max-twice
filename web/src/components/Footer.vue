<script setup>
const currentYear = new Date().getFullYear()

const links = [
  { title: '主页', route: '/' },
  { title: '关于我们', route: '/about' },
  { title: '联系我们', route: '/contact' },
  { title: '隐私政策', route: '/privacy' },
  { title: '服务条款', route: '/terms' }
]
</script>

<template>
  <div class="footer-container" color="">
    <v-container fluid>
      <v-row class="py-6">
        <!-- 导航链接 -->
        <v-col cols="12" md="8" class="text-center text-md-start">
          <div class="d-flex flex-wrap justify-center justify-md-start align-center">
            <v-btn
              v-for="link in links"
              :key="link.title"
              :to="link.route"
              variant="text"
              color="grey-darken-1"
              class="mx-2 my-1 footer-link"
              size="small"
            >
              {{ link.title }}
            </v-btn>
          </div>
        </v-col>

        <!-- 版权信息 -->
        <v-col cols="12" md="4" class="text-center text-md-end">
          <p class="text-body-2 text-grey-darken-2 mb-1">
            职业应该是自我实现
          </p>
          <p class="text-body-2 text-grey-darken-1 mb-0">
            © {{ currentYear }} <strong>Max Twice</strong>
          </p>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<style scoped>
.footer-container {
  height:100px;
  margin-top: 80px;
  border-top: 0px solid rgba(0, 0, 0, 0.08);
}

.footer-link {
  border-radius: 6px !important;
  text-transform: none !important;
  font-weight: 400 !important;
  transition: all 0.2s ease !important;
}

.footer-link:hover {
  background-color: rgba(0, 0, 0, 0.04) !important;
  color: rgba(0, 0, 0, 0.87) !important;
}

/* 深色主题支持 */
.v-theme--dark .footer-container {
  border-top-color: rgba(255, 255, 255, 0.12);
}

.v-theme--dark .footer-link:hover {
  background-color: rgba(255, 255, 255, 0.08) !important;
  color: rgba(255, 255, 255, 0.87) !important;
}

/* 响应式调整 */   
@media (max-width: 960px) {   
  .footer-link {
    margin: 2px !important;
  }
}
</style>
