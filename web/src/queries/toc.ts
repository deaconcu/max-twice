import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { tocApi } from '@/api/modules/toc'
import { pageKeys } from './keys'

export function useUpdateNodeTocMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ nodeId, indexArray }: { nodeId: number; indexArray: string }) =>
      tocApi.updateUserNodeToc(nodeId, indexArray),
    onSuccess: (_, { nodeId }) => {
      void queryClient.invalidateQueries({ queryKey: pageKeys.node(nodeId) })
    },
  })
}
