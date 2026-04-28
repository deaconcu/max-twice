import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { upvoteApi } from '@/api/modules/upvote'
import { bookmarkApi } from '@/api/modules/bookmark'
import { upvoteKeys, bookmarkKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue } from 'vue'
import type { ObjectType, VoteType } from '@/enums'

export function useUpvoteStatusQuery(objectId: MaybeRef<number>, objectType: MaybeRef<ObjectType>) {
  return useQuery({
    queryKey: upvoteKeys.status(toValue(objectId), toValue(objectType)),
    queryFn: () => upvoteApi.getUpvoteStatus(toValue(objectId), toValue(objectType)),
  })
}

export function useUpvoteMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ objectId, objectType, type }: { objectId: number; objectType: ObjectType; type: VoteType }) =>
      upvoteApi.upvote(objectId, objectType, type),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: upvoteKeys.all })
    },
  })
}

export function useBookmarkToggleMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ contentType, contentId }: { contentType: Parameters<typeof bookmarkApi.toggle>[0]; contentId: number }) =>
      bookmarkApi.toggle(contentType, contentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: bookmarkKeys.all })
    },
  })
}

export function useBookmarksQuery(contentType: MaybeRef<Parameters<typeof bookmarkApi.getBookmarks>[0]>) {
  return useQuery({
    queryKey: bookmarkKeys.list(toValue(contentType)),
    queryFn: () => bookmarkApi.getBookmarks(toValue(contentType)),
  })
}
