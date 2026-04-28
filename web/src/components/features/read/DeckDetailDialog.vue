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
          <v-icon
            icon="mdi-cards-outline"
            size="22"
            color="primary"
            class="mr-2 flex-shrink-0"
          ></v-icon>
          <h2 class="text-subtitle-1 font-weight-bold text-grey-darken-3 flex-shrink-0">
            {{ deck.node?.name || t('deckDetail.title') }}
          </h2>

          <v-spacer />

          <!-- Tab 导航 -->
          <v-tabs
            v-model="currentTab"
            color="primary"
            density="compact"
            class="header-tabs flex-shrink-0"
          >
            <v-tab value="all" size="small">
              {{ t('deckDetail.tabAll') }} ({{ deckDetail?.cardCount || deck.cardCount || 0 }})
            </v-tab>
            <v-tab value="study" size="small">
              {{ t('deckDetail.tabStudy') }} ({{ studyCards.length }})
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

      <!-- 错误提示 -->
      <Transition name="slide-down">
        <v-alert
          v-if="errorMessage"
          type="error"
          variant="tonal"
          density="compact"
          closable
          class="mx-4 mt-3 mb-0"
          rounded="lg"
          @click:close="errorMessage = ''"
        >
          {{ errorMessage }}
        </v-alert>
      </Transition>

      <!-- 课程归属提示 -->
      <v-alert
        v-if="hasCardsInOtherCourse && props.courseId"
        density="compact"
        class="mx-4 mt-3 mb-0 bg-surface-variant"
        closable
      >
        <span>
          {{ t('deckDetail.otherCourseAlert', { name: otherCourseName })
          }}<a href="javascript:void(0)" @click="moveToCurrentCourse">{{
            t('deckDetail.moveToCurrent')
          }}</a>
        </span>
      </v-alert>

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
                      {{ deck.creator?.name || t('common.anonymous') }}
                    </router-link>
                    <span v-else>{{ deck.creator?.name || t('common.anonymous') }}</span>
                    <span v-if="deck.updatedAt" class="ml-2">{{
                      formatRelativeTime(deck.updatedAt)
                    }}</span>
                  </div>
                  <div v-if="deck.description" class="text-body-2 text-grey-darken-2 mt-1">
                    {{ deck.description }}
                  </div>
                </div>
                <div class="d-flex align-center" style="gap: 8px">
                  <!-- 筛选按钮 -->
                  <v-btn
                    v-if="addedDiffs.length > 0"
                    variant="text"
                    color="grey"
                    size="small"
                    rounded="lg"
                    @click="showOnlyNotLearned = !showOnlyNotLearned"
                  >
                    {{
                      showOnlyNotLearned
                        ? t('deckDetail.filterShowAll')
                        : t('deckDetail.filterShowNotLearned', { count: addedDiffs.length })
                    }}
                  </v-btn>
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
                  <v-tooltip :text="t('deckDetail.addCard')" location="top">
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
                <p class="text-body-1 text-grey-darken-1 mt-4">{{ t('common.loading') }}</p>
              </div>

              <!-- 卡片列表 -->
              <div
                v-else-if="deckDetail && deckDetail.cards && deckDetail.cards.length > 0"
                class="cards-container"
              >
                <v-card
                  v-for="(card, index) in filteredDeckCards"
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
                            <span class="text-caption text-primary mr-2 flex-shrink-0">{{
                              t('deckDetail.question')
                            }}</span>
                            <span class="text-body-1" v-html="renderMathText(card.front)"></span>
                          </div>
                        </div>

                        <!-- 答案 -->
                        <div class="answer-section">
                          <div class="d-flex align-center">
                            <span class="text-caption text-success mr-2 flex-shrink-0">{{
                              t('deckDetail.answer')
                            }}</span>
                            <span class="text-body-1" v-html="renderMathText(card.back)"></span>
                          </div>
                        </div>
                      </div>

                      <!-- 右侧：操作按钮和状态标签 -->
                      <div
                        class="d-flex flex-column align-end justify-space-between"
                        style="align-self: stretch"
                      >
                        <!-- 操作按钮 -->
                        <div class="d-flex align-center">
                          <v-tooltip :text="t('deckDetail.preview')" location="top">
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
                            <v-tooltip :text="t('deckDetail.addToStudy')" location="top">
                              <template #activator="{ props: tooltipProps }">
                                <v-btn
                                  v-if="!isCardStudying(card.id)"
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
                              v-if="isCardStudying(card.id)"
                              icon="mdi-check-circle"
                              variant="text"
                              color="success"
                              size="small"
                              disabled
                            ></v-btn>

                            <v-tooltip :text="t('common.edit')" location="top">
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

                            <v-tooltip :text="t('common.delete')" location="top">
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
                            <v-tooltip :text="t('deckDetail.addToStudy')" location="top">
                              <template #activator="{ props: tooltipProps }">
                                <v-btn
                                  v-if="!isCardStudying(card.id)"
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
                              v-if="isCardStudying(card.id)"
                              icon="mdi-check-circle"
                              variant="text"
                              color="success"
                              size="small"
                              disabled
                            ></v-btn>
                          </template>
                        </div>

                        <!-- 状态标签 -->
                        <v-tooltip :text="t('deckDetail.notStudiedTooltip')" location="top">
                          <template #activator="{ props: tooltipProps }">
                            <v-chip
                              v-if="!isCardStudying(card.id)"
                              v-bind="tooltipProps"
                              size="x-small"
                              color="warning"
                              variant="flat"
                            >
                              {{ t('deckDetail.notStudied') }}
                            </v-chip>
                          </template>
                        </v-tooltip>
                      </div>
                    </div>
                  </v-card-text>
                </v-card>
              </div>

              <!-- 卡片组为空 -->
              <div
                v-else-if="deckDetail && deckDetail.cards && deckDetail.cards.length === 0"
                class="text-center pa-8"
              >
                <v-icon
                  icon="mdi-cards-outline"
                  size="64"
                  color="grey-lighten-2"
                  class="mb-4"
                ></v-icon>
                <h4 class="text-h6 text-grey-darken-1 mb-2">{{ t('deckDetail.noCards') }}</h4>
                <p class="text-body-2 text-grey-darken-1">{{ t('deckDetail.noCardsHint') }}</p>
                <v-btn
                  v-if="isOwnDeck"
                  color="primary"
                  variant="tonal"
                  rounded="lg"
                  prepend-icon="mdi-plus"
                  class="mt-4"
                  @click="createNewCard"
                >
                  {{ t('deckDetail.addFirstCard') }}
                </v-btn>
              </div>

              <!-- 加载失败 -->
              <div v-else class="text-center pa-8">
                <v-icon
                  icon="mdi-alert-circle-outline"
                  size="64"
                  color="grey-lighten-2"
                  class="mb-4"
                ></v-icon>
                <h4 class="text-h6 text-grey-darken-1 mb-2">{{ t('deckDetail.loadFailed') }}</h4>
                <p class="text-body-2 text-grey-darken-1">{{ t('deckDetail.loadFailedHint') }}</p>
              </div>
            </v-window-item>

            <!-- 我的学习卡片 Tab -->
            <v-window-item value="study">
              <div v-if="loading" class="text-center pa-8">
                <v-progress-circular indeterminate color="primary" size="40"></v-progress-circular>
                <p class="text-body-1 text-grey-darken-1 mt-4">{{ t('common.loading') }}</p>
              </div>

              <div v-else-if="studyCards.length === 0" class="text-center pa-8">
                <v-icon
                  icon="mdi-school-outline"
                  size="64"
                  color="grey-lighten-2"
                  class="mb-4"
                ></v-icon>
                <h4 class="text-h6 text-grey-darken-1 mb-2">{{ t('deckDetail.noStudyCards') }}</h4>
                <p class="text-body-2 text-grey-darken-1">{{ t('deckDetail.noStudyCardsHint') }}</p>
              </div>

              <div v-else>
                <!-- 筛选栏 -->
                <div v-if="learnedCardsFromOtherDeckCount > 0" class="d-flex justify-end mb-3">
                  <v-btn
                    variant="text"
                    color="grey"
                    size="small"
                    rounded="lg"
                    @click="showOnlyCurrentDeck = !showOnlyCurrentDeck"
                  >
                    {{
                      showOnlyCurrentDeck
                        ? t('deckDetail.filterShowAll')
                        : t('deckDetail.filterShowCurrentDeck')
                    }}
                  </v-btn>
                </div>

                <div class="cards-container">
                  <v-card
                    v-for="(card, index) in filteredStudyCards"
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
                              <span class="text-caption text-primary mr-2 flex-shrink-0">{{
                                t('deckDetail.question')
                              }}</span>
                              <span class="text-body-1" v-html="renderMathText(card.front)"></span>
                            </div>
                          </div>

                          <!-- 答案 -->
                          <div class="answer-section">
                            <div class="d-flex align-center">
                              <span class="text-caption text-success mr-2 flex-shrink-0">{{
                                t('deckDetail.answer')
                              }}</span>
                              <span class="text-body-1" v-html="renderMathText(card.back)"></span>
                            </div>
                          </div>

                          <!-- 学习进度 -->
                          <div
                            v-if="card.srsState"
                            class="mt-3 d-flex align-center flex-wrap text-caption text-grey"
                            style="gap: 8px"
                          >
                            <span v-if="card.srsState.repetitions >= 3" class="text-success">{{
                              t('deckDetail.mastered')
                            }}</span>
                            <span v-else>{{
                              t('deckDetail.studiedTimes', { count: card.srsState.repetitions })
                            }}</span>
                            <span
                              >{{ t('deckDetail.nextReview')
                              }}{{ new Date(card.srsState.reviewDueAt).toLocaleDateString() }}</span
                            >
                          </div>
                        </div>

                        <!-- 右侧：操作按钮和状态标签 -->
                        <div
                          class="d-flex flex-column align-end justify-space-between"
                          style="align-self: stretch"
                        >
                          <!-- 操作按钮 -->
                          <div class="d-flex align-center">
                            <!-- 有更新时显示同步按钮 -->
                            <v-tooltip
                              v-if="card.hasUpdate"
                              :text="t('deckDetail.syncUpdate')"
                              location="top"
                            >
                              <template #activator="{ props: tooltipProps }">
                                <v-btn
                                  v-bind="tooltipProps"
                                  icon="mdi-sync"
                                  variant="text"
                                  color="warning"
                                  size="small"
                                  @click="acceptUpdate(card.id)"
                                ></v-btn>
                              </template>
                            </v-tooltip>

                            <v-tooltip :text="t('deckDetail.preview')" location="top">
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

                            <v-tooltip :text="t('deckDetail.removeStudy')" location="top">
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

                          <!-- 状态标签 -->
                          <div
                            v-if="card.hasUpdate || card.isDeleted || card.isFromOtherDeck"
                            class="d-flex flex-wrap justify-end"
                            style="gap: 4px"
                          >
                            <v-tooltip :text="t('deckDetail.hasUpdateTooltip')" location="top">
                              <template #activator="{ props: tooltipProps }">
                                <v-chip
                                  v-if="card.hasUpdate"
                                  v-bind="tooltipProps"
                                  size="x-small"
                                  color="warning"
                                  variant="flat"
                                >
                                  {{ t('deckDetail.hasUpdate') }}
                                </v-chip>
                              </template>
                            </v-tooltip>
                            <v-tooltip :text="t('deckDetail.cardDeletedTooltip')" location="top">
                              <template #activator="{ props: tooltipProps }">
                                <v-chip
                                  v-if="card.isDeleted"
                                  v-bind="tooltipProps"
                                  size="x-small"
                                  color="error"
                                  variant="flat"
                                >
                                  {{ t('deckDetail.cardDeleted') }}
                                </v-chip>
                              </template>
                            </v-tooltip>
                            <v-tooltip :text="t('deckDetail.fromOtherDeckTooltip')" location="top">
                              <template #activator="{ props: tooltipProps }">
                                <v-chip
                                  v-if="card.isFromOtherDeck"
                                  v-bind="tooltipProps"
                                  size="x-small"
                                  color="grey"
                                  variant="flat"
                                >
                                  {{ t('deckDetail.fromOtherDeck') }}
                                </v-chip>
                              </template>
                            </v-tooltip>
                          </div>
                        </div>
                      </div>
                    </v-card-text>
                  </v-card>
                </div>
              </div>
            </v-window-item>
          </v-window>
        </div>
      </div>

      <!-- 底部固定操作栏 -->
      <div
        class="bottom-actions pa-6 bg-grey-lighten-5 d-flex align-center justify-end"
        style="flex-shrink: 0"
      >
        <!-- 我复习的卡片 tab 的一键操作按钮 -->
        <div
          v-if="currentTab === 'study' && hasAnyChanges"
          class="d-flex align-center"
          style="gap: 12px"
        >
          <v-tooltip :text="t('deckDetail.fullSyncTooltip')" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                color="primary"
                variant="flat"
                rounded="lg"
                prepend-icon="mdi-sync"
                @click="fullSyncToDeck"
              >
                {{ t('deckDetail.fullSyncBtn') }}
              </v-btn>
            </template>
          </v-tooltip>
          <v-tooltip :text="t('deckDetail.syncUpdatesTooltip')" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-if="syncTotalCount > 0"
                v-bind="tooltipProps"
                color="grey-darken-2"
                variant="tonal"
                rounded="lg"
                prepend-icon="mdi-update"
                @click="syncUpdatesOnly"
              >
                {{ t('deckDetail.syncUpdatesBtn', { count: syncTotalCount }) }}
              </v-btn>
            </template>
          </v-tooltip>
        </div>

        <!-- 当前卡片组 tab 有未学习卡片时的一键添加按钮 -->
        <div
          v-else-if="currentTab === 'all' && addedDiffs.length > 0"
          class="d-flex align-center"
          style="gap: 12px"
        >
          <v-tooltip :text="t('deckDetail.addAllNewTooltip')" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                color="primary"
                variant="flat"
                rounded="lg"
                prepend-icon="mdi-plus"
                @click="addAllNewCards"
              >
                {{ t('deckDetail.addAllNewBtn', { count: addedDiffs.length }) }}
              </v-btn>
            </template>
          </v-tooltip>
        </div>

        <!-- 当前卡片组 tab 的提示信息 -->
        <div
          v-else-if="currentTab === 'all'"
          class="text-body-2 text-grey-darken-1 d-flex align-center"
        >
          <v-icon icon="mdi-information" size="16" class="mr-1"></v-icon>
          <span v-if="studyCards.length === 0">{{ t('deckDetail.studyHintEmpty') }}</span>
          <span v-else>{{ t('deckDetail.studyHintAllLearned') }}</span>
        </div>

        <!-- 其他情况的空占位 -->
        <div v-else></div>
      </div>
    </v-card>

    <!-- 卡片预览对话框 -->
    <v-dialog v-model="showCardPreview" width="600">
      <v-card v-if="selectedCard" rounded="xl" elevation="0">
        <div class="preview-header px-4 py-2 border-b">
          <div class="d-flex align-center justify-space-between">
            <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-3">
              {{ t('deckDetail.cardPreviewTitle') }}
            </h3>
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
                  <h4 class="text-h6 font-weight-bold text-primary mb-4">
                    {{ t('deckDetail.question') }}
                  </h4>
                  <p
                    class="text-h6 text-grey-darken-3"
                    v-html="renderMathText(selectedCard.front)"
                  ></p>
                </div>
                <div class="text-center mt-6">
                  <v-chip size="small" color="primary" variant="outlined">
                    <v-icon icon="mdi-gesture-tap" size="16" class="mr-1"></v-icon>
                    {{ t('deckDetail.flipToAnswer') }}
                  </v-chip>
                </div>
              </div>

              <!-- 反面（答案） -->
              <div class="card-face card-back">
                <div class="d-flex align-center justify-center mb-4">
                  <v-icon icon="mdi-lightbulb" color="success" size="32"></v-icon>
                </div>
                <div class="text-center">
                  <h4 class="text-h6 font-weight-bold text-success mb-4">
                    {{ t('deckDetail.answer') }}
                  </h4>
                  <p
                    class="text-h6 text-grey-darken-3"
                    v-html="renderMathText(selectedCard.back)"
                  ></p>
                </div>
                <div class="text-center mt-6">
                  <v-chip size="small" color="success" variant="outlined">
                    <v-icon icon="mdi-gesture-tap" size="16" class="mr-1"></v-icon>
                    {{ t('deckDetail.flipToQuestion') }}
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
              {{ editingCard ? t('deckDetail.editCard') : t('deckDetail.newCard') }}
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
                {{ t('deckDetail.questionLabel') }}
              </label>
              <v-textarea
                v-model="editCardFront"
                variant="outlined"
                rounded="lg"
                :placeholder="t('deckDetail.questionPlaceholder')"
                rows="3"
                :rules="frontRules"
                :counter="frontMaxLength"
              ></v-textarea>
            </div>

            <div class="mb-4">
              <label class="text-caption text-success mb-2 d-block">
                {{ t('deckDetail.answerLabel') }}
              </label>
              <v-textarea
                v-model="editCardBack"
                variant="outlined"
                rounded="lg"
                :placeholder="t('deckDetail.answerPlaceholder')"
                rows="4"
                :rules="backRules"
                :counter="backMaxLength"
              ></v-textarea>
            </div>
          </v-form>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="text" rounded="lg" @click="showEditDialog = false">{{
            t('common.cancel')
          }}</v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            :loading="updatingCard || creatingCard"
            :disabled="!editCardFormValid || !editCardFront.trim() || !editCardBack.trim()"
            @click="saveCard"
          >
            {{ editingCard ? t('deckDetail.saveCard') : t('deckDetail.createCard') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 移除学习确认对话框 -->
    <v-dialog v-model="showRemoveConfirmDialog" width="400">
      <v-card rounded="xl" elevation="0">
        <div class="px-4 py-2 border-b">
          <div class="d-flex align-center justify-space-between">
            <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-3">
              {{ t('deckDetail.removeStudyTitle') }}
            </h3>
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
          <p class="text-body-1 text-grey-darken-2">{{ t('deckDetail.removeStudyConfirm') }}</p>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="text" rounded="lg" @click="showRemoveConfirmDialog = false">{{
            t('common.cancel')
          }}</v-btn>
          <v-btn
            color="error"
            variant="flat"
            rounded="lg"
            :loading="removingFromStudy"
            @click="removeFromStudy"
          >
            {{ t('deckDetail.removeStudyBtn') }}
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
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { useUserStore } from '@/stores'
import { formatRelativeTime } from '@/utils/format'
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { MemoryCardDeck, DeckDetail, MemoryCardView } from '@/types/memory'
import { useI18n } from '@/composables/useI18n'
import {
  useDeckDetailQuery,
  useUpvoteDeckMutation,
  useAddCardToStudyMutation,
  useDeleteCardMutation,
  useUpdateCardMutation,
  useCreateCardMutation,
  useAcceptDeckChangesMutation,
  useRemoveCardsFromStudyMutation,
  useMoveNodeToCourseMutation,
} from '@/queries/memory'

const props = defineProps<Props>()

const { t } = useI18n()

interface Props {
  deck: MemoryCardDeck | null
  courseId?: number // 当前浏览的课程ID
}

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
const deckDetail = ref<DeckDetail | null>(null)
const studyCards = ref<MemoryCardView[]>([]) // 用户学习的卡片

// 卡片预览
const selectedCard = ref<MemoryCardView | null>(null)
const showCardPreview = ref(false)
const isFlipped = ref(false)

// 错误提示
const errorMessage = ref('')
let errorTimer: ReturnType<typeof setTimeout> | null = null

const showError = (msg: string) => {
  errorMessage.value = msg
  if (errorTimer) clearTimeout(errorTimer)
  errorTimer = setTimeout(() => {
    errorMessage.value = ''
    errorTimer = null
  }, 10000)
}

// 卡片编辑
const showEditDialog = ref(false)
const editingCard = ref<any>(null)
const editCardFront = ref('')
const editCardBack = ref('')
const editCardFormValid = ref(true)

// 移除学习确认
const showRemoveConfirmDialog = ref(false)
const cardToRemove = ref<any>(null)

// 筛选状态：只看未学习的卡片
const showOnlyNotLearned = ref(false)

// 筛选状态：只看当前卡片组的卡片（用于"我复习的卡片"tab）
const showOnlyCurrentDeck = ref(false)

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

// 使用 TanStack Query 加载卡片组详情
const deckId = computed(() => props.deck?.id ?? 0)

const {
  data: deckDetailData,
  isLoading: loading,
  refetch: refreshDeckDetail,
} = useDeckDetailQuery(deckId)

// 加载 study cards（在 deckDetail 变化时触发）
watch(deckDetailData, async (data) => {
  if (!data) return
  deckDetail.value = data as DeckDetail
  const nodeId = deckDetail.value?.nodeId
  if (nodeId && currentUserId.value) {
    try {
      const cards = await memoryApi.getUserCardsByNode(nodeId)
      studyCards.value = cards ?? []
    } catch {
      studyCards.value = []
    }
  }
})

// 用户正在学习的卡片ID集合
const studiedCardIds = computed(() => {
  if (!studyCards.value) return new Set<number>()
  return new Set(studyCards.value.map((card) => card.id))
})

// 判断卡片是否正在学习
const isCardStudying = (cardId: number) => {
  return studiedCardIds.value.has(cardId)
}

// 前端计算差异 - 未学习的卡片（deck中有但用户没学习的）
const addedDiffs = computed(() => {
  if (!deckDetail.value?.cards || !studyCards.value) return []

  return deckDetail.value.cards
    .filter((card) => !studiedCardIds.value.has(card.id))
    .map((card) => ({
      cardId: card.id,
      type: 'added',
      newVersion: {
        front: card.front,
        back: card.back,
      },
    }))
})

// 过滤后的卡片列表（用于"当前卡片组"tab）
const filteredDeckCards = computed(() => {
  if (!deckDetail.value?.cards) return []
  if (!showOnlyNotLearned.value) {
    return deckDetail.value.cards
  }
  // 只显示未学习的卡片
  return deckDetail.value.cards.filter((card) => !studiedCardIds.value.has(card.id))
})

// 前端计算 - 正在学习的卡片（用户在该node下学习的所有卡片，并标记是否有更新）
// 显示全部卡片，用于"我复习的卡片"tab
const studyCardsWithStatus = computed(() => {
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
    const studyCardDeckId = studyCard.deck?.id // deckId 在 deck 对象里
    const isFromCurrentDeck = studyCardDeckId === currentDeckId
    const isDeleted = isFromCurrentDeck && !deckCard // 是当前deck的卡片，但deck中已找不到
    const isFromOtherDeck = !isFromCurrentDeck // 不是当前deck的卡片
    const hasUpdate =
      deckCard && (deckCard.front !== studyCard.front || deckCard.back !== studyCard.back)

    return {
      ...studyCard,
      hasUpdate,
      isDeleted,
      isFromOtherDeck,
      deckVersion: deckCard ? { front: deckCard.front, back: deckCard.back } : null,
    }
  })
})

