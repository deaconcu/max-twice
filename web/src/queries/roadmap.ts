import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import { roadmapApi } from '@/api/modules/roadmap'
import { roadmapKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue, computed } from 'vue'

export function useRoadmapDetailQuery(
  id: MaybeRef<number>,
  options?: { enabled?: MaybeRef<boolean> }
) {
  return useQuery({
    queryKey: computed(() => roadmapKeys.detail(toValue(id))),
    queryFn: () => roadmapApi.getRoadmap(toValue(id)),
    enabled: options?.enabled !== undefined ? () => toValue(options.enabled!) : true,
  })
}

/**
 * 作者编辑视图（draft 优先 → current revision）
 */
export function useRoadmapEditQuery(
  id: MaybeRef<number>,
  options?: { enabled?: MaybeRef<boolean> }
) {
  return useQuery({
    queryKey: computed(() => [...roadmapKeys.detail(toValue(id)), 'edit'] as const),
    queryFn: () => roadmapApi.getRoadmapEdit(toValue(id)),
    enabled: options?.enabled !== undefined ? () => toValue(options.enabled!) : true,
  })
}

export function useRoleRoadmapsQuery(roleId: MaybeRef<number>, sortBy?: MaybeRef<string>) {
  return useInfiniteQuery({
    queryKey: computed(() => [...roadmapKeys.roleRoadmaps(toValue(roleId)), toValue(sortBy)]),
    queryFn: ({ pageParam }) =>
      roadmapApi.getRoleRoadmaps(toValue(roleId), pageParam as string | undefined, toValue(sortBy)),
    initialPageParam: undefined,
    getNextPageParam: (lastPage) => {
      if (!Array.isArray(lastPage) || lastPage.length < 20) return undefined
      return (lastPage[lastPage.length - 1] as { id: number }).id.toString()
    },
  })
}

export function useCreateRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      roleId,
      content,
      description,
      submit,
    }: {
      roleId: number
      content: string
      description: string
      submit: boolean
    }) => roadmapApi.createRoadmap(roleId, content, description, submit),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: roadmapKeys.all })
    },
  })
}

export function useUpdateRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      content,
      description,
    }: {
      id: number
      content: string
      description: string
    }) => roadmapApi.updateRoadmap(id, content, description),
    onSuccess: (_, { id }) => {
      void queryClient.invalidateQueries({ queryKey: roadmapKeys.detail(id) })
    },
  })
}

export function useSubmitRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => roadmapApi.submitRoadmap(id),
    onSuccess: (_, id) => {
      void queryClient.invalidateQueries({ queryKey: roadmapKeys.detail(id) })
    },
  })
}

export function useWithdrawRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => roadmapApi.withdrawRoadmap(id),
    onSuccess: (_, id) => {
      void queryClient.invalidateQueries({ queryKey: roadmapKeys.detail(id) })
    },
  })
}
