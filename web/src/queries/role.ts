import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import { roleApi, type RoleUpdateRequest } from '@/api/modules/role'
import { roleKeys, userKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue, computed } from 'vue'

export function useRoleDetailQuery(id: MaybeRef<number>) {
  return useQuery({
    queryKey: roleKeys.detail(toValue(id)),
    queryFn: () => roleApi.getRole(toValue(id)),
  })
}

export function useHotRolesQuery() {
  return useQuery({
    queryKey: roleKeys.hot(),
    queryFn: () => roleApi.getHotRoles(),
  })
}

export function useRoleListQuery(
  mainCategory?: MaybeRef<number | undefined>,
  subCategory?: MaybeRef<number | undefined>
) {
  return useInfiniteQuery({
    queryKey: computed(() => roleKeys.list(undefined, toValue(mainCategory), toValue(subCategory))),
    queryFn: ({ pageParam }) =>
      roleApi.getRolesByCategory(
        pageParam as string | undefined,
        toValue(mainCategory),
        toValue(subCategory)
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined,
  })
}

export function useRoleSearchQuery(keyword: MaybeRef<string>) {
  return useQuery({
    queryKey: roleKeys.search(toValue(keyword)),
    queryFn: () => roleApi.searchRoles(toValue(keyword)),
    enabled: () => !!toValue(keyword),
  })
}

export function useCreateRoleMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: roleApi.createRole,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: roleKeys.all })
    },
  })
}

/**
 * 当前用户角色申请列表（"我的角色"）。state 仅 NEVER_PUBLISHED / PUBLISHED；BANNED 由后端拦截。
 */
export function useMyRolesQuery(state?: MaybeRef<string | undefined>, enabled?: MaybeRef<boolean>) {
  return useInfiniteQuery({
    queryKey: computed(() => userKeys.myRoles(undefined, toValue(state))),
    queryFn: ({ pageParam }) =>
      roleApi.getCurrentUserRoles(pageParam as string | undefined, toValue(state)),
    initialPageParam: undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined,
    enabled: enabled !== undefined ? () => toValue(enabled) ?? true : true,
  })
}

export function useResubmitRoleMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RoleUpdateRequest }) =>
      roleApi.resubmitRole(id, data),
    onSuccess: (_, { id }) => {
      void queryClient.invalidateQueries({ queryKey: roleKeys.detail(id) })
      void queryClient.invalidateQueries({ queryKey: ['users', 'me', 'roles'] })
    },
  })
}

export function useWithdrawRoleMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => roleApi.withdrawRole(id),
    onSuccess: (_, id) => {
      void queryClient.invalidateQueries({ queryKey: roleKeys.detail(id) })
      void queryClient.invalidateQueries({ queryKey: ['users', 'me', 'roles'] })
    },
  })
}
