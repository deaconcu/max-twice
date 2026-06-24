import { useQuery } from '@tanstack/vue-query'
import { homeApi } from '@/api/modules/home'
import { homeKeys } from './keys'

export function useHomePageQuery(enabled = true) {
  return useQuery({
    queryKey: homeKeys.page(),
    queryFn: () => homeApi.getHomePageData(),
    enabled,
  })
}
