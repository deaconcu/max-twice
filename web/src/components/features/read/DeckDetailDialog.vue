<template>
  <v-dialog v-model="dialog" width="900" persistent scrollable>
    <v-card
      v-if="deck"
      rounded="xl"
      elevation="0"
      class="no-border"
      style="max-height: 85vh; display: flex; flex-direction: column"
    >
      <!-- 头部 -->
      <div class="header-section px-4 py-2">
        <div class="d-flex align-center">
          <v-icon icon="mdi-cards-outline" size="22" color="primary" class="mr-2 flex-shrink-0"></v-icon>
          <h2 class="text-subtitle-1 font-weight-bold text-grey-darken-3 flex-shrink-0">
            {{ deck.course?.name }} - {{ deck.node?.name }}
          </h2>

          <v-spacer />

          <!-- Tab 导航 -->
          <v-tabs v-model="currentTab" color="primary" density="compact" class="header-tabs flex-shrink-0">
            <v-tab value="all" size="small">
              当前卡片组
              <v-icon icon="mdi-cards-outline" size="14" class="ml-2 mr-1"></v-icon>
              {{ deckDetail?.cardCount || deck.cardCount || 0 }}
            </v-tab>
            <v-tab v-if="studyCards.length > 0" value="study" size="small">
              我的学习
              <v-icon icon="mdi-cards-outline" size="14" class="ml-2 mr-1"></v-icon>
              {{ studyCards.length }}
            </v-tab>
            <v-tab v-if="studyCards.length > 0" value="diff" size="small">
              更新差异
            </v-tab>
          </v-tabs>

          <v-btn
            icon="mdi-close"
            variant="text"
            color="grey-darken-1"
            size="small"
            class="ml-2 flex-shrink-0"
            @click="closeDialog"
          ></v-btn>
        </div>
      </div>

      <!-- 卡片列表 - 可滚动区域 -->
      <div class="flex-grow-1" style="overflow-y: auto">
        <div class="px-6 pt-4 pb-6">
          <!-- Tab 内容 -->
          <v-window v-model="currentTab">
            <!-- 所有卡片 Tab -->
            <v-window-item value="all">
              <!-- 卡片组信息 -->
              <div class="deck-info mb-4 d-flex align-center justify-space-between">
                <div>
                  <div class="text-caption text-grey">
                    <router-link
                      v-if="deck.creator?.id"
                      :to="`/users/${deck.creator.id}`"
                      class="text-grey-darken-1"
                      @click.stop
                    >
                      {{ deck.creator?.name || '匿名用户' }}
                    </router-link>
                    <span v-else>{{ deck.creator?.name || '匿名用户' }}</span>
                    <span v-if="deck.updatedAt" class="ml-2">{{ formatRelativeTime(deck.updatedAt) }}</span>
                  </div>
                  <div v-if="deck.description" class="text-body-2 text-grey-darken-2 mt-1">
                    {{ deck.description }}
                  </div>
                </div>
                <v-btn
                  variant="text"
                  :color="deck.hasLiked ? 'error' : 'grey'"
                  size="default"
                  rounded="lg"
                  :prepend-icon="deck.hasLiked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
                  :disabled="isOwnDeck"
                  @click="handleUpvote"
                >
                  {{ deck.likeCount || 0 }}
                </v-btn>
              </div>

              <!-- 加载状态 -->
              <div v-if="loading" class="text-center pa-8">
                <v-progress-circular indeterminate color="primary" size="40"></v-progress-circular>
                <p class="text-body-1 text-grey-darken-1 mt-4">加载中...</p>
              </div>

              <!-- 卡片列表 -->
              <div v-else-if="deckDetail && deckDetail.cards" class="cards-container">
                <v-card
                  v-for="(card, index) in deckDetail.cards"
                  :key="card.id"
                  class="mb-4 card-item"
                  rounded="lg"
                  elevation="0"
                  variant="outlined"
                >
                  <v-card-text class="pa-4 pl-8 position-relative">
                    <!-- 卡片序号角标 -->
                    <span class="card-index">{{ index + 1 }}</span>
                    <div class="d-flex align-center justify-space-between">
                      <div class="flex-grow-1 mr-4">
                        <!-- 问题 -->
                        <div class="question-section mb-3">
                          <div class="d-flex align-center">
                            <span class="text-caption text-primary mr-2 flex-shrink-0">问题</span>
                            <span class="text-body-1">{{ card.front }}</span>
                          </div>
                        </div>

                        <!-- 答案 -->
                        <div class="answer-section">
                          <div class="d-flex align-center">
                            <span class="text-caption text-success mr-2 flex-shrink-0">答案</span>
                            <span class="text-body-1">{{ card.back }}</span>
                          </div>
                        </div>
                      </div>

                      <!-- 操作按钮 -->
                      <div
                        class="d-flex flex-column align-center"
                        style="gap: 12px; min-width: 100px"
                      >
                        <v-btn
                          color="primary"
                          variant="tonal"
                          size="small"
                          rounded="lg"
                          prepend-icon="mdi-eye"
                          @click="viewCard(card)"
                        >
                          预览
                        </v-btn>

                        <!-- 如果是当前用户的卡片组，显示编辑和删除按钮 -->
                        <template v-if="isOwnDeck">
                          <v-btn
                            color="warning"
                            variant="tonal"
                            size="small"
                            rounded="lg"
                            prepend-icon="mdi-pencil"
                            @click="editCard(card)"
                          >
                            编辑
                          </v-btn>

                          <v-btn
                            color="error"
                            variant="tonal"
                            size="small"
                            rounded="lg"
                            prepend-icon="mdi-delete"
                            @click="deleteCard(card)"
                          >
                            删除
                          </v-btn>
                        </template>

                        <!-- 如果不是当前用户的卡片组，显示学习按钮 -->
                        <template v-else>
                          <v-btn
                            v-if="!card.srsState"
                            color="success"
                            variant="tonal"
                            size="small"
                            rounded="lg"
                            prepend-icon="mdi-plus"
                            @click="addCardToStudy(card)"
                          >
                            学习
                          </v-btn>

                          <v-btn
                            v-else
                            color="success"
                            variant="tonal"
                            size="small"
                            rounded="lg"
                            disabled
                          >
                            <v-icon icon="mdi-check-circle" size="16" class="mr-1"></v-icon>
                            已添加
                          </v-btn>
                        </template>
                      </div>
                    </div>
                  </v-card-text>
                </v-card>
              </div>

              <!-- 加载失败 -->
              <div v-else class="text-center pa-8">
                <v-icon
                  icon="mdi-alert-circle-outline"
                  size="64"
                  color="grey-lighten-2"
                  class="mb-4"
                ></v-icon>
                <h4 class="text-h6 text-grey-darken-1 mb-2">加载失败</h4>
                <p class="text-body-2 text-grey-darken-1">请检查网络连接后重试</p>
              </div>
            </v-window-item>

            <!-- 我的学习卡片 Tab -->
            <v-window-item value="study">
              <div v-if="loading" class="text-center pa-8">
                <v-progress-circular indeterminate color="primary" size="40"></v-progress-circular>
                <p class="text-body-1 text-grey-darken-1 mt-4">加载中...</p>
              </div>

              <div v-else-if="studyCards.length === 0" class="text-center pa-8">
                <v-icon
                  icon="mdi-school-outline"
                  size="64"
                  color="grey-lighten-2"
                  class="mb-4"
                ></v-icon>
                <h4 class="text-h6 text-grey-darken-1 mb-2">暂无学习卡片</h4>
                <p class="text-body-2 text-grey-darken-1">您还没有开始学习此卡片组中的任何卡片</p>
              </div>

              <div v-else class="cards-container">
                <v-card
                  v-for="(card, index) in studyCards"
                  :key="card.id"
                  class="mb-4 card-item"
                  rounded="lg"
                  elevation="0"
                  variant="outlined"
                >
                  <v-card-text class="pa-5">
                    <div class="d-flex align-start justify-space-between">
                      <div class="flex-grow-1 mr-4">
                        <!-- 卡片标题和状态 -->
                        <div class="d-flex align-center justify-space-between mb-3">
                          <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                            学习卡片 {{ index + 1 }}
                          </h4>
                          <div class="d-flex align-center" style="gap: 8px">
                            <v-chip
                              v-if="card.srsState"
                              size="small"
                              :color="card.srsState.repetitions >= 3 ? 'success' : 'warning'"
                              variant="flat"
                              prepend-icon="mdi-trophy"
                            >
                              {{
                                card.srsState.repetitions >= 3
                                  ? '已掌握'
                                  : `学习${card.srsState.repetitions}次`
                              }}
                            </v-chip>
                          </div>
                        </div>

                        <!-- 问题 -->
                        <div class="question-section mb-4">
                          <div class="d-flex align-center mb-2">
                            <v-icon
                              icon="mdi-help-circle"
                              color="primary"
                              size="20"
                              class="mr-2"
                            ></v-icon>
                            <span class="text-subtitle-2 font-weight-bold text-primary">问题</span>
                          </div>
                          <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                            <p class="text-body-1 mb-0">{{ card.front }}</p>
                          </div>
                        </div>

                        <!-- 答案 -->
                        <div class="answer-section">
                          <div class="d-flex align-center mb-2">
                            <v-icon
                              icon="mdi-lightbulb"
                              color="success"
                              size="20"
                              class="mr-2"
                            ></v-icon>
                            <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                          </div>
                          <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                            <p class="text-body-1 mb-0">{{ card.back }}</p>
                          </div>
                        </div>

                        <!-- 学习进度信息 -->
                        <div v-if="card.srsState" class="mt-4">
                          <div
                            class="d-flex align-center justify-space-between text-body-2 text-grey-darken-2"
                          >
                            <span>复习次数：{{ card.srsState.repetitions }}次</span>
                            <span>难度系数：{{ card.srsState.easeFactor }}</span>
                            <span
                              >下次复习：{{
                                new Date(card.srsState.reviewDueAt).toLocaleDateString()
                              }}</span
                            >
                          </div>
                        </div>
                      </div>

                      <!-- 操作按钮 -->
                      <div
                        class="d-flex flex-column align-center"
                        style="gap: 12px; min-width: 100px"
                      >
                        <v-btn
                          color="primary"
                          variant="tonal"
                          size="small"
                          rounded="lg"
                          prepend-icon="mdi-eye"
                          @click="viewCard(card)"
                        >
                          预览
                        </v-btn>
                      </div>
                    </div>
                  </v-card-text>
                </v-card>
              </div>
            </v-window-item>

            <!-- 更新差异 Tab -->
            <v-window-item value="diff">
              <div
                v-if="
                  addedDiffs.length === 0 && modifiedDiffs.length === 0 && nodeOnlyCardsCount === 0
                "
                class="text-center pa-8"
              >
                <v-icon icon="mdi-check-circle" size="64" color="success" class="mb-4"></v-icon>
                <h4 class="text-h6 text-success mb-2">没有差异</h4>
                <p class="text-body-2 text-grey-darken-1">卡片组与您的学习记录完全同步</p>
              </div>

              <div v-else>
                <!-- 更新摘要 - 作为Tab导航 -->
                <div class="mb-4">
                  <v-card flat color="blue-lighten-5" rounded="lg" class="py-0 px-4">
                    <div class="d-flex align-center justify-space-between">
                      <div class="d-flex align-center">
                        <v-icon color="blue-darken-2" size="20" class="mr-2">mdi-compare</v-icon>
                        <h4 class="text-body-1 text-blue-darken-2 mb-0">
                          差异对比 - 检测到
                          {{ addedDiffs.length + modifiedDiffs.length + nodeOnlyCardsCount }} 项差异
                        </h4>
                      </div>
                      <div class="d-flex align-center justify-end">
                        <div
                          class="diff-tab-item"
                          :class="{ 'diff-tab-active': diffTab === 'modified' }"
                          @click="diffTab = 'modified'"
                        >
                          <div class="diff-tab-number text-warning">{{ modifiedDiffs.length }}</div>
                          <div class="diff-tab-label">内容有更新</div>
                        </div>
                        <div
                          class="diff-tab-item"
                          :class="{ 'diff-tab-active': diffTab === 'added' }"
                          @click="diffTab = 'added'"
                        >
                          <div class="diff-tab-number text-success">{{ addedDiffs.length }}</div>
                          <div class="diff-tab-label">可添加</div>
                        </div>
                        <div
                          class="diff-tab-item"
                          :class="{ 'diff-tab-active': diffTab === 'nodeOnly' }"
                          @click="diffTab = 'nodeOnly'"
                        >
                          <div class="diff-tab-number text-info">{{ nodeOnlyCardsCount }}</div>
                          <div class="diff-tab-label">其他来源</div>
                        </div>
                      </div>
                    </div>
                  </v-card>
                </div>

                <!-- Tab内容 -->
                <v-window v-model="diffTab">
                  <!-- 内容有更新Tab -->
                  <v-window-item value="modified">
                    <div class="cards-container">
                      <!-- 解释说明 -->
                      <div class="mb-4 pa-3">
                        <div class="d-flex align-center">
                          <v-icon
                            icon="mdi-information"
                            size="16"
                            color="grey-lighten-1"
                            class="mr-2"
                          ></v-icon>
                          <span class="text-body-2 text-grey-lighten-1">
                            这些卡片的内容在卡片组中已更新，与您学习记录中的版本不同。您可以选择接受更新以获取最新内容。
                          </span>
                        </div>
                      </div>

                      <div v-if="modifiedDiffs.length === 0" class="text-center pa-8">
                        <v-icon
                          icon="mdi-check-circle"
                          size="64"
                          color="success"
                          class="mb-4"
                        ></v-icon>
                        <h4 class="text-h6 text-success mb-2">没有修改</h4>
                        <p class="text-body-2 text-grey-darken-1">当前没有修改的卡片</p>
                      </div>

                      <template v-else>
                        <v-card
                          v-for="(diff, index) in modifiedDiffs"
                          :key="'modified-' + diff.cardId"
                          class="mb-4 card-item"
                          rounded="lg"
                          elevation="0"
                          variant="outlined"
                        >
                          <v-card-text class="pa-5">
                            <div class="d-flex align-start justify-space-between">
                              <div class="flex-grow-1 mr-4">
                                <!-- 卡片标题 -->
                                <div class="d-flex align-center justify-space-between mb-3">
                                  <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                                    卡片 {{ index + 1 }}
                                  </h4>
                                  <v-chip
                                    size="small"
                                    color="warning"
                                    variant="flat"
                                    prepend-icon="mdi-pencil"
                                  >
                                    内容有更新
                                  </v-chip>
                                </div>

                                <!-- 新版本 -->
                                <div class="version-section mb-4">
                                  <div class="d-flex align-center mb-2">
                                    <v-chip size="small" color="success" variant="flat" class="mr-2"
                                      >新版本</v-chip
                                    >
                                    <span class="text-caption text-grey-darken-1"
                                      >卡片组最新内容</span
                                    >
                                  </div>

                                  <div class="question-section mb-3">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon
                                        icon="mdi-help-circle"
                                        color="primary"
                                        size="20"
                                        class="mr-2"
                                      ></v-icon>
                                      <span class="text-subtitle-2 font-weight-bold text-primary"
                                        >问题</span
                                      >
                                    </div>
                                    <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                                      <p class="text-body-1 mb-0">{{ diff.newVersion.front }}</p>
                                    </div>
                                  </div>

                                  <div class="answer-section">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon
                                        icon="mdi-lightbulb"
                                        color="success"
                                        size="20"
                                        class="mr-2"
                                      ></v-icon>
                                      <span class="text-subtitle-2 font-weight-bold text-success"
                                        >答案</span
                                      >
                                    </div>
                                    <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                                      <p class="text-body-1 mb-0">{{ diff.newVersion.back }}</p>
                                    </div>
                                  </div>
                                </div>

                                <!-- 旧版本 -->
                                <div class="version-section">
                                  <div class="d-flex align-center mb-2">
                                    <v-chip size="small" color="grey" variant="flat" class="mr-2"
                                      >学习版本</v-chip
                                    >
                                    <span class="text-caption text-grey-darken-1"
                                      >您当前学习的内容</span
                                    >
                                  </div>

                                  <div class="question-section mb-3">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon
                                        icon="mdi-help-circle"
                                        color="primary"
                                        size="20"
                                        class="mr-2"
                                      ></v-icon>
                                      <span class="text-subtitle-2 font-weight-bold text-primary"
                                        >问题</span
                                      >
                                    </div>
                                    <div class="question-content pa-3 bg-grey-lighten-4 rounded-lg">
                                      <p class="text-body-1 mb-0 text-grey-darken-2">
                                        {{ diff.oldVersion.front }}
                                      </p>
                                    </div>
                                  </div>

                                  <div class="answer-section">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon
                                        icon="mdi-lightbulb"
                                        color="success"
                                        size="20"
                                        class="mr-2"
                                      ></v-icon>
                                      <span class="text-subtitle-2 font-weight-bold text-success"
                                        >答案</span
                                      >
                                    </div>
                                    <div class="answer-content pa-3 bg-grey-lighten-4 rounded-lg">
                                      <p class="text-body-1 mb-0 text-grey-darken-2">
                                        {{ diff.oldVersion.back }}
                                      </p>
                                    </div>
                                  </div>
                                </div>
                              </div>

                              <!-- 操作按钮 -->
                              <div
                                class="d-flex flex-column align-center"
                                style="gap: 12px; min-width: 100px"
                              >
                                <v-btn
                                  color="primary"
                                  variant="tonal"
                                  size="small"
                                  rounded="lg"
                                  prepend-icon="mdi-eye"
                                  @click="viewCard({ ...diff.newVersion, id: diff.cardId })"
                                >
                                  预览
                                </v-btn>

                                <v-btn
                                  color="warning"
                                  variant="tonal"
                                  size="small"
                                  rounded="lg"
                                  prepend-icon="mdi-check"
                                  @click="acceptUpdate(diff.cardId)"
                                >
                                  接受修改
                                </v-btn>
                              </div>
                            </div>
                          </v-card-text>
                        </v-card>
                      </template>
                    </div>
                  </v-window-item>

                  <!-- 可添加Tab -->
                  <v-window-item value="added">
                    <div class="cards-container">
                      <!-- 解释说明 -->
                      <div class="mb-4 pa-3">
                        <div class="d-flex align-center">
                          <v-icon
                            icon="mdi-information"
                            size="16"
                            color="grey-lighten-1"
                            class="mr-2"
                          ></v-icon>
                          <span class="text-body-2 text-grey-lighten-1">
                            这些是卡片组中新增的卡片，您还未开始学习。可以选择添加到您的学习计划中。
                          </span>
                        </div>
                      </div>

                      <div v-if="addedDiffs.length === 0" class="text-center pa-8">
                        <v-icon
                          icon="mdi-plus-circle-outline"
                          size="64"
                          color="grey-lighten-2"
                          class="mb-4"
                        ></v-icon>
                        <h4 class="text-h6 text-grey-darken-1 mb-2">没有可添加的卡片</h4>
                        <p class="text-body-2 text-grey-darken-1">当前没有新的卡片可以添加</p>
                      </div>

                      <template v-else>
                        <v-card
                          v-for="(diff, index) in addedDiffs"
                          :key="'added-' + diff.cardId"
                          class="mb-4 card-item"
                          rounded="lg"
                          elevation="0"
                          variant="outlined"
                        >
                          <v-card-text class="pa-5">
                            <div class="d-flex align-start justify-space-between">
                              <div class="flex-grow-1 mr-4">
                                <!-- 卡片标题 -->
                                <div class="d-flex align-center justify-space-between mb-3">
                                  <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                                    卡片 {{ index + 1 }}
                                  </h4>
                                  <v-chip
                                    size="small"
                                    color="success"
                                    variant="flat"
                                    prepend-icon="mdi-plus"
                                  >
                                    未学习
                                  </v-chip>
                                </div>

                                <!-- 问题 -->
                                <div class="question-section mb-4">
                                  <div class="d-flex align-center mb-2">
                                    <v-icon
                                      icon="mdi-help-circle"
                                      color="primary"
                                      size="20"
                                      class="mr-2"
                                    ></v-icon>
                                    <span class="text-subtitle-2 font-weight-bold text-primary"
                                      >问题</span
                                    >
                                  </div>
                                  <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                                    <p class="text-body-1 mb-0">{{ diff.newVersion.front }}</p>
                                  </div>
                                </div>

                                <!-- 答案 -->
                                <div class="answer-section">
                                  <div class="d-flex align-center mb-2">
                                    <v-icon
                                      icon="mdi-lightbulb"
                                      color="success"
                                      size="20"
                                      class="mr-2"
                                    ></v-icon>
                                    <span class="text-subtitle-2 font-weight-bold text-success"
                                      >答案</span
                                    >
                                  </div>
                                  <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                                    <p class="text-body-1 mb-0">{{ diff.newVersion.back }}</p>
                                  </div>
                                </div>
                              </div>

                              <!-- 操作按钮 -->
                              <div
                                class="d-flex flex-column align-center"
                                style="gap: 12px; min-width: 100px"
                              >
                                <v-btn
                                  color="primary"
                                  variant="tonal"
                                  size="small"
                                  rounded="lg"
                                  prepend-icon="mdi-eye"
                                  @click="viewCard({ ...diff.newVersion, id: diff.cardId })"
                                >
                                  预览
                                </v-btn>

                                <v-btn
                                  color="success"
                                  variant="tonal"
                                  size="small"
                                  rounded="lg"
                                  prepend-icon="mdi-plus"
                                  @click="addCardToStudyFromDiff(diff.cardId)"
                                >
                                  添加学习
                                </v-btn>
                              </div>
                            </div>
                          </v-card-text>
                        </v-card>
                      </template>
                    </div>
                  </v-window-item>

                  <!-- 其他来源Tab -->
                  <v-window-item value="nodeOnly">
                    <div class="cards-container">
                      <!-- 解释说明 -->
                      <div class="mb-4 pa-3">
                        <div class="d-flex align-center">
                          <v-icon
                            icon="mdi-information"
                            size="16"
                            color="grey-lighten-1"
                            class="mr-2"
                          ></v-icon>
                          <span class="text-body-2 text-grey-lighten-1">
                            这些卡片来自其他卡片组，或者是已经被删除的卡片。如果不需要，可以选择移除学习。
                          </span>
                        </div>
                      </div>

                      <div v-if="nodeOnlyCards.length === 0" class="text-center pa-8">
                        <v-icon
                          icon="mdi-bookmark-outline"
                          size="64"
                          color="grey-lighten-2"
                          class="mb-4"
                        ></v-icon>
                        <h4 class="text-h6 text-grey-darken-1 mb-2">没有其他来源的卡片</h4>
                        <p class="text-body-2 text-grey-darken-1">当前没有来自其他来源的卡片</p>
                      </div>

                      <template v-else>
                        <v-card
                          v-for="(card, index) in nodeOnlyCards"
                          :key="'node-only-' + card.id"
                          class="mb-4 card-item"
                          rounded="lg"
                          elevation="0"
                          variant="outlined"
                        >
                          <v-card-text class="pa-5">
                            <div class="d-flex align-start justify-space-between">
                              <div class="flex-grow-1 mr-4">
                                <!-- 卡片标题 -->
                                <div class="d-flex align-center justify-space-between mb-3">
                                  <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                                    卡片 {{ index + 1 }}
                                  </h4>
                                  <v-chip
                                    size="small"
                                    color="info"
                                    variant="flat"
                                    prepend-icon="mdi-bookmark"
                                  >
                                    其他来源
                                  </v-chip>
                                </div>

                                <!-- 问题 -->
                                <div class="question-section mb-4">
                                  <div class="d-flex align-center mb-2">
                                    <v-icon
                                      icon="mdi-help-circle"
                                      color="primary"
                                      size="20"
                                      class="mr-2"
                                    ></v-icon>
                                    <span class="text-subtitle-2 font-weight-bold text-primary"
                                      >问题</span
                                    >
                                  </div>
                                  <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                                    <p class="text-body-1 mb-0">{{ card.front }}</p>
                                  </div>
                                </div>

                                <!-- 答案 -->
                                <div class="answer-section mb-4">
                                  <div class="d-flex align-center mb-2">
                                    <v-icon
                                      icon="mdi-lightbulb"
                                      color="success"
                                      size="20"
                                      class="mr-2"
                                    ></v-icon>
                                    <span class="text-subtitle-2 font-weight-bold text-success"
                                      >答案</span
                                    >
                                  </div>
                                  <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                                    <p class="text-body-1 mb-0">{{ card.back }}</p>
                                  </div>
                                </div>

                                <!-- 学习进度信息 -->
                                <div v-if="card.srsState" class="mt-4">
                                  <div
                                    class="d-flex align-center justify-space-between text-body-2 text-grey-darken-2"
                                  >
                                    <span>复习次数：{{ card.srsState.repetitions }}次</span>
                                    <span>难度系数：{{ card.srsState.easeFactor }}</span>
                                    <span
                                      >下次复习：{{
                                        new Date(card.srsState.reviewDueAt).toLocaleDateString()
                                      }}</span
                                    >
                                  </div>
                                </div>
                              </div>

                              <!-- 操作按钮 -->
                              <div
                                class="d-flex flex-column align-center"
                                style="gap: 12px; min-width: 100px"
                              >
                                <v-btn
                                  color="primary"
                                  variant="tonal"
                                  size="small"
                                  rounded="lg"
                                  prepend-icon="mdi-eye"
                                  @click="viewCard(card)"
                                >
                                  预览
                                </v-btn>
                              </div>
                            </div>
                          </v-card-text>
                        </v-card>
                      </template>
                    </div>
                  </v-window-item>
                </v-window>
              </div>
            </v-window-item>
          </v-window>
        </div>
      </div>

      <!-- 底部固定操作栏 -->
      <div
        class="bottom-actions pa-6 bg-grey-lighten-5 d-flex align-center justify-space-between"
        style="flex-shrink: 0"
      >
        <!-- Diff标签页的操作按钮 -->
        <div
          v-if="
            currentTab === 'diff' &&
            (addedDiffs.length > 0 || modifiedDiffs.length > 0 || nodeOnlyCardsCount > 0)
          "
          class="d-flex align-center"
          style="gap: 12px"
        >
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            prepend-icon="mdi-sync"
            @click="acceptAllChanges"
          >
            同步所有更新
          </v-btn>
          <v-btn
            color="success"
            variant="outlined"
            rounded="lg"
            prepend-icon="mdi-plus"
            @click="addAllNewCards"
          >
            添加所有新卡片
          </v-btn>
        </div>

        <!-- 其他标签页的提示信息 -->
        <div v-else class="text-body-2 text-grey-darken-1">
          <v-icon icon="mdi-information" size="16" class="mr-1"></v-icon>
          <span v-if="studyCards.length === 0">点击"学习卡片组"将所有卡片加入您的学习计划</span>
          <span v-else>点击"学习卡片组"查看更新差异并选择要学习的卡片</span>
        </div>

        <div class="d-flex" style="gap: 12px">
          <v-btn variant="text" rounded="lg" @click="closeDialog">关闭</v-btn>

          <v-btn
            v-if="deckDetail && currentTab === 'study'"
            color="primary"
            variant="flat"
            rounded="lg"
            prepend-icon="mdi-compare"
            @click="goToDiffTab"
          >
            查看对比更新
          </v-btn>

          <v-btn
            v-else-if="deckDetail && currentTab === 'all' && studyCards.length === 0"
            color="primary"
            variant="flat"
            rounded="lg"
            prepend-icon="mdi-playlist-plus"
            @click="addToStudy"
          >
            学习卡片组
          </v-btn>

          <v-btn
            v-else-if="deckDetail && currentTab === 'all' && studyCards.length > 0"
            color="primary"
            variant="flat"
            rounded="lg"
            prepend-icon="mdi-compare"
            @click="goToDiffTab"
          >
            学习卡片组
          </v-btn>
        </div>
      </div>
    </v-card>

    <!-- 卡片预览对话框 -->
    <v-dialog v-model="showCardPreview" width="600">
      <v-card v-if="selectedCard" rounded="xl" elevation="0">
        <div class="preview-header px-4 py-2 border-b">
          <div class="d-flex align-center justify-space-between">
            <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-3">卡片预览</h3>
            <v-btn
              icon="mdi-close"
              variant="text"
              size="small"
              color="grey-darken-1"
              @click="showCardPreview = false"
            ></v-btn>
          </div>
        </div>

        <v-card-text class="pa-8">
          <!-- 卡片容器 -->
          <div class="card-container" @click="flipCard">
            <div class="card" :class="{ flipped: isFlipped }">
              <!-- 正面（问题） -->
              <div class="card-face card-front">
                <div class="d-flex align-center justify-center mb-4">
                  <v-icon icon="mdi-help-circle" color="primary" size="32"></v-icon>
                </div>
                <div class="text-center">
                  <h4 class="text-h6 font-weight-bold text-primary mb-4">问题</h4>
                  <p class="text-h6 text-grey-darken-3">{{ selectedCard.front }}</p>
                </div>
                <div class="text-center mt-6">
                  <v-chip size="small" color="primary" variant="outlined">
                    <v-icon icon="mdi-gesture-tap" size="16" class="mr-1"></v-icon>
                    点击翻转查看答案
                  </v-chip>
                </div>
              </div>

              <!-- 反面（答案） -->
              <div class="card-face card-back">
                <div class="d-flex align-center justify-center mb-4">
                  <v-icon icon="mdi-lightbulb" color="success" size="32"></v-icon>
                </div>
                <div class="text-center">
                  <h4 class="text-h6 font-weight-bold text-success mb-4">答案</h4>
                  <p class="text-h6 text-grey-darken-3">{{ selectedCard.back }}</p>
                </div>
                <div class="text-center mt-6">
                  <v-chip size="small" color="success" variant="outlined">
                    <v-icon icon="mdi-gesture-tap" size="16" class="mr-1"></v-icon>
                    点击翻转查看问题
                  </v-chip>
                </div>
              </div>
            </div>
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- 编辑/新建卡片对话框 -->
    <v-dialog v-model="showEditDialog" width="600" persistent>
      <v-card rounded="xl" elevation="0">
        <v-card-title class="pa-6 bg-primary text-white">
          <div class="d-flex align-center justify-space-between">
            <h3 class="text-h5 font-weight-bold">
              {{ editingCard ? '编辑卡片' : '新建卡片' }}
            </h3>
            <v-btn
              icon="mdi-close"
              variant="text"
              color="white"
              @click="showEditDialog = false"
            ></v-btn>
          </div>
        </v-card-title>

        <v-card-text class="pa-6">
          <v-form v-model="editCardFormValid">
            <div class="mb-6">
              <label class="text-subtitle-2 font-weight-bold text-grey-darken-3 mb-2 d-block">
                <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                问题 (卡片正面)
              </label>
              <v-textarea
                v-model="editCardFront"
                variant="outlined"
                rounded="lg"
                placeholder="请输入问题内容..."
                rows="3"
                :rules="frontRules"
                :counter="frontMaxLength"
              ></v-textarea>
            </div>

            <div class="mb-4">
              <label class="text-subtitle-2 font-weight-bold text-grey-darken-3 mb-2 d-block">
                <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                答案 (卡片背面)
              </label>
              <v-textarea
                v-model="editCardBack"
                variant="outlined"
                rounded="lg"
                placeholder="请输入答案内容..."
                rows="4"
                :rules="backRules"
                :counter="backMaxLength"
              ></v-textarea>
            </div>
          </v-form>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="outlined" rounded="lg" @click="showEditDialog = false">取消</v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            :loading="updatingCard || creatingCard"
            :disabled="!editCardFormValid || !editCardFront.trim() || !editCardBack.trim()"
            @click="saveCard"
          >
            {{ editingCard ? '保存修改' : '创建卡片' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick } from 'vue'
import renderMathInElement from 'katex/contrib/auto-render'
import 'katex/dist/katex.min.css'
import { memoryApi } from '@/api'
import { useFetch, useMutation } from '@/composables'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { useUserStore } from '@/stores'
import { formatRelativeTime } from '@/utils/format'
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { MemoryCardDeck } from '@/types/memory'

interface Props {
  deck: MemoryCardDeck | null
}

type Emits = (e: 'addToStudy', deck: MemoryCardDeck) => void

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const dialog = defineModel<boolean>({ default: false })
const userStore = useUserStore()

// 验证规则
const frontRules = useValidationRules('card-front')
const backRules = useValidationRules('card-back')
const frontMaxLength = useMaxLength('card-front')
const backMaxLength = useMaxLength('card-back')

// Tab相关状态
const currentTab = ref('all') // 'all' | 'study' | 'diff'
const diffTab = ref('modified') // diff页面内的子tab: 'modified' | 'added' | 'nodeOnly'

// 卡片数据
const deckDetail = ref<any>(null)
const studyCards = ref<any[]>([]) // 用户学习的卡片

// 卡片预览
const selectedCard = ref<any>(null)
const showCardPreview = ref(false)
const isFlipped = ref(false)

// 卡片编辑
const showEditDialog = ref(false)
const editingCard = ref<any>(null)
const editCardFront = ref('')
const editCardBack = ref('')
const editCardFormValid = ref(true)

const currentUserId = computed(() => userStore.currentUser?.id)
const isOwnDeck = computed(() => props.deck?.creator?.id === currentUserId.value)

// 使用 useFetch 加载卡片组详情
const {
  data: deckDetailData,
  loading,
  execute: refreshDeckDetail,
} = useFetch({
  fetchFn: () => (props.deck ? memoryApi.getDeckDetail(props.deck.id) : Promise.reject('No deck')),
  immediate: false,
  onSuccess: async (data) => {
    deckDetail.value = data

    // 获取用户在这个node下学习的所有卡片
    const nodeId = deckDetail.value?.nodeId
    console.log('Loading study cards for nodeId:', nodeId)
    console.log('Loading study cards for userId:', userStore.user?.id)
    if (nodeId && currentUserId.value) {
      try {
        const response = await memoryApi.getUserCardsByNode(nodeId)
        if (response?.data) {
          studyCards.value = response.data
        }
      } catch (error) {
        console.error('Failed to load study cards:', error)
        studyCards.value = []
      }
    }
  },
})

// 前端计算差异 - 新增的卡片（deck中有但用户没学习的）
const addedDiffs = computed(() => {
  if (!deckDetail.value?.cards || !studyCards.value) return []

  const studiedCardIds = new Set(studyCards.value.map((card) => card.id))
  return deckDetail.value.cards
    .filter((card) => !studiedCardIds.has(card.id))
    .map((card) => ({
      cardId: card.id,
      type: 'added',
      newVersion: {
        front: card.front,
        back: card.back,
      },
    }))
})

// 前端计算差异 - 修改的卡片（版本不同的）
const modifiedDiffs = computed(() => {
  if (!deckDetail.value?.cards || !studyCards.value) return []

  const studiedCardsMap = new Map(studyCards.value.map((card) => [card.id, card]))

  return deckDetail.value.cards
    .filter((deckCard) => {
      const studiedCard = studiedCardsMap.get(deckCard.id)
      // 存在学习记录且内容不同
      return (
        studiedCard && (studiedCard.front !== deckCard.front || studiedCard.back !== deckCard.back)
      )
    })
    .map((deckCard) => {
      const studiedCard = studiedCardsMap.get(deckCard.id)!
      return {
        cardId: deckCard.id,
        type: 'modified',
        newVersion: {
          front: deckCard.front,
          back: deckCard.back,
        },
        oldVersion: {
          front: studiedCard.front,
          back: studiedCard.back,
        },
      }
    })
})

// 计算学习记录独有的卡片
const nodeOnlyCards = computed(() => {
  if (!studyCards.value || !deckDetail.value?.cards) return []

  const deckCardIds = new Set(deckDetail.value.cards.map((card) => card.id))
  return studyCards.value.filter((card) => !deckCardIds.has(card.id))
})

// 计算学习记录独有的卡片数量
const nodeOnlyCardsCount = computed(() => nodeOnlyCards.value.length)

// 自动选择第一个有数据的tab
const getFirstAvailableTab = (): string => {
  if (modifiedDiffs.value.length > 0) return 'modified'
  if (addedDiffs.value.length > 0) return 'added'
  if (nodeOnlyCardsCount.value > 0) return 'nodeOnly'
  return 'modified'
}

// 监听数据变化，自动切换到第一个有数据的tab
watch(
  [modifiedDiffs, addedDiffs, nodeOnlyCards],
  () => {
    const currentTabHasData =
      (diffTab.value === 'modified' && modifiedDiffs.value.length > 0) ||
      (diffTab.value === 'added' && addedDiffs.value.length > 0) ||
      (diffTab.value === 'nodeOnly' && nodeOnlyCardsCount.value > 0)

    if (!currentTabHasData) {
      diffTab.value = getFirstAvailableTab()
    }
  },
  { immediate: true }
)

watch(
  () => props.deck,
  (newDeck) => {
    if (newDeck && dialog.value) {
      refreshDeckDetail()
    }
  }
)

watch(dialog, (newVal) => {
  if (newVal && props.deck) {
    refreshDeckDetail()
  }
})

// 使用 useMutation 处理点赞
const { execute: upvoteDeck } = useMutation((deckId: number) => memoryApi.upvoteDeck(deckId), {
  showToast: false,
  onSuccess: (result) => {
    if (props.deck && result) {
      props.deck.hasLiked = result.liked
      props.deck.likeCount = result.likeCount
    }
  },
})

// 使用 useMutation 处理添加卡片到学习
const { execute: addCardToStudyMutation } = useMutation(
  (cardId: number) => memoryApi.addCardToStudy(cardId),
  {
    successMessage: '添加成功',
    onSuccess: () => {
      refreshDeckDetail()
    },
  }
)

// 使用 useMutation 处理删除卡片
const { execute: deleteCardMutation } = useMutation(
  (cardId: number) => memoryApi.deleteCard(cardId),
  {
    successMessage: '删除成功',
    onSuccess: () => {
      refreshDeckDetail()
    },
  }
)

// 使用 useMutation 处理更新卡片
const { execute: updateCardMutation, loading: updatingCard } = useMutation(
  ({ cardId, data }: { cardId: number; data: { id: number; front: string; back: string } }) =>
    memoryApi.updateCard(cardId, data),
  {
    successMessage: '更新成功',
    onSuccess: () => {
      showEditDialog.value = false
      refreshDeckDetail()
    },
  }
)

// 使用 useMutation 处理创建卡片
const { execute: createCardMutation, loading: creatingCard } = useMutation(
  (data: { deckId: number; front: string; back: string }) => memoryApi.createCard(data),
  {
    successMessage: '创建成功',
    onSuccess: () => {
      showEditDialog.value = false
      refreshDeckDetail()
    },
  }
)

// 使用 useMutation 处理接受更新
const { execute: acceptUpdateMutation } = useMutation(
  ({ deckId, cardIds }: { deckId: number; cardIds: number[] }) =>
    memoryApi.acceptDeckChanges(deckId, cardIds),
  {
    successMessage: '更新成功',
    onSuccess: () => {
      refreshDeckDetail()
    },
  }
)

const handleUpvote = async () => {
  if (!props.deck) return
  await upvoteDeck(props.deck.id)
}

const viewCard = (card: any) => {
  selectedCard.value = card
  isFlipped.value = false
  showCardPreview.value = true
}

const flipCard = () => {
  isFlipped.value = !isFlipped.value
}

const editCard = (card: any) => {
  editingCard.value = card
  editCardFront.value = card.front
  editCardBack.value = card.back
  showEditDialog.value = true
}

const deleteCard = async (card: any) => {
  await deleteCardMutation(card.id)
}

const addCardToStudy = async (card: any) => {
  await addCardToStudyMutation(card.id)
}

const createNewCard = () => {
  if (!props.deck) return
  editingCard.value = null
  editCardFront.value = ''
  editCardBack.value = ''
  showEditDialog.value = true
}

const saveCard = async () => {
  if (!props.deck || !editCardFront.value.trim() || !editCardBack.value.trim()) return

  if (editingCard.value) {
    await updateCardMutation({
      cardId: editingCard.value.id,
      data: {
        id: editingCard.value.id,
        front: editCardFront.value,
        back: editCardBack.value,
      },
    })
  } else {
    await createCardMutation({
      deckId: props.deck.id,
      front: editCardFront.value,
      back: editCardBack.value,
    })
  }
}

const closeDialog = () => {
  dialog.value = false
}

// 跳转到更新差异标签页
const goToDiffTab = () => {
  currentTab.value = 'diff'
}

// 添加卡片组到学习
const addToStudy = () => {
  if (!props.deck) return
  emit('addToStudy', props.deck)
  dialog.value = false
}

// 接受单个卡片更新
const acceptUpdate = async (cardId: number) => {
  if (!props.deck) return
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: [cardId] })
}

