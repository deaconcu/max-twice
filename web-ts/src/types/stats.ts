/**
 * 平台统计数据类型定义
 * 基于后端 PlatformStatsDTO.java, DailyStatsDTO.java
 */

// 平台统计数据
export interface PlatformStats {
  courseCount: number          // 课程总数
  careerPathCount: number      // 职业路径总数
  roadmapCount: number         // 学习路线图总数
  knowledgeNodeCount: number   // 知识节点总数
  articleCount: number         // 文章数量
  lastUpdated: string          // 最后更新时间
}

// 每日统计数据 (已在 user.ts 中定义，这里重新导出)
export interface DailyStats {
  date: string                 // 格式: yyyy-MM-dd
  views: number               // 浏览量
  twice: number               // "两遍就懂"数量
  helpful: number             // "有帮助"数量
  comments: number            // 评论数量
}