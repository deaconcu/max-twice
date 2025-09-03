import { defineStore } from 'pinia'

export const usePostingsStore = defineStore('postings', {
  state: () => ({
    listItems: [], // 列表数据
    scrollPosition: 0, // 滚动位置
  }),
  actions: {
    addItems(newItems) {
      this.listItems.push(...newItems)
    },
    setScrollPosition(position) {
      this.scrollPosition = position
    },
  },
})
