import { computed } from 'vue'
import { useQuery, useMutation, useInfiniteQuery } from '@tanstack/vue-query'
import { messageApi } from '@/api/modules/message'
import { messageKeys } from './keys'
import type { MaybeRef } from 'vue'
import { toValue } from 'vue'
import type { MessageType } from '@/enums'

export function useMessagesQuery(category: MaybeRef<number>, type?: MessageType) {
  return useInfiniteQuery({
    queryKey: computed(() => messageKeys.byCategory(toValue(category))),
    queryFn: ({ pageParam }) =>
      messageApi.getMessagesByCategory(toValue(category), pageParam as string | undefined, type),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => {
      const msgs = lastPage.messages
      if (!msgs || msgs.length < 10) return undefined
      return msgs[msgs.length - 1]?.id?.toString()
    },
  })
}

export function useUnreadCountQuery() {
  return useQuery({
    queryKey: messageKeys.unreadCount(),
    queryFn: () => messageApi.getUnreadCount(),
    refetchInterval: 30000,
    initialData: 0,
  })
}

export function useInviteUserMutation() {
  return useMutation({
    mutationFn: ({ userId, nodeId }: { userId: number; nodeId: number }) =>
      messageApi.inviteUser(userId, nodeId),
  })
}
