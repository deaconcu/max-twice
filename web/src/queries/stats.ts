import { useQuery } from '@tanstack/vue-query'
import { statsApi } from '@/api/modules/stats'
import { statsKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue } from 'vue'

export function useHeatmapQuery(userId: MaybeRef<number>, months: MaybeRef<number> = 12) {
  return useQuery({
    queryKey: statsKeys.heatmap(toValue(userId), toValue(months)),
    queryFn: () => statsApi.getHeatmap(toValue(userId), toValue(months)),
  })
}

export function useAllTimeStatsQuery(userId: MaybeRef<number>) {
  return useQuery({
    queryKey: statsKeys.allTime(toValue(userId)),
    queryFn: () => statsApi.getUserAllTimeStats(toValue(userId)),
  })
}

export function useHistoryStatsQuery(userId: MaybeRef<number>, days: MaybeRef<number> = 7) {
  return useQuery({
    queryKey: statsKeys.history(toValue(userId), toValue(days)),
    queryFn: () => statsApi.getUserHistoryStats(toValue(userId), toValue(days)),
  })
}

export function useTodayStatsQuery(userId: MaybeRef<number>) {
  return useQuery({
    queryKey: statsKeys.today(toValue(userId)),
    queryFn: () => statsApi.getUserTodayStats(toValue(userId)),
  })
}

export function usePlatformStatsQuery() {
  return useQuery({
    queryKey: statsKeys.platform(),
    queryFn: () => statsApi.getPlatformStats(),
  })
}
