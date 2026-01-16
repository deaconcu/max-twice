/**
 * 平台统计数据类型定义
 * 参考：web-ts/src/types/stats.ts
 */

/**
 * 平台统计数据
 */
export interface PlatformStats {
  courseCount: number // 课程总数
  careerPathCount: number // 职业路径总数
  roadmapCount: number // 学习路线图总数
  knowledgeNodeCount: number // 知识节点总数
  articleCount: number // 文章数量
  lastUpdated: string // 最后更新时间
}

/**
 * 每日统计数据
 * 注意：DailyStatsDTO 已在 user.d.ts 中定义，这里仅作为重新导出
 */
export interface DailyStats {
  date: string // 格式: yyyy-MM-dd
  viewCount: number // 浏览量
  twiceCount: number // "两遍就懂"数量
  likeCount: number // "喜欢/有帮助"数量
  commentCount: number // 评论数量
}
