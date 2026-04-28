import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import { progressApi } from '@/api/modules/progress'
import { progressKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue, computed } from 'vue'

export function useCourseProgressQuery(courseId: MaybeRef<number>) {
  return useQuery({
    queryKey: progressKeys.courseProgress(toValue(courseId)),
    queryFn: () => progressApi.getCourseProgress(toValue(courseId)),
  })
}

export function useAllCoursesProgressQuery(state?: MaybeRef<'learning' | 'completed' | undefined>) {
  return useInfiniteQuery({
    queryKey: progressKeys.allCourses(toValue(state)),
    queryFn: ({ pageParam }) => progressApi.getAllCourseProgress(toValue(state), pageParam as string | undefined),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => {
      if (!Array.isArray(lastPage) || lastPage.length < 20) return undefined
      return (lastPage[lastPage.length - 1] as { id: number }).id.toString()
    },
  })
}

export function useRoadmapProgressQuery(roadmapId: MaybeRef<number>) {
  return useQuery({
    queryKey: progressKeys.roadmapProgress(toValue(roadmapId)),
    queryFn: () => progressApi.getRoadmapProgress(toValue(roadmapId)),
  })
}

export function useAllRoadmapsProgressQuery(state?: MaybeRef<'learning' | 'completed' | undefined>) {
  return useInfiniteQuery({
    queryKey: progressKeys.allRoadmaps(toValue(state)),
    queryFn: ({ pageParam }) => progressApi.getUserRoadmaps(toValue(state), pageParam as string | undefined),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => {
      if (!Array.isArray(lastPage) || lastPage.length < 20) return undefined
      return (lastPage[lastPage.length - 1] as { id: number }).id.toString()
    },
  })
}

export function useNodeStatusQuery(nodeId: MaybeRef<number>) {
  return useQuery({
    queryKey: progressKeys.nodeStatus(toValue(nodeId)),
    queryFn: () => progressApi.getNodeStatus(toValue(nodeId)),
  })
}

export function useLearningRoadmapsByRoleQuery(roleId: MaybeRef<number>, enabled?: MaybeRef<boolean>) {
  return useQuery({
    queryKey: computed(() => progressKeys.learningRoadmapsByRole(toValue(roleId))),
    queryFn: () => progressApi.getLearningRoadmapsByRole(toValue(roleId)),
    enabled: enabled !== undefined ? () => toValue(enabled) : true,
  })
}

export function useStartCourseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (courseId: number) => progressApi.startCourse(courseId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: progressKeys.all })
    },
  })
}

export function useCancelCourseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (courseId: number) => progressApi.cancelCourse(courseId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: progressKeys.all })
    },
  })
}

export function useStartRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (roadmapId: number) => progressApi.startRoadmap(roadmapId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: progressKeys.all })
    },
  })
}

export function useCancelRoadmapMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (roadmapId: number) => progressApi.cancelRoadmap(roadmapId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: progressKeys.all })
    },
  })
}

export function useMarkNodeCompleteMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ nodeId, rootNodeId }: { nodeId: number; rootNodeId: number }) =>
      progressApi.markNodeComplete(nodeId, rootNodeId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: progressKeys.all })
    },
  })
}

export function useUnmarkNodeCompleteMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ nodeId, rootNodeId }: { nodeId: number; rootNodeId: number }) =>
      progressApi.unmarkNodeComplete(nodeId, rootNodeId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: progressKeys.all })
    },
  })
}

export function useDeleteCourseProgressMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (courseId: number) => progressApi.deleteCourseProgress(courseId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: progressKeys.all })
    },
  })
}
