import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import { postApi } from '@/api/modules/post'
import { postKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue, computed } from 'vue'

export function usePostDetailQuery(id: MaybeRef<number>) {
  return useQuery({
    queryKey: postKeys.detail(toValue(id)),
    queryFn: () => postApi.getPost(toValue(id)),
  })
}

export function useNodePostsQuery(nodeId: MaybeRef<number | undefined>) {
  return useInfiniteQuery({
    queryKey: computed(() => postKeys.nodeList(toValue(nodeId) ?? 0)),
    queryFn: ({ pageParam }) => postApi.getNodePosts(toValue(nodeId)!, pageParam as string | undefined),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
    enabled: () => !!toValue(nodeId),
  })
}

export function usePostsByIdsQuery(ids: MaybeRef<number[]>) {
  return useQuery({
    queryKey: postKeys.byIds(toValue(ids)),
    queryFn: () => postApi.getPostsByIds(toValue(ids)),
    enabled: () => toValue(ids).length > 0,
  })
}

export function useCreatePostMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: postApi.createPost,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.all })
    },
  })
}

export function useUpdatePostMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof postApi.updatePost>[1] }) =>
      postApi.updatePost(id, data),
    onSuccess: (_, { id }) => {
      void queryClient.invalidateQueries({ queryKey: postKeys.detail(id) })
    },
  })
}

export function useDeletePostMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => postApi.deletePost(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.all })
    },
  })
}

export function useOperateContentMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: Parameters<typeof postApi.operateContent>[0]) =>
      postApi.operateContent(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.all })
    },
  })
}
