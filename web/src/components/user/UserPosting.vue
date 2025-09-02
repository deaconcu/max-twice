<script setup>
import { ref, onMounted, nextTick, watch, toRef, inject } from 'vue';
import { upvoteServiceV1, postServiceV1 } from '@/services/api/v1/apiServiceV1';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css';

const props = defineProps(['posting', 'type']);
const emit = defineEmits(['switchMainArea', 'deletePosting']);
const { t } = useI18n();

const contentRef = ref(null)
const isOverflow = ref(false)

onMounted(async() => {
  await nextTick()
  const el = contentRef.value;
  if (el && el.scrollHeight > el.clientHeight + 1) {
    isOverflow.value = true;
  }
})

const upvote = async (posting, type) => {
  try {
    console.log("begin post");

    const response = await upvoteServiceV1.upvote(posting.id, 0, type);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      posting.once = response.data.once;
      posting.twice = response.data.twice;
      posting.helpful = response.data.helpful;
      posting.voteType = response.data.voteType;
      if (posting.voteType == 0) posting.voteType = null;
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const deletePosting = async (postingId) => {
  try {
    console.log("begin post");

    const response = await postServiceV1.deletePost(postingId);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      emit("deletePosting", postingId);
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

</script>

<template>
  <div class="w-100">
    <v-row class="ma-0 pb-0 d-flex align-center" :class="type == 'detail' ? 'sticky-top' : ''"
      :style="type == 'detail' ? 'background-color: white; transform: translateX(-37px); width: 110%;' : ''">
      <v-btn v-if="type == 'detail'" variant="flat" class="me-0" color="" density="comfortable" icon="mdi-chevron-left"
        @click="emit('switchMainArea', 'list')"></v-btn>
      <v-avatar icon="mdi-account" size="34" color="red">
        <span class="text-body-1">CJ</span>
      </v-avatar>
      <div class="pl-3">
        <div class="text-body-2 text-grey-darken-4 font-weight-bold">{{ posting.creator.name}}</div>
        <div class="text-body-2 text-grey">{{ posting.createdAt}}</div>
      </div>
    </v-row>

    <v-row class="ma-0 pa-0 pt-3">
      <!-- is contents -->
      <template v-if="posting.type == 1">
        <div class="w-100 d-flex justify-space-between align-end">
          <v-list class="">
            <v-list-item v-for="(item, index) in posting.content.split(',')" :key="index" class="px-0 py-0"
              min-height="48">
              <v-list-item-title class="pb-1" style="border-bottom: 1px dashed #ddd;">{{ item }}</v-list-item-title>
            </v-list-item>
          </v-list>
        </div>
      </template>

      <!-- is article -->
      <template v-else>
        <div ref="contentRef" :class="props.type == 'list' ? 'text-limited' : ''">
          <div ref="content" class="tiptap pt-3 pb-5 w-100" v-html="posting.content"></div>
        </div>
        <v-btn v-if="isOverflow" variant="text" class="px-0 mt-2 text-body-1" 
        @click="props.type == 'list' ? emit('switchMainArea', 'detail', posting) : ''">{{ t('userPosting.viewFullText') }}...</v-btn>
        <br />
      </template>
    </v-row>

    <div class="w-100 d-flex justify-space-between align-end pb-2 pt-3">
      <div class="">
        <!-- is contents -->
        <template v-if="posting.type == 1">
          <v-btn :variant="posting.voteType === 3 ? 'text' : 'outlined'" rounded="lg" density="comfortable"
            :color="posting.voteType === 3 ? 'pink-lighten-1' : ''" class="ps-4 me-4 border"
            @click="upvote(posting, 3, 0)">
            <template v-slot:prepend>
              <v-icon v-if="posting.voteType === 3">mdi-check</v-icon>
            </template>
            <span :class="posting.voteType === 3 ? 'font-weight-medium' : ''">{{ t('userPosting.agree') }} {{ posting.helpful }}</span>
          </v-btn>
        </template>

        <!-- is article -->
        <template v-else>
          <v-btn-toggle v-model="posting.voteType" variant="plain" rounded="xl" class="pe-4 custom-btn-toggle"
            style="height: 28px;">
            <v-btn :value="1" color="pink" class="px-3 border border-e-0" @click="upvote(posting, 1)">
              <template v-slot:prepend>
                <v-icon v-if="posting.voteType === 1">mdi-check</v-icon>
              </template>
              <span class="selected" :class="posting.voteType === 1 ? 'font-weight-medium' : ''">{{ t('userPosting.understandOnce') }}
                {{ posting.once }}</span>
            </v-btn>

            <v-btn :value="2" color="teal" class="px-3 border border-e-0" @click="upvote(posting, 2)">
              <template v-slot:prepend>
                <v-icon v-if="posting.voteType === 2">mdi-check</v-icon>
              </template>
              <span class="" :class="posting.voteType === 2 ? 'font-weight-medium' : ''">{{ t('userPosting.understandTwice') }} {{ posting.twice }}</span>
            </v-btn>

            <v-btn :value="3" color="brown" class="px-3 border" @click="upvote(posting, 3)">
              <template v-slot:prepend>
                <v-icon v-if="posting.voteType === 3">mdi-check</v-icon>
              </template>
              <span class="" :class="posting.voteType === 3 ? 'font-weight-medium' : ''">{{ t('userPosting.helpful') }} {{ posting.helpful
                }}</span>
            </v-btn>
          </v-btn-toggle>
        </template>

        <v-btn @click.stop.prevent="null" variant="plain" class="px-3"
          @click="props.type == 'list' ? emit('switchMainArea', 'detail', posting) : ''"
          prepend-icon="mdi-comment-outline" :ripple="false">
          <template v-slot:prepend><v-icon size="16"></v-icon></template>
          {{ posting.commentCount }}
        </v-btn>

        <!-- 阅读量显示 -->
        <v-btn variant="plain" class="px-3" :ripple="false">
          <template v-slot:prepend>
            <v-icon size="16">mdi-eye</v-icon>
          </template>
          {{ posting.views || 0 }}
        </v-btn>
      </div>
      <div class="">
        <v-btn variant="flat" rounded="lg" color="grey-lighten-3"
          @click="emit('switchMainArea', 'edit', posting)">{{ t('userPosting.edit') }}</v-btn>
        <v-btn class="ms-4" variant="flat" rounded="lg" color="grey-lighten-3"
          @click="deletePosting(posting.id)">{{ t('userPosting.delete') }}</v-btn>
      </div>
    </div>

  </div>
</template>

<style lang="scss">
@use "@/styles/style.scss" as *;

/* Basic editor styles */
.tiptap {
  :first-child {
    margin-top: 0;
  }

  /* List styles */
  ul,
  ol {
    padding: 0 1rem;
    margin: 1.25rem 1rem 1.25rem 0.4rem;

    li p {
      margin-top: 0.25em;
      margin-bottom: 0.25em;
    }
  }

  /* Heading styles */
  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    line-height: 1.1;
    margin-top: 2.5rem;
    text-wrap: pretty;
  }

  h1,
  h2 {
    margin-top: 3.5rem;
    margin-bottom: 1.5rem;
  }

  h1 {
    font-size: 1.4rem;
  }

  h2 {
    font-size: 1.2rem;
  }

  h3 {
    font-size: 1.1rem;
  }

  h4,
  h5,
  h6 {
    font-size: 1rem;
  }

  /* Code and preformatted text styles */
  code {
    background-color: var(--purple-light);
    border-radius: 0.4rem;
    color: var(--black);
    font-size: 0.85rem;
    padding: 0.25em 0.3em;
  }

  pre {
    background: var(--black);
    border-radius: 0.5rem;
    color: var(--white);
    font-family: 'JetBrainsMono', monospace;
    margin: 1.5rem 0;
    padding: 0.75rem 1rem;

    code {
      background: none;
      color: inherit;
      font-size: 0.8rem;
      padding: 0;
    }

    /* Code styling */
    .hljs-comment,
    .hljs-quote {
      color: #616161;
    }

    .hljs-variable,
    .hljs-template-variable,
    .hljs-attribute,
    .hljs-tag,
    .hljs-name,
    .hljs-regexp,
    .hljs-link,
    .hljs-name,
    .hljs-selector-id,
    .hljs-selector-class {
      color: #f98181;
    }

    .hljs-number,
    .hljs-meta,
    .hljs-built_in,
    .hljs-builtin-name,
    .hljs-literal,
    .hljs-type,
    .hljs-params {
      color: #fbbc88;
    }

    .hljs-string,
    .hljs-symbol,
    .hljs-bullet {
      color: #b9f18d;
    }

    .hljs-title,
    .hljs-section {
      color: #faf594;
    }

    .hljs-keyword,
    .hljs-selector-tag {
      color: #70cff8;
    }

    .hljs-emphasis {
      font-style: italic;
    }

    .hljs-strong {
      font-weight: 700;
    }
  }

  blockquote {
    border-left: 3px solid var(--gray-3);
    margin: 1.5rem 0;
    padding-left: 1rem;
  }

  hr {
    border: none;
    border-top: 1px solid var(--gray-2);
    cursor: pointer;
    margin: 2rem 0;

    &.ProseMirror-selectednode {
      border-top: 1px solid var(--purple);
    }
  }

  mark {
    background-color: #FAF594;
    border-radius: 0.4rem;
    box-decoration-break: clone;
    padding: 0.1rem 0.3rem;
  }

  a {
    color: var(--purple);
    cursor: pointer;

    &:hover {
      color: var(--purple-contrast);
    }
  }

  img {
    display: block;
    height: auto;
    margin: 1.5rem 0;
    max-width: 100%;

    &.ProseMirror-selectednode {
      outline: 3px solid var(--purple);
    }
  }

  table {
    border-collapse: collapse;
    margin: 0;
    overflow: hidden;
    table-layout: fixed;
    width: 100%;

    td,
    th {
      border: 1px solid var(--gray-3);
      box-sizing: border-box;
      min-width: 1em;
      padding: 6px 8px;
      position: relative;
      vertical-align: top;

      >* {
        margin-bottom: 0;
      }
    }

    th {
      background-color: var(--gray-1);
      font-weight: bold;
      text-align: left;
    }

    .selectedCell:after {
      background: var(--gray-2);
      content: "";
      left: 0;
      right: 0;
      top: 0;
      bottom: 0;
      pointer-events: none;
      position: absolute;
      z-index: 2;
    }

    .column-resize-handle {
      background-color: var(--purple);
      bottom: -2px;
      pointer-events: none;
      position: absolute;
      right: -2px;
      top: 0;
      width: 4px;
    }
  }

  .tableWrapper {
    margin: 1.5rem 0;
    overflow-x: auto;
  }

  &.resize-cursor {
    cursor: ew-resize;
    cursor: col-resize;
  }

  p.is-editor-empty:first-child::before {
    color: var(--gray-4);
    content: attr(data-placeholder);
    float: left;
    height: 0;
    pointer-events: none;
  }

  .Tiptap-mathematics-editor {
    background: #202020;
    color: #fff;
    font-family: monospace;
    padding: 0.2rem 0.5rem;
  }

  .Tiptap-mathematics-render {
    padding: 0 0.25rem;

    &--editable {
      cursor: pointer;
      transition: background 0.2s;

      &:hover {
        background: #eee;
      }
    }
  }

  .Tiptap-mathematics-editor,
  .Tiptap-mathematics-render {
    border-radius: 0.25rem;
    display: inline-block;
  }

  [aria-hidden="true"] {
    display: none;
  }
}
</style>
