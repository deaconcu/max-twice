package com.prosper.learn.domain.util;

import com.prosper.learn.common.Enums;
import com.prosper.learn.dto.*;
import com.prosper.learn.dto.message.MessageDTO;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.CourseMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.LinkedList;
import java.util.List;

// 只需要指出字段不一致的情况，支持复杂嵌套
// 如果字段没有不一致，不需要注解
@Mapper
public interface Converter {

    Converter INSTANCE = Mappers.getMapper(Converter.class);

    // course

    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "UTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "parent", ignore = true) // 自定义处理 parent 字段
    CourseDTOV4 toCourseDTOV4(CourseDO item);

    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "UTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "parent", ignore = true) // 自定义处理 parent 字段
    List<CourseDTOV4> toCourseDTOV4(List<CourseDO> list);
    List<CourseDTOV2> toCourseDTOV2(List<CourseDO> list);
    CourseDTOV3 toCourseDTOV3(CourseDO item);
    CourseDTOV2 toCourseDTOV2(CourseDO item);
    List<CourseDTOV3> toCourseDTOV3(List<CourseDO> list);

    // node

    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "UTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    NodeDTO toNodeDTO(NodeDO item);
    NodeDTOV1 toNodeDTOV1(NodeDO item);

    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "UTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    NodeDO toNodeDO(NodeDTO itemDTO);

    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "UTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<NodeDTO> toNodeDTO(List<NodeDO> list);

    // posting
    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(source = "creator", target="creatorId")
    @Mapping(target = "creator", ignore = true)
    PostDTO toPostDTO(PostDO item);
    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(source = "creator", target="creatorId")
    @Mapping(target = "creator", ignore = true)
    List<PostDTO> toPostDTO(List<PostDO> list);

    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(source = "creatorId", target="creator")
    PostDO toPostDO(PostDTO item);

    // follow
    @Mapping(target = "createTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<FollowDTO> toFollowDTO(List<FollowDO> list);

    // user
    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "UTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    UserDTO toUserDTO(UserDO userDO);
    UserDTOV1 toUserDTOV1(UserDO userDO);
    UserDTOV4 toUserDTOV4(UserDO userDO);
    List<UserDTOV1> toUserDTOV1(List<UserDO> userDOList);
    List<UserDTOV4> toUserDTOV4(List<UserDO> userDOList);
    UserDTOV2 toUserDTOV2(UserDO userDO);
    UserDTOV3 toUserDTOV3(UserDO userDO);

    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "UTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    UserDO toUserDO(UserDTO userDTO);

    // comment
    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    CommentDTO toCommentDTO(CommentDO item);
    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    CommentDTOV1 toCommentDTOV1(CommentDO item);

    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<CommentDTO> toCommentDTO(List<CommentDO> list);
    List<CommentDTOV1> toCommentDTOV1(List<CommentDO> list);

    @Mapping(target = "CTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    CommentDO toCommentDO(CommentDTO item);


    // message
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    MessageDTO toMessageDTO(MessageDO item);
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<MessageDTO> toMessageDTO(List<MessageDO> list);

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    MessageDO toMessageDO(MessageDTO item);
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<MessageDO> toMessageDO(List<MessageDTO> list);


    // roadmap
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    RoadmapDTO toRoadMapDTO(RoadmapDO item);

    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<RoadmapDTO> toRoadMapDTO(List<RoadmapDO> list);

    // roadmapV2 转换方法
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "profession", ignore = true)
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    RoadmapDTOV2 toRoadmapDTOV2(RoadmapDO item);

    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "professionDTO", ignore = true)
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<RoadmapDTOV2> toRoadmapDTOV2(List<RoadmapDO> list);

    default RoadmapDTO toRoadmapDTOWithUser(RoadmapDO item, UserMapper userMapper) {
        if (item == null) {
            return null;
        }
        RoadmapDTO dto = toRoadMapDTO(item);
        UserDO creator = userMapper.getById(item.getCreatorId());
        if (creator != null) {
            dto.setCreator(toUserDTOV4(creator));
        }
        // voted字段需要在调用处设置，因为需要当前用户信息
        dto.setUpvoted(false);
        return dto;
    }

    default RoadmapDTOV2 toRoadmapDTOV2WithUser(RoadmapDO item, UserMapper userMapper) {
        if (item == null) {
            return null;
        }
        RoadmapDTOV2 dto = toRoadmapDTOV2(item);
        UserDO creator = userMapper.getById(item.getCreatorId());
        if (creator != null) {
            dto.setCreator(toUserDTOV4(creator));
        }
        // professionDTO字段需要在调用处设置，因为需要profession信息
        return dto;
    }

    // user progress
    @Mapping(target = "course", ignore = true)
    UserCourseDTO toUserCourseDTO(UserCourseDO item);
    @Mapping(target = "course", ignore = true)
    List<UserCourseDTO> toUserCourseDTO(List<UserCourseDO> list);

    @Mapping(target = "roadmap", ignore = true)
    UserRoadmapDTO toUserRoadmapDTO(UserRoadmapDO item);

    // profession
    ProfessionDTO toProfessionDTO(ProfessionDO item);
    List<ProfessionDTO> toProfessionDTO(List<ProfessionDO> list);
    ProfessionDO toProfessionDO(ProfessionDTO professionDTO);

    // user stats (removed compatibility methods)
    
    // mapping method

    // 自定义方法来处理 parent 字段的转换
    default CourseDTOV4 toCourseDTOWithParent(CourseDO item, CourseMapper courseMapper) {
        if (item == null) {
            return null;
        }
        CourseDTOV4 dto = toCourseDTOV4(item);

        // 处理 parent 字段
        if (item.getParent() != null && item.getParent() > 0) {
            CourseDO parentCourse = courseMapper.getById(item.getParent());
            if (parentCourse != null) {
                dto.setParent(toCourseDTOV3(parentCourse));
            }
        }

        return dto;
    }

    // 自定义方法来处理 subscribed 字段的转换
    default CourseDTOV4 toCourseDTOV4(CourseDO item, boolean subscribed) {
        if (item == null) {
            return null;
        }
        CourseDTOV4 dto = toCourseDTOV4(item);
        dto.setSubscribed(subscribed);
        return dto;
    }

    default int map(Enums.CourseState state) {
        return state.value;
    }

    default int map(Enums.CourseRequestState state) {
        return state.value;
    }

    default List<Integer> map(String value) {
        List<Integer> list = new LinkedList<>();
        for (String s: value.split(",")) {
            list.add(Integer.parseInt(s));
        }
        return list;
    }

    default String map(List<Integer> list) {
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        list.forEach(s -> {
            sb.append(s);
            sb.append(",");
        });
        return sb.substring(0, sb.length() - 1);
    }


}
