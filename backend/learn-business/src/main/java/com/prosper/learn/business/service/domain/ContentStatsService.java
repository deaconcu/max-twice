package com.prosper.learn.business.service.domain;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.dto.response.UpvoteStatusDTO;
import com.prosper.learn.persistence.dataobject.ContentStatsDO;
import com.prosper.learn.persistence.dataobject.UpvoteDO;
import com.prosper.learn.business.service.data.ContentStatsDataService;
import com.prosper.learn.business.service.data.UpvoteDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 内容统计业务服务
 *
 * 负责内容统计相关的业务逻辑，包括：
 * - 点赞状态查询
 * - 统计数据汇总
 * - 排行榜业务逻辑
 * - 统计数据展示格式化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentStatsService {

    private final ContentStatsDataService contentStatsDataService;
    private final UpvoteDataService upvoteDataService;

    /**
     * 获取用户对指定对象的点赞状态（重载方法，支持int类型的objectType）
     *
     * @param objectId 对象ID
     * @param objectType 对象类型（int值）
     * @param userId 用户ID
     * @return 点赞状态DTO
     * @throws BusinessException 当参数无效时抛出异常
     */
    public UpvoteStatusDTO getUpvoteStatus(Long objectId, int objectType, long userId) {
        // 转换int类型的objectType为枚举
        Enums.ContentType contentType = Enums.ContentType.fromValue(objectType);
        if (contentType == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("无效的对象类型: " + objectType);
        }

        // 调用主方法
        return getUpvoteStatus(objectId, contentType, userId);
    }

    /**
     * 获取用户对指定对象的点赞状态
     *
     * @param objectId 对象ID
     * @param contentType 内容类型
     * @param userId 用户ID
     * @return 点赞状态DTO
     * @throws BusinessException 当参数无效时抛出异常
     */
    public UpvoteStatusDTO getUpvoteStatus(Long objectId, Enums.ContentType contentType, long userId) {
        // 参数验证
        if (objectId == null || objectId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("对象ID无效: " + objectId);
        }
        if (userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID无效: " + userId);
        }

        // 查询用户点赞记录
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, objectId, contentType.value());

        // 从 contentStats 表获取统计数据
        Optional<ContentStatsDO> statsOpt = contentStatsDataService.getByContent(contentType, objectId);
        ContentStatsDO stats = statsOpt.orElse(new ContentStatsDO());

        // 构建状态DTO
        Integer upvotes = stats.getLikes() != null ? stats.getLikes() : 0;
        Integer twiceUpvotes = null;
        Integer helpfulUpvotes = null;
        Boolean upvoted = upvoteDO != null;
        Boolean twiceUpvoted = false;
        Boolean helpfulUpvoted = false;

        // 帖子特殊处理：支持 twice 和 helpful 分别统计
        if (contentType == Enums.ContentType.post) {
            twiceUpvotes = stats.getTwice() != null ? stats.getTwice() : 0;
            helpfulUpvotes = stats.getLikes() != null ? stats.getLikes() : 0;
            upvotes = twiceUpvotes + helpfulUpvotes;

            // 检查用户具体的点赞类型
            if (upvoteDO != null) {
                twiceUpvoted = upvoteDO.getType() == Enums.VoteType.twice.value();
                helpfulUpvoted = upvoteDO.getType() == Enums.VoteType.like.value();
            }
        }

        return UpvoteStatusDTO.builder()
                .objectId(objectId)
                .objectType(contentType.value())
                .upvotes(upvotes)
                .upvoted(upvoted)
                .twiceUpvotes(twiceUpvotes)
                .twiceUpvoted(twiceUpvoted)
                .helpfulUpvotes(helpfulUpvotes)
                .helpfulUpvoted(helpfulUpvoted)
                .build();
    }

    /**
     * 获取内容统计数据
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 统计数据，如果不存在则返回空
     */
    public Optional<ContentStatsDO> getContentStats(Enums.ContentType contentType, Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("内容ID无效: " + contentId);
        }

        return contentStatsDataService.getByContent(contentType, contentId);
    }

    /**
     * 获取或创建内容统计记录
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 统计数据
     */
    public ContentStatsDO getOrCreateContentStats(Enums.ContentType contentType, Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("内容ID无效: " + contentId);
        }

        return contentStatsDataService.getOrCreate(contentType, contentId);
    }
}