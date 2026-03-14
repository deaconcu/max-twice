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
              <v-icon icon="mdi-cards-outline" size="14" class="ml-1 mr-0"></v-icon>
              {{ deckDetail?.cardCount || deck.cardCount || 0 }}
            </v-tab>
            <v-tab v-if="studyCards.length > 0" value="study" size="small">
              我复习的卡片
              <v-icon icon="mdi-cards-outline" size="14" class="ml-1 mr-0"></v-icon>
              {{ studyCards.length }}
            </v-tab>
            <v-tab v-if="studyCards.length > 0" value="diff" size="small">
              对比差异
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
                <div class="d-flex align-center" style="gap: 8px">
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
                  <v-tooltip text="添加卡片" location="top">
                    <template #activator="{ props: tooltipProps }">
                      <v-btn
                        v-if="isOwnDeck"
                        v-bind="tooltipProps"
                        icon="mdi-plus"
                        variant="text"
                        color="grey"
                        size="default"
                        density="comfortable"
                        @click="createNewCard"
                      ></v-btn>
                    </template>
                  </v-tooltip>
                </div>
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
                    <div class="d-flex align-start justify-space-between">
                      <div class="flex-grow-1 mr-1">
                        <!-- 问题 -->
                        <div class="question-section mb-3">
                          <div class="d-flex align-center">
                            <span class="text-caption text-primary mr-2 flex-shrink-0">问题</span>
                            <span class="text-body-1" v-html="renderMathText(card.front)"></span>
                          </div>
                        </div>

                        <!-- 答案 -->
                        <div class="answer-section">
                          <div class="d-flex align-center">
                            <span class="text-caption text-success mr-2 flex-shrink-0">答案</span>
                            <span class="text-body-1" v-html="renderMathText(card.back)"></span>
                          </div>
                        </div>
                      </div>

                      <!-- 操作按钮 -->
                      <div class="d-flex align-center" style="gap: 4px">
                        <v-tooltip text="预览" location="top">
                          <template #activator="{ props: tooltipProps }">
                            <v-btn
                              v-bind="tooltipProps"
                              icon="mdi-eye"
                              variant="text"
                              color="primary"
                              size="small"
                              @click="viewCard(card)"
                            ></v-btn>
                          </template>
                        </v-tooltip>

                        <!-- 如果是当前用户的卡片组，显示编辑和删除按钮 -->
                        <template v-if="isOwnDeck">
                          <v-tooltip text="编辑" location="top">
                            <template #activator="{ props: tooltipProps }">
                              <v-btn
                                v-bind="tooltipProps"
                                icon="mdi-pencil"
                                variant="text"
                                color="warning"
                                size="small"
                                @click="editCard(card)"
                              ></v-btn>
                            </template>
                          </v-tooltip>

                          <v-tooltip text="删除" location="top">
                            <template #activator="{ props: tooltipProps }">
                              <v-btn
                                v-bind="tooltipProps"
                                icon="mdi-delete"
                                variant="text"
                                color="error"
                                size="small"
                                @click="deleteCard(card)"
                              ></v-btn>
                            </template>
                          </v-tooltip>
                        </template>

                        <!-- 如果不是当前用户的卡片组，显示学习按钮 -->
                        <template v-else>
                          <v-tooltip text="添加到学习" location="top">
                            <template #activator="{ props: tooltipProps }">
                              <v-btn
                                v-if="!card.srsState"
                                v-bind="tooltipProps"
                                icon="mdi-plus"
                                variant="text"
                                color="success"
                                size="small"
                                @click="addCardToStudy(card)"
                              ></v-btn>
                            </template>
                          </v-tooltip>

                          <v-btn
                            v-if="card.srsState"
                            icon="mdi-check-circle"
                            variant="text"
                            color="success"
                            size="small"
                            disabled
                          ></v-btn>
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
                  <v-card-text class="pa-4 pl-8 position-relative">
                    <!-- 卡片序号角标 -->
                    <span class="card-index">{{ index + 1 }}</span>
                    <div class="d-flex align-start justify-space-between">
                      <div class="flex-grow-1 mr-1">
                        <!-- 问题 -->
                        <div class="question-section mb-3">
                          <div class="d-flex align-center">
                            <span class="text-caption text-primary mr-2 flex-shrink-0">问题</span>
                            <span class="text-body-1" v-html="renderMathText(card.front)"></span>
                          </div>
                        </div>

                        <!-- 答案 -->
                        <div class="answer-section">
                          <div class="d-flex align-center">
                            <span class="text-caption text-success mr-2 flex-shrink-0">答案</span>
                            <span class="text-body-1" v-html="renderMathText(card.back)"></span>
                          </div>
                        </div>

                        <!-- 学习进度信息 -->
                        <div v-if="card.srsState" class="mt-3 d-flex align-center flex-wrap text-caption text-grey">
                          <span v-if="card.srsState.repetitions >= 3" class="text-success mr-3">已掌握</span>
                          <span v-else class="mr-3">学习{{ card.srsState.repetitions }}次</span>
                          <span>下次复习：{{ new Date(card.srsState.reviewDueAt).toLocaleDateString() }}</span>
                        </div>
                      </div>

                      <!-- 操作按钮 -->
                      <div class="d-flex align-start mr-1" style="gap: 4px">
                        <v-tooltip text="预览" location="top">
                          <template #activator="{ props: tooltipProps }">
                            <v-btn
                              v-bind="tooltipProps"
                              icon="mdi-eye"
                              variant="text"
                              color="primary"
                              size="small"
                              @click="viewCard(card)"
                            ></v-btn>
                          </template>
                        </v-tooltip>

                        <v-tooltip text="移除学习" location="top">
                          <template #activator="{ props: tooltipProps }">
                            <v-btn
                              v-bind="tooltipProps"
                              icon="mdi-delete"
                              variant="text"
                              color="error"
                              size="small"
                              @click="confirmRemoveFromStudy(card)"
                            ></v-btn>
                          </template>
                        </v-tooltip>
                      </div>
                    </div>
                  </v-card-text>
                </v-card>
              </div>
            </v-window-item>

            <!-- 对比差异 Tab -->
            <v-window-item value="diff">
              <div
                v-if="addedDiffs.length === 0 && learnedCards.length === 0"
                class="text-center pa-8"
              >
                <v-icon icon="mdi-check-circle" size="64" color="success" class="mb-4"></v-icon>
                <h4 class="text-h6 text-success mb-2">没有差异</h4>
                <p class="text-body-2 text-grey-darken-1">卡片组与您的学习记录完全同步</p>
              </div>

              <div v-else>
                <!-- 对比摘要 - 作为Tab导航 -->
                <div class="mb-4">
                  <div class="d-flex align-center justify-space-between py-2">
                    <div class="d-flex align-center">
                      <div
                        class="diff-tab-item-inline"
                        :class="{ 'diff-tab-active': diffTab === 'added' }"
                        @click="diffTab = 'added'"
                      >
                        <span class="diff-tab-label">未学习的卡片</span>
                        <v-chip size="x-small" color="grey-darken-2" variant="tonal" class="ml-1">{{ addedDiffs.length }}</v-chip>
                      </div>
                      <div
                        class="diff-tab-item-inline"
                        :class="{ 'diff-tab-active': diffTab === 'learned' }"
                        @click="diffTab = 'learned'"
                      >
                        <span class="diff-tab-label">正在学习的卡片</span>
                        <v-chip size="x-small" color="grey-darken-2" variant="tonal" class="ml-1">{{ learnedCards.length }}</v-chip>
                      </div>
                    </div>
                    <div class="text-body-2 text-grey-lighten-1">
                      <span v-if="diffTab === 'added'">这些是卡片组中您还未学习的卡片，可以选择添加到学习计划</span>
                      <span v-else>这些是您在该节点下正在学习的所有卡片。标有"有更新"的卡片内容已发生变化。</span>
                    </div>
                  </div>
                </div>

                <!-- Tab内容 -->
                <v-window v-model="diffTab">
                  <!-- 未学习的卡片Tab -->
                  <v-window-item value="added">
                    <div class="cards-container">
                      <div v-if="addedDiffs.length === 0" class="text-center pa-8">
                        <h4 class="text-h6 text-grey-darken-1 mb-2">没有未学习的卡片</h4>
                        <p class="text-body-2 text-grey-darken-1">您已学习了卡片组中的所有卡片</p>
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
                          <v-card-text class="pa-4 pl-8 position-relative">
                            <!-- 卡片序号角标 -->
                            <span class="card-index">{{ index + 1 }}</span>
                            <div class="d-flex align-start justify-space-between">
                              <div class="flex-grow-1 mr-4">
                                <!-- 问题 -->
                                <div class="question-section mb-3">
                                  <div class="d-flex align-center">
                                    <span class="text-caption text-primary mr-2 flex-shrink-0">问题</span>
                                    <span class="text-body-1" v-html="renderMathText(diff.newVersion.front)"></span>
                                  </div>
                                </div>

                                <!-- 答案 -->
                                <div class="answer-section">
                                  <div class="d-flex align-center">
                                    <span class="text-caption text-success mr-2 flex-shrink-0">答案</span>
                                    <span class="text-body-1" v-html="renderMathText(diff.newVersion.back)"></span>
                                  </div>
                                </div>
                              </div>

                              <!-- 操作按钮 -->
                              <div
                                class="d-flex flex-column align-start"
                                style="gap: 12px; min-width: 100px"
                              >
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

                  <!-- 正在学习的卡片Tab -->
                  <v-window-item value="learned">
                    <div class="cards-container">
                      <div v-if="learnedCards.length === 0" class="text-center pa-8">
                        <v-icon
                          icon="mdi-school-outline"
                          size="64"
                          color="grey-lighten-2"
                          class="mb-4"
                        ></v-icon>
                        <h4 class="text-h6 text-grey-darken-1 mb-2">没有正在学习的卡片</h4>
                        <p class="text-body-2 text-grey-darken-1">您还没有开始学习该节点的卡片</p>
                      </div>

                      <template v-else>
                        <v-card
                          v-for="(card, index) in learnedCards"
                          :key="'learned-' + card.id"
                          class="mb-4 card-item"
                          rounded="lg"
                          elevation="0"
                          variant="outlined"
                        >
                          <v-card-text class="pa-4 pl-8 position-relative">
                            <!-- 卡片序号角标 -->
                            <span class="card-index">{{ index + 1 }}</span>

                            <div class="d-flex align-start justify-space-between">
                              <div class="flex-grow-1 mr-4">
                                <!-- 问题 -->
                                <div class="question-section mb-3">
                                  <div class="d-flex align-center">
                                    <span class="text-caption text-primary mr-2 flex-shrink-0">问题</span>
                                    <span class="text-body-1" v-html="renderMathText(card.front)"></span>
                                  </div>
                                </div>

                                <!-- 答案 -->
                                <div class="answer-section">
                                  <div class="d-flex align-center">
                                    <span class="text-caption text-success mr-2 flex-shrink-0">答案</span>
                                    <span class="text-body-1" v-html="renderMathText(card.back)"></span>
                                  </div>
                                </div>

                                <!-- 状态标签和学习进度 -->
                                <div class="mt-3 d-flex align-center flex-wrap text-caption text-grey" style="gap: 8px">
                                  <v-chip
                                    v-if="card.hasUpdate"
                                    size="x-small"
                                    color="warning"
                                    variant="flat"
                                    class="cursor-pointer"
                                    @click="toggleCardExpand(card.id)"
                                  >
                                    有更新
                                    <v-icon size="12" class="ml-1">{{ expandedCardIds.has(card.id) ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
                                  </v-chip>
                                  <span v-if="card.isDeleted" class="text-error">已被删除</span>
                                  <span v-else-if="card.isFromOtherDeck" class="text-grey">来自其它卡片组</span>
                                  <template v-if="card.srsState">
                                    <span v-if="card.srsState.repetitions >= 3" class="text-success">已掌握</span>
                                    <span v-else>学习{{ card.srsState.repetitions }}次</span>
                                    <span>下次复习：{{ new Date(card.srsState.reviewDueAt).toLocaleDateString() }}</span>
                                  </template>
                                </div>

                                <!-- 新版本内容（展开时显示） -->
                                <div v-if="card.hasUpdate && card.deckVersion && expandedCardIds.has(card.id)" class="mt-4 pa-3 bg-grey-lighten-5 rounded-lg">
                                  <div class="text-caption text-warning font-weight-bold mb-2">新版本内容</div>
                                  <div class="question-section mb-2">
                                    <div class="d-flex align-center">
                                      <span class="text-caption text-primary mr-2 flex-shrink-0">问题</span>
                                      <span class="text-body-2" v-html="renderMathText(card.deckVersion.front)"></span>
                                    </div>
                                  </div>
                                  <div class="answer-section">
                                    <div class="d-flex align-center">
                                      <span class="text-caption text-success mr-2 flex-shrink-0">答案</span>
                                      <span class="text-body-2" v-html="renderMathText(card.deckVersion.back)"></span>
                                    </div>
                                  </div>
                                </div>
                              </div>

                              <!-- 操作按钮 -->
                              <div class="d-flex align-center" style="gap: 4px">
                                <v-btn
                                  v-if="card.hasUpdate"
                                  color="warning"
                                  variant="tonal"
                                  size="small"
                                  rounded="lg"
                                  @click="acceptUpdate(card.id)"
                                >
                                  更新
                                </v-btn>
                                <v-btn
                                  icon="mdi-delete"
                                  variant="text"
                                  color="error"
                                  size="small"
                                  @click="confirmRemoveFromStudy(card)"
                                ></v-btn>
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
          v-if="currentTab === 'diff'"
          class="d-flex align-center"
          style="gap: 12px"
        >
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            prepend-icon="mdi-sync"
            @click="fullSyncToDeck"
          >
            完全同步为当前卡片组
          </v-btn>
          <v-btn
            v-if="learnedCardsWithUpdateCount > 0 || learnedCardsDeletedCount > 0 || addedDiffs.length > 0"
            color="grey-darken-2"
            variant="tonal"
            rounded="lg"
            prepend-icon="mdi-update"
            @click="syncUpdatesOnly"
          >
            同步卡片组更新 ({{ learnedCardsWithUpdateCount + learnedCardsDeletedCount + addedDiffs.length }})
          </v-btn>
          <v-btn
            v-if="addedDiffs.length > 0"
            color="grey-darken-2"
            variant="tonal"
            rounded="lg"
            prepend-icon="mdi-plus"
            @click="addAllNewCards"
          >
            只添加未学习卡片 ({{ addedDiffs.length }})
          </v-btn>
        </div>

        <!-- 其他标签页的提示信息 -->
        <div v-else class="text-body-2 text-grey-darken-1">
          <v-icon icon="mdi-information" size="16" class="mr-1"></v-icon>
          <span v-if="studyCards.length === 0">点击"学习卡片组"将所有卡片加入您的学习计划</span>
          <span v-else>点击"对比差异"查看更新并选择要学习的卡片</span>
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
            对比差异
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
            对比差异
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
                  <p class="text-h6 text-grey-darken-3" v-html="renderMathText(selectedCard.front)"></p>
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
                  <p class="text-h6 text-grey-darken-3" v-html="renderMathText(selectedCard.back)"></p>
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
        <div class="px-4 py-2 border-b">
          <div class="d-flex align-center justify-space-between">
            <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-3">
              {{ editingCard ? '编辑卡片' : '新建卡片' }}
            </h3>
            <v-btn
              icon="mdi-close"
              variant="text"
              size="small"
              color="grey-darken-1"
              @click="showEditDialog = false"
            ></v-btn>
          </div>
        </div>

        <v-card-text class="pa-6">
          <v-form v-model="editCardFormValid">
            <div class="mb-6">
              <label class="text-caption text-primary mb-2 d-block">
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
              <label class="text-caption text-success mb-2 d-block">
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
          <v-btn variant="text" rounded="lg" @click="showEditDialog = false">取消</v-btn>
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

    <!-- 移除学习确认对话框 -->
    <v-dialog v-model="showRemoveConfirmDialog" width="400">
      <v-card rounded="xl" elevation="0">
        <div class="px-4 py-2 border-b">
          <div class="d-flex align-center justify-space-between">
            <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-3">移除学习</h3>
            <v-btn
              icon="mdi-close"
              variant="text"
              size="small"
              color="grey-darken-1"
              @click="showRemoveConfirmDialog = false"
            ></v-btn>
          </div>
        </div>

        <v-card-text class="pa-6">
          <p class="text-body-1 text-grey-darken-2">
            确定要将此卡片从复习计划中完全移除吗？
          </p>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="text" rounded="lg" @click="showRemoveConfirmDialog = false">取消</v-btn>
          <v-btn
            color="error"
            variant="flat"
            rounded="lg"
            :loading="removingFromStudy"
            @click="removeFromStudy"
          >
            确认移除
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick } from 'vue'
import katex from 'katex'
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
  courseId?: number // 当前浏览的课程ID
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

// 移除学习确认
const showRemoveConfirmDialog = ref(false)
const cardToRemove = ref<any>(null)

// 展开的卡片ID集合（用于显示新版本内容）
const expandedCardIds = ref<Set<number>>(new Set())

const toggleCardExpand = (cardId: number) => {
  if (expandedCardIds.value.has(cardId)) {
    expandedCardIds.value.delete(cardId)
  } else {
    expandedCardIds.value.add(cardId)
  }
  // 触发响应式更新
  expandedCardIds.value = new Set(expandedCardIds.value)
}

const currentUserId = computed(() => userStore.currentUser?.id)
const isOwnDeck = computed(() => props.deck?.creator?.id === currentUserId.value)

// 渲染数学公式
const renderMathText = (text: string): string => {
  if (!text) return ''

  // 处理块级公式 $$...$$ 和 \[...\]
  let result = text.replace(/\$\$([\s\S]*?)\$\$/g, (_, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: true, throwOnError: false })
    } catch {
      return `$$${formula}$$`
    }
  })

  result = result.replace(/\\\[([\s\S]*?)\\\]/g, (_, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: true, throwOnError: false })
    } catch {
      return `\\[${formula}\\]`
    }
  })

  // 处理行内公式 $...$ 和 \(...\)
  result = result.replace(/\$([^\$\n]+?)\$/g, (_, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: false, throwOnError: false })
    } catch {
      return `$${formula}$`
    }
  })

  result = result.replace(/\\\(([\s\S]*?)\\\)/g, (_, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: false, throwOnError: false })
    } catch {
      return `\\(${formula}\\)`
    }
  })

  return result
}

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