// 添加单个卡片到学习（diff界面用）
const addCardToStudyFromDiff = async (cardId: number) => {
  if (!props.deck) return
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: [cardId] })
}

// 接受所有更新
const acceptAllChanges = async () => {
  if (!props.deck) return
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: [] })
}

// 添加所有新卡片
const addAllNewCards = async () => {
  if (!props.deck) return
  const addedCardIds = addedDiffs.value.map((diff) => diff.cardId)
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: addedCardIds })
}
</script>

<style scoped>
/* 移除边框 */
.no-border {
  border: none !important;
}

/* 头部样式 */
.header-section {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.header-tabs {
  flex-shrink: 0;
}

.header-tabs :deep(.v-tab) {
  min-width: auto;
  padding: 0 12px;
  font-size: 13px;
  text-transform: none;
}

.like-btn {
  cursor: pointer;
  padding: 4px 8px;
  margin: -4px -8px;
  border-radius: 6px;
  transition: background-color 0.15s ease;
}

.like-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

/* 卡片项样式 */
.card-item {
  transition: all 0.3s ease;
  border: 1px solid #e0e0e0;
  position: relative;
}

/* 卡片序号角标 */
.card-index {
  position: absolute;
  top: 0;
  left: 0;
  background: rgba(0, 0, 0, 0.06);
  color: rgba(0, 0, 0, 0.5);
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 0 0 6px 0;
}

.card-item:hover {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.02);
}

