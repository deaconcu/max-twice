/**
 * 帖子相关的工具函数
 */

/**
 * 节点信息接口
 */
export interface NodeInfo {
  id: number
  name: string
  description?: string
}

/**
 * 解析目录内容
 * 目录类型的 post，content 字段是 JSON 格式的节点列表
 * 格式：[{"id": 1, "name": "章节名", "description": "描述"}, ...]
 *
 * @param content - 目录内容字符串
 * @returns 解析后的节点列表
 */
export function parseContentNodes(content: string): NodeInfo[] {
  if (!content) {
    return []
  }

  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed)) {
      return parsed
    }
    return []
  } catch (e) {
    // 向后兼容：如果解析失败，尝试按逗号分割
    return content.split(',').map((item: string, index: number) => ({
      id: index,
      name: item.trim(),
      description: '',
    }))
  }
}

/**
 * 格式化目录内容为纯文本摘要
 * @param content - 目录内容字符串
 * @param maxLength - 最大长度
 * @returns 格式化后的文本
 */
export function formatContentSummary(content: string, maxLength = 100): string {
  const nodes = parseContentNodes(content)
  if (nodes.length === 0) {
    return ''
  }

  const summary = nodes.map((node) => node.name).join('、')
  if (summary.length > maxLength) {
    return summary.substring(0, maxLength) + '...'
  }
  return summary
}
