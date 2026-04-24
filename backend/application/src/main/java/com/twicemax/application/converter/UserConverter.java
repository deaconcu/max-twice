package com.twicemax.application.converter;

import com.twicemax.application.dto.response.UserDTO;
import com.twicemax.application.dto.response.user.*;
import com.twicemax.user.profile.UserDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 用户 DTO 转换器
 *
 * 提供 UserDO 到各种 DTO 的转换方法
 * 使用 MapStruct 自动生成实现代码
 *
 * @author Claude
 * @since 2025-01-18
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface UserConverter {

    // ==================== 旧版方法（保留以兼容现有代码）====================

    /**
     * 转换为旧版 UserDTO（完整字段）
     * @deprecated 使用 toProfileDTO 替代
     */
    @Deprecated
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "password")
    @Mapping(target = "phone")
    @Mapping(target = "email")
    @Mapping(target = "emailValidated")
    @Mapping(target = "biography")
    @Mapping(target = "state")
    @Mapping(target = "role")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    UserDTO toDTO(UserDO userDO);

    /**
     * 批量转换为旧版 UserDTO
     * @deprecated 使用 toProfileDTO 替代
     */
    @Deprecated
    @IterableMapping(qualifiedByName = "toDTO")
    List<UserDTO> toDTO(List<UserDO> userDOList);

    /**
     * 转换为旧版 V1
     * @deprecated 使用 toSummaryDTO 替代
     */
    @Deprecated
    @Named("toDTOV1")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "state")
    @Mapping(target = "biography")
    UserDTO toDTOV1(UserDO userDO);

    /**
     * 批量转换为旧版 V1
     * @deprecated 使用 toSummaryDTO 替代
     */
    @Deprecated
    @IterableMapping(qualifiedByName = "toDTOV1")
    List<UserDTO> toDTOV1(List<UserDO> userDOList);

    /**
     * 转换为旧版 V2
     * @deprecated 使用 toBriefDTO 或 toWithSubscriptionsDTO 替代
     */
    @Deprecated
    @Named("toDTOV2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "state")
    @Mapping(target = "role")
    @Mapping(target = "biography")
    @Mapping(target = "avatar")
    UserDTO toDTOV2(UserDO userDO);

    /**
     * 批量转换为旧版 V2
     * @deprecated 使用 toBriefDTO 或 toWithSubscriptionsDTO 替代
     */
    @Deprecated
    @IterableMapping(qualifiedByName = "toDTOV2")
    List<UserDTO> toDTOV2(List<UserDO> userDOList);

    /**
     * 旧版 V1 + followed 转换
     * @deprecated 使用 toPublicDTO 替代
     */
    @Deprecated
    default UserDTO toDTOV1(UserDO userDO, boolean followed) {
        if (userDO == null) return null;

        UserDTO dto = toDTOV1(userDO);
        dto.setFollowing(followed);
        return dto;
    }

    // ==================== 新版语义化方法 ====================

    /**
     * 转换为用户简要 DTO（仅 id + name + avatar）
     * 用途：作者署名、用户引用
     */
    @Named("toBriefDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "avatar")
    UserBriefDTO toBriefDTO(UserDO userDO);

    /**
     * 批量转换为用户简要 DTO
     */
    @IterableMapping(qualifiedByName = "toBriefDTO")
    List<UserBriefDTO> toBriefDTO(List<UserDO> userDOList);

    /**
     * 转换为用户摘要 DTO（公开信息）
     * 用途：用户列表、作者信息
     */
    @Named("toSummaryDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "state")
    @Mapping(target = "biography")
    @Mapping(target = "avatar")
    UserSummaryDTO toSummaryDTO(UserDO userDO);

    /**
     * 批量转换为用户摘要 DTO
     */
    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<UserSummaryDTO> toSummaryDTO(List<UserDO> userDOList);

    /**
     * 转换为带订阅信息的用户 DTO
     * 用途：用户个人中心
     * 注意：subscriptions 需要在 Service 层额外填充
     */
    @Named("toWithSubscriptionsDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "state")
    @Mapping(target = "role")
    UserWithSubscriptionsDTO toWithSubscriptionsDTO(UserDO userDO);

    /**
     * 批量转换为带订阅信息的用户 DTO
     */
    @IterableMapping(qualifiedByName = "toWithSubscriptionsDTO")
    List<UserWithSubscriptionsDTO> toWithSubscriptionsDTO(List<UserDO> userDOList);

    /**
     * 转换为公开用户信息 DTO
     * 用途：查看其他用户主页
     * 注意：isFollowing 需要在 Service 层额外填充
     */
    @Named("toPublicDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "state")
    @Mapping(target = "biography")
    @Mapping(target = "avatar")
    UserPublicDTO toPublicDTO(UserDO userDO);

    /**
     * 批量转换为公开用户信息 DTO
     */
    @IterableMapping(qualifiedByName = "toPublicDTO")
    List<UserPublicDTO> toPublicDTO(List<UserDO> userDOList);

    /**
     * 转换为用户完整资料 DTO（含敏感信息）
     * 用途：用户查看自己的完整资料
     * 注意：subscriptions 需要在 Service 层额外填充
     */
    @Named("toProfileDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "phone")
    @Mapping(target = "email")
    @Mapping(target = "emailValidated")
    @Mapping(target = "biography")
    @Mapping(target = "avatar")
    @Mapping(target = "state")
    @Mapping(target = "role")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    @Mapping(target = "timezone")
    @Mapping(target = "locale")
    @Mapping(target = "hasPassword", expression = "java(userDO != null && userDO.getPassword() != null && !userDO.getPassword().isEmpty())")
    UserProfileDTO toProfileDTO(UserDO userDO);

    /**
     * 批量转换为用户完整资料 DTO
     */
    @IterableMapping(qualifiedByName = "toProfileDTO")
    List<UserProfileDTO> toProfileDTO(List<UserDO> userDOList);

    /**
     * 转换为用户管理 DTO（管理后台使用）
     * 用途：管理后台用户列表
     */
    @Named("toAdminDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "avatar")
    @Mapping(target = "phone")
    @Mapping(target = "email")
    @Mapping(target = "emailValidated")
    @Mapping(target = "biography")
    @Mapping(target = "state")
    @Mapping(target = "role")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    UserAdminDTO toAdminDTO(UserDO userDO);

    /**
     * 批量转换为用户管理 DTO
     */
    @IterableMapping(qualifiedByName = "toAdminDTO")
    List<UserAdminDTO> toAdminDTO(List<UserDO> userDOList);
}
