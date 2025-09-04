import { defineStore } from 'pinia'

interface PostingsState {
  listItems: any[]
  scrollPosition: number
}

export const usePostingsStore = defineStore('postings', {
  state: (): PostingsState => ({
    listItems: [], // 列表数据
    scrollPosition: 0, // 滚动位置
  }),
  actions: {
    addItems(newItems: any[]): void {
      this.listItems.push(...newItems)
    },
    setScrollPosition(position: number): void {
      this.scrollPosition = position
    },
  },
})