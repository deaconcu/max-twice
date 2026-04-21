package com.twicemax.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Robot 队列统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RobotQueueStatsDTO {
    /**
     * 当前待处理任务数
     */
    private long pendingCount;

    /**
     * 今天已完成任务数
     */
    private long todayCompletedCount;

    /**
     * 最后执行时间
     */
    private String lastExecuteTime;

    /**
     * 队列状态：IDLE(空闲)/RUNNING(执行中)/PAUSED(已暂停)
     */
    private String status;
}
