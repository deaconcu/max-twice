import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import { roleApi } from '@/api/modules/role'
import { roleKeys } from './keys'
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

export function useRoleListQuery(mainCategory?: MaybeRef<number | undefined>, subCategory?: MaybeRef<number | undefined>) {
  return useInfiniteQuery({
    queryKey: computed(() => roleKeys.list(undefined, toValue(mainCategory), toValue(subCategory))),
    queryFn: ({ pageParam }) =>
      roleApi.getRolesByCategory(pageParam as string | undefined, toValue(mainCategory), toValue(subCategory)),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
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
