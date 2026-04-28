import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import { commentApi } from '@/api/modules/comment'
import { commentKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue } from 'vue'
import type { ObjectType } from '@/enums'

export function useCommentListQuery(
  objectId: MaybeRef<number>,
  objectType: MaybeRef<ObjectType>,
  enabled?: MaybeRef<boolean>
) {
  return useInfiniteQuery({
    queryKey: commentKeys.list(toValue(objectId), toValue(objectType)),
    queryFn: ({ pageParam }) =>
      commentApi.getComments(toValue(objectId), toValue(objectType), pageParam as string | undefined),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
    enabled: () => (enabled === undefined ? true : !!toValue(enabled)),
  })
}

export function useCommentRepliesQuery(commentId: MaybeRef<number>) {
  return useInfiniteQuery({
    queryKey: commentKeys.replies(toValue(commentId)),
    queryFn: ({ pageParam }) =>
      commentApi.getCommentReplies(toValue(commentId), pageParam as string | undefined),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
  })
}

export function useCommentBasicQuery(id: MaybeRef<number>) {
  return useQuery({
    queryKey: commentKeys.basic(toValue(id)),
    queryFn: () => commentApi.getCommentBasic(toValue(id)),
  })
}

export function useCommentContextQuery(id: MaybeRef<number | null>) {
  return useQuery({
    queryKey: commentKeys.context(toValue(id)!),
    queryFn: () => commentApi.getCommentContext(toValue(id)!),
    enabled: () => !!toValue(id),
  })
}

export function useCreateCommentMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      objectId,
      objectType,
      replyTo,
      toUser,
      content,
    }: {
      objectId: number
      objectType: ObjectType
      replyTo?: number | null
      toUser?: number | null
      content?: string
    }) => commentApi.createComment(objectId, objectType, replyTo, toUser, content),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: commentKeys.all })
    },
  })
}
