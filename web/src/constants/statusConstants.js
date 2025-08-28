// 状态常量定义 - 与后端数据库字段对应

/**
 * 审批状态枚举 (Course和Profession通用)
 */
export const APPROVAL_STATE = {
  SUBMITTED: 0,    // 已提交
  APPROVED: 1,     // 已批准
  REJECTED: 2      // 已拒绝
};

/**
 * 进度状态枚举 (UserCourse和UserRoadmap通用)
 * 注意：后端字段名从status改为state
 */
export const PROGRESS_STATE = {
  NOT_STARTED: 0,  // 未开始
  IN_PROGRESS: 1,  // 进行中
  COMPLETED: 2     // 已完成
};

/**
 * PostStats类型枚举
 */
export const POST_STATS_TYPE = {
  POST: 0,         // 帖子
  ROADMAP: 1       // 学习路线
};

/**
 * 状态值到显示文本的映射
 */
export const APPROVAL_STATE_TEXT = {
  [APPROVAL_STATE.SUBMITTED]: '待审核',
  [APPROVAL_STATE.APPROVED]: '已批准', 
  [APPROVAL_STATE.REJECTED]: '已拒绝'
};

export const PROGRESS_STATE_TEXT = {
  [PROGRESS_STATE.NOT_STARTED]: '未开始',
  [PROGRESS_STATE.IN_PROGRESS]: '进行中',
  [PROGRESS_STATE.COMPLETED]: '已完成'
};

/**
 * 工具函数：获取状态对应的CSS类名
 */
export function getApprovalStateClass(state) {
  switch (state) {
    case APPROVAL_STATE.SUBMITTED:
      return 'status-submitted';
    case APPROVAL_STATE.APPROVED:
      return 'status-approved';
    case APPROVAL_STATE.REJECTED:
      return 'status-rejected';
    default:
      return 'status-unknown';
  }
}

export function getProgressStateClass(state) {
  switch (state) {
    case PROGRESS_STATE.NOT_STARTED:
      return 'progress-not-started';
    case PROGRESS_STATE.IN_PROGRESS:
      return 'progress-in-progress';
    case PROGRESS_STATE.COMPLETED:
      return 'progress-completed';
    default:
      return 'progress-unknown';
  }
}