/* 问题和答案内容区域 */
.question-content,
.answer-content {
  border-left: 4px solid transparent;
  transition: all 0.2s ease;
}

.question-content {
  border-left-color: rgb(var(--v-theme-primary));
}

.answer-content {
  border-left-color: rgb(var(--v-theme-success));
}

/* 底部操作栏 */
.bottom-actions {
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

/* 差异对比tab样式 */
.diff-tab-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 0px;
  padding: 8px 12px;
  min-width: 80px;
  background: transparent;
  text-align: center;
}

.diff-tab-item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.diff-tab-active {
  background: rgba(var(--v-theme-primary), 0.08) !important;
}

.diff-tab-number {
  font-size: 1.125rem;
  font-weight: 600;
  line-height: 1.2;
  margin-bottom: 4px;
}

.diff-tab-label {
  font-size: 0.75rem;
  color: rgba(0, 0, 0, 0.7);
  font-weight: 400;
  line-height: 1;
}

/* 3D翻转卡片样式 */
.card-container {
  perspective: 1000px;
  height: 300px;
  cursor: pointer;
  overflow: hidden;
}

.card {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.6s ease-in-out;
}

.card.flipped {
  transform: rotateY(180deg);
}

.card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 2rem;
  border-radius: 16px;
  background: white;
}

.card-back {
  transform: rotateY(180deg);
}
</style>
