package com.prosper.learn.business.util;

import com.prosper.learn.common.Enums;
import com.prosper.learn.business.service.data.CourseDataService;
import com.prosper.learn.business.service.data.UserDataService;
import com.prosper.learn.dto.response.message.MessageDTO;
import com.prosper.learn.dto.response.*;
import com.prosper.learn.dto.response.old.*;
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

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    //@Mapping(target = "parent", ignore = true) // 自定义处理 parent 字段
    CourseDTOV4 toCourseDTOV4(CourseDO item);

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "parent", ignore = true) // 自定义处理 parent 字段
    List<CourseDTOV4> toCourseDTOV4(List<CourseDO> list);
    List<CourseDTOV2> toCourseDTOV2(List<CourseDO> list);
    CourseDTOV3 toCourseDTOV3(CourseDO item);
    CourseDTOV2 toCourseDTOV2(CourseDO item);
    List<CourseDTOV3> toCourseDTOV3(List<CourseDO> list);

    // node

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    NodeDTOV0 toNodeDTO(NodeDO item);
    NodeDTOV1 toNodeDTOV1(NodeDO item);
    NodeDTOV2 toNodeDTOV2(NodeDO item);
    
    // 带完成状态参数的重载方法
    default NodeDTOV2 toNodeDTOV2(NodeDO item, boolean isCompleted) {
        if (item == null) {
            return null;
        }
        NodeDTOV2 dto = toNodeDTOV2(item);
        dto.setIsCompleted(isCompleted);
        return dto;
    }

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    NodeDO toNodeDO(NodeDTOV0 itemDTO);

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<NodeDTOV0> toNodeDTO(List<NodeDO> list);

    // posting
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    //@Mapping(source = "creator", target="creatorId")
    //@Mapping(target = "creator", ignore = true)
    PostDTOV1 toPostDTO(PostDO item);
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    //@Mapping(source = "creator", target="creatorId")
    //@Mapping(target = "creator", ignore = true)
    List<PostDTOV1> toPostDTO(List<PostDO> list);

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    //@Mapping(source = "creator", target="creatorId")
    //@Mapping(target = "creator", ignore = true)
    PostDTOV2 toPostDTOV2(PostDO item);
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    //@Mapping(source = "creator", target="creatorId")
    //@Mapping(target = "creator", ignore = true)
    List<PostDTOV2> toPostDTOV2(List<PostDO> list);

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    //@Mapping(source = "creatorId", target="creator")
    PostDO toPostDO(PostDTOV1 item);

    // follow
    @Mapping(target = "createTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<FollowDTO> toFollowDTO(List<FollowDO> list);

    // user
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    UserDTOV0 toUserDTO(UserDO userDO);
    List<UserDTO> toUserDTO(List<UserDO> userDOList);
    UserDTOV1 toUserDTOV1(UserDO userDO);
    UserDTOV4 toUserDTOV4(UserDO userDO);
    List<UserDTOV1> toUserDTOV1(List<UserDO> userDOList);
    List<UserDTOV4> toUserDTOV4(List<UserDO> userDOList);
    UserDTOV2 toUserDTOV2(UserDO userDO);
    UserDTOV3 toUserDTOV3(UserDO userDO);

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    UserDO toUserDO(UserDTOV0 userDTOV0);

    // comment
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    CommentDTO toCommentDTO(CommentDO item);
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    CommentDTOV1 toCommentDTOV1(CommentDO item);

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<CommentDTO> toCommentDTO(List<CommentDO> list);
    List<CommentDTOV1> toCommentDTOV1(List<CommentDO> list);

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
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
    RoadmapDTOV1 toRoadMapDTO(RoadmapDO item);

    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    List<RoadmapDTOV1> toRoadMapDTO(List<RoadmapDO> list);

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

    default RoadmapDTOV1 toRoadmapDTOWithUser(RoadmapDO item, UserMapper userMapper) {
        if (item == null) {
            return null;
        }
        RoadmapDTOV1 dto = toRoadMapDTO(item);
        UserDO creator = userMapper.getById(item.getCreatorId());
        if (creator != null) {
            dto.setCreator(toUserDTOV4(creator));
        }
        // voted字段需要在调用处设置，因为需要当前用户信息
        dto.setUpvoted(false);
        return dto;
    }

    default RoadmapDTOV1 toRoadmapDTOWithUser(RoadmapDO item, UserDataService userDataService) {
        if (item == null) {
            return null;
        }
        RoadmapDTOV1 dto = toRoadMapDTO(item);
        UserDO creator = userDataService.getById(item.getCreatorId());
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

    default RoadmapDTOV2 toRoadmapDTOV2WithUser(RoadmapDO item, UserDataService userDataService) {
        if (item == null) {
            return null;
        }
        RoadmapDTOV2 dto = toRoadmapDTOV2(item);
        UserDO creator = userDataService.getById(item.getCreatorId());
        if (creator != null) {
            dto.setCreator(toUserDTOV4(creator));
        }
        // professionDTO字段需要在调用处设置，因为需要profession信息
        return dto;
    }

    // user progress
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "state", source = "state") // status字段已重命名为state
    UserCourseDTO toUserCourseDTO(UserCourseDO item);
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "state", source = "state") // status字段已重命名为state
    List<UserCourseDTO> toUserCourseDTO(List<UserCourseDO> list);

    @Mapping(target = "roadmap", ignore = true)
    @Mapping(target = "state", source = "state") // status字段已重命名为state
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
        if (item.getParentCourseId() != null && item.getParentCourseId() > 0) {
            CourseDO parentCourse = courseMapper.getById(item.getParentCourseId());
            if (parentCourse != null) {
                dto.setParentCourse(toCourseDTOV3(parentCourse));
            }
        }

        return dto;
    }

    default CourseDTOV4 toCourseDTOWithParent(CourseDO item, CourseDataService courseDataService) {
        if (item == null) {
            return null;
        }
        CourseDTOV4 dto = toCourseDTOV4(item);

        // 处理 parent 字段
        if (item.getParentCourseId() != null && item.getParentCourseId() > 0) {
            CourseDO parentCourse = courseDataService.getById(item.getParentCourseId());
            if (parentCourse != null) {
                dto.setParentCourse(toCourseDTOV3(parentCourse));
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

    // 自定义方法来处理 subscribed 和 progress 字段的转换
    default CourseDTOV4 toCourseDTOV4(CourseDO item, boolean subscribed, Integer progress) {
        if (item == null) {
            return null;
        }
        CourseDTOV4 dto = toCourseDTOV4(item);
        dto.setSubscribed(subscribed);
        dto.setProgress(progress);
        return dto;
    }

    default int map(Enums.ContentState state) {
        return state.value();
    }

    /*
    default int map(Enums.CourseRequestState state) {
        return state.value();
    }
     */

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