// 过滤后的学习卡片列表（用于"我复习的卡片"tab）
const filteredStudyCards = computed(() => {
  if (!showOnlyCurrentDeck.value) return studyCardsWithStatus.value
  return studyCardsWithStatus.value.filter((card) => !card.isFromOtherDeck)
})

// 只显示有变化的卡片（用于计算数量）
const learnedCards = computed(() => {
  return studyCardsWithStatus.value.filter(
    (card) => card.hasUpdate || card.isDeleted || card.isFromOtherDeck
  )
})

// 已学习卡片中有更新的数量
const learnedCardsWithUpdateCount = computed(
  () => learnedCards.value.filter((card) => card.hasUpdate).length
)

// 已学习卡片中已被删除的数量
const learnedCardsDeletedCount = computed(
  () => learnedCards.value.filter((card) => card.isDeleted).length
)

// 已学习卡片中来自其他卡片组的数量
const learnedCardsFromOtherDeckCount = computed(
  () => learnedCards.value.filter((card) => card.isFromOtherDeck).length
)

// 是否有任何需要同步的变化
const hasAnyChanges = computed(
  () =>
    learnedCardsWithUpdateCount.value > 0 ||
    learnedCardsDeletedCount.value > 0 ||
    learnedCardsFromOtherDeckCount.value > 0 ||
    addedDiffs.value.length > 0
)

