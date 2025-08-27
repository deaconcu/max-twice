<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import draggable from 'vuedraggable'
import { learnService } from '@/services/learnService';
import { useI18n } from 'vue-i18n'

const route = useRoute();
const router = useRouter();
const { t } = useI18n()

const emit = defineEmits(['loadData']);

const props = defineProps(['courseId', 'contents']);
const list = ref(Array.from({ length: props.contents.length }, (_, i) => i + 1));
const dialog = defineModel();

watch(() => props.contents, (newContents) => {
  list.value = Array.from({ length: newContents.length }, (_, i) => i + 1);
}, { immediate: true });


const submit = async () => {
  const pathParts = route.query.path.split("-");
  const currIndex = Number(pathParts[0]);
  const index = list.value.indexOf(currIndex) + 1;

  let nextPath;
  if (index == 0) {
    nextPath = "1-" + pathParts[1];
  } else {
    nextPath = index + "-" + pathParts.slice(1).join('-');
  }

  try {
    const response = await learnService.postToc(props.courseId, list.value.join(','));
    console.log('response: ' + JSON.stringify(response));
    if (response.code === 200) {
      dialog.value = false;
      if (nextPath === route.query.path) {
        emit("loadData", []);
      } else {
        router.replace({
          name: 'read',
          query: {
            courseId: props.courseId,
            path: nextPath
          },
        });
      }
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const addItem = () => {
  list.value.push(0);
  console.log("list: " + JSON.stringify(list.value));
}

const copyItem = (index, val) => {
  list.value.splice(index + 1, 0, -val);
  console.log("list: " + JSON.stringify(list.value));
}

const removeItem = (index) => {
  list.value.splice(index, 1);
  console.log("list: " + JSON.stringify(list.value));
}

</script>

<template>
  <v-dialog v-model="dialog" width="800" height="600" content-class="fix-dialog">
    <v-card class="px-1 py-2" rounded="lg">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-file-cog-outline" size="small" class=""></v-icon>
        <span class="ps-2">{{ t('configContents.modifyContents') }}</span>
      </v-card-title>
      <v-card-subtitle>
        {{ t('configContents.subtitle') }}
      </v-card-subtitle>
      <v-sheet height="500px" class="overflow-auto">
        <v-card-text>
          <draggable v-model="list" item-key="id" class="pt-3">
            <template #item="{ element, index }">
              <div class="d-flex justify-space-between align-center mb-5 pb-1 text-body-1"
                style="border-bottom: 1px dashed #ddd;">
                <span v-if="element > 0">{{ t('configContents.contentItem', { number: element }) }}</span>
                <span v-if="element == 0" class="text-red-darken-3">{{ t('configContents.newContent') }}</span>
                <span v-if="element < 0" class="text-red-darken-3">{{ t('configContents.copyOfContent', { number: -element }) }}</span>
                <v-spacer />
                <div>
                  <v-btn v-if="element > 0" flat size="small" variant="text" v-ripple="false" elevation="0"
                    prepend-icon="mdi-content-copy" @click="copyItem(index, element)" class="me-1">
                    {{ t('configContents.copy') }}
                  </v-btn>
                  <v-btn flat size="small" variant="text" v-ripple="false" elevation="0" prepend-icon="mdi-close"
                    @click="removeItem(index)">
                    {{ t('configContents.delete') }}
                  </v-btn>
                </div>
              </div>
            </template>
          </draggable>
        </v-card-text>
      </v-sheet>
      <v-card-actions class="d-flex justify-end">
        <v-btn color="teal-lighten-1" variant="text" density="comfortable" size="large" class="rounded-lg"
          @click="addItem()"><span class="font-weight-medium">{{ t('configContents.addContent') }}</span></v-btn>
        <v-btn color="teal-lighten-1" variant="text" density="comfortable" size="large" class="rounded-lg"
          @click="submit()"><span class="font-weight-medium">{{ t('configContents.done') }}</span></v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
:deep(.fix-dialog) {
  top: 150px !important;
  position: absolute !important;
}
</style>