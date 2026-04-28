import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import { roadmapApi } from '@/api/modules/roadmap'
import { roadmapKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue, computed } from 'vue'

export function useRoadmapDetailQuery(id: MaybeRef<number>, options?: { enabled?: MaybeRef<boolean> }) {
  return useQuery({
    queryKey: computed(() => roadmapKeys.detail(toValue(id))),
    queryFn: () => roadmapApi.getRoadmap(toValue(id)),
    enabled: options?.enabled !== undefined ? () => toValue(options.enabled!) : true,
  })
}

export function useRoleRoadmapsQuery(roleId: MaybeRef<number>, sortBy?: MaybeRef<string>) {
  return useInfiniteQuery({
    queryKey: computed(() => [...roadmapKeys.roleRoadmaps(toValue(roleId)), toValue(sortBy)]),
    queryFn: ({ pageParam }) =>
      roadmapApi.getRoleRoadmaps(toValue(roleId), pageParam as string | undefined, toValue(sortBy)),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => {
      if (!Array.isArray(lastPage) || lastPage.length < 20) return undefined
      return (lastPage[lastPage.length - 1] as { id: number }).id.toString()
    },
  })
}

export function useCreateRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ roleId, content, description, state }: { roleId: number; content: string; description: string; state: number }) =>
      roadmapApi.createRoadmap(roleId, content, description, state),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: roadmapKeys.all })
    },
  })
}

export function useUpdateRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, content, description, state }: { id: number; content: string; description: string; state: number }) =>
      roadmapApi.updateRoadmap(id, content, description, state),
    onSuccess: (_, { id }) => {
      void queryClient.invalidateQueries({ queryKey: roadmapKeys.detail(id) })
    },
  })
}

export function usePinRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ roleId, roadmapId }: { roleId: number; roadmapId: number }) =>
      roadmapApi.pinRoadmap(roleId, roadmapId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: roadmapKeys.all })
    },
  })
}
