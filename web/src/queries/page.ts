import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { pageApi } from '@/api/modules/page'
import { pageKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue } from 'vue'

/**
 * 读取内容页：根据 nodeId / courseId / path 自动选择 API
 */
export function useReadPageQuery(
  params: MaybeRef<{ nodeId?: number; courseId?: number; path?: string }>
) {
  const resolved = computed(() => toValue(params))
  return useQuery({
    queryKey: computed(() =>
      pageKeys.read(resolved.value.nodeId, resolved.value.courseId, resolved.value.path)
    ),
    queryFn: () => {
      const { nodeId, courseId, path } = resolved.value
      if (nodeId) {
        return pageApi.readByNode(nodeId, path ?? '')
      }
      if (courseId) {
        return pageApi.readByCoursePath(courseId, path ?? '')
      }
      return Promise.reject(new Error('nodeId or courseId is required'))
    },
    enabled: () => !!(resolved.value.nodeId || resolved.value.courseId),
  })
}

export function usePostDetailPageQuery(params: MaybeRef<{ postId?: number; commentId?: number }>) {
  return useQuery({
    queryKey: pageKeys.post(toValue(params)),
    queryFn: () => pageApi.readPostDetail(toValue(params)),
    enabled: () => {
      const p = toValue(params)
      return !!(p.postId || p.commentId)
    },
  })
}
