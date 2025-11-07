<script setup lang="ts">
import { ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'

// 用户信息
const userInfo = ref({
  name: '用户名',
  email: 'user@example.com',
  avatar: '',
  joinDate: '2024-01-01',
  bio: '这是一段个人简介'
})

// 统计数据
const stats = ref({
  totalCourses: 12,
  completedCourses: 5,
  totalCareers: 3,
  studyDays: 45,
  studyHours: 128,
  followers: 24,
  following: 36,
  articles: 8,
  roadmaps: 3
})

// Tab 选择
const activeTab = ref('info')
</script>

<template>
  <div class="profile-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
      <!-- 页面标题 -->
      <div class="mb-6">
        <div class="d-flex align-center">
          <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-3">
            <v-icon size="32" color="#666666">mdi-account</v-icon>
          </v-avatar>
          <div>
            <h1 class="text-h4 font-weight-bold text-grey-darken-4">我的</h1>
            <p class="text-body-2 text-grey-darken-2 mt-1">管理您的个人信息和学习数据</p>
          </div>
        </div>
      </div>

      <!-- 用户卡片 -->
      <v-card border rounded="lg" class="mb-6">
        <v-card-text class="pa-6">
          <div class="d-flex align-center mb-6">
            <v-avatar size="80" color="primary" class="mr-4">
              <v-icon icon="mdi-account" size="40" color="white"></v-icon>
            </v-avatar>
            <div class="flex-grow-1">
              <h2 class="text-h5 font-weight-bold mb-1">{{ userInfo.name }}</h2>
              <p class="text-body-2 text-grey-darken-2 mb-1">{{ userInfo.email }}</p>
              <p class="text-caption text-grey">加入于 {{ userInfo.joinDate }}</p>
            </div>
            <v-btn color="primary" variant="outlined" rounded="lg">
              <v-icon icon="mdi-pencil" size="18" class="mr-2"></v-icon>
              编辑资料
            </v-btn>
          </div>

          <!-- 统计信息 -->
          <v-divider class="mb-4"></v-divider>
          <div class="d-flex justify-space-around flex-wrap" style="gap: 16px;">
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-primary mb-1">{{ stats.totalCourses }}</div>
              <div class="text-caption text-grey">学习课程</div>
            </div>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-success mb-1">{{ stats.completedCourses }}</div>
              <div class="text-caption text-grey">完成课程</div>
            </div>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-info mb-1">{{ stats.totalCareers }}</div>
              <div class="text-caption text-grey">关注职业</div>
            </div>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-warning mb-1">{{ stats.studyDays }}</div>
              <div class="text-caption text-grey">学习天数</div>
            </div>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-purple mb-1">{{ stats.studyHours }}</div>
              <div class="text-caption text-grey">学习时长(h)</div>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <!-- Tab 导航 -->
      <v-tabs v-model="activeTab" color="primary" class="mb-6">
        <v-tab value="info">
          <v-icon icon="mdi-account-circle" size="18" class="mr-2"></v-icon>
          个人信息
        </v-tab>
        <v-tab value="studying">
          <v-icon icon="mdi-school" size="18" class="mr-2"></v-icon>
          正在学习
        </v-tab>
        <v-tab value="stats">
          <v-icon icon="mdi-chart-line" size="18" class="mr-2"></v-icon>
          数据统计
        </v-tab>
        <v-tab value="courses">
          <v-icon icon="mdi-book-multiple" size="18" class="mr-2"></v-icon>
          关注的课程
        </v-tab>
        <v-tab value="people">
          <v-icon icon="mdi-account-multiple" size="18" class="mr-2"></v-icon>
          关注的人
        </v-tab>
        <v-tab value="catalogs">
          <v-icon icon="mdi-folder-multiple" size="18" class="mr-2"></v-icon>
          创建的目录
        </v-tab>
        <v-tab value="articles">
          <v-icon icon="mdi-file-document-multiple" size="18" class="mr-2"></v-icon>
          创建的文章
        </v-tab>
        <v-tab value="decks">
          <v-icon icon="mdi-cards" size="18" class="mr-2"></v-icon>
          我的卡片组
        </v-tab>
        <v-tab value="roadmaps">
          <v-icon icon="mdi-map-marker-path" size="18" class="mr-2"></v-icon>
          创建的路线图
        </v-tab>
      </v-tabs>

      <!-- Tab 内容 -->
      <v-window v-model="activeTab">
        <!-- 个人信息 -->
        <v-window-item value="info">
          <v-card border rounded="lg">
            <v-card-text class="pa-6">
              <h3 class="text-h6 font-weight-bold mb-4">个人信息</h3>
              <v-row>
                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="userInfo.name"
                    label="用户名"
                    variant="outlined"
                    rounded="lg"
                  ></v-text-field>
                </v-col>
                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="userInfo.email"
                    label="邮箱"
                    variant="outlined"
                    rounded="lg"
                  ></v-text-field>
                </v-col>
                <v-col cols="12">
                  <v-textarea
                    v-model="userInfo.bio"
                    label="个人简介"
                    variant="outlined"
                    rounded="lg"
                    rows="4"
                  ></v-textarea>
                </v-col>
              </v-row>
              <div class="mt-4">
                <v-btn color="primary" variant="flat" rounded="lg" class="mr-3">
                  <v-icon icon="mdi-content-save" size="18" class="mr-2"></v-icon>
                  保存
                </v-btn>
                <v-btn variant="outlined" rounded="lg">取消</v-btn>
              </div>
            </v-card-text>
          </v-card>
        </v-window-item>

        <!-- 正在学习 -->
        <v-window-item value="studying">
          <v-card border rounded="lg">
            <v-card-text class="pa-6">
              <div class="d-flex align-center justify-space-between mb-4">
                <h3 class="text-h6 font-weight-bold">正在学习</h3>
                <v-btn color="primary" variant="text" rounded="lg" to="/learning">
                  查看全部课程
                  <v-icon icon="mdi-chevron-right" class="ml-1"></v-icon>
                </v-btn>
              </div>
              <div class="text-center py-12">
                <v-icon icon="mdi-school" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <p class="text-body-1 text-grey-darken-2">暂无正在学习的课程</p>
                <p class="text-body-2 text-grey">开始学习新课程，掌握新技能</p>
                <v-btn color="primary" variant="flat" rounded="lg" class="mt-4" to="/learning">
                  <v-icon icon="mdi-plus" size="18" class="mr-2"></v-icon>
                  浏览课程
                </v-btn>
              </div>
            </v-card-text>
          </v-card>
        </v-window-item>

        <!-- 数据统计 -->
        <v-window-item value="stats">
          <v-card border rounded="lg">
            <v-card-text class="pa-6">
              <h3 class="text-h6 font-weight-bold mb-4">数据统计</h3>

              <v-row>
                <v-col cols="12" md="6" lg="3">
                  <v-card border rounded="lg" class="pa-4">
                    <div class="d-flex align-center mb-2">
                      <v-icon icon="mdi-book-open-outline" color="primary" size="24" class="mr-3"></v-icon>
                      <div>
                        <div class="text-h5 font-weight-bold text-primary">{{ stats.totalCourses }}</div>
                        <div class="text-caption text-grey">学习课程</div>
                      </div>
                    </div>
                  </v-card>
                </v-col>

                <v-col cols="12" md="6" lg="3">
                  <v-card border rounded="lg" class="pa-4">
                    <div class="d-flex align-center mb-2">
                      <v-icon icon="mdi-check-circle" color="success" size="24" class="mr-3"></v-icon>
                      <div>
                        <div class="text-h5 font-weight-bold text-success">{{ stats.completedCourses }}</div>
                        <div class="text-caption text-grey">完成课程</div>
                      </div>
                    </div>
                  </v-card>
                </v-col>

                <v-col cols="12" md="6" lg="3">
                  <v-card border rounded="lg" class="pa-4">
                    <div class="d-flex align-center mb-2">
                      <v-icon icon="mdi-calendar-check" color="warning" size="24" class="mr-3"></v-icon>
                      <div>
                        <div class="text-h5 font-weight-bold text-warning">{{ stats.studyDays }}</div>
                        <div class="text-caption text-grey">学习天数</div>
                      </div>
                    </div>
                  </v-card>
                </v-col>

                <v-col cols="12" md="6" lg="3">
                  <v-card border rounded="lg" class="pa-4">
                    <div class="d-flex align-center mb-2">
                      <v-icon icon="mdi-clock-outline" color="info" size="24" class="mr-3"></v-icon>
                      <div>
                        <div class="text-h5 font-weight-bold text-info">{{ stats.studyHours }}</div>
                        <div class="text-caption text-grey">学习时长(h)</div>
                      </div>
                    </div>
                  </v-card>
                </v-col>
              </v-row>

              <v-divider class="my-6"></v-divider>

              <h4 class="text-body-1 font-weight-bold mb-4">学习趋势</h4>
              <div class="text-center py-8">
                <v-icon icon="mdi-chart-areaspline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <p class="text-body-2 text-grey">学习数据统计图表</p>
              </div>
            </v-card-text>
          </v-card>
        </v-window-item>

        <!-- 我关注的课程 -->
        <v-window-item value="courses">
          <v-card border rounded="lg">
            <v-card-text class="pa-6">
              <div class="d-flex align-center justify-space-between mb-4">
                <h3 class="text-h6 font-weight-bold">我关注的课程</h3>
                <v-btn color="primary" variant="text" rounded="lg" to="/learning">
                  查看全部
                  <v-icon icon="mdi-chevron-right" class="ml-1"></v-icon>
                </v-btn>
              </div>
              <div class="text-center py-12">
                <v-icon icon="mdi-book-multiple" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <p class="text-body-1 text-grey-darken-2">暂无关注的课程</p>
                <p class="text-body-2 text-grey">关注感兴趣的课程，及时获取更新</p>
                <v-btn color="primary" variant="flat" rounded="lg" class="mt-4" to="/learning">
                  <v-icon icon="mdi-plus" size="18" class="mr-2"></v-icon>
                  浏览课程
                </v-btn>
              </div>
            </v-card-text>
          </v-card>
        </v-window-item>

        <!-- 我关注的人 -->
        <v-window-item value="people">
          <v-card border rounded="lg">
            <v-card-text class="pa-6">
              <div class="d-flex align-center justify-space-between mb-4">
                <h3 class="text-h6 font-weight-bold">我关注的人</h3>
                <div class="text-body-2 text-grey">
                  <span class="font-weight-bold text-primary">{{ stats.following }}</span> 关注 ·
                  <span class="font-weight-bold text-success">{{ stats.followers }}</span> 粉丝
                </div>
              </div>
              <div class="text-center py-12">
                <v-icon icon="mdi-account-multiple" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <p class="text-body-1 text-grey-darken-2">暂无关注的人</p>
                <p class="text-body-2 text-grey">关注优秀的创作者，获取精彩内容</p>
              </div>
            </v-card-text>
          </v-card>
        </v-window-item>

        <!-- 我创建的目录 -->
        <v-window-item value="catalogs">
          <v-card border rounded="lg">
            <v-card-text class="pa-6">
              <div class="d-flex align-center justify-space-between mb-4">
                <h3 class="text-h6 font-weight-bold">我创建的目录</h3>
                <v-btn color="primary" variant="flat" rounded="lg">
                  <v-icon icon="mdi-plus" size="18" class="mr-2"></v-icon>
                  创建目录
                </v-btn>
              </div>
              <div class="text-center py-12">
                <v-icon icon="mdi-folder-multiple" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <p class="text-body-1 text-grey-darken-2">暂无创建的目录</p>
                <p class="text-body-2 text-grey">创建目录来组织您的学习内容</p>
              </div>
            </v-card-text>
          </v-card>
        </v-window-item>

        <!-- 我创建的文章 -->
        <v-window-item value="articles">
          <v-card border rounded="lg">
            <v-card-text class="pa-6">
              <div class="d-flex align-center justify-space-between mb-4">
                <h3 class="text-h6 font-weight-bold">我创建的文章</h3>
                <v-btn color="primary" variant="flat" rounded="lg">
                  <v-icon icon="mdi-plus" size="18" class="mr-2"></v-icon>
                  创建文章
                </v-btn>
              </div>
              <div class="text-center py-12">
                <v-icon icon="mdi-file-document-multiple" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <p class="text-body-1 text-grey-darken-2">暂无创建的文章</p>
                <p class="text-body-2 text-grey">分享您的学习心得和经验</p>
              </div>
            </v-card-text>
          </v-card>
        </v-window-item>

        <!-- 我的卡片组 -->
        <v-window-item value="decks">
          <v-card border rounded="lg">
            <v-card-text class="pa-6">
              <div class="d-flex align-center justify-space-between mb-4">
                <h3 class="text-h6 font-weight-bold">我的卡片组</h3>
                <v-btn color="primary" variant="flat" rounded="lg" to="/memory-review">
                  <v-icon icon="mdi-plus" size="18" class="mr-2"></v-icon>
                  创建卡片组
                </v-btn>
              </div>
              <div class="text-center py-12">
                <v-icon icon="mdi-cards" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <p class="text-body-1 text-grey-darken-2">暂无卡片组</p>
                <p class="text-body-2 text-grey">创建记忆卡片，高效复习知识点</p>
                <v-btn color="primary" variant="outlined" rounded="lg" class="mt-4" to="/memory-review">
                  <v-icon icon="mdi-brain" size="18" class="mr-2"></v-icon>
                  前往复习中心
                </v-btn>
              </div>
            </v-card-text>
          </v-card>
        </v-window-item>

        <!-- 我创建的路线图 -->
        <v-window-item value="roadmaps">
          <v-card border rounded="lg">
            <v-card-text class="pa-6">
              <div class="d-flex align-center justify-space-between mb-4">
                <h3 class="text-h6 font-weight-bold">我创建的路线图</h3>
                <v-btn color="primary" variant="flat" rounded="lg">
                  <v-icon icon="mdi-plus" size="18" class="mr-2"></v-icon>
                  创建路线图
                </v-btn>
              </div>
              <div class="text-center py-12">
                <v-icon icon="mdi-map-marker-path" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <p class="text-body-1 text-grey-darken-2">暂无创建的路线图</p>
                <p class="text-body-2 text-grey">创建学习路线图，规划职业发展路径</p>
              </div>
            </v-card-text>
          </v-card>
        </v-window-item>
      </v-window>
    </div>

  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
  width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
}

@media (min-width: 2229px) {
  .main-content {
    margin-left: max(160px, calc((100vw - 1550px) / 2));
    padding: 80px 40px 40px 40px;
    width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
    max-width: 1550px;
  }
}

/* 移动端 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    max-width: 100%;
    padding: 80px 20px 80px 20px;
  }
}
</style>