// 前端计算差异 - 未学习的卡片（deck中有但用户没学习的）
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

// 前端计算 - 正在学习的卡片（用户在该node下学习的所有卡片，并标记是否有更新）
const learnedCards = computed(() => {
  if (!studyCards.value) return []
  if (!deckDetail.value?.cards || !props.deck) {
    // 如果没有deck数据，直接返回studyCards，都标记为来自其他卡片组
    return studyCards.value.map((card) => ({
      ...card,
      hasUpdate: false,
      isDeleted: false,
      isFromOtherDeck: true,
      deckVersion: null,
    }))
  }

  const deckCardsMap = new Map(deckDetail.value.cards.map((card) => [card.id, card]))
  const currentDeckId = props.deck.id

  return studyCards.value.map((studyCard) => {
    const deckCard = deckCardsMap.get(studyCard.id)
    const isFromCurrentDeck = studyCard.deckId === currentDeckId
    const isDeleted = isFromCurrentDeck && !deckCard // 是当前deck的卡片，但deck中已找不到
    const isFromOtherDeck = !isFromCurrentDeck // 不是当前deck的卡片
    const hasUpdate = deckCard && (deckCard.front !== studyCard.front || deckCard.back !== studyCard.back)

    return {
      ...studyCard,
      hasUpdate,
      isDeleted,
      isFromOtherDeck,
      deckVersion: deckCard ? { front: deckCard.front, back: deckCard.back } : null,
    }
  })
})

