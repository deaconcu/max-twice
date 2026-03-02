package com.prosper.learn.interaction.comment;

import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.prosper.learn.shared.domain.Enums.ContentState.*;

/**
 * 评论领域服务
 * 只依赖 interaction 模块，处理评论的核心业务逻辑
 */
@Service
@RequiredArgsConstructor
public class CommentDomainService {

    private final CommentDataService commentDataService;
    private final SystemProperties systemProperties;

    /**
     * 创建评论（完整业务逻辑）
     * 包含 interaction 领域内的所有验证
     *
     * @param objectId 被评论对象ID
     * @param objectType 被评论对象类型
     * @param replyToCommentId 回复的评论ID（0表示不是回复）
     * @param toUserId 回复给的用户ID（0表示没有特定接收者）
     * @param content 评论内容
     * @param creatorId 评论创建者ID
     * @return 创建后的评论对象
     */
    @Transactional
    public CommentDO createComment(Long objectId, Integer objectType, Long replyToCommentId, Long toUserId, String content, Long creatorId) {
        // 验证 replyToCommentId
        if (replyToCommentId != null && replyToCommentId > 0) {
            CommentDO replyToComment = commentDataService.validateAndGet(replyToCommentId);
            // 确保回复的评论属于同一个对象
            if (!replyToComment.getObjectId().equals(objectId) ||
                !replyToComment.getObjectType().equals(objectType)) {
                throw StatusCode.INVALID_PARAMETER.exception("回复的评论不属于当前对象");
            }
        }

        // 构建 CommentDO
        CommentDO commentDO = new CommentDO();
        commentDO.setObjectId(objectId);
        commentDO.setObjectType(objectType);
        commentDO.setReplyToCommentId(replyToCommentId == null ? 0 : replyToCommentId);
        commentDO.setToUserId(toUserId == null ? 0 : toUserId);
        commentDO.setContent(content);
        commentDO.setCreatorId(creatorId);
        commentDO.setState(SUBMITTED.value());
        commentDO.setScore(0.0);

        // 插入数据库
        commentDataService.insert(commentDO);
        return commentDataService.getById(commentDO.getId());
    }

    /**
     * 审核通过评论
     * @return 更新后的评论对象
     */
    @Transactional
    public CommentDO approveComment(Long id) {
        CommentDO commentDO = commentDataService.validateAndGet(id);

        if (commentDO.getState() != PUBLISHED.value()) {
            commentDO.setState(PUBLISHED.value());
            commentDO.setReason(null);  // 清空拒绝原因
            commentDataService.update(commentDO);
        }

        return commentDO;
    }

    /**
     * 拒绝评论
     */
    @Transactional
    public void rejectComment(Long id, String reason) {
        commentDataService.validateAndGet(id);
        commentDataService.reject(id, reason);
    }

    /**
     * 封禁评论
     */
    @Transactional
    public void banComment(Long id, String reason) {
        commentDataService.validateAndGet(id);
        commentDataService.ban(id, reason);
    }

    /**
     * 获取对象的评论列表（分页）
     * @return List<CommentDO>
     */
    public List<CommentDO> getCommentsByObject(Long objectId, Integer type, Double lastScore, Long lastId, int pageSize) {
        int querySize = pageSize + 1;

        List<CommentDO> commentDOList;
        // 第一页：lastScore 和 lastId 都为 null
        if (lastScore == null && lastId == null) {
            commentDOList = commentDataService.getByObjectId(objectId, type, querySize);
        } else {
            // 后续页：需要同时传递 lastScore 和 lastId
            if (lastScore == null || lastId == null) {
                throw StatusCode.INVALID_PARAMETER.exception("分页查询需要同时提供 lastScore 和 lastId");
            }
            commentDOList = commentDataService.getByObjectIdPaginated(objectId, type, lastScore, lastId, querySize);
        }

        return commentDOList;
    }

    /**
     * 获取评论的子评论（每个父评论只返回一条分数最高的子评论）
     * @param parentCommentIds 父评论ID列表
     * @return 子评论列表
     */
    public List<CommentDO> getChildren(List<Long> parentCommentIds) {
        if (parentCommentIds == null || parentCommentIds.isEmpty()) {
            return new ArrayList<>();
        }
        return commentDataService.getChildren(parentCommentIds);
    }

    /**
     * 获取子评论列表（分页）
     * @return List<CommentDO>
     */
    public List<CommentDO> getCommentReplies(Long id, Double lastScore, Long lastId, int pageSize) {
        int querySize = pageSize + 1;

        List<CommentDO> commentDOList;
        // 第一页：lastScore 和 lastId 都为 null
        if (lastScore == null && lastId == null) {
            commentDOList = commentDataService.getByTopic(id, querySize);
        } else {
            // 后续页：需要同时传递 lastScore 和 lastId
            if (lastScore == null || lastId == null) {
                throw StatusCode.INVALID_PARAMETER.exception("分页查询需要同时提供 lastScore 和 lastId");
            }
            commentDOList = commentDataService.getByTopicPaginated(id, lastScore, lastId, querySize);
        }

        return commentDOList;
    }

    /**
     * 根据状态获取评论列表
     */
    public List<CommentDO> listByState(Byte state, Long lastId, int pageSize) {
        return commentDataService.listByState(state, lastId, pageSize);
    }

    /**
     * 高级筛选评论列表
     */
    public List<CommentDO> listByFilter(Integer objectType, Long objectId, Long creatorId, Long lastId, int pageSize) {
        return commentDataService.listByFilter(objectType, objectId, creatorId, lastId, pageSize);
    }

    /**
     * 根据状态字符串获取评论列表（分页）
     * @param state pending(待审核), approved(已通过), rejected(已拒绝), banned(已封禁)
     * @return List<CommentDO>
     */
    public List<CommentDO> getCommentsByState(String state, Long lastId, int pageSize) {
        byte stateValue = convertStateStringToValue(state);
        return commentDataService.listByState(stateValue, lastId, pageSize);
    }

    /**
     * 将状态字符串转换为状态值
     */
    private byte convertStateStringToValue(String state) {
        switch (state.toLowerCase()) {
            case "pending":
                return SUBMITTED.value();
            case "approved":
                return PUBLISHED.value();
            case "rejected":
                return REJECTED.value();
            case "banned":
                return BANNED.value();
            default:
                throw StatusCode.INVALID_PARAMETER.exception("无效的状态参数: " + state);
        }
    }

    /**
     * 按状态获取评论列表
     */
    public List<CommentDO> listByState(Enums.ContentState state, Long lastId, int pageSize) {
        return commentDataService.listByState(state.value(), lastId, pageSize);
    }
}
