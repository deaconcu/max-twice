package com.prosper.learn.analytics.stats.service;

import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 内容统计业务服务
 *
 * 负责内容统计相关的业务逻辑，包括：
 * - 统计数据查询
 * - 统计数据汇总
 * - 排行榜业务逻辑
 * - 统计数据展示格式化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentStatsDomainService {

    private final ContentStatsDataService contentStatsDataService;

    /**
     * 获取内容统计数据
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 统计数据DTO
     */
    public ContentStatsDTO getContentStats(Enums.ContentType contentType, Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("内容ID无效: " + contentId);
        }

        Optional<ContentStatsDO> statsOpt = contentStatsDataService.getByContent(contentType, contentId);
        ContentStatsDO stats = statsOpt.orElse(new ContentStatsDO());

        // 构建统计DTO
        ContentStatsDTO.ContentStatsDTOBuilder builder = ContentStatsDTO.builder()
                .views(stats.getViews() != null ? stats.getViews() : 0)
                .comments(stats.getComments() != null ? stats.getComments() : 0)
                .shares(stats.getShares() != null ? stats.getShares() : 0)
                .bookmarks(stats.getBookmarks() != null ? stats.getBookmarks() : 0)
                .completedUsers(stats.getCompletedUsers() != null ? stats.getCompletedUsers() : 0)
                .inProgressUsers(stats.getInProgressUsers() != null ? stats.getInProgressUsers() : 0);

        // 帖子特殊处理：支持 twice 和 like 分别统计
        if (contentType == Enums.ContentType.post) {
            builder.twiceUpvotes(stats.getTwices() != null ? stats.getTwices() : 0)
                   .likeUpvotes(stats.getLikes() != null ? stats.getLikes() : 0);
        } else {
            // 其他内容类型只有 like 统计
            builder.twiceUpvotes(null)
                   .likeUpvotes(stats.getLikes() != null ? stats.getLikes() : 0);
        }

        return builder.build();
    }
}