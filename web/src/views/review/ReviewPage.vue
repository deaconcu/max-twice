<template>
  <DefaultLayout>
    <div class="review-page">
    <!-- 页面标题 -->
    <div class="mb-4 mb-md-6">
      <div class="d-flex align-center justify-space-between mb-4">
        <div class="d-flex align-center">
          <v-avatar
            color="surface-variant"
            :size="$vuetify.display.mobile ? 48 : 64"
            rounded="lg"
            class="mr-3 flex-shrink-0"
          >
            <v-icon :size="$vuetify.display.mobile ? 24 : 32" color="grey-darken-1"
              >mdi-brain</v-icon
            >
          </v-avatar>
          <div>
            <h1 class="text-h5 text-md-h4 font-weight-bold text-grey-darken-4">
              {{ t('review.title') }}
            </h1>
            <p class="text-caption text-md-body-2 text-grey-darken-2 mt-1">
              {{
                activeTab === 'all'
                  ? t('review.subtitle')
                  : `${selectedCourse?.course.name || ''}${t('review.courseSubtitle')}`
              }}
            </p>
          </div>
        </div>

        <!-- 视图模式选择器 -->
        <div class="d-flex align-center ga-2">
          <v-btn
            :color="viewMode === 'review' ? 'primary' : 'grey'"
            :variant="viewMode === 'review' ? 'flat' : 'outlined'"
            rounded="lg"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            @click="switchViewMode('review')"
          >
            <v-icon
              icon="mdi-play"
              :size="$vuetify.display.mobile ? 14 : 16"
              class="mr-1 mr-sm-2"
            ></v-icon>
            <span class="d-none d-sm-inline">{{ t('review.modeReview') }}</span>
            <span class="d-sm-none">复习</span>
          </v-btn>

          <v-btn
            :color="viewMode === 'list' ? 'primary' : 'grey'"
            :variant="viewMode === 'list' ? 'flat' : 'outlined'"
            rounded="lg"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            @click="switchViewMode('list')"
          >
            <v-icon
              icon="mdi-format-list-bulleted"
              :size="$vuetify.display.mobile ? 14 : 16"
              class="mr-1 mr-sm-2"
            ></v-icon>
            <span class="d-none d-sm-inline">{{ t('review.modeList') }}</span>
            <span class="d-sm-none">列表</span>
          </v-btn>

          <v-btn
            v-if="selectedCourse"
            :color="viewMode === 'manage' ? 'primary' : 'grey'"
            :variant="viewMode === 'manage' ? 'flat' : 'outlined'"
            rounded="lg"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            @click="switchViewMode('manage')"
          >
            <v-icon
              icon="mdi-cog"
              :size="$vuetify.display.mobile ? 14 : 16"
              class="mr-1 mr-sm-2"
            ></v-icon>
            <span class="d-none d-sm-inline">{{ t('review.modeManage') }}</span>
            <span class="d-sm-none">管理</span>
          </v-btn>
        </div>
      </div>
    </div>

    <v-row>
      <!-- 主内容区域 -->
      <v-col cols="12" lg="9" class="order-0">
        <!-- 移动端课程分类（可展开） -->
        <v-card rounded="lg" class="mb-4 d-lg-none mobile-category-panel" elevation="0" border>
          <v-expansion-panels v-model="expansionPanel" variant="accordion">
            <v-expansion-panel value="0" elevation="0">
              <v-expansion-panel-title class="px-4 py-3">
                <div class="d-flex align-center w-100">
                  <v-avatar
                    :color="
                      activeTab === 'all'
                        ? 'primary'
                        : getCourseStatusColor(selectedCourse?.setting.state || 1)
                    "
                    size="36"
                    class="mr-3 flex-shrink-0"
                  >
                    <v-icon
                      :icon="
                        activeTab === 'all' ? 'mdi-view-dashboard' : 'mdi-book-open-page-variant'
                      "
                      color="white"
                      size="18"
                    ></v-icon>
                  </v-avatar>
                  <div class="flex-grow-1 min-w-0">
                    <div class="text-body-2 font-weight-bold text-grey-darken-4 text-truncate">
                      {{
                        activeTab === 'all'
                          ? t('review.allCourses')
                          : selectedCourse?.course.name || t('review.allCourses')
                      }}
                    </div>
                    <div class="text-caption text-grey text-truncate">
                      {{
                        activeTab === 'all'
                          ? `${t('review.dueReview')}${totalDueCards}${t('review.cards')}`
                          : `${t('review.dueReview')}${selectedCourseDueCards}${t('review.cards')}`
                      }}
                    </div>
                  </div>
                  <div class="d-flex align-center ga-2 text-body-2 ml-2">
                    <span
                      v-if="(activeTab === 'all' ? totalNewCards : selectedCourseNewCards) > 0"
                      class="text-success font-weight-bold"
                    >
                      +{{ activeTab === 'all' ? totalNewCards : selectedCourseNewCards }}
                    </span>
                    <span
                      :class="(activeTab === 'all' ? totalDueCards : selectedCourseDueCards) > 0
                        ? 'text-error font-weight-bold'
                        : 'text-grey'"
                    >
                      {{ activeTab === 'all' ? totalDueCards : selectedCourseDueCards }}
                    </span>
                  </div>
                </div>
              </v-expansion-panel-title>
              <v-expansion-panel-text class="px-2 pt-2 pb-0">
                <!-- 全部标签 -->
                <div
                  class="nav-item pa-2 pa-sm-3 rounded-lg mb-2"
                  :class="[activeTab === 'all' ? 'nav-item-active' : 'nav-item-inactive']"
                  @click="switchTab('all')"
                >
                  <div class="d-flex align-center">
                    <v-avatar
                      :color="activeTab === 'all' ? 'primary' : 'grey-lighten-2'"
                      size="32"
                      class="mr-2 mr-sm-3"
                    >
                      <v-icon
                        icon="mdi-view-dashboard"
                        :color="activeTab === 'all' ? 'white' : 'grey'"
                        size="16"
                      ></v-icon>
                    </v-avatar>
                    <div class="flex-grow-1 min-w-0">
                      <div
                        class="text-caption text-md-body-2 font-weight-bold text-truncate"
                        :class="activeTab === 'all' ? 'text-primary' : 'text-grey-darken-3'"
                      >
                        {{ t('review.allCourses') }}
                      </div>
                      <div class="text-caption text-grey text-truncate">
                        {{ t('review.dueReview') }}{{ totalDueCards }}{{ t('review.cards') }}
                      </div>
                    </div>
                    <div class="d-flex align-center ga-2 text-body-2">
                      <span v-if="totalNewCards > 0" class="text-success font-weight-bold">+{{ totalNewCards }}</span>
                      <span :class="totalDueCards > 0 ? 'text-error font-weight-bold' : 'text-grey'">{{ totalDueCards }}</span>
                    </div>
                  </div>
                </div>

                <!-- 分课程标签 -->
                <div
                  v-for="bank in courseMemoryBanks"
                  :key="bank.course.id"
                  class="nav-item pa-2 pa-sm-3 rounded-lg mb-2"
                  :class="[
                    activeTab === bank.course.id.toString()
                      ? 'nav-item-active'
                      : 'nav-item-inactive',
                  ]"
                  @click="switchTab(bank.course.id.toString())"
                >
                  <div class="d-flex align-center">
                    <div class="course-icon-container mr-2 mr-sm-3">
                      <DynamicIcon
                        :icon="bank.course.icon"
                        default-icon="mdi-book-open-variant"
                        :size="18"
                        :color="getColorByString(bank.course.name)"
                      />
                    </div>
                    <div class="flex-grow-1 min-w-0">
                      <div
                        class="text-caption text-md-body-2 font-weight-bold text-truncate"
                        :class="
                          activeTab === bank.course.id.toString()
                            ? 'text-primary'
                            : 'text-grey-darken-3'
                        "
                      >
                        {{ bank.course.name }}
                      </div>
                      <div class="text-caption text-grey text-truncate">
                        {{ getFrequencyText(bank.setting.frequencySetting) }}
                      </div>
                    </div>
                    <div class="d-flex align-center ga-2 text-body-2">
                      <span
                        :class="((bank.newCardCount || 0) + (bank.learningCount || 0) + (bank.dueCardCount || 0)) > 0 ? 'text-primary font-weight-bold' : 'text-grey'"
                      >
                        {{ (bank.newCardCount || 0) + (bank.learningCount || 0) + (bank.dueCardCount || 0) }}
                      </span>
                    </div>
                  </div>
                </div>
              </v-expansion-panel-text>
            </v-expansion-panel>
          </v-expansion-panels>
        </v-card>

        <!-- 复习模式 -->
        <div v-if="viewMode === 'review'">
          <!-- 加载状态 -->
          <LoadingSpinner v-if="reviewLoading" />

          <!-- 全部课程 - 提示选择具体课程 -->
          <div v-else-if="!isReviewing && !selectedCourse" class="text-center">
            <v-card border rounded="lg" style="padding: 200px 32px">
              <v-icon icon="mdi-book-open-page-variant" size="64" color="primary" class="mb-4"></v-icon>
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('review.selectCourseToReview') }}
              </h3>
              <p class="text-body-1 text-grey-darken-1 mb-4">
                {{ t('review.selectCourseToReviewHint') }}
              </p>
              <div class="d-flex justify-center flex-wrap ga-3 mt-4">
                <div class="text-body-2 text-grey-darken-1">
                  {{ t('review.dueReview') }}{{ totalDueCards }}{{ t('review.cards') }} ·
                  {{ t('review.newCards') }}{{ totalNewCards }}{{ t('review.cards') }}
                </div>
              </div>
            </v-card>
          </div>

          <!-- 空队列状态 -->
          <div v-else-if="!isReviewing && selectedCourse && selectedCourseDueCards === 0 && selectedCourseNewCards === 0" class="text-center">
            <v-card rounded="lg" class="pa-8" elevation="0">
              <v-icon icon="mdi-check-circle" size="64" color="success" class="mb-4"></v-icon>
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('review.excellent') }}
              </h3>
              <p class="text-body-1 text-grey-darken-1 mb-4">
                {{ `${selectedCourse.course.name}${t('review.courseNoDue')}` }}
              </p>
            </v-card>
          </div>

          <!-- 开始复习状态 -->
          <div v-else-if="!isReviewing && selectedCourse" class="text-center">
            <v-card border rounded="lg" style="padding: 200px 32px">
              <v-icon icon="mdi-cards" size="64" color="primary" class="mb-4"></v-icon>
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('review.readyToReview') }}
              </h3>
              <p class="text-body-1 text-grey-darken-1 mb-4">
                {{ selectedCourse.course.name }}{{ t('review.has') }}
                <span v-if="selectedCourseLearningCards > 0">
                  {{ t('review.learningCards') }}
                  <span class="font-weight-bold text-warning">{{ selectedCourseLearningCards }}</span>
                  {{ t('review.cards') }} ·
                </span>
                {{ t('review.dueReview') }}
                <span class="font-weight-bold text-error">{{ selectedCourse.dueCardCount || 0 }}</span>
                {{ t('review.cards') }}
                <span v-if="selectedCourseNewCards > 0">
                  · {{ t('review.newCards') }}
                  <span class="font-weight-bold text-success">{{ selectedCourseNewCards }}</span>
                  {{ t('review.cards') }}
                </span>
              </p>
              <v-btn color="primary" variant="flat" rounded="lg" size="large" @click="startReview">
                <v-icon icon="mdi-play" class="mr-2"></v-icon>
                {{ t('review.startReview') }}
              </v-btn>
            </v-card>
          </div>

          <!-- 复习中状态 -->
          <div v-else-if="currentCard">
            <!-- 进度条 -->
            <v-card rounded="lg" class="mb-0 py-1 pb-2 no-border">
              <div class="d-flex align-center justify-space-between">
                <span class="text-caption text-grey-darken-1">
                  {{ t('review.inProgress') }}
                </span>
                <span v-if="submitting" class="text-caption text-grey-darken-1">
                  {{ t('review.submitting') }}
                </span>
              </div>
            </v-card>

            <!-- 卡片区域 -->
            <v-card rounded="lg" class="mb-4">
              <div
                class="card-container pa-8 d-flex align-center justify-center"
                :style="{ minHeight: `${cardHeight}px` }"
              >
                <!-- 被屏蔽的卡片 -->
                <div v-if="isCurrentCardBlocked" class="text-center">
                  <div class="d-flex align-center justify-center mb-4">
                    <v-icon icon="mdi-alert-circle" color="error" size="64"></v-icon>
                  </div>
                  <h3 class="text-h5 font-weight-bold text-error mb-4">
                    {{ t('review.cardBlocked') }}
                  </h3>
                  <p class="text-body-1 text-grey-darken-1 mb-6">
                    {{ t('review.cardBlockedHint') }}
                  </p>
                  <div class="d-flex justify-center ga-3">
                    <v-btn
                      color="error"
                      variant="outlined"
                      rounded="lg"
                      @click="deleteBlockedCard"
                    >
                      <v-icon icon="mdi-delete" class="mr-2"></v-icon>
                      {{ t('review.deleteCard') }}
                    </v-btn>
                    <v-btn
                      v-if="currentCard.deck?.nodeId"
                      color="primary"
                      variant="flat"
                      rounded="lg"
                      :to="`/read/${currentCard.deck.nodeId}`"
                    >
                      <v-icon icon="mdi-plus" class="mr-2"></v-icon>
                      {{ t('review.findNewDeck') }}
                    </v-btn>
                  </div>
                </div>

                <!-- 问题面 -->
                <div v-else-if="!showAnswer" class="text-center">
                  <div class="d-flex align-center justify-center mb-4">
                    <v-icon icon="mdi-help-circle" color="primary" size="48"></v-icon>
                  </div>
                  <h3 class="text-h5 font-weight-bold text-primary mb-4">
                    {{ t('review.question') }}
                  </h3>

                  <div v-if="currentCard.deck" class="mb-4">
                    <v-chip size="small" color="primary" variant="outlined">
                      <v-icon icon="mdi-book-open-page-variant" size="16" class="mr-1"></v-icon>
                      {{ currentCard.deck.courseName }} - {{ currentCard.deck.nodeName }}
                    </v-chip>
                  </div>

                  <div class="question-content pa-6 mx-auto" style="max-width: 600px">
                    <p class="text-h6 text-grey-darken-3">{{ currentCard.front }}</p>
                  </div>
                  <div class="mt-8">
                    <v-btn
                      color="primary"
                      variant="flat"
                      rounded="lg"
                      size="large"
                      @click="revealAnswer"
                    >
                      <v-icon icon="mdi-eye" class="mr-2"></v-icon>
                      {{ t('review.showAnswer') }}
                    </v-btn>
                  </div>
                </div>

                <!-- 答案面 -->
                <div v-else class="text-center">
                  <div class="d-flex align-center justify-center mb-4">
                    <v-icon icon="mdi-lightbulb" color="success" size="48"></v-icon>
                  </div>
                  <h3 class="text-h5 font-weight-bold text-success mb-4">
                    {{ t('review.answer') }}
                  </h3>

                  <div v-if="currentCard.deck" class="mb-4">
                    <v-chip size="small" color="success" variant="outlined">
                      <v-icon icon="mdi-book-open-page-variant" size="16" class="mr-1"></v-icon>
                      {{ currentCard.deck.courseName }} - {{ currentCard.deck.nodeName }}
                    </v-chip>
                  </div>

                  <div class="answer-content pa-6 mx-auto" style="max-width: 600px">
                    <p class="text-h6 text-grey-darken-3">{{ currentCard.back }}</p>
                  </div>

                  <!-- 评价按钮 -->
                  <div class="mt-8">
                    <p class="text-body-1 text-grey-darken-1 mb-4">
                      {{ t('review.ratePrompt') }}
                    </p>
                    <div class="d-flex justify-center flex-wrap" style="gap: 12px">
                      <v-btn
                        color="error"
                        variant="outlined"
                        rounded="lg"
                        @click="submitReview(ReviewResult.AGAIN)"
                      >
                        <v-icon icon="mdi-close" class="mr-2"></v-icon>
                        {{ t('review.forgot') }}
                      </v-btn>
                      <v-btn
                        color="warning"
                        variant="outlined"
                        rounded="lg"
                        @click="submitReview(ReviewResult.HARD)"
                      >
                        <v-icon icon="mdi-help" class="mr-2"></v-icon>
                        {{ t('review.hard') }}
                      </v-btn>
                      <v-btn
                        color="success"
                        variant="outlined"
                        rounded="lg"
                        @click="submitReview(ReviewResult.GOOD)"
                      >
                        <v-icon icon="mdi-check" class="mr-2"></v-icon>
                        {{ t('review.good') }}
                      </v-btn>
                      <v-btn
                        color="primary"
                        variant="flat"
                        rounded="lg"
                        @click="submitReview(ReviewResult.EASY)"
                      >
                        <v-icon icon="mdi-thumb-up" class="mr-2"></v-icon>
                        {{ t('review.easy') }}
                      </v-btn>
                    </div>
                  </div>
                </div>
              </div>
            </v-card>

            <!-- 操作按钮 -->
            <div class="d-flex justify-space-between align-center">
              <v-btn variant="tonal" rounded="lg" @click="resetReview">
                <v-icon icon="mdi-stop" class="mr-2"></v-icon>
                {{ t('review.stopReview') }}
              </v-btn>

              <div class="text-caption text-grey-darken-1 d-flex align-center ga-2">
                <span v-if="selectedCourseLearningCards > 0">
                  {{ t('review.learningCards') }}
                  <span class="font-weight-bold text-warning">{{ selectedCourseLearningCards }}</span>
                  {{ t('review.cards') }}
                </span>
                <span v-if="selectedCourseLearningCards > 0 && (selectedCourse?.dueCardCount || 0) > 0">·</span>
                <span v-if="(selectedCourse?.dueCardCount || 0) > 0">
                  {{ t('review.dueReview') }}
                  <span class="font-weight-bold text-error">{{ selectedCourse?.dueCardCount || 0 }}</span>
                  {{ t('review.cards') }}
                </span>
                <span v-if="selectedCourseNewCards > 0">·</span>
                <span v-if="selectedCourseNewCards > 0">
                  {{ t('review.newCards') }}
                  <span class="font-weight-bold text-success">{{ selectedCourseNewCards }}</span>
                  {{ t('review.cards') }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 列表模式 -->
        <div v-else-if="viewMode === 'list'">
          <!-- 加载状态 -->
          <LoadingSpinner v-if="listLoading && listCards.length === 0" />

          <div v-else>
            <div
              class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between mb-4 ga-3"
            >
              <h3 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-4">
                {{ t('review.cardList') }} ({{ listCards.length }} {{ t('review.cards') }})
              </h3>

              <!-- 批量操作按钮 -->
              <div class="d-flex align-center flex-wrap ga-2">
                <v-btn
                  v-if="selectedCards.length > 0"
                  color="primary"
                  variant="tonal"
                  rounded="lg"
                  disabled
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  @click="reviewSelectedCards"
                >
                  <v-icon
                    icon="mdi-play"
                    :size="$vuetify.display.mobile ? 14 : 16"
                    :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
                  ></v-icon>
                  <span class="d-none d-sm-inline"
                    >{{ t('review.reviewSelected') }} ({{ selectedCards.length }})</span
                  >
                  <span class="d-sm-none">复习 ({{ selectedCards.length }})</span>
                </v-btn>

                <v-btn
                  v-if="selectedCards.length > 0"
                  color="warning"
                  variant="tonal"
                  rounded="lg"
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  @click="resetSelectedCards"
                >
                  <v-icon
                    icon="mdi-restart"
                    :size="$vuetify.display.mobile ? 14 : 16"
                    :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
                  ></v-icon>
                  <span class="d-none d-sm-inline">{{ t('review.resetLearning') }}</span>
                  <span class="d-sm-none">重置</span>
                </v-btn>

                <v-btn
                  v-if="selectedCards.length > 0"
                  color="error"
                  variant="tonal"
                  rounded="lg"
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  @click="deleteSelectedCards"
                >
                  <v-icon
                    icon="mdi-delete"
                    :size="$vuetify.display.mobile ? 14 : 16"
                    :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
                  ></v-icon>
                  <span class="d-none d-sm-inline">{{ t('common.delete') }}</span>
                  <span class="d-sm-none">删除</span>
                </v-btn>

                <v-btn
                  :color="
                    selectedCards.length === listCards.length && listCards.length > 0
                      ? 'primary'
                      : 'grey'
                  "
                  variant="tonal"
                  rounded="lg"
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  :disabled="listCards.length === 0"
                  @click="toggleSelectAll"
                >
                  <v-icon
                    :icon="
                      selectedCards.length === listCards.length && listCards.length > 0
                        ? 'mdi-checkbox-marked'
                        : 'mdi-checkbox-blank-outline'
                    "
                    :size="$vuetify.display.mobile ? 14 : 16"
                    :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
                  ></v-icon>
                  <span class="d-none d-sm-inline">
                    {{
                      selectedCards.length === listCards.length && listCards.length > 0
                        ? t('common.deselectAll')
                        : t('common.selectAll')
                    }}
                  </span>
                  <span class="d-sm-none">
                    {{
                      selectedCards.length === listCards.length && listCards.length > 0
                        ? '取消'
                        : '全选'
                    }}
                  </span>
                </v-btn>
              </div>
            </div>

            <div v-if="listCards.length === 0" class="text-center pa-6 pa-sm-8">
              <v-icon
                icon="mdi-format-list-bulleted"
                :size="$vuetify.display.mobile ? 48 : 64"
                color="grey-lighten-2"
                class="mb-3 mb-md-4"
              ></v-icon>
              <h3 class="text-subtitle-1 text-md-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('review.noCards') }}
              </h3>
              <p class="text-body-2 text-md-body-1 text-grey-darken-1">
                {{ t('review.noCardsInCourse') }}
              </p>
            </div>

            <div v-else>
              <div
                v-for="(card, index) in listCards"
                :key="card.id"
                class="card-item pa-3 pa-sm-4 rounded-lg mb-2"
                :class="[selectedCards.includes(card.id) ? 'card-selected' : '']"
                @click="toggleCardSelection(card.id)"
              >
                <div class="d-flex align-center">
                  <!-- 选择框 -->
                  <div :class="$vuetify.display.mobile ? 'mr-2' : 'mr-3'">
                    <v-checkbox
                      :model-value="selectedCards.includes(card.id)"
                      density="compact"
                      hide-details
                      @click.stop="toggleCardSelection(card.id)"
                    ></v-checkbox>
                  </div>

                  <!-- 序号 -->
                  <div
                    class="rank-number mr-3 mr-sm-4 text-center d-none d-sm-flex"
                    style="min-width: 40px"
                  >
                    <div class="text-body-2 text-md-body-1 font-weight-bold text-grey-darken-2">
                      {{ index + 1 }}
                    </div>
                  </div>

                  <!-- 卡片内容 -->
                  <div class="flex-grow-1 min-w-0">
                    <!-- 被屏蔽的卡片 -->
                    <template v-if="isCardBlocked(card)">
                      <div class="d-flex align-center mb-1">
                        <v-icon icon="mdi-alert-circle" color="error" size="16" class="mr-1"></v-icon>
                        <div class="text-body-2 text-md-body-1 font-weight-medium text-error">
                          {{ t('review.cardBlocked') }}
                        </div>
                      </div>
                      <div class="text-caption text-md-body-2 text-grey-darken-2 mb-1">
                        {{ t('review.cardBlockedHint') }}
                      </div>
                    </template>
                    <!-- 正常卡片 -->
                    <template v-else>
                      <div class="d-flex align-center mb-1">
                        <div class="text-body-2 text-md-body-1 font-weight-medium text-truncate">
                          {{ card.front }}
                        </div>
                        <span
                          v-if="card.deck"
                          class="text-caption text-md-body-2 text-grey ml-2 flex-shrink-0 d-none d-sm-inline"
                        >
                          - {{ card.deck.courseName }} / {{ card.deck.nodeName }}
                        </span>
                      </div>
                      <div class="text-caption text-md-body-2 text-grey-darken-2 mb-1 line-clamp-2">
                        {{ card.back }}
                      </div>
                      <!-- 到期时间 -->
                      <div v-if="card.srsState" class="text-caption text-grey-darken-1">
                        <v-icon
                          icon="mdi-clock-outline"
                          :size="$vuetify.display.mobile ? 12 : 14"
                          class="mr-1"
                        ></v-icon>
                        {{ t('review.nextReview') }}: {{ formatDueDate(card.srsState.reviewDueAt) }}
                      </div>
                    </template>
                  </div>

                  <!-- 状态标签 -->
                  <div class="d-flex align-center flex-shrink-0 ga-1 ga-sm-2">
                    <v-chip
                      v-for="(chip, idx) in getCardStatusChips(card)"
                      :key="idx"
                      :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                      :color="chip.color"
                      variant="flat"
                    >
                      {{ chip.text }}
                    </v-chip>
                  </div>
                </div>
              </div>

              <!-- 加载更多 -->
              <div v-if="listHasMore" class="text-center mt-4">
                <v-btn
                  variant="outlined"
                  rounded="lg"
                  :size="$vuetify.display.mobile ? 'default' : 'large'"
                  :loading="listLoading"
                  @click="loadMoreListCards"
                >
                  {{ t('common.loadMore') }}
                </v-btn>
              </div>
            </div>
          </div>
        </div>

        <!-- 管理模式 -->
        <div v-else-if="viewMode === 'manage'">
          <div v-if="!selectedCourse" class="text-center pa-6 pa-sm-8">
            <v-card border rounded="lg" class="pa-6 pa-sm-8">
              <v-icon
                icon="mdi-cog"
                :size="$vuetify.display.mobile ? 48 : 64"
                color="grey-lighten-2"
                class="mb-3 mb-md-4"
              ></v-icon>
              <h3 class="text-subtitle-1 text-md-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('review.selectCourse') }}
              </h3>
              <p class="text-body-2 text-md-body-1 text-grey-darken-1">
                {{ t('review.selectCourseHint') }}
              </p>
            </v-card>
          </div>

          <v-card v-else border rounded="lg" class="pa-4 pa-sm-6">
            <h3 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-3 mb-4">
              {{ selectedCourse.course.name }} - {{ t('review.reviewSettings') }}
            </h3>

            <div
              class="py-3 py-sm-4 rounded-lg mb-4"
              style="background-color: rgb(var(--v-theme-surface))"
            >
              <div class="d-flex justify-space-between align-center mb-3">
                <div>
                  <div class="text-body-1 text-grey-darken-3">
                    {{ t('review.reviewFrequency') }}
                  </div>
                  <div class="text-caption text-grey-darken-1">
                    {{ t('review.reviewFrequencyHint') }}
                  </div>
                </div>
                <v-select
                  v-model="selectedCourse.setting.frequencySetting"
                  :items="frequencyOptions"
                  variant="outlined"
                  rounded="lg"
                  hide-details
                  density="compact"
                  style="max-width: 160px"
                ></v-select>
              </div>

              <div class="d-flex justify-space-between align-center mb-3">
                <div>
                  <div class="text-body-1 text-grey-darken-3">
                    {{ t('review.learningStatus') }}
                  </div>
                  <div class="text-caption text-grey-darken-1">
                    {{ t('review.learningStatusHint') }}
                  </div>
                </div>
                <v-select
                  v-model="selectedCourse.setting.state"
                  :items="statusOptions"
                  variant="outlined"
                  rounded="lg"
                  hide-details
                  density="compact"
                  style="max-width: 160px"
                ></v-select>
              </div>

              <div class="d-flex justify-space-between align-center mb-3">
                <div>
                  <div class="text-body-1 text-grey-darken-3">
                    {{ t('review.cardOrder') }}
                  </div>
                  <div class="text-caption text-grey-darken-1">
                    {{ t('review.cardOrderHint') }}
                  </div>
                </div>
                <v-select
                  v-model="selectedCourse.setting.cardOrder"
                  :items="cardOrderOptions"
                  variant="outlined"
                  rounded="lg"
                  hide-details
                  density="compact"
                  style="max-width: 160px"
                ></v-select>
              </div>

              <div class="d-flex justify-space-between align-center mb-3">
                <div>
                  <div class="text-body-1 text-grey-darken-3">
                    {{ t('review.dailyNewLimit') }}
                  </div>
                  <div class="text-caption text-grey-darken-1">
                    {{ t('review.dailyNewLimitHint') }}
                  </div>
                </div>
                <v-text-field
                  v-model.number="settingDailyNewLimit"
                  type="number"
                  variant="outlined"
                  rounded="lg"
                  hide-details
                  density="compact"
                  :min="0"
                  :max="999"
                  style="max-width: 100px"
                ></v-text-field>
              </div>

              <div class="d-flex justify-space-between align-center">
                <div>
                  <div class="text-body-1 text-grey-darken-3">
                    {{ t('review.dailyReviewLimit') }}
                  </div>
                  <div class="text-caption text-grey-darken-1">
                    {{ t('review.dailyReviewLimitHint') }}
                  </div>
                </div>
                <v-text-field
                  v-model.number="settingDailyReviewLimit"
                  type="number"
                  variant="outlined"
                  rounded="lg"
                  hide-details
                  density="compact"
                  :min="0"
                  :max="9999"
                  style="max-width: 100px"
                ></v-text-field>
              </div>
            </div>

            <div class="d-flex ga-3">
              <v-btn color="primary" variant="flat" rounded="lg" @click="updateCourseSetting">
                {{ t('common.saveSettings') }}
              </v-btn>
            </div>
          </v-card>
        </div>
      </v-col>

      <!-- 右侧课程分类 - 仅大屏幕显示 -->
      <v-col cols="12" lg="3" class="d-none d-lg-block">
        <v-card rounded="lg" class="sticky-nav px-3 px-sm-4 no-border">
          <h3 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-4 mb-3 mb-md-4">
            <v-icon icon="mdi-chart-line" color="primary" size="18" class="mr-2"></v-icon>
            {{ t('review.courseCategory') }}
          </h3>

          <!-- 状态切换 Tab -->
          <v-tabs v-model="courseStateTab" density="compact" class="mb-3">
            <v-tab :value="1" size="small">{{ t('review.statusStudying') }}</v-tab>
            <v-tab :value="2" size="small">{{ t('review.statusFrozen') }}</v-tab>
            <v-tab :value="3" size="small">{{ t('review.statusHidden') }}</v-tab>
          </v-tabs>

          <!-- 课程列表 -->
          <div v-if="courseListLoadingComputed" class="text-center pa-4">
            <v-progress-circular indeterminate size="24"></v-progress-circular>
          </div>
          <div v-else-if="currentCourseList.length === 0" class="text-center pa-4 text-grey">
            {{ t('review.noCourses') }}
          </div>
          <div v-else>
            <div
              v-for="bank in currentCourseList"
              :key="bank.course.id"
              class="nav-item pa-2 pa-sm-3 rounded-lg mb-2"
              :class="[
                activeTab === bank.course.id.toString() ? 'nav-item-active' : 'nav-item-inactive',
              ]"
              @click="switchTab(bank.course.id.toString())"
            >
              <div class="d-flex align-center">
                <div class="course-icon-container mr-2 mr-sm-3">
                  <DynamicIcon
                    :icon="bank.course.icon"
                    default-icon="mdi-book-open-variant"
                    :size="18"
                    :color="getColorByString(bank.course.name)"
                  />
                </div>
                <div class="flex-grow-1 min-w-0">
                  <div
                    class="text-caption text-md-body-2 font-weight-bold text-truncate"
                    :class="
                      activeTab === bank.course.id.toString() ? 'text-primary' : 'text-grey-darken-3'
                    "
                  >
                    {{ bank.course.name }}
                  </div>
                  <div class="text-caption text-grey text-truncate">
                    {{ getFrequencyText(bank.setting.frequencySetting) }}
                  </div>
                </div>
                <!-- 只有学习中状态显示统计数字 -->
                <div v-if="courseStateTab === 1" class="d-flex align-center ga-2 text-body-2">
                  <span
                    :class="((bank.newCardCount || 0) + (bank.learningCount || 0) + (bank.dueCardCount || 0)) > 0 ? 'text-primary font-weight-bold' : 'text-grey'"
                  >
                    {{ (bank.newCardCount || 0) + (bank.learningCount || 0) + (bank.dueCardCount || 0) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useI18n } from '@/composables/useI18n'
import { useFetch, useMutation } from '@/composables'
import { memoryApi } from '@/api'
import type {
  MemoryCardView,
  CourseStudyStatus,
  ReviewSummary,
  CourseMemoryBank,
} from '@/types/memory'
import {
  ReviewResult,
  FrequencySetting,
  CourseStudyStatus as Status,
  CardOrder,
  DeckState,
} from '@/types/memory'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import DynamicIcon from '@/components/common/DynamicIcon.vue'
import { getColorByString } from '@/utils/color'

const { t } = useI18n()

// 状态
const activeTab = ref<string>('all')
const viewMode = ref<'review' | 'list' | 'manage'>('review')
const isReviewing = ref(false)
const showAnswer = ref(false)
const selectedCards = ref<number[]>([])
const expansionPanel = ref<number[]>([]) // 控制展开面板状态
const courseStateTab = ref<number>(Status.STUDYING) // 课程状态 tab：1=学习中，2=冻结，3=隐藏

// 当前卡片（由后端维护）
const currentCard = ref<MemoryCardView | null>(null)
const reviewLoading = ref(false)

// 动态卡片高度
const cardHeight = ref(800)

// 列表分页
const listCards = ref<MemoryCardView[]>([])
const listLoading = ref(false)
const listLastId = ref<number | undefined>(undefined)
const listHasMore = ref(true)

// 学习中的课程
const { data: studyingSummary, refresh: refreshStudying } = useFetch<ReviewSummary>({
  fetchFn: () => memoryApi.getReviewSummary(Status.STUDYING),
  immediate: true,
  defaultValue: { todayTotal: 0, todayCompleted: 0, streakDays: 0, courses: [] },
})

// 冻结的课程
const { data: frozenSummary, refresh: refreshFrozen, loading: frozenLoading } = useFetch<ReviewSummary>({
  fetchFn: () => memoryApi.getReviewSummary(Status.FROZEN),
  immediate: false,
  defaultValue: { todayTotal: 0, todayCompleted: 0, streakDays: 0, courses: [] },
})

// 隐藏的课程
const { data: hiddenSummary, refresh: refreshHidden, loading: hiddenLoading } = useFetch<ReviewSummary>({
  fetchFn: () => memoryApi.getReviewSummary(Status.HIDDEN),
  immediate: false,
  defaultValue: { todayTotal: 0, todayCompleted: 0, streakDays: 0, courses: [] },
})

// 当前状态 tab 对应的课程列表
const currentCourseList = computed(() => {
  switch (courseStateTab.value) {
    case Status.STUDYING:
      return studyingSummary.value.courses
    case Status.FROZEN:
      return frozenSummary.value.courses
    case Status.HIDDEN:
      return hiddenSummary.value.courses
    default:
      return []
  }
})

// 课程列表加载状态
const courseListLoadingComputed = computed(() => {
  return frozenLoading.value || hiddenLoading.value
})

// 从复习概览中提取课程列表（学习中的课程，兼容现有代码）
const courseMemoryBanks = computed(() => studyingSummary.value.courses)

// 刷新课程列表的方法（兼容现有代码）
const refreshCourses = refreshStudying

// 监听 courseStateTab 变化，懒加载数据
watch(courseStateTab, (newState) => {
  if (newState === Status.FROZEN && frozenSummary.value.courses.length === 0) {
    void refreshFrozen()
  } else if (newState === Status.HIDDEN && hiddenSummary.value.courses.length === 0) {
    void refreshHidden()
  }
})

// 计算属性
const totalDueCards = computed(() => {
  return courseMemoryBanks.value.reduce((sum, bank) => sum + (bank.dueCardCount || 0) + (bank.learningCount || 0), 0)
})

const totalNewCards = computed(() => {
  return courseMemoryBanks.value.reduce((sum, bank) => sum + (bank.newCardCount || 0), 0)
})

// 获取选中课程的新卡数
const selectedCourseNewCards = computed(() => {
  if (!selectedCourse.value) return 0
  return selectedCourse.value.newCardCount || 0
})

// 获取选中课程的学习中卡片数
const selectedCourseLearningCards = computed(() => {
  if (!selectedCourse.value) return 0
  return selectedCourse.value.learningCount || 0
})

// 获取选中课程的待复习数
const selectedCourseDueCards = computed(() => {
  if (!selectedCourse.value) return 0
  return (selectedCourse.value.dueCardCount || 0) + (selectedCourse.value.learningCount || 0)
})

const selectedCourse = computed(() => {
  if (activeTab.value === 'all') return null
  const courseId = parseInt(activeTab.value)
  return courseMemoryBanks.value.find((bank) => bank.course.id === courseId)
})

// 带默认值的设置 computed（用于表单绑定）
const settingDailyNewLimit = computed({
  get: () => selectedCourse.value?.setting.dailyNewLimit,
  set: (val: number) => {
    if (selectedCourse.value) {
      selectedCourse.value.setting.dailyNewLimit = val
    }
  }
})

const settingDailyReviewLimit = computed({
  get: () => selectedCourse.value?.setting.dailyReviewLimit,
  set: (val: number) => {
    if (selectedCourse.value) {
      selectedCourse.value.setting.dailyReviewLimit = val
    }
  }
})

const frequencyOptions = computed(() => [
  { title: t('review.frequencyHigh'), value: FrequencySetting.HIGH },
  { title: t('review.frequencyNormal'), value: FrequencySetting.NORMAL },
  { title: t('review.frequencyLow'), value: FrequencySetting.LOW },
])

const statusOptions = computed(() => [
  { title: t('review.statusStudying'), value: Status.STUDYING },
  { title: t('review.statusFrozen'), value: Status.FROZEN },
  { title: t('review.statusHidden'), value: Status.HIDDEN },
])

const cardOrderOptions = computed(() => [
  { title: t('review.cardOrderReviewFirst'), value: CardOrder.REVIEW_FIRST },
  { title: t('review.cardOrderNewFirst'), value: CardOrder.NEW_FIRST },
])

// 检测当前卡片是否被屏蔽（卡片或卡片组状态不是 PUBLISHED）
const isCurrentCardBlocked = computed(() => {
  if (!currentCard.value) return false
  const cardState = currentCard.value.state
  const deckState = currentCard.value.deck?.state
  // 卡片或卡片组不是 PUBLISHED 状态都视为被屏蔽
  return (cardState && cardState !== DeckState.PUBLISHED) ||
         (deckState && deckState !== DeckState.PUBLISHED)
})

// 方法
const switchTab = (tabValue: string) => {
  activeTab.value = tabValue
  expansionPanel.value = [] // 关闭展开面板

  if (viewMode.value === 'review') {
    resetReview()
    // 切换课程时重置复习状态，用户需要点击开始复习按钮
  } else if (viewMode.value === 'list') {
    listCards.value = []
    listLastId.value = undefined
    listHasMore.value = true
    void loadListCards(true)
  }
}

const switchViewMode = (mode: 'review' | 'list' | 'manage') => {
  viewMode.value = mode
  selectedCards.value = []
  if (mode === 'review') {
    resetReview()
  } else if (mode === 'list') {
    listCards.value = []
    listLastId.value = undefined
    listHasMore.value = true
    void loadListCards(true)
  }
}

const startReview = async () => {
  // 必须选择具体课程才能开始复习
  if (!selectedCourse.value) return
  const courseId = selectedCourse.value.course.id
  const courseDueCards = selectedCourseDueCards.value + selectedCourseNewCards.value
  if (courseDueCards === 0) return

  reviewLoading.value = true
  try {
    const response = await memoryApi.getNextCard({ courseId })
    if (response.code === 200 && response.data) {
      currentCard.value = response.data.nextCard
      if (currentCard.value) {
        isReviewing.value = true
        showAnswer.value = false
      }
    }
  } finally {
    reviewLoading.value = false
  }
}

const resetReview = () => {
  isReviewing.value = false
  currentCard.value = null
  showAnswer.value = false
}

const revealAnswer = () => {
  showAnswer.value = true
}

// 提交复习
const { execute: executeReview, loading: submitting } = useMutation(
  (params: { cardId: number; result: ReviewResult; courseId?: number; timeSpent?: number }) => {
    return memoryApi.reviewCard(params)
  },
  {
    showToast: false,
    onSuccess: (result) => {
      if (!result) return

      // 更新当前课程卡片统计
      if (result.courseStats && selectedCourse.value) {
        const courseId = selectedCourse.value.course.id
        const bank = studyingSummary.value.courses.find((b) => b.course.id === courseId)
        if (bank) {
          bank.newCardCount = result.courseStats.newCardCount
          bank.dueCardCount = result.courseStats.dueCardCount
          bank.learningCount = result.courseStats.learningCount
        }
      }

      // 后端返回下一张卡片
      currentCard.value = result.nextCard

      if (!currentCard.value) {
        // 无更多卡片，复习完成
        void completeReview()
      } else {
        showAnswer.value = false
      }
    },
  }
)

const submitReview = (result: ReviewResult) => {
  if (!currentCard.value || submitting.value) return

  const courseId = selectedCourse.value?.course.id
  void executeReview({
    cardId: currentCard.value.id,
    result,
    courseId,
    timeSpent: 5,
  })
}

const completeReview = async () => {
  isReviewing.value = false
  currentCard.value = null
  await refreshCourses()
}

// 列表卡片
const loadListCards = async (reset = false) => {
  if (listLoading.value) return

  listLoading.value = true
  try {
    const response = await memoryApi.getCardList({
      courseId: selectedCourse.value?.course.id,
      limit: 20,
      lastId: reset ? undefined : listLastId.value,
    })

    if (response.code === 200 && response.data) {
      if (reset) {
        listCards.value = response.data
        listLastId.value = undefined
      } else {
        listCards.value = [...listCards.value, ...response.data]
      }

      if (response.data.length > 0) {
        const lastCard = response.data[response.data.length - 1]
        if (lastCard) {
          listLastId.value = lastCard.id
        }
      }

      listHasMore.value = response.data.length === 20
    }
  } finally {
    listLoading.value = false
  }
}

const loadMoreListCards = () => {
  void loadListCards(false)
}

// 卡片选择
const toggleCardSelection = (cardId: number) => {
  const index = selectedCards.value.indexOf(cardId)
  if (index > -1) {
    selectedCards.value.splice(index, 1)
  } else {
    selectedCards.value.push(cardId)
  }
}

const toggleSelectAll = () => {
  if (selectedCards.value.length === listCards.value.length) {
    selectedCards.value = []
  } else {
    selectedCards.value = listCards.value.map((card) => card.id)
  }
}

// 批量操作
const { execute: executeDelete } = useMutation(memoryApi.deleteCards, {
  successMessage: t('review.deleteSuccess'),
  onSuccess: () => {
    listCards.value = listCards.value.filter((card) => !selectedCards.value.includes(card.id))
    selectedCards.value = []
    void refreshCourses() // 刷新课程列表以更新待复习数量
  },
})

const deleteSelectedCards = () => {
  if (selectedCards.value.length === 0) return
  void executeDelete(selectedCards.value)
}

// 获取下一张卡片
const { refresh: fetchNextCard } = useFetch({
  fetchFn: () => memoryApi.getNextCard({ courseId: selectedCourse.value?.course.id }),
  immediate: false,
  onSuccess: (result) => {
    if (result) {
      currentCard.value = result.nextCard
      if (!currentCard.value) {
        void completeReview()
      } else {
        showAnswer.value = false
      }
    }
  },
})

// 删除被屏蔽的卡片
const { execute: executeDeleteBlockedCard } = useMutation(
  (cardIds: number[]) => memoryApi.deleteCards(cardIds),
  {
    successMessage: t('review.deleteSuccess'),
    onSuccess: () => {
      void refreshCourses()
      void fetchNextCard()
    },
  }
)

const deleteBlockedCard = () => {
  if (!currentCard.value) return
  void executeDeleteBlockedCard([currentCard.value.id])
}

const { execute: executeReset } = useMutation(memoryApi.resetCardProgress, {
  successMessage: t('review.resetSuccess'),
  onSuccess: () => {
    selectedCards.value = []
    void refreshCourses() // 刷新课程列表以更新待复习数量
    if (viewMode.value === 'list') {
      void loadListCards(true)
    }
  },
})

const resetSelectedCards = () => {
  if (selectedCards.value.length === 0) return
  void executeReset(selectedCards.value)
}

const reviewSelectedCards = () => {
  // TODO: 后端暂不支持按指定卡片列表复习
}

// 更新课程设置
const { execute: executeUpdateSetting } = useMutation(
  () => {
    if (!selectedCourse.value) throw new Error('No course selected')
    return memoryApi.updateCourseMemorySetting({
      courseId: selectedCourse.value.course.id,
      status: selectedCourse.value.setting.state,
      frequencySetting: selectedCourse.value.setting.frequencySetting,
      cardOrder: selectedCourse.value.setting.cardOrder,
      dailyNewLimit: settingDailyNewLimit.value,
      dailyReviewLimit: settingDailyReviewLimit.value,
    })
  },
  {
    successMessage: t('review.updateSuccess'),
  }
)

const updateCourseSetting = () => {
  void executeUpdateSetting()
}

// 移除课程
const { execute: executeRemove } = useMutation(
  () => {
    if (!selectedCourse.value) throw new Error('No course selected')
    return memoryApi.removeCourseMemoryBank(selectedCourse.value.course.id)
  },
  {
    successMessage: t('review.removeSuccess'),
    onSuccess: () => {
      void refreshCourses()
      activeTab.value = 'all'
    },
  }
)

const removeCourse = () => {
  void executeRemove()
}

// 工具函数
const getCourseStatusColor = (status: CourseStudyStatus): string => {
  switch (status) {
    case Status.STUDYING:
      return 'success'
    case Status.PAUSED:
      return 'warning'
    case Status.ARCHIVED:
      return 'grey'
    default:
      return 'grey'
  }
}

const getFrequencyText = (frequency: FrequencySetting): string => {
  switch (frequency) {
    case FrequencySetting.HIGH:
      return t('review.frequencyHigh')
    case FrequencySetting.NORMAL:
      return t('review.frequencyNormal')
    case FrequencySetting.LOW:
      return t('review.frequencyLow')
    default:
      return t('review.frequencyNormal')
  }
}

// 检测卡片是否被屏蔽
const isCardBlocked = (card: MemoryCardView): boolean => {
  const cardState = card.state
  const deckState = card.deck?.state
  return (cardState !== undefined && cardState !== DeckState.PUBLISHED) ||
         (deckState !== undefined && deckState !== DeckState.PUBLISHED)
}

const getCardStatusChips = (card: MemoryCardView): { text: string; color: string }[] => {
  const chips: { text: string; color: string }[] = []

  // 如果被屏蔽，优先显示屏蔽状态
  if (isCardBlocked(card)) {
    chips.push({ text: t('review.blocked'), color: 'error' })
    return chips
  }

  if (!card.srsState) {
    chips.push({ text: t('review.newCard'), color: 'grey' })
    return chips
  }

  const isDue = new Date(card.srsState.reviewDueAt).getTime() <= Date.now()

  if (isDue) {
    chips.push({ text: t('review.dueCard'), color: 'primary' })
  }

  if (card.srsState.repetitions === 0) {
    chips.push({ text: t('review.newCard'), color: 'grey' })
  } else if (card.srsState.repetitions >= 3 && !isDue) {
    chips.push({ text: t('review.mastered'), color: 'success' })
  } else if (card.srsState.repetitions > 0 && card.srsState.repetitions < 3 && !isDue) {
    chips.push({
      text: `${t('review.reviewed')}${card.srsState.repetitions}${t('review.times')}`,
      color: 'warning',
    })
  }

  return chips.length > 0 ? chips : [{ text: t('review.unknownStatus'), color: 'grey' }]
}

// 格式化到期日期
const formatDueDate = (dateString: string): string => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

// 计算卡片高度
const calculateCardHeight = () => {
  const windowHeight = window.innerHeight
  // 减去其他元素的高度（标题、进度条、按钮等），估算约 400px
  const availableHeight = windowHeight - 400
  // 最小 500px，最大 800px
  cardHeight.value = Math.max(500, Math.min(800, availableHeight))
}

// 组件挂载
onMounted(() => {
  // 计算初始高度
  calculateCardHeight()

  // 监听窗口大小变化
  window.addEventListener('resize', calculateCardHeight)
})

// 组件卸载
onBeforeUnmount(() => {
  window.removeEventListener('resize', calculateCardHeight)
})
</script>

<style scoped>
.review-page {
  max-width: 1550px;
  margin: 0 auto;
  padding-top: 24px;
}

@media (max-width: 960px) {
  .review-page {
    padding-top: 16px;
  }
}

.sticky-nav {
  position: sticky;
  top: 80px;
}

.mobile-category-panel {
  background-color: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline));
}