// 已学习卡片中有更新的数量
const learnedCardsWithUpdateCount = computed(() =>
  learnedCards.value.filter((card) => card.hasUpdate).length
)

// 已学习卡片中已被删除的数量
const learnedCardsDeletedCount = computed(() =>
  learnedCards.value.filter((card) => card.isDeleted).length
)

// 需要同步的总数量（有更新 + 已被删除 + 未学习）
const syncTotalCount = computed(() =>
  learnedCardsWithUpdateCount.value + learnedCardsDeletedCount.value + addedDiffs.value.length
)

// 自动选择第一个有数据的tab
const getFirstAvailableTab = (): string => {
  if (addedDiffs.value.length > 0) return 'added'
  if (learnedCards.value.length > 0) return 'learned'
  return 'added'
}

// 监听数据变化，自动切换到第一个有数据的tab
watch(
  [addedDiffs, learnedCards],
  () => {
    const currentTabHasData =
      (diffTab.value === 'added' && addedDiffs.value.length > 0) ||
      (diffTab.value === 'learned' && learnedCards.value.length > 0)

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
  ({ cardId, data }: { cardId: number; data: { front: string; back: string } }) =>
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
  ({ deckId, cardIds, courseId }: { deckId: number; cardIds: number[]; courseId?: number }) =>
    memoryApi.acceptDeckChanges(deckId, cardIds, courseId),
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
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: [cardId], courseId: props.courseId })
}

