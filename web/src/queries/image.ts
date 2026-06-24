import { useMutation, useQuery } from '@tanstack/vue-query'
import { imageApi } from '@/api/modules/image'
import { imageKeys } from './keys'

export function useImageUploadMutation() {
  return useMutation({
    mutationFn: ({ file, refType }: { file: File; refType: string }) =>
      imageApi.upload(file, refType),
  })
}

export function useImageQuotaQuery() {
  return useQuery({
    queryKey: imageKeys.quota(),
    queryFn: () => imageApi.getQuota(),
  })
}
