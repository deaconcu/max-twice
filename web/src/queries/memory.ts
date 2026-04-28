import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import * as memoryApi from '@/api/modules/memory'
import { memoryKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue, computed } from 'vue'

export function useReviewSummaryQuery(state?: MaybeRef<number | undefined>, enabled?: MaybeRef<boolean>) {
  return useQuery({
    queryKey: memoryKeys.reviewSummary(toValue(state)),
    queryFn: () => memoryApi.getReviewSummary(toValue(state)),
    enabled,
  })
}

export function useNextCardQuery(courseId?: MaybeRef<number | undefined>) {
  return useQuery({
    queryKey: memoryKeys.nextCard(toValue(courseId)),
    queryFn: () => memoryApi.getNextCard(toValue(courseId) ? { courseId: toValue(courseId) } : undefined),
  })
}

export function useDeckDetailQuery(deckId: MaybeRef<number>) {
  return useQuery({
    queryKey: memoryKeys.deckDetail(toValue(deckId)),
    queryFn: () => memoryApi.getDeckDetail(toValue(deckId)),
  })
}

export function useDeckDiffQuery(deckId: MaybeRef<number>) {
  return useQuery({
    queryKey: memoryKeys.deckDiff(toValue(deckId)),
    queryFn: () => memoryApi.getDeckDiff(toValue(deckId)),
  })
}

export function useDecksByNodeQuery(nodeId: MaybeRef<number>) {
  return useInfiniteQuery({
    queryKey: memoryKeys.decksByNode(toValue(nodeId)),
    queryFn: ({ pageParam }) =>
      memoryApi.getDecksByNode(toValue(nodeId), { cursor: pageParam as string | undefined }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
  })
}

export function usePostDecksQuery(postId: MaybeRef<number>, sortBy?: MaybeRef<string | undefined>) {
  return useInfiniteQuery({
    queryKey: computed(() => memoryKeys.postDecks(toValue(postId), toValue(sortBy))),
    queryFn: ({ pageParam }) =>
      memoryApi.getPostPublicDecks(toValue(postId), {
        cursor: pageParam as string | undefined,
        sortBy: toValue(sortBy),
        sortOrder: 'desc',
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
  })
}

export function usePostCreatorDeckQuery(postId: MaybeRef<number>, sortBy?: MaybeRef<string | undefined>) {
  return useInfiniteQuery({
    queryKey: computed(() => memoryKeys.postCreatorDeck(toValue(postId), toValue(sortBy))),
    queryFn: ({ pageParam }) =>
      memoryApi.getPostCreatorDeck(toValue(postId), {
        cursor: pageParam as string | undefined,
        sortBy: toValue(sortBy),
        sortOrder: 'desc',
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
  })
}

export function useMyPostDeckQuery(postId: MaybeRef<number>, sortBy?: MaybeRef<string | undefined>) {
  return useInfiniteQuery({
    queryKey: computed(() => memoryKeys.myPostDeck(toValue(postId), toValue(sortBy))),
    queryFn: ({ pageParam }) =>
      memoryApi.getMyPostDeck(toValue(postId), {
        cursor: pageParam as string | undefined,
        sortBy: toValue(sortBy),
        sortOrder: 'desc',
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
  })
}

export function useMyDecksQuery(state?: MaybeRef<number | undefined>, enabled?: MaybeRef<boolean>) {
  return useInfiniteQuery({
    queryKey: computed(() => memoryKeys.myDecks(toValue(state))),
    queryFn: ({ pageParam }) =>
      memoryApi.getCurrentUserDecks({ cursor: pageParam as string | undefined, state: toValue(state) }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
    enabled,
  })
}

export function useUserDecksQuery(userId: MaybeRef<number>, enabled?: MaybeRef<boolean>) {
  return useInfiniteQuery({
    queryKey: computed(() => memoryKeys.userDecks(toValue(userId))),
    queryFn: ({ pageParam }) =>
      memoryApi.getUserDecks(toValue(userId), { cursor: pageParam as string | undefined }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
    enabled,
  })
}

export function useCardListQuery(courseId: MaybeRef<number | undefined>) {
  return useInfiniteQuery({
    queryKey: memoryKeys.cardList(toValue(courseId)),
    queryFn: ({ pageParam }) =>
      memoryApi.getCardList({ courseId: toValue(courseId), cursor: pageParam as string | undefined }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
  })
}

export function useReviewCardMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: memoryApi.reviewCard,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useCreateDeckMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: memoryApi.createDeck,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useDeleteDeckMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (deckId: number) => memoryApi.deleteDeck(deckId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useDeleteCardsMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (cardIds: number[]) => memoryApi.deleteCards(cardIds),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useUpvoteDeckMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (deckId: number) => memoryApi.upvoteDeck(deckId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useAddDeckToMemoryBankMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ deckId, courseId }: { deckId: number; courseId: number }) =>
      memoryApi.addDeckToMemoryBank({ deckId, courseId }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useAddCardToStudyMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (cardId: number) => memoryApi.addCardToStudy(cardId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useResetCardProgressMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (cardIds: number[]) => memoryApi.resetCardProgress(cardIds),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useUpdateCourseMemorySettingMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (params: Parameters<typeof memoryApi.updateCourseMemorySetting>[0]) =>
      memoryApi.updateCourseMemorySetting(params),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useDeleteCardMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (cardId: number) => memoryApi.deleteCard(cardId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useUpdateCardMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ cardId, data }: { cardId: number; data: { front: string; back: string } }) =>
      memoryApi.updateCard(cardId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useCreateCardMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: { deckId: number; front: string; back: string }) =>
      memoryApi.createCard(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useAcceptDeckChangesMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      deckId,
      cardIds,
      courseId,
      removeOtherDeckCards,
    }: {
      deckId: number
      cardIds: number[]
      courseId?: number
      removeOtherDeckCards?: boolean
    }) => memoryApi.acceptDeckChanges(deckId, cardIds, courseId, removeOtherDeckCards),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useRemoveCardsFromStudyMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (cardIds: number[]) => memoryApi.removeCardsFromStudy(cardIds),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}

export function useMoveNodeToCourseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ nodeId, courseId }: { nodeId: number; courseId: number }) =>
      memoryApi.moveNodeToCourse(nodeId, courseId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memoryKeys.all })
    },
  })
}