// 添加单个卡片到学习（diff界面用）
const addCardToStudyFromDiff = async (cardId: number) => {
  if (!props.deck) return
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: [cardId], courseId: props.courseId })
}

// 接受所有更新（原有方法，保留用于单个卡片更新）
const acceptAllChanges = async () => {
  if (!props.deck) return
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: [], courseId: props.courseId })
}

// 完全同步为当前卡片组（添加未学习 + 更新已修改 + 删除已删除 + 删除来自其他卡片组）
const fullSyncToDeck = async () => {
  if (!props.deck) return
  // 先删除来自其他卡片组的卡片
  const otherDeckCardIds = learnedCards.value
    .filter((card) => card.isFromOtherDeck)
    .map((card) => card.id)
  if (otherDeckCardIds.length > 0) {
    await removeCardsFromStudyMutation(otherDeckCardIds)
  }
  // 再同步当前卡片组
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: [], courseId: props.courseId })
}

// 同步卡片组更新（更新已修改 + 删除已删除，不添加未学习，不删除其他卡片组）
const syncUpdatesOnly = async () => {
  if (!props.deck) return
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: [], courseId: props.courseId })
}

// 添加所有新卡片
const addAllNewCards = async () => {
  if (!props.deck) return
  const addedCardIds = addedDiffs.value.map((diff) => diff.cardId)
  await acceptUpdateMutation({ deckId: props.deck.id, cardIds: addedCardIds, courseId: props.courseId })
}

