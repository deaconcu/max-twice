<script setup>
  import { ref } from 'vue'
  //import { defineProps } from 'vue';
  import { postServiceV1 } from '@/services/api/v1/apiServiceV1'
  import Tiptap from './TiptapInput.vue'
  import { useI18n } from 'vue-i18n'

  const props = defineProps({
    nodeId: {
      type: [String, Number],
      required: true,
    },
    pathText: {
      type: String,
      required: true,
    },
  })
  const emit = defineEmits(['loadData'])
  const { t } = useI18n()

  const dialog = defineModel({ type: Boolean })
  const editorRef = ref(null)

  const submitAddArticle = async () => {
    try {
      console.log('begin post')
      console.log('编辑器内容:', editorRef.value.editor.getHTML())

      const data = {
        content: editorRef.value.editor.getHTML(),
        nodeId: props.nodeId,
        type: 2,
      }

      console.log(`request: ${JSON.stringify(data)}`)
      const response = await postServiceV1.createPost(data)
      console.log(`response: ${JSON.stringify(response)}`)

      if (response.code === 200) {
        console.log('Form submitted successfully')
        emit('loadData', [])
        //emit('update:dialog', false)
        dialog.value = false
      }
    } catch (error) {
      // todo
      console.error('Error submitting form:', error)
    }
  }
</script>

<template>
  <v-dialog v-model="dialog" width="800" height="1200px">
    <v-card>
      <v-card-item class="border-b-sm">
        <v-card-title class="d-flex align-center">
          <v-icon size="small" class="px-4" icon="mdi-account"></v-icon>
          <span class="ps-2 font-weight-medium">{{ t('addArticle.createArticle') }}</span>
        </v-card-title>
      </v-card-item>
      <div class="overflow-y-scroll dialog-content">
        <Tiptap ref="editorRef" :path-text="props.pathText" class="px-6" />
        <div class="pt-1 pb-4 px-6 sticky-bottom">
          <v-btn
            variant="flat"
            color="grey-darken-2"
            size="large"
            class="rounded-lg text-white"
            block
            @click="submitAddArticle"
            >{{ t('addArticle.submitArticle') }}</v-btn
          >
        </div>
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
  .dialog-content {
    height: 1200px;
  }

  .sticky-bottom {
    position: sticky;
    bottom: 0px;
    background-color: #fff;
  }
</style>