// 检测是否有卡片属于其他课程（通过 srsState.course.id 判断）
const cardsInOtherCourse = computed(() => {
  if (!props.courseId || !studyCards.value) return []
  return studyCards.value.filter(
    (card) => card.srsState?.course?.id && card.srsState.course.id !== props.courseId
  )
})

// 是否有卡片在其他课程学习
const hasCardsInOtherCourse = computed(() => cardsInOtherCourse.value.length > 0)

// 获取其他课程的名称（用于提示）
const otherCourseName = computed(() => {
  if (!hasCardsInOtherCourse.value) return ''
  // 假设同一节点的卡片都在同一个课程
  const firstCard = cardsInOtherCourse.value[0]
  return firstCard?.srsState?.course?.name || t('deckDetail.otherCourseName')
})

// 需要同步的总数量（有更新 + 已被删除 + 未学习）
const syncTotalCount = computed(
  () => learnedCardsWithUpdateCount.value + learnedCardsDeletedCount.value + addedDiffs.value.length
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
      void refreshDeckDetail()
    }
  }
)

watch(dialog, (newVal) => {
  if (newVal && props.deck) {
    currentTab.value = 'all'
    errorMessage.value = ''
    void refreshDeckDetail()
  }
})

// Mutations
const upvoteDeckMutation = useUpvoteDeckMutation()
const addCardToStudyMutation = useAddCardToStudyMutation()
const deleteCardMutation = useDeleteCardMutation()
const updateCardMutation = useUpdateCardMutation()
const createCardMutation = useCreateCardMutation()
const acceptDeckChangesMutation = useAcceptDeckChangesMutation()
const removeCardsFromStudyMutation = useRemoveCardsFromStudyMutation()
const moveNodeToCourseMutation = useMoveNodeToCourseMutation()

const updatingCard = updateCardMutation.isPending
const creatingCard = createCardMutation.isPending
const removingFromStudy = removeCardsFromStudyMutation.isPending
const movingToCourse = moveNodeToCourseMutation.isPending

const handleUpvote = () => {
  if (!props.deck) return
  errorMessage.value = ''
  upvoteDeckMutation.mutate(props.deck.id, {
    onSuccess: (result) => {
      if (props.deck && result) {
        props.deck.hasLiked = result.liked
        props.deck.likeCount = result.likeCount
      }
    },
  })
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

const deleteCard = (card: any) => {
  errorMessage.value = ''
  deleteCardMutation.mutate(card.id, {
    onSuccess: () => void refreshDeckDetail(),
    onError: (err) => showError(err.message),
  })
}

const addCardToStudy = (card: any) => {
  if (!props.deck) return
  errorMessage.value = ''
  acceptDeckChangesMutation.mutate(
    { deckId: props.deck.id, cardIds: [card.id], courseId: props.courseId },
    {
      onSuccess: () => void refreshDeckDetail(),
      onError: (err) => showError(err.message),
    }
  )
}

const createNewCard = () => {
  if (!props.deck) return
  editingCard.value = null
  editCardFront.value = ''
  editCardBack.value = ''
  showEditDialog.value = true
}

const saveCard = () => {
  if (!props.deck || !editCardFront.value.trim() || !editCardBack.value.trim()) return
  errorMessage.value = ''
  if (editingCard.value) {
    updateCardMutation.mutate(
      { cardId: editingCard.value.id, data: { front: editCardFront.value, back: editCardBack.value } },
      {
        onSuccess: () => {
          showEditDialog.value = false
          void refreshDeckDetail()
        },
        onError: (err) => showError(err.message),
      }
    )
  } else {
    createCardMutation.mutate(
      { deckId: props.deck.id, front: editCardFront.value, back: editCardBack.value },
      {
        onSuccess: () => {
          showEditDialog.value = false
          void refreshDeckDetail()
        },
        onError: (err) => showError(err.message),
      }
    )
  }
}

const closeDialog = () => {
  dialog.value = false
}

// 接受单个卡片更新
const acceptUpdate = (cardId: number) => {
  if (!props.deck) return
  errorMessage.value = ''
  acceptDeckChangesMutation.mutate(
    { deckId: props.deck.id, cardIds: [cardId], courseId: props.courseId },
    {
      onSuccess: () => void refreshDeckDetail(),
      onError: (err) => showError(err.message),
    }
  )
}

// 添加单个卡片到学习（diff界面用）
const addCardToStudyFromDiff = (cardId: number) => {
  if (!props.deck) return
  errorMessage.value = ''
  acceptDeckChangesMutation.mutate(
    { deckId: props.deck.id, cardIds: [cardId], courseId: props.courseId },
    {
      onSuccess: () => void refreshDeckDetail(),
      onError: (err) => showError(err.message),
    }
  )
}

// 接受所有更新
const acceptAllChanges = () => {
  if (!props.deck) return
  errorMessage.value = ''
  acceptDeckChangesMutation.mutate(
    { deckId: props.deck.id, cardIds: [], courseId: props.courseId },
    {
      onSuccess: () => void refreshDeckDetail(),
      onError: (err) => showError(err.message),
    }
  )
}

// 完全同步为当前卡片组
const fullSyncToDeck = () => {
  if (!props.deck) return
  errorMessage.value = ''
  acceptDeckChangesMutation.mutate(
    { deckId: props.deck.id, cardIds: [], courseId: props.courseId, removeOtherDeckCards: true },
    {
      onSuccess: () => void refreshDeckDetail(),
      onError: (err) => showError(err.message),
    }
  )
}

// 同步卡片组更新
const syncUpdatesOnly = () => {
  if (!props.deck) return
  errorMessage.value = ''
  acceptDeckChangesMutation.mutate(
    { deckId: props.deck.id, cardIds: [], courseId: props.courseId },
    {
      onSuccess: () => void refreshDeckDetail(),
      onError: (err) => showError(err.message),
    }
  )
}

// 只添加未学习的卡片
const addAllNewCards = () => {
  if (!props.deck) return
  errorMessage.value = ''
  const addedCardIds = addedDiffs.value.map((diff) => diff.cardId)
  acceptDeckChangesMutation.mutate(
    { deckId: props.deck.id, cardIds: addedCardIds, courseId: props.courseId },
    {
      onSuccess: () => void refreshDeckDetail(),
      onError: (err) => showError(err.message),
    }
  )
}

// 确认移除学习
const confirmRemoveFromStudy = (card: any) => {
  cardToRemove.value = card
  showRemoveConfirmDialog.value = true
}

// 执行移除学习
const removeFromStudy = () => {
  if (!cardToRemove.value) return
  errorMessage.value = ''
  removeCardsFromStudyMutation.mutate([cardToRemove.value.id], {
    onSuccess: () => {
      showRemoveConfirmDialog.value = false
      cardToRemove.value = null
      void refreshDeckDetail()
    },
    onError: (err) => showError(err.message),
  })
}

// 移动节点到当前课程
const moveToCurrentCourse = () => {
  if (!props.courseId || !deckDetail.value?.nodeId) return
  errorMessage.value = ''
  moveNodeToCourseMutation.mutate(
    { nodeId: deckDetail.value.nodeId, courseId: props.courseId },
    {
      onSuccess: () => void refreshDeckDetail(),
      onError: (err) => showError(err.message),
    }
  )
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

.diff-tab-item-inline:first-child {
  margin-left: 0;
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
/* 错误提示从上往下移入动画 */
.slide-down-enter-active {
  transition: all 0.25s ease;
}
.slide-down-leave-active {
  transition: all 0.2s ease;
}
.slide-down-enter-from {
  transform: translateY(-10px);
  opacity: 0;
}
.slide-down-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}
</style>
