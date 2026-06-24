import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import { userApi, followApi } from '@/api/modules/user'
import { userKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue, computed } from 'vue'

export function useCurrentUserQuery() {
  return useQuery({
    queryKey: userKeys.current(),
    queryFn: () => userApi.getCurrentUser(),
  })
}

export function useUserDetailQuery(username: MaybeRef<string>) {
  return useQuery({
    queryKey: userKeys.detail(toValue(username)),
    queryFn: () => userApi.getUser(toValue(username)),
  })
}

export function useUpdateCurrentUserMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      name,
      biography,
      avatar,
      timezone,
    }: {
      name: string
      biography: string
      avatar?: string
      timezone?: string
    }) => userApi.updateCurrentUser(name, biography, avatar, timezone),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userKeys.current() })
    },
  })
}

export function useUpdateAvatarMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => userApi.updateAvatar(file),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userKeys.current() })
    },
  })
}

export function useUserPostsQuery(userId: MaybeRef<number>, type = 2, enabled?: MaybeRef<boolean>) {
  return useInfiniteQuery({
    queryKey: computed(() => userKeys.posts(toValue(userId))),
    queryFn: ({ pageParam }) =>
      userApi.getUserPosts(toValue(userId), pageParam as string | undefined, type),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined,
    enabled,
  })
}

export function useMyPostsQuery(
  type?: MaybeRef<number | undefined>,
  state?: MaybeRef<number | undefined>,
  enabled?: MaybeRef<boolean>
) {
  return useInfiniteQuery({
    queryKey: computed(() => userKeys.myPosts(undefined, toValue(type), toValue(state))),
    queryFn: ({ pageParam }) =>
      userApi.getCurrentUserAllPosts(
        pageParam as string | undefined,
        toValue(type),
        toValue(state)
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined,
    enabled,
  })
}

export function useUserRoadmapsQuery(userId: MaybeRef<number>, enabled?: MaybeRef<boolean>) {
  return useInfiniteQuery({
    queryKey: computed(() => userKeys.roadmaps(toValue(userId))),
    queryFn: ({ pageParam }) =>
      userApi.getUserRoadmaps(toValue(userId), pageParam as string | undefined),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (last) =>
      Array.isArray(last) && last.length === 20
        ? (last[last.length - 1] as { id: number }).id.toString()
        : undefined,
    enabled,
  })
}

export function useMyRoadmapsQuery(
  state?: MaybeRef<string | undefined>,
  enabled?: MaybeRef<boolean>
) {
  return useInfiniteQuery({
    queryKey: computed(() => userKeys.myRoadmaps(undefined, toValue(state))),
    queryFn: ({ pageParam }) =>
      userApi.getCurrentUserRoadmaps(pageParam as string | undefined, toValue(state)),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (last) =>
      Array.isArray(last) && last.length === 20
        ? (last[last.length - 1] as { id: number }).id.toString()
        : undefined,
    enabled,
  })
}

export function useFolloweesQuery(userId: MaybeRef<number>) {
  return useQuery({
    queryKey: userKeys.followees(toValue(userId)),
    queryFn: () => followApi.getFollowees(toValue(userId)),
  })
}

export function useFollowMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (followeeId: number) => followApi.follow(followeeId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userKeys.all })
    },
  })
}

export function useUnfollowMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (followeeId: number) => followApi.unfollow(followeeId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userKeys.all })
    },
  })
}

export function useDeleteRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (roadmapId: number) => userApi.deleteRoadmap(roadmapId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userKeys.myRoadmaps() })
    },
  })
}
