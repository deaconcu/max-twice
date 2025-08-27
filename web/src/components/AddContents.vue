<script setup>

import { ref, watch } from 'vue';
import draggable from 'vuedraggable'
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { learnService } from '@/services/learnService';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const props = defineProps(['nodeId' , 'pathText']);

const dialog = defineModel();

const emit = defineEmits(['loadData']);

const createContentsTab = ref(0);
const newContentsItem = ref('');
const newContents = ref([]);

watch(() => route.query.path, (newItems, oldItems) => {
  newContents.value = [];
})


const addContentsItem = () => {
  if (newContentsItem.value.trim() !== '') {
    newContents.value.push(newContentsItem.value.trim());
    newContentsItem.value = '';
  }
};

const removeContentsItem = (index) => {
  newContents.value.splice(index, 1); // 根据索引从数组中移除
};

const submitAddContents = async () => {
  try {
    console.log("begin post");

    const data = {
      "content": JSON.stringify(newContents.value),
      "nodeId": props.nodeId,
      "type": 1
    }

    console.log('request: ' + JSON.stringify(data));
    const response = await learnService.addPosting(data);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      dialog.value = false;
      emit('loadData', []);
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const addAIContents = async () => {
  try {
    console.log('begin get');
    const response = await learnService.openAI(
      t('addContents.aiPrompt', { pathText: props.pathText }),
      "openai/gpt-4o-mini"
    );
    console.log('response: ' + JSON.stringify(response));
    if (response.code === 200) {
      newContents.value = JSON.parse(response.data);
    }
  } catch (error) {
    console.error('Error submitting form:', error);
  }
};
</script>


<template>
  <v-dialog v-model="dialog" width="1100" height="700px">
    <v-card prepend-icon="mdi-account" :title="t('addContents.title')">
      <v-row class="ma-0 border-t-sm">

        <v-col class="border-e-sm">

          <v-card-text>
            <v-tabs v-model="createContentsTab">
              <v-tab value="one">{{ t('addContents.createNode') }}</v-tab>
              <v-tab value="two">{{ t('addContents.selectExisting') }}</v-tab>
              <v-tab value="three">{{ t('addContents.myNodes') }}</v-tab>
            </v-tabs>

            <v-card-text class="px-0">
              <v-tabs-window v-model="createContentsTab">
                <v-tabs-window-item value="one">
                  <v-text-field v-model="newContentsItem" :label="t('addContents.nodeName')" variant="outlined" class="pt-5"></v-text-field>
                  <v-btn variant="tonal" class="me-4" @click="addContentsItem">{{ t('addContents.submit') }}</v-btn>
                  <v-btn variant="plain" @click="addAIContents">{{ t('addContents.aiGenerate') }}</v-btn>
                </v-tabs-window-item>
                <v-tabs-window-item value="two">
                  Two
                </v-tabs-window-item>
                <v-tabs-window-item value="three">
                  Three
                </v-tabs-window-item>
              </v-tabs-window>
            </v-card-text>
          </v-card-text>
        </v-col>

        <v-col>
          <div class="scrollable-div px-5 py-1">
            <draggable v-model="newContents" item-key="id">
              <template #item="{ element, index }">
                <div class="d-flex justify-space-between align-center pt-3" style="border-bottom: 1px dashed #ddd;">
                  <span>{{ index + 1 }}. {{ element }}</span>
                  <v-btn icon flat size="small" variant="text" v-ripple="false" elevation="0"
                    @click="removeContentsItem(index)">
                    <v-icon size="15">mdi-close</v-icon>
                  </v-btn>
                </div>
              </template>
            </draggable>
          </div>
        </v-col>
      </v-row>
      <v-card-actions class="d-flex justify-center py-5 border-t-sm">
        <v-btn @click="submitAddContents" :style="{ backgroundColor: '#1976d2', color: '#fff', width: '100px' }">{{ t('addContents.confirm') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>