// 使用 useMutation 处理移除学习
const { execute: removeCardsFromStudyMutation, loading: removingFromStudy } = useMutation(
  (cardIds: number[]) => memoryApi.removeCardsFromStudy(cardIds),
  {
    successMessage: '移除成功',
    onSuccess: () => {
      showRemoveConfirmDialog.value = false
      cardToRemove.value = null
      refreshDeckDetail()
    },
  }
)

// 确认移除学习
const confirmRemoveFromStudy = (card: any) => {
  cardToRemove.value = card
  showRemoveConfirmDialog.value = true
}

// 执行移除学习
const removeFromStudy = async () => {
  if (!cardToRemove.value) return
  await removeCardsFromStudyMutation([cardToRemove.value.id])
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

.cursor-pointer {
  cursor: pointer;
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

/* 差异对比tab样式 - 行内布局 */
.diff-tab-item-inline {
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;
  padding: 6px 12px;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
}

.diff-tab-item-inline:hover {
  background: rgba(0, 0, 0, 0.04);
}

.diff-tab-item-inline.diff-tab-active {
  background: rgba(var(--v-theme-primary), 0.08);
}

.diff-tab-item-inline .diff-tab-label {
  font-size: 0.875rem;
  color: rgba(0, 0, 0, 0.7);
}

.diff-tab-item-inline .diff-tab-number {
  font-size: 0.875rem;
  font-weight: 600;
}

.diff-tab-badge-inline {
  background-color: rgb(var(--v-theme-warning));
  color: white;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 5px;
  border-radius: 8px;
  min-width: 16px;
  text-align: center;
}

/* 差异对比tab样式 - 原有样式保留 */
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

.diff-tab-item {
  position: relative;
}

.diff-tab-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background-color: rgb(var(--v-theme-warning));
  color: white;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 4px;
  border-radius: 8px;
  min-width: 16px;
  text-align: center;
}

/* 有更新的卡片样式 */
.card-has-update {
  border-color: rgb(var(--v-theme-warning)) !important;
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
