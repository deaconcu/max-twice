<script setup>
import { ref, onMounted, inject } from 'vue';
import { useI18n } from 'vue-i18n';
import { commentServiceV1 } from '@/services/api/v1/apiServiceV1';
import { COMMENT_STATE } from '@/constants/statusConstants';

const { t } = useI18n();
const showSnackbar = inject('showSnackbar');

const commentList = ref([]);

const getCommentSensorList = async () => {
  try {
    const response = await commentServiceV1.getPendingComments()

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      commentList.value = response.data;
      console.log('done');
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
  }
}

const approveComment = async (comment, approve) => {
  try {
    const response = await commentServiceV1.approveComment(comment.id, approve)

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('done');
      console.log("comment: " + JSON.stringify(response.data))
      comment.state = response.data.state;
      showSnackbar(t('admin.operationSuccess'))
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
  }
}

onMounted(() => {
  getCommentSensorList();
});
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-comment-check-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">{{ t('admin.commentReview') }}</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">{{ t('admin.reviewUserComments') }}</p>
        </div>
      </div>
      <v-chip variant="flat" color="purple-lighten-4" rounded="lg">
        <v-icon icon="mdi-comment-multiple" color="purple-darken-2" size="16" class="mr-1"></v-icon>
        <span class="text-purple-darken-2 text-caption">{{ commentList.length }} {{ t('admin.commentsAwaitingReview') }}</span>
      </v-chip>
    </div>

    <div v-if="commentList.length === 0" class="text-center py-12">
      <v-icon icon="mdi-comment-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">{{ t('admin.noCommentsToReview') }}</p>
    </div>

    <div v-for="comment in commentList" :key="comment.id" class="mb-4">
      <v-card flat class="border rounded-lg pa-5" hover>
        <div class="d-flex align-start">
          <!-- 状态和操作区域 -->
          <div class="mr-4" style="min-width: 200px;">
            <div class="mb-3">
              <v-chip v-if="comment.state == COMMENT_STATE.SUBMITTED" variant="flat" color="orange-lighten-4" rounded="lg" size="small">
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                {{ t('admin.pending') }}
              </v-chip>
              <v-chip v-if="comment.state == COMMENT_STATE.APPROVED" variant="flat" color="green-lighten-4" rounded="lg" size="small">
                <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.approved') }}
              </v-chip>
              <v-chip v-if="comment.state == COMMENT_STATE.DELETED" variant="flat" color="red-lighten-4" rounded="lg" size="small">
                <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.rejected') }}
              </v-chip>
            </div>
            <div class="d-flex flex-column ga-2">
              <v-btn variant="flat" color="green-lighten-4" rounded="lg" size="small" @click="approveComment(comment, true)">
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.approve') }}
              </v-btn>
              <v-btn variant="flat" color="red-lighten-4" rounded="lg" size="small" @click="approveComment(comment, false)">
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.reject') }}
              </v-btn>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center justify-space-between mb-3">
              <div class="d-flex align-center">
                <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                  <v-icon icon="mdi-account" color="grey-darken-1" size="18"></v-icon>
                </v-avatar>
                <div>
                  <div class="text-body-2 font-weight-medium text-grey-darken-2">{{ t('admin.commentId') }}: {{ comment.id }}</div>
                  <div class="text-caption text-grey-darken-1">{{ comment.createdAt}}</div>
                </div>
              </div>
              <v-btn variant="outlined" color="teal" size="small" rounded="lg" 
                     :href="`/read?commentId=${comment.id}`" target="_blank">
                <v-icon icon="mdi-open-in-new" size="14" class="mr-1"></v-icon>
                {{ t('admin.viewOriginal') }}
              </v-btn>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div class="text-caption text-grey-darken-1 mb-2">{{ t('admin.commentContent') }}</div>
              <div class="text-body-1 text-grey-darken-2 line-height-relaxed">
                {{ comment.content }}
              </div>
            </div>
          </div>
        </div>
      </v-card>
    </div>
  </div>
</template>

<style scoped>
.comment-content {
  max-height: 150px;
  overflow-y: auto;
}
</style>