package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.CommentDTO;
import com.prosper.learn.dto.response.comment.CommentAdminDTO;
import com.prosper.learn.dto.response.comment.CommentDetailDTO;
import com.prosper.learn.dto.response.comment.CommentSummaryDTO;
import com.prosper.learn.dto.response.comment.CommentWithRepliesDTO;
import com.prosper.learn.persistence.dataobject.CommentDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 评论 DTO 转换器
 *
 * 提供 CommentDO 到各种 DTO 的转换方法
 * 使用 MapStruct 自动生成实现代码
 *
 * @author Claude
 * @since 2025-01-18
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface CommentConverter {

    // ==================== 旧版方法（保留以兼容现有代码）====================

    /**
     * 转换为旧版 CommentDTO
     * @deprecated 使用 toSummaryDTO 替代
     */
    @Deprecated
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "objectType")
    @Mapping(target = "objectId")
    @Mapping(target = "replyToCommentId")
    @Mapping(target = "creatorId")
    @Mapping(target = "toUserId")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    CommentDTO toDTO(CommentDO commentDO);

    /**
     * 批量转换为旧版 CommentDTO
     * @deprecated 使用 toSummaryDTO 替代
     */
    @Deprecated
    @IterableMapping(qualifiedByName = "toDTO")
    List<CommentDTO> toDTO(List<CommentDO> commentDOList);

    default void addChild(CommentDTO parent, CommentDTO child) {
        if (parent != null && child != null) {
            parent.addChild(child);
        }
    }

    // ==================== 新版语义化方法 ====================

    /**
     * 转换为评论摘要 DTO（基础信息）
     * 用途：管理端列表、不需要用户名和点赞状态的场景
     */
    @Named("toSummaryDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "objectType")
    @Mapping(target = "objectId")
    @Mapping(target = "replyToCommentId")
    @Mapping(target = "creatorId")
    @Mapping(target = "toUserId")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    CommentSummaryDTO toSummaryDTO(CommentDO commentDO);

    /**
     * 批量转换为评论摘要 DTO
     */
    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<CommentSummaryDTO> toSummaryDTO(List<CommentDO> commentDOList);

    /**
     * 转换为评论详情 DTO（含用户名和点赞状态）
     * 注意：creatorName、toUserName、upvoted 需要在 Service 层额外填充
     */
    @Named("toDetailDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "objectType")
    @Mapping(target = "objectId")
    @Mapping(target = "replyToCommentId")
    @Mapping(target = "creatorId")
    @Mapping(target = "toUserId")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    CommentDetailDTO toDetailDTO(CommentDO commentDO);

    /**
     * 批量转换为评论详情 DTO
     */
    @IterableMapping(qualifiedByName = "toDetailDTO")
    List<CommentDetailDTO> toDetailDTO(List<CommentDO> commentDOList);

    /**
     * 转换为带回复的评论 DTO
     * 注意：children 需要在 Service 层额外填充
     */
    @Named("toWithRepliesDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "objectType")
    @Mapping(target = "objectId")
    @Mapping(target = "replyToCommentId")
    @Mapping(target = "creatorId")
    @Mapping(target = "toUserId")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    CommentWithRepliesDTO toWithRepliesDTO(CommentDO commentDO);

    /**
     * 批量转换为带回复的评论 DTO
     */
    @IterableMapping(qualifiedByName = "toWithRepliesDTO")
    List<CommentWithRepliesDTO> toWithRepliesDTO(List<CommentDO> commentDOList);

    /**
     * 转换为管理员评论 DTO（含审核原因）
     */
    @Named("toAdminDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "objectType")
    @Mapping(target = "objectId")
    @Mapping(target = "replyToCommentId")
    @Mapping(target = "creatorId")
    @Mapping(target = "toUserId")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    @Mapping(target = "reason")
    CommentAdminDTO toAdminDTO(CommentDO commentDO);

    /**
     * 批量转换为管理员评论 DTO
     */
    @IterableMapping(qualifiedByName = "toAdminDTO")
    List<CommentAdminDTO> toAdminDTO(List<CommentDO> commentDOList);
}