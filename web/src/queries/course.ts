import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/vue-query'
import { courseApi, subscriptionApi, type CourseUpdateRequest } from '@/api/modules/course'
import { courseKeys, userKeys } from './keys'
import type { MaybeRef } from 'vue'
import { computed, toValue } from 'vue'

export function useCourseDetailQuery(id: MaybeRef<number>) {
  return useQuery({
    queryKey: courseKeys.detail(toValue(id)),
    queryFn: () => courseApi.getCourse(toValue(id)),
  })
}

export function useHotCoursesQuery() {
  return useQuery({
    queryKey: courseKeys.hot(),
    queryFn: () => courseApi.getHotCourses(),
  })
}

export function useSubCoursesQuery(parentId: MaybeRef<number>) {
  return useQuery({
    queryKey: courseKeys.subcourses(toValue(parentId)),
    queryFn: () => courseApi.getSubCourses(toValue(parentId)),
  })
}

export function useCourseListQuery(
  mainCategory?: MaybeRef<number | undefined>,
  subCategory?: MaybeRef<number | undefined>
) {
  return useInfiniteQuery({
    queryKey: computed(() => courseKeys.list(toValue(mainCategory), toValue(subCategory))),
    queryFn: ({ pageParam }) =>
      courseApi.getCoursesByCategory(
        toValue(mainCategory),
        toValue(subCategory),
        pageParam as string | undefined
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined,
  })
}

export function useCourseSearchQuery(name: MaybeRef<string>) {
  return useQuery({
    queryKey: courseKeys.search(toValue(name)),
    queryFn: () => courseApi.searchCourses(toValue(name)),
    enabled: () => !!toValue(name),
  })
}

export function useCreateSubcourseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      parentId,
      name,
      description,
    }: {
      parentId: number
      name: string
      description: string
    }) => courseApi.createSubcourse(parentId, name, description),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: courseKeys.all })
    },
  })
}

export function useCreateCourseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: courseApi.createCourse,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: courseKeys.all })
    },
  })
}

export function useSubscribeMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (courseId: number) => subscriptionApi.subscribe(courseId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: courseKeys.all })
    },
  })
}

export function useUnsubscribeMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (courseId: number) => subscriptionApi.unsubscribe(courseId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: courseKeys.all })
    },
  })
}

/**
 * 当前用户课程申请列表（"我的课程"）。state 仅 NEVER_PUBLISHED / PUBLISHED；BANNED 由后端拦截。
 */
export function useMyCoursesQuery(
  state?: MaybeRef<string | undefined>,
  enabled?: MaybeRef<boolean>
) {
  return useInfiniteQuery({
    queryKey: computed(() => userKeys.myCourses(undefined, toValue(state))),
    queryFn: ({ pageParam }) =>
      courseApi.getCurrentUserCourses(pageParam as string | undefined, toValue(state)),
    initialPageParam: undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined,
    enabled: enabled !== undefined ? () => toValue(enabled) ?? true : true,
  })
}

export function useResubmitCourseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: CourseUpdateRequest }) =>
      courseApi.resubmitCourse(id, data),
    onSuccess: (_, { id }) => {
      void queryClient.invalidateQueries({ queryKey: courseKeys.detail(id) })
      void queryClient.invalidateQueries({ queryKey: ['users', 'me', 'courses'] })
    },
  })
}

export function useWithdrawCourseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => courseApi.withdrawCourse(id),
    onSuccess: (_, id) => {
      void queryClient.invalidateQueries({ queryKey: courseKeys.detail(id) })
      void queryClient.invalidateQueries({ queryKey: ['users', 'me', 'courses'] })
    },
  })
}
