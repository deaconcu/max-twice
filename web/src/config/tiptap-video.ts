/**
 * TipTap 视频扩展
 * 支持 YouTube、Bilibili、Vimeo 嵌入和直接视频链接
 */

import { Node, mergeAttributes } from '@tiptap/core'
import type { Editor } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import { defineComponent, h, computed } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'

/**
 * 视频平台类型
 */
type VideoPlatform = 'youtube' | 'bilibili' | 'vimeo' | 'video'

/**
 * 解析视频 URL，返回平台类型和嵌入地址
 */
function parseVideoUrl(url: string): { platform: VideoPlatform; embedUrl: string } | null {
  const trimmedUrl = url.trim()
  if (!trimmedUrl) return null

  // YouTube
  // 支持格式：
  // - https://www.youtube.com/watch?v=VIDEO_ID
  // - https://youtu.be/VIDEO_ID
  // - https://www.youtube.com/embed/VIDEO_ID
  const youtubeMatch =
    /(?:youtube\.com\/(?:watch\?v=|embed\/)|youtu\.be\/)([a-zA-Z0-9_-]{11})/.exec(trimmedUrl)
  if (youtubeMatch?.[1]) {
    return {
      platform: 'youtube',
      embedUrl: `https://www.youtube.com/embed/${youtubeMatch[1]}`,
    }
  }

  // Bilibili
  // 支持格式：
  // - https://www.bilibili.com/video/BV1xxxxxxxxx
  // - https://www.bilibili.com/video/av123456
  // - https://player.bilibili.com/player.html?bvid=BV1xxxxxxxxx
  const bilibiliMatch =
    /bilibili\.com\/(?:video\/(BV[a-zA-Z0-9]+|av\d+)|player\.html\?(?:.*&)?bvid=(BV[a-zA-Z0-9]+))/.exec(
      trimmedUrl
    )
  if (bilibiliMatch) {
    const videoId = bilibiliMatch[1] ?? bilibiliMatch[2]
    if (videoId?.startsWith('BV')) {
      return {
        platform: 'bilibili',
        embedUrl: `https://player.bilibili.com/player.html?bvid=${videoId}&autoplay=0`,
      }
    } else if (videoId) {
      // av 号
      return {
        platform: 'bilibili',
        embedUrl: `https://player.bilibili.com/player.html?aid=${videoId.replace('av', '')}&autoplay=0`,
      }
    }
  }

  // Vimeo
  // 支持格式：
  // - https://vimeo.com/123456789
  // - https://player.vimeo.com/video/123456789
  const vimeoMatch = /vimeo\.com\/(?:video\/)?(\d+)/.exec(trimmedUrl)
  if (vimeoMatch?.[1]) {
    return {
      platform: 'vimeo',
      embedUrl: `https://player.vimeo.com/video/${vimeoMatch[1]}`,
    }
  }

  // 直接视频链接
  // 支持 mp4, webm, ogg
  if (/\.(mp4|webm|ogg)(\?.*)?$/i.test(trimmedUrl)) {
    return {
      platform: 'video',
      embedUrl: trimmedUrl,
    }
  }

  return null
}

interface VideoNodeAttrs {
  platform: VideoPlatform
  embedUrl: string
}

/**
 * 视频 Vue 组件
 */
const VideoBlockComponent = defineComponent({
  name: 'VideoBlockComponent',
  props: {
    node: {
      type: Object,
      required: true,
    },
    selected: {
      type: Boolean,
      default: false,
    },
    editor: {
      type: Object as () => Editor,
      required: true,
    },
    updateAttributes: {
      type: Function,
      required: true,
    },
    deleteNode: {
      type: Function,
      required: true,
    },
  },
  setup(props) {
    const platform = computed(() => (props.node.attrs as VideoNodeAttrs).platform)
    const embedUrl = computed(() => (props.node.attrs as VideoNodeAttrs).embedUrl)

    return () => {
      const content =
        platform.value === 'video'
          ? h('video', {
              src: embedUrl.value,
              controls: true,
              class: 'video-player',
            })
          : h('iframe', {
              src: embedUrl.value,
              frameborder: '0',
              allowfullscreen: true,
              allow:
                'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture',
              class: 'video-iframe',
            })

      return h(
        NodeViewWrapper,
        {
          class: ['video-node', { 'video-selected': props.selected }],
        },
        () => h('div', { class: 'video-wrapper' }, [content])
      )
    }
  },
})

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    videoBlock: {
      insertVideo: (options: { src: string }) => ReturnType
    }
  }
}

/**
 * 视频扩展
 */
export const VideoBlock = Node.create({
  name: 'videoBlock',

  group: 'block',

  atom: true,

  addAttributes() {
    return {
      src: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-src') ?? '',
        renderHTML: (attributes: Record<string, unknown>) => ({
          'data-src': attributes.src,
        }),
      },
      platform: {
        default: 'video',
        parseHTML: (element) => element.getAttribute('data-platform') ?? 'video',
        renderHTML: (attributes: Record<string, unknown>) => ({
          'data-platform': attributes.platform,
        }),
      },
      embedUrl: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-embed-url') ?? '',
        renderHTML: (attributes: Record<string, unknown>) => ({
          'data-embed-url': attributes.embedUrl,
        }),
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'div[data-type="video-block"]',
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'video-block' })]
  },

  addNodeView() {
    return VueNodeViewRenderer(VideoBlockComponent as any)
  },

  addCommands() {
    return {
      insertVideo:
        (options: { src: string }) =>
        ({ commands }: { commands: any }) => {
          const parsed = parseVideoUrl(options.src)
          if (!parsed) return false

          return commands.insertContent({
            type: this.name,
            attrs: {
              src: options.src,
              platform: parsed.platform,
              embedUrl: parsed.embedUrl,
            },
          })
        },
    } as any
  },
})

/**
 * 导出解析函数供外部使用
 */
export { parseVideoUrl }
