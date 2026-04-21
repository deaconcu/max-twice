package com.twicemax.shared.domain.event.content.toc;

import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 用户ToC更新事件
 * 当用户修改目录组的第一个目录时触发（选择/取消选择/交换顺序）
 *
 * 触发条件：
 * - choose/unchoose: tocIndex = 1（第一个目录）
 * - updateUserNodeToc: 第一个目录的hash发生变化
 *
 * 用途：
 * - 更新 user_learning.nodes 字段
 * - 触发课程进度重新计算
 */
@Data
@AllArgsConstructor
public class TocChosenEvent {

    /** 用户ID */
    private Long userId;

    /** 节点ID（课程根节点） */
    private Long nodeId;

    /** 新的第一个目录的哈希值 */
    private String newFirstTocHash;
}
