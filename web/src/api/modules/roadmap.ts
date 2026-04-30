import apiClient from '../client'
import type { Roadmap } from '@/types/roadmap'

/**
 * 路线图保存（创建/更新/提交）的返回结果。
 *
 * - 草稿场景（submit=false）：即使引用失效也允许落库，失效引用通过 invalidReferences 反馈
 * - 提交场景（submit=true）：引用失效会抛 ROADMAP_CONTENT_INVALID（异常 details 携带失效列表）
 */
export interface RoadmapSaveResult {
  id: number
  invalidReferences?: {
    missingCourseIds: number[]
    missingNodeIds: number[]
  }
}

/**
 * 编辑页响应（GET /roadmaps/{id}/edit）
 */
export interface RoadmapEditView {
  id: number
  roleId: number
  description: string | null
  /** 主体状态：NEVER_PUBLISHED / PUBLISHED / BANNED */
  state: 'NEVER_PUBLISHED' | 'PUBLISHED' | 'BANNED'
  /** 当前可编辑内容（已回填 c/n label）。BANNED 时为 null。 */
  content: string | null
  /** content 来源：DRAFT / CURRENT / EMPTY。 */
  contentSource: 'DRAFT' | 'CURRENT' | 'EMPTY'
  contentUpdatedAt: string | null
  /** 审核中版本（无则为 null） */
  pending: {
    revisionId: number
    revisionNo: number
    submittedAt: string | null
  } | null
  /** 最近一次被驳回的版本信息（无则为 null） */
  lastReject: {
    revisionId: number
    revisionNo: number
    reason: string
    reviewedAt: string | null
  } | null
}

/**
 * 路线图管理相关 API（revision 模型）
 */
export const roadmapApi = {
  /**
   * 获取角色的路线图列表
   */
  getRoleRoadmaps(roleId: number, cursor?: string, sortBy?: string): Promise<Roadmap[]> {
    const params: Record<string, string> = {}
    if (cursor != null) params.cursor = cursor
    if (sortBy) params.sortBy = sortBy
    return apiClient.get(`/roles/${String(roleId)}/roadmaps`, { params })
  },

  /**
   * 创建路线图
   * - submit=true：创建后立即提交审核
   * - submit=false：仅保存草稿
   */
  createRoadmap(
    roleId: number,
    content: string,
    description: string,
    submit: boolean
  ): Promise<RoadmapSaveResult> {
    return apiClient.post('/roadmaps', {
      roleId,
      content,
      description,
      submit,
    })
  },

  /**
   * 保存草稿（不会触发审核）
   */
  updateRoadmap(id: number, content: string, description: string): Promise<RoadmapSaveResult> {
    return apiClient.put(`/roadmaps/${String(id)}`, { content, description })
  },

  /**
   * 提交当前 draft 进入审核
   */
  submitRoadmap(id: number): Promise<RoadmapSaveResult> {
    return apiClient.post(`/roadmaps/${String(id)}/submit`)
  },

  /**
   * 作者撤回 pending revision
   */
  withdrawRoadmap(id: number): Promise<void> {
    return apiClient.post(`/roadmaps/${String(id)}/withdraw`)
  },

  /**
   * 获取路线图详情（公共展示）
   */
  getRoadmap(id: number): Promise<Roadmap> {
    return apiClient.get(`/roadmaps/${String(id)}`)
  },

  /**
   * 获取作者编辑视图（draft 优先，回退到 current revision）
   */
  getRoadmapEdit(id: number): Promise<RoadmapEditView> {
    return apiClient.get(`/roadmaps/${String(id)}/edit`)
  },

  /**
   * 删除路线图
   */
  deleteRoadmap(id: number): Promise<void> {
    return apiClient.delete(`/roadmaps/${String(id)}`)
  },
}