.mobile-category-panel :deep(.v-expansion-panel) {
  background-color: transparent;
}

.mobile-category-panel :deep(.v-expansion-panel-title) {
  min-height: 64px;
}

.mobile-category-panel :deep(.v-expansion-panel-text__wrapper) {
  padding: 0 8px 8px 8px;
}

.nav-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.nav-item:hover {
  transform: translateX(4px);
  border-color: rgba(var(--v-theme-primary), 0.2);
  background-color: rgb(var(--v-theme-surface));
}

.nav-item-active {
  background: rgba(var(--v-theme-primary), 0.08);
  border-color: rgba(var(--v-theme-primary), 0.2);
}

.nav-item-inactive {
  background: rgb(var(--v-theme-surface));
}

.course-icon-container {
  width: 32px;
  height: 32px;
  border: 1px solid rgb(var(--v-theme-outline));
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.question-content,
.answer-content {
  background: rgb(var(--v-theme-surface));
  border-radius: 12px;
}

.card-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgb(var(--v-theme-outline));
  background-color: rgb(var(--v-theme-surface));
}

.card-item:hover {
  transform: translateX(4px);
  border-color: rgba(var(--v-theme-primary), 0.3);
}

.card-selected {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.05);
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.min-w-0 {
  min-width: 0;
}

@media (max-width: 1264px) {
  .sticky-nav {
    position: static;
  }
}
</style>
