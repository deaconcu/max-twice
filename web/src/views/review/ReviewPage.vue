<template>
  <DefaultLayout>
    <div class="review-page">
      <!-- 页面标题 -->
      <div class="mb-4 mb-md-6">
        <div class="d-flex align-center mb-4">
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
              {{ t('review.subtitle') }}
            </p>
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
                        :class="
                          (activeTab === 'all' ? totalDueCards : selectedCourseDueCards) > 0
                            ? 'text-error font-weight-bold'
                            : 'text-grey'
                        "
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
                        <span v-if="totalNewCards > 0" class="text-success font-weight-bold"
                          >+{{ totalNewCards }}</span
                        >
                        <span
                          :class="totalDueCards > 0 ? 'text-error font-weight-bold' : 'text-grey'"
                          >{{ totalDueCards }}</span
                        >
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
                          :class="
                            (bank.newCardCount || 0) +
                              (bank.learningCount || 0) +
                              (bank.dueCardCount || 0) >
                            0
                              ? 'text-primary font-weight-bold'
                              : 'text-grey'
                          "
                        >
                          {{
                            (bank.newCardCount || 0) +
                            (bank.learningCount || 0) +
                            (bank.dueCardCount || 0)
                          }}
                        </span>
                      </div>
                    </div>
                  </div>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-card>

          <!-- 选中课程时显示课程名 header + 模式切换按钮 (公共区域) -->
          <div v-if="selectedCourse" class="d-flex align-center justify-space-between mb-4">
            <div class="d-flex align-baseline ga-4">
              <span class="text-h6 font-weight-bold text-grey-darken-3">{{
                selectedCourse.course.name
              }}</span>
              <!-- 复习模式下显示额外信息 -->
              <div
                v-if="viewMode === 'review'"
                class="text-caption text-grey-darken-1 d-flex align-center ga-2"
              >
                <span v-if="selectedCourseLearningCards > 0">
                  {{ t('review.learningCards') }}
                  <span class="font-weight-bold text-warning">{{
                    selectedCourseLearningCards
                  }}</span>
                  {{ t('review.cards') }}
                </span>
                <span
                  v-if="selectedCourseLearningCards > 0 && (selectedCourse?.dueCardCount || 0) > 0"
                  >·</span
                >
                <span v-if="(selectedCourse?.dueCardCount || 0) > 0">
                  {{ t('review.dueReview') }}
                  <span class="font-weight-bold text-error">{{
                    selectedCourse?.dueCardCount || 0
                  }}</span>
                  {{ t('review.cards') }}
                </span>
                <span v-if="selectedCourseNewCards > 0">·</span>
                <span v-if="selectedCourseNewCards > 0">
                  {{ t('review.newCards') }}
                  <span class="font-weight-bold text-success">{{ selectedCourseNewCards }}</span>
                  {{ t('review.cards') }}
                </span>
                <span v-if="submitting">· {{ t('review.submitting') }}</span>
              </div>
            </div>
            <!-- 模式切换按钮 -->
            <div class="d-flex align-center ga-1">
              <v-btn
                :color="viewMode === 'review' ? 'primary' : 'grey'"
                :variant="viewMode === 'review' ? 'flat' : 'text'"
                rounded="lg"
                size="small"
                @click="switchViewMode('review')"
              >
                <v-icon icon="mdi-play" size="16" class="mr-1"></v-icon>
                复习
              </v-btn>
              <v-btn
                :color="viewMode === 'list' ? 'primary' : 'grey'"
                :variant="viewMode === 'list' ? 'flat' : 'text'"
                rounded="lg"
                size="small"
                @click="switchViewMode('list')"
              >
                <v-icon icon="mdi-format-list-bulleted" size="16" class="mr-1"></v-icon>
                卡片管理
              </v-btn>
              <v-btn
                :color="viewMode === 'manage' ? 'primary' : 'grey'"
                :variant="viewMode === 'manage' ? 'flat' : 'text'"
                rounded="lg"
                size="small"
                @click="switchViewMode('manage')"
              >
                <v-icon icon="mdi-cog" size="16" class="mr-1"></v-icon>
                复习设置
              </v-btn>
            </div>
          </div>

          <!-- 复习模式 -->
          <div v-if="viewMode === 'review'">
            <!-- 加载状态 -->
            <LoadingSpinner v-if="reviewLoading" />

            <!-- 全部课程 - 提示选择具体课程 -->
            <div v-else-if="!isReviewing && !selectedCourse" class="text-center">
              <v-card border rounded="lg" style="padding: 200px 32px">
                <v-icon
                  icon="mdi-book-open-page-variant"
                  size="64"
                  color="primary"
                  class="mb-4"
                ></v-icon>
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
            <div
              v-else-if="
                !isReviewing &&
                selectedCourse &&
                selectedCourseDueCards === 0 &&
                selectedCourseNewCards === 0
              "
              class="text-center"
            >
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
                    <span class="font-weight-bold text-warning">{{
                      selectedCourseLearningCards
                    }}</span>
                    {{ t('review.cards') }} ·
                  </span>
                  {{ t('review.dueReview') }}
                  <span class="font-weight-bold text-error">{{
                    selectedCourse.dueCardCount || 0
                  }}</span>
                  {{ t('review.cards') }}
                  <span v-if="selectedCourseNewCards > 0">
                    · {{ t('review.newCards') }}
                    <span class="font-weight-bold text-success">{{ selectedCourseNewCards }}</span>
                    {{ t('review.cards') }}
                  </span>
                </p>
                <v-btn
                  color="primary"
                  variant="flat"
                  rounded="lg"
                  size="large"
                  @click="startReview"
                >
                  <v-icon icon="mdi-play" class="mr-2"></v-icon>
                  {{ t('review.startReview') }}
                </v-btn>
              </v-card>
            </div>

            <!-- 复习中状态 -->
            <div v-else-if="currentCard">
              <!-- 卡片区域 -->
              <v-card rounded="lg" class="mb-4 position-relative">
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
                      {{ isCurrentDeckBlocked ? t('review.deckBlocked') : t('review.cardBlocked') }}
                    </h3>
                    <p class="text-body-1 text-grey-darken-1 mb-6">
                      {{
                        isCurrentDeckBlocked
                          ? t('review.deckBlockedHint')
                          : t('review.cardBlockedHint')
                      }}
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
                        v-if="isCurrentDeckBlocked && currentCard.deck?.nodeId"
                        color="primary"
                        variant="flat"
                        rounded="lg"
                        :href="`/read?nodeId=${currentCard.deck.nodeId}`"
                        target="_blank"
                        prepend-icon="mdi-book-open-page-variant"
                      >
                        {{ t('review.goToNode') }}
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
                      <a
                        v-if="currentCard.deck.nodeId"
                        :href="`/read?nodeId=${currentCard.deck.nodeId}`"
                        target="_blank"
                        class="text-decoration-none"
                      >
                        <v-chip size="small" color="primary" variant="outlined" link>
                          <v-icon icon="mdi-book-open-page-variant" size="16" class="mr-1"></v-icon>
                          {{ currentCard.deck.courseName }} - {{ currentCard.deck.nodeName }}
                        </v-chip>
                      </a>
                      <v-chip v-else size="small" color="primary" variant="outlined">
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
                      <a
                        v-if="currentCard.deck.nodeId"
                        :href="`/read?nodeId=${currentCard.deck.nodeId}`"
                        target="_blank"
                        class="text-decoration-none"
                      >
                        <v-chip size="small" color="success" variant="outlined" link>
                          <v-icon icon="mdi-book-open-page-variant" size="16" class="mr-1"></v-icon>
                          {{ currentCard.deck.courseName }} - {{ currentCard.deck.nodeName }}
                        </v-chip>
                      </a>
                      <v-chip v-else size="small" color="success" variant="outlined">
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

                <!-- 结束复习按钮 - 右下角 -->
                <v-btn
                  variant="text"
                  color="grey"
                  rounded="lg"
                  size="small"
                  class="stop-review-btn"
                  @click="resetReview"
                >
                  <v-icon icon="mdi-stop" size="16" class="mr-1"></v-icon>
                  {{ t('review.stopReview') }}
                </v-btn>
              </v-card>
            </div>
          </div>

          <!-- 列表模式 -->
          <div v-else-if="viewMode === 'list'">
            <!-- 加载状态 -->
            <LoadingSpinner v-if="listLoading && listCards.length === 0" />

            <div v-else>
              <div
                class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between mb-4 mt-6 ga-3"
              >
                <span class="text-caption text-grey-darken-2">
                  <v-icon icon="mdi-cards-outline" size="14" class=""></v-icon>
                  {{ t('review.cardList') }}
                </span>

                <!-- 批量操作按钮 -->
                <div class="d-flex align-center flex-wrap ga-1">
                  <v-btn
                    v-if="selectedCards.length > 0"
                    color="primary"
                    variant="tonal"
                    rounded="lg"
                    disabled
                    size="small"
                    @click="reviewSelectedCards"
                  >
                    <v-icon icon="mdi-play" size="14" class="mr-1"></v-icon>
                    <span class="d-none d-sm-inline"
                      >{{ t('review.reviewSelected') }} ({{ selectedCards.length }})</span
                    >
                    <span class="d-sm-none"
                      >{{ t('review.modeReview') }} ({{ selectedCards.length }})</span
                    >
                  </v-btn>

                  <v-btn
                    v-if="selectedCards.length > 0"
                    color="warning"
                    variant="tonal"
                    rounded="lg"
                    size="small"
                    @click="resetSelectedCards"
                  >
                    <v-icon icon="mdi-restart" size="14" class="mr-1"></v-icon>
                    <span class="d-none d-sm-inline">{{ t('review.resetLearning') }}</span>
                    <span class="d-sm-none">{{ t('common.reset') }}</span>
                  </v-btn>

                  <v-btn
                    v-if="selectedCards.length > 0"
                    color="error"
                    variant="tonal"
                    rounded="lg"
                    size="small"
                    @click="deleteSelectedCards"
                  >
                    <v-icon icon="mdi-delete" size="14" class="mr-1"></v-icon>
                    <span class="d-none d-sm-inline">{{ t('common.delete') }}</span>
                    <span class="d-sm-none">{{ t('common.delete') }}</span>
                  </v-btn>

                  <v-btn
                    :color="
                      selectedCards.length === listCards.length && listCards.length > 0
                        ? 'primary'
                        : 'grey'
                    "
                    variant="tonal"
                    rounded="lg"
                    size="small"
                    :disabled="listCards.length === 0"
                    @click="toggleSelectAll"
                  >
                    <v-icon
                      :icon="
                        selectedCards.length === listCards.length && listCards.length > 0
                          ? 'mdi-checkbox-marked'
                          : 'mdi-checkbox-blank-outline'
                      "
                      size="14"
                      class="mr-1"
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
                          ? t('common.cancel')
                          : t('common.selectAll')
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
                  class="card-item pa-3 pa-sm-4 rounded-lg mb-2 position-relative"
                  :class="[selectedCards.includes(card.id) ? 'card-selected' : '']"
                  @click="toggleCardSelection(card.id)"
                >
                  <!-- 左上角序号角标 -->
                  <span class="card-index-badge">{{ index + 1 }}</span>

                  <div class="d-flex align-start">
                    <!-- 选择框 -->
                    <div :class="$vuetify.display.mobile ? 'mr-2' : 'mr-3'">
                      <v-checkbox
                        :model-value="selectedCards.includes(card.id)"
                        density="compact"
                        hide-details
                        @click.stop="toggleCardSelection(card.id)"
                      ></v-checkbox>
                    </div>

                    <!-- 卡片内容 -->
                    <div class="flex-grow-1 min-w-0">
                      <!-- 被屏蔽的卡片 -->
                      <template v-if="isCardBlocked(card)">
                        <div class="d-flex align-center mb-1">
                          <v-icon
                            icon="mdi-alert-circle"
                            color="error"
                            size="16"
                            class="mr-1"
                          ></v-icon>
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
                        <div
                          class="text-caption text-md-body-2 text-grey-darken-2 my-2 line-clamp-2"
                        >
                          {{ card.back }}
                        </div>
                        <!-- 到期时间 -->
                        <div v-if="card.srsState" class="text-caption text-grey-darken-1">
                          <v-icon
                            icon="mdi-clock-outline"
                            :size="$vuetify.display.mobile ? 12 : 14"
                            class="mr-1"
                          ></v-icon>
                          {{ t('review.nextReview') }}:
                          {{ formatDueDate(card.srsState.reviewDueAt) }}
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
                    :loading="isFetchingNextPage"
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

            <div v-else class="mt-6">
              <v-card rounded="lg" border class="pa-5 pa-md-6">
                <!-- 设置项列表 -->
                <div class="settings-list">
                  <!-- 复习频率 -->
                  <div class="setting-item d-flex justify-space-between align-center pb-5">
                    <div class="d-flex align-center">
                      <v-avatar color="grey" variant="tonal" size="40" class="mr-4">
                        <v-icon icon="mdi-clock-outline" size="20" color="grey-darken-1"></v-icon>
                      </v-avatar>
                      <div>
                        <div class="text-body-1 font-weight-medium text-grey-darken-3">
                          {{ t('review.reviewFrequency') }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          {{ t('review.reviewFrequencyHint') }}
                        </div>
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

                  <v-divider></v-divider>

                  <!-- 学习状态 -->
                  <div class="setting-item d-flex justify-space-between align-center py-5">
                    <div class="d-flex align-center">
                      <v-avatar color="grey" variant="tonal" size="40" class="mr-4">
                        <v-icon
                          icon="mdi-bookmark-outline"
                          size="20"
                          color="grey-darken-1"
                        ></v-icon>
                      </v-avatar>
                      <div>
                        <div class="text-body-1 font-weight-medium text-grey-darken-3">
                          {{ t('review.learningStatus') }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          {{ t('review.learningStatusHint') }}
                        </div>
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

                  <v-divider></v-divider>

                  <!-- 卡片顺序 -->
                  <div class="setting-item d-flex justify-space-between align-center py-5">
                    <div class="d-flex align-center">
                      <v-avatar color="grey" variant="tonal" size="40" class="mr-4">
                        <v-icon icon="mdi-sort" size="20"></v-icon>
                      </v-avatar>
                      <div>
                        <div class="text-body-1 font-weight-medium text-grey-darken-3">
                          {{ t('review.cardOrder') }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          {{ t('review.cardOrderHint') }}
                        </div>
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

                  <v-divider></v-divider>

                  <!-- 每日新卡上限 -->
                  <div class="setting-item d-flex justify-space-between align-center py-5">
                    <div class="d-flex align-center">
                      <v-avatar color="grey" variant="tonal" size="40" class="mr-4">
                        <v-icon icon="mdi-card-plus-outline" size="20"></v-icon>
                      </v-avatar>
                      <div>
                        <div class="text-body-1 font-weight-medium text-grey-darken-3">
                          {{ t('review.dailyNewLimit') }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          {{ t('review.dailyNewLimitHint') }}
                        </div>
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

                  <v-divider></v-divider>

                  <!-- 每日复习上限 -->
                  <div class="setting-item d-flex justify-space-between align-center pt-5">
                    <div class="d-flex align-center">
                      <v-avatar color="grey" variant="tonal" size="40" class="mr-4">
                        <v-icon icon="mdi-repeat" size="20"></v-icon>
                      </v-avatar>
                      <div>
                        <div class="text-body-1 font-weight-medium text-grey-darken-3">
                          {{ t('review.dailyReviewLimit') }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          {{ t('review.dailyReviewLimitHint') }}
                        </div>
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
              </v-card>

              <div class="d-flex ga-3 mt-6">
                <v-btn color="primary" variant="flat" rounded="lg" @click="updateCourseSetting">
                  {{ t('common.saveSettings') }}
                </v-btn>
              </div>
            </div>
          </div>
        </v-col>

        <!-- 右侧课程分类 - 仅大屏幕显示 -->
        <v-col cols="12" lg="3" class="d-none d-lg-block">
          <v-card rounded="lg" class="sticky-nav pl-3 pl-sm-4 pr-1 no-border">
            <div class="d-flex align-center justify-space-between mb-3 mb-md-4 px-2 pr-0">
              <h3 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-4">
                {{ t('review.courseCategory') }}
              </h3>
              <div class="d-flex align-center ga-1">
                <v-btn
                  icon
                  size="x-small"
                  :color="courseStateTab === 1 ? 'primary' : 'grey'"
                  :variant="courseStateTab === 1 ? 'tonal' : 'text'"
                  @click="switchCourseStateTab(1)"
                >
                  <v-icon size="16">mdi-play-circle</v-icon>
                  <v-tooltip activator="parent" location="bottom">{{
                    t('review.statusStudying')
                  }}</v-tooltip>
                </v-btn>
                <v-btn
                  icon
                  size="x-small"
                  :color="courseStateTab === 2 ? 'primary' : 'grey'"
                  :variant="courseStateTab === 2 ? 'tonal' : 'text'"
                  @click="switchCourseStateTab(2)"
                >
                  <v-icon size="16">mdi-snowflake</v-icon>
                  <v-tooltip activator="parent" location="bottom">{{
                    t('review.statusFrozen')
                  }}</v-tooltip>
                </v-btn>
                <v-btn
                  icon
                  size="x-small"
                  :color="courseStateTab === 3 ? 'primary' : 'grey'"
                  :variant="courseStateTab === 3 ? 'tonal' : 'text'"
                  @click="switchCourseStateTab(3)"
                >
                  <v-icon size="16">mdi-eye-off</v-icon>
                  <v-tooltip activator="parent" location="bottom">{{
                    t('review.statusHidden')
                  }}</v-tooltip>
                </v-btn>
              </div>
            </div>

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
                  <!-- 只有学习中状态显示统计数字 -->
                  <div v-if="courseStateTab === 1" class="d-flex align-center ga-2 text-body-2">
                    <span
                      :class="
                        (bank.newCardCount || 0) +
                          (bank.learningCount || 0) +
                          (bank.dueCardCount || 0) >
                        0
                          ? 'text-primary font-weight-bold'
                          : 'text-grey'
                      "
                    >
                      {{
                        (bank.newCardCount || 0) +
                        (bank.learningCount || 0) +
                        (bank.dueCardCount || 0)
                      }}
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '@/composables/useI18n'
import * as memoryApi from '@/api/modules/memory'
import type {
  MemoryCardView,
  CourseStudyStatus,
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
import {
  useReviewSummaryQuery,
  useReviewCardMutation,
  useDeleteCardsMutation,
  useResetCardProgressMutation,
  useUpdateCourseMemorySettingMutation,
  useCardListQuery,
} from '@/queries/memory'

const { t } = useI18n()

// 状态
const activeTab = ref<string>('all')
const viewMode = ref<string>('review')
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

// 列表分页（useInfiniteQuery）
const selectedCourseId = computed(() => {
  if (activeTab.value === 'all') return undefined
  return parseInt(activeTab.value)
})

const {
  data: cardListData,
  isLoading: listLoading,
  isFetchingNextPage,
  hasNextPage: listHasMore,
  fetchNextPage,
} = useCardListQuery(selectedCourseId)

const listCards = computed(() =>
  cardListData.value?.pages.flatMap((p) => p.items) ?? []
)

const loadMoreListCards = () => {
  void fetchNextPage()
}

// 学习中的课程（立即加载）
const { data: studyingSummary, refetch: refreshStudying } = useReviewSummaryQuery(Status.STUDYING)

// 冻结/隐藏的课程（懒加载，点击 tab 时才触发）
const frozenEnabled = ref(false)
const hiddenEnabled = ref(false)

const { data: frozenSummary, isLoading: frozenLoading } = useReviewSummaryQuery(Status.FROZEN, frozenEnabled)
const { data: hiddenSummary, isLoading: hiddenLoading } = useReviewSummaryQuery(Status.HIDDEN, hiddenEnabled)

// 当前状态 tab 对应的课程列表
const currentCourseList = computed(() => {
  switch (courseStateTab.value) {
    case Status.STUDYING:
      return studyingSummary.value?.courses ?? []
    case Status.FROZEN:
      return frozenSummary.value?.courses ?? []
    case Status.HIDDEN:
      return hiddenSummary.value?.courses ?? []
    default:
      return []
  }
})

// 课程列表加载状态
const courseListLoadingComputed = computed(() => {
  if (courseStateTab.value === Status.FROZEN) return frozenLoading.value
  if (courseStateTab.value === Status.HIDDEN) return hiddenLoading.value
  return false
})

// 从复习概览中提取课程列表（学习中的课程，兼容现有代码）
const courseMemoryBanks = computed(() => studyingSummary.value?.courses ?? [])

// 切换课程状态 Tab，懒加载数据
const switchCourseStateTab = (state: number) => {
  courseStateTab.value = state
  if (state === Status.FROZEN) {
    frozenEnabled.value = true
  } else if (state === Status.HIDDEN) {
    hiddenEnabled.value = true
  }
}

// 计算属性
const totalDueCards = computed(() => {
  return courseMemoryBanks.value.reduce(
    (sum, bank) => sum + (bank.dueCardCount || 0) + (bank.learningCount || 0),
    0
  )
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
  },
})

const settingDailyReviewLimit = computed({
  get: () => selectedCourse.value?.setting.dailyReviewLimit,
  set: (val: number) => {
    if (selectedCourse.value) {
      selectedCourse.value.setting.dailyReviewLimit = val
    }
  },
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

// 检测当前卡片组是否被屏蔽
const isCurrentDeckBlocked = computed(() => {
  if (!currentCard.value) return false
  const deckState = currentCard.value.deck?.state
  return deckState !== undefined && deckState !== DeckState.PUBLISHED
})

// 检测当前卡片本身是否被屏蔽（卡片组正常，但卡片被屏蔽）
const isCurrentCardOnlyBlocked = computed(() => {
  if (!currentCard.value) return false
  const cardState = currentCard.value.state
  return cardState !== undefined && cardState !== DeckState.PUBLISHED && !isCurrentDeckBlocked.value
})

// 检测当前卡片是否被屏蔽（卡片或卡片组任一被屏蔽）
const isCurrentCardBlocked = computed(
  () => isCurrentDeckBlocked.value || isCurrentCardOnlyBlocked.value
)

// 方法
const switchTab = (tabValue: string) => {
  activeTab.value = tabValue
  expansionPanel.value = [] // 关闭展开面板

  // 切换课程时重置为复习模式
  viewMode.value = 'review'
  resetReview()
}

const switchViewMode = (mode: 'review' | 'list' | 'manage') => {
  viewMode.value = mode
  selectedCards.value = []
  if (mode === 'review') {
    resetReview()
  } else if (mode === 'list') {
    // useCardListQuery 会根据 selectedCourseId 自动重新获取
  }
}

const startReview = async () => {
  if (!selectedCourse.value) return
  const courseId = selectedCourse.value.course.id
  const courseDueCards = selectedCourseDueCards.value + selectedCourseNewCards.value
  if (courseDueCards === 0) return

  reviewLoading.value = true
  try {
    const result = await memoryApi.getNextCard({ courseId })
    currentCard.value = result.nextCard ?? null
    if (currentCard.value) {
      isReviewing.value = true
      showAnswer.value = false
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
const reviewCardMutation = useReviewCardMutation()
const submitting = reviewCardMutation.isPending

const submitReview = (result: ReviewResult) => {
  if (!currentCard.value || submitting.value) return

  const courseId = selectedCourse.value?.course.id
  reviewCardMutation.mutate(
    { cardId: currentCard.value.id, result, courseId, timeSpent: 5 },
    {
      onSuccess: (data) => {
        if (!data) return
        // 更新当前课程卡片统计
        if (data.courseStats && selectedCourse.value) {
          const courseId = selectedCourse.value.course.id
          const bank = studyingSummary.value?.courses.find((b) => b.course.id === courseId)
          if (bank) {
            bank.newCardCount = data.courseStats.newCardCount
            bank.dueCardCount = data.courseStats.dueCardCount
            bank.learningCount = data.courseStats.learningCount
          }
        }
        currentCard.value = data.nextCard
        if (!currentCard.value) {
          void completeReview()
        } else {
          showAnswer.value = false
        }
      },
    }
  )
}

const completeReview = async () => {
  isReviewing.value = false
  currentCard.value = null
  await refreshStudying()
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

// 批量删除卡片
const deleteCardsMutation = useDeleteCardsMutation()

const deleteSelectedCards = () => {
  if (selectedCards.value.length === 0) return
  deleteCardsMutation.mutate(selectedCards.value, {
    onSuccess: () => {
      selectedCards.value = []
      void refreshStudying()
    },
  })
}

// 删除被屏蔽的卡片（复用 deleteCardsMutation，成功后拿下一张）
const deleteBlockedCard = () => {
  if (!currentCard.value) return
  deleteCardsMutation.mutate([currentCard.value.id], {
    onSuccess: async () => {
      void refreshStudying()
      const result = await memoryApi.getNextCard({ courseId: selectedCourse.value?.course.id })
      currentCard.value = result.nextCard ?? null
      if (!currentCard.value) void completeReview()
      else showAnswer.value = false
    },
  })
}

// 重置卡片进度
const resetCardsMutation = useResetCardProgressMutation()

const resetSelectedCards = () => {
  if (selectedCards.value.length === 0) return
  resetCardsMutation.mutate(selectedCards.value, {
    onSuccess: () => {
      selectedCards.value = []
      void refreshStudying()
      // useResetCardProgressMutation 已通过 invalidateQueries 自动刷新卡片列表
    },
  })
}

const reviewSelectedCards = () => {
  // TODO: 后端暂不支持按指定卡片列表复习
}

// 更新课程设置
const updateCourseSettingMutation = useUpdateCourseMemorySettingMutation()

const updateCourseSetting = () => {
  if (!selectedCourse.value) return
  updateCourseSettingMutation.mutate(
    {
      courseId: selectedCourse.value.course.id,
      status: selectedCourse.value.setting.state,
      frequencySetting: selectedCourse.value.setting.frequencySetting,
      cardOrder: selectedCourse.value.setting.cardOrder,
      dailyNewLimit: settingDailyNewLimit.value,
      dailyReviewLimit: settingDailyReviewLimit.value,
    },
    { onSuccess: () => void refreshStudying() }
  )
}

// 工具函数
const getCourseStatusColor = (status: CourseStudyStatus): string => {
  switch (status) {
    case Status.STUDYING:
      return 'success'
    case Status.FROZEN:
      return 'warning'
    case Status.HIDDEN:
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
  return (
    (cardState !== undefined && cardState !== DeckState.PUBLISHED) ||
    (deckState !== undefined && deckState !== DeckState.PUBLISHED)
  )
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

.stop-review-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
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
  background: rgb(var(--v-theme-surface-variant));
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

.card-index-badge {
  position: absolute;
  top: 0;
  left: 0;
  background: rgba(0, 0, 0, 0.06);
  color: rgba(0, 0, 0, 0.5);
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 0 0 6px 0;
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
