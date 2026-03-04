package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.PostDTO;
import com.prosper.learn.application.dto.response.post.*;
import com.prosper.learn.content.post.PostDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 帖子 DTO 转换器
 *
 * 提供 PostDO 到各种 Post DTO 的转换方法
 * 使用 MapStruct 自动生成实现代码
 *
 * @author Claude
 * @since 2025-01-18
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface PostConverter {

    // ==================== 旧版方法（保留以兼容现有代码）====================

    /**
     * 转换为旧版 PostDTO
     * @deprecated 使用新的语义化方法替代
     */
    @Deprecated
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "nodeId")
    @Mapping(target = "creatorId")
    @Mapping(target = "type")
    @Mapping(target = "content")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    PostDTO toDTO(PostDO postDO);

    /**
     * 批量转换为旧版 PostDTO
     * @deprecated 使用新的语义化方法替代
     */
    @Deprecated
    @IterableMapping(qualifiedByName = "toDTO")
    List<PostDTO> toDTO(List<PostDO> postDOList);

    // ==================== 新版语义化方法 ====================

    /**
     * 转换为帖子摘要 DTO
     * 用途：基础帖子信息，不含关联对象
     */
    @Named("toSummaryDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "nodeId")
    @Mapping(target = "creatorId")
    @Mapping(target = "type")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    PostSummaryDTO toSummaryDTO(PostDO postDO);

    /**
     * 批量转换为帖子摘要 DTO
     */
    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<PostSummaryDTO> toSummaryDTO(List<PostDO> postDOList);

    /**
     * 转换为帖子管理 DTO
     * 用途：管理后台使用，包含节点信息和审核原因
     * 注意：node 需要在 Service 层额外填充
     */
    @Named("toAdminDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "nodeId")
    @Mapping(target = "creatorId")
    @Mapping(target = "type")
    @Mapping(target = "state")
    @Mapping(target = "reason")
    @Mapping(target = "createdAt")
    @Mapping(target = "score")
    PostAdminDTO toAdminDTO(PostDO postDO);

    /**
     * 批量转换为帖子管理 DTO
     */
    @IterableMapping(qualifiedByName = "toAdminDTO")
    List<PostAdminDTO> toAdminDTO(List<PostDO> postDOList);

    /**
     * 转换为帖子（含创建者）DTO
     * 用途：包含创建者信息的帖子
     * 注意：creator 需要在 Service 层额外填充
     */
    @Named("toWithCreatorDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "nodeId")
    @Mapping(target = "creatorId")
    @Mapping(target = "type")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    PostWithCreatorDTO toWithCreatorDTO(PostDO postDO);

    /**
     * 批量转换为帖子（含创建者）DTO
     */
    @IterableMapping(qualifiedByName = "toWithCreatorDTO")
    List<PostWithCreatorDTO> toWithCreatorDTO(List<PostDO> postDOList);

    /**
     * 转换为帖子（含投票状态）DTO
     * 用途：包含创建者和投票类型的帖子
     * 注意：creator 和 voteType 需要在 Service 层额外填充
     */
    @Named("toWithVoteDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "nodeId")
    @Mapping(target = "creatorId")
    @Mapping(target = "type")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    PostWithVoteDTO toWithVoteDTO(PostDO postDO);

    /**
     * 批量转换为帖子（含投票状态）DTO
     */
    @IterableMapping(qualifiedByName = "toWithVoteDTO")
    List<PostWithVoteDTO> toWithVoteDTO(List<PostDO> postDOList);

    /**
     * 转换为帖子详情 DTO
     * 用途：包含完整节点信息的帖子
     * 注意：creator 和 node 需要在 Service 层额外填充
     */
    @Named("toDetailDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "nodeId")
    @Mapping(target = "creatorId")
    @Mapping(target = "type")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    PostDetailDTO toDetailDTO(PostDO postDO);

    /**
     * 批量转换为帖子详情 DTO
     */
    @IterableMapping(qualifiedByName = "toDetailDTO")
    List<PostDetailDTO> toDetailDTO(List<PostDO> postDOList);

    /**
     * 转换为帖子完整信息 DTO
     * 用途：包含节点、创建者、投票状态的完整帖子信息
     * 注意：creator、node 和 voteType 需要在 Service 层额外填充
     */
    @Named("toFullDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "nodeId")
    @Mapping(target = "creatorId")
    @Mapping(target = "type")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    PostFullDTO toFullDTO(PostDO postDO);

    /**
     * 批量转换为帖子完整信息 DTO
     */
    @IterableMapping(qualifiedByName = "toFullDTO")
    List<PostFullDTO> toFullDTO(List<PostDO> postDOList);
}