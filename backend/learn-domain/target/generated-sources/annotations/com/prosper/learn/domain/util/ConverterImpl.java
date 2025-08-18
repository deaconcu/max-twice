package com.prosper.learn.domain.util;

import com.prosper.learn.dto.CommentDTO;
import com.prosper.learn.dto.CommentDTOV1;
import com.prosper.learn.dto.CourseDTOV2;
import com.prosper.learn.dto.CourseDTOV3;
import com.prosper.learn.dto.CourseDTOV4;
import com.prosper.learn.dto.FollowDTO;
import com.prosper.learn.dto.NodeDTO;
import com.prosper.learn.dto.NodeDTOV1;
import com.prosper.learn.dto.PostDTO;
import com.prosper.learn.dto.ProfessionDTO;
import com.prosper.learn.dto.RoadmapDTO;
import com.prosper.learn.dto.RoadmapDTOV2;
import com.prosper.learn.dto.UserCourseDTO;
import com.prosper.learn.dto.UserDTO;
import com.prosper.learn.dto.UserDTOV1;
import com.prosper.learn.dto.UserDTOV2;
import com.prosper.learn.dto.UserDTOV3;
import com.prosper.learn.dto.UserDTOV4;
import com.prosper.learn.dto.UserRoadmapDTO;
import com.prosper.learn.dto.message.MessageDTO;
import com.prosper.learn.persistence.dataobject.CommentDO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.FollowDO;
import com.prosper.learn.persistence.dataobject.MessageDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.dataobject.ProfessionDO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.dataobject.UserRoadmapDO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-16T22:07:51+0800",
    comments = "version: 1.6.1, compiler: javac, environment: Java 17.0.13 (BellSoft)"
)
public class ConverterImpl implements Converter {

    private final DateTimeFormatter dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168 = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" );

    @Override
    public CourseDTOV4 toCourseDTOV4(CourseDO item) {
        if ( item == null ) {
            return null;
        }

        CourseDTOV4 courseDTOV4 = new CourseDTOV4();

        if ( item.getCTime() != null ) {
            courseDTOV4.setCTime( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( item.getCTime() ) );
        }
        if ( item.getUTime() != null ) {
            courseDTOV4.setUTime( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( item.getUTime() ) );
        }
        courseDTOV4.setId( item.getId() );
        courseDTOV4.setName( item.getName() );
        courseDTOV4.setDescription( item.getDescription() );
        courseDTOV4.setCreator( item.getCreator() );
        courseDTOV4.setRootNode( item.getRootNode() );
        courseDTOV4.setState( item.getState() );
        courseDTOV4.setMainCategory( item.getMainCategory() );
        courseDTOV4.setSubCategory( item.getSubCategory() );
        courseDTOV4.setRejectedReason( item.getRejectedReason() );

        return courseDTOV4;
    }

    @Override
    public List<CourseDTOV4> toCourseDTOV4(List<CourseDO> list) {
        if ( list == null ) {
            return null;
        }

        List<CourseDTOV4> list1 = new ArrayList<CourseDTOV4>( list.size() );
        for ( CourseDO courseDO : list ) {
            list1.add( toCourseDTOV4( courseDO ) );
        }

        return list1;
    }

    @Override
    public List<CourseDTOV2> toCourseDTOV2(List<CourseDO> list) {
        if ( list == null ) {
            return null;
        }

        List<CourseDTOV2> list1 = new ArrayList<CourseDTOV2>( list.size() );
        for ( CourseDO courseDO : list ) {
            list1.add( toCourseDTOV2( courseDO ) );
        }

        return list1;
    }

    @Override
    public CourseDTOV3 toCourseDTOV3(CourseDO item) {
        if ( item == null ) {
            return null;
        }

        CourseDTOV3 courseDTOV3 = new CourseDTOV3();

        if ( item.getId() != null ) {
            courseDTOV3.setId( item.getId() );
        }
        courseDTOV3.setName( item.getName() );

        return courseDTOV3;
    }

    @Override
    public CourseDTOV2 toCourseDTOV2(CourseDO item) {
        if ( item == null ) {
            return null;
        }

        CourseDTOV2 courseDTOV2 = new CourseDTOV2();

        if ( item.getId() != null ) {
            courseDTOV2.setId( item.getId() );
        }
        courseDTOV2.setName( item.getName() );
        courseDTOV2.setDescription( item.getDescription() );
        if ( item.getMainCategory() != null ) {
            courseDTOV2.setMainCategory( item.getMainCategory() );
        }
        if ( item.getSubCategory() != null ) {
            courseDTOV2.setSubCategory( item.getSubCategory() );
        }

        return courseDTOV2;
    }

    @Override
    public List<CourseDTOV3> toCourseDTOV3(List<CourseDO> list) {
        if ( list == null ) {
            return null;
        }

        List<CourseDTOV3> list1 = new ArrayList<CourseDTOV3>( list.size() );
        for ( CourseDO courseDO : list ) {
            list1.add( toCourseDTOV3( courseDO ) );
        }

        return list1;
    }

    @Override
    public NodeDTO toNodeDTO(NodeDO item) {
        if ( item == null ) {
            return null;
        }

        NodeDTO nodeDTO = new NodeDTO();

        if ( item.getCTime() != null ) {
            nodeDTO.setCTime( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( item.getCTime() ) );
        }
        if ( item.getUTime() != null ) {
            nodeDTO.setUTime( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( item.getUTime() ) );
        }
        nodeDTO.setId( item.getId() );
        nodeDTO.setName( item.getName() );
        nodeDTO.setDescription( item.getDescription() );
        nodeDTO.setCourseId( item.getCourseId() );
        nodeDTO.setRoot( item.getRoot() );
        nodeDTO.setCreator( item.getCreator() );
        nodeDTO.setCommentCount( item.getCommentCount() );

        return nodeDTO;
    }

    @Override
    public NodeDTOV1 toNodeDTOV1(NodeDO item) {
        if ( item == null ) {
            return null;
        }

        NodeDTOV1 nodeDTOV1 = new NodeDTOV1();

        nodeDTOV1.setId( item.getId() );
        nodeDTOV1.setName( item.getName() );

        return nodeDTOV1;
    }

    @Override
    public NodeDO toNodeDO(NodeDTO itemDTO) {
        if ( itemDTO == null ) {
            return null;
        }

        NodeDO nodeDO = new NodeDO();

        if ( itemDTO.getCTime() != null ) {
            nodeDO.setCTime( LocalDateTime.parse( itemDTO.getCTime(), dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168 ) );
        }
        if ( itemDTO.getUTime() != null ) {
            nodeDO.setUTime( LocalDateTime.parse( itemDTO.getUTime(), dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168 ) );
        }
        if ( itemDTO.getId() != null ) {
            nodeDO.setId( itemDTO.getId() );
        }
        nodeDO.setName( itemDTO.getName() );
        nodeDO.setDescription( itemDTO.getDescription() );
        nodeDO.setCourseId( itemDTO.getCourseId() );
        nodeDO.setRoot( itemDTO.getRoot() );
        nodeDO.setCreator( itemDTO.getCreator() );
        nodeDO.setCommentCount( itemDTO.getCommentCount() );

        return nodeDO;
    }

    @Override
    public List<NodeDTO> toNodeDTO(List<NodeDO> list) {
        if ( list == null ) {
            return null;
        }

        List<NodeDTO> list1 = new ArrayList<NodeDTO>( list.size() );
        for ( NodeDO nodeDO : list ) {
            list1.add( toNodeDTO( nodeDO ) );
        }

        return list1;
    }

    @Override
    public PostDTO toPostDTO(PostDO item) {
        if ( item == null ) {
            return null;
        }

        PostDTO postDTO = new PostDTO();

        if ( item.getCTime() != null ) {
            postDTO.setCTime( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( item.getCTime() ) );
        }
        postDTO.setCreatorId( item.getCreator() );
        postDTO.setId( item.getId() );
        postDTO.setContent( item.getContent() );
        postDTO.setNodeId( item.getNodeId() );
        postDTO.setType( item.getType() );
        postDTO.setOnce( item.getOnce() );
        postDTO.setTwice( item.getTwice() );
        postDTO.setHelpful( item.getHelpful() );
        postDTO.setCommentCount( item.getCommentCount() );
        postDTO.setState( item.getState() );
        postDTO.setScore( item.getScore() );
        if ( item.getUTime() != null ) {
            postDTO.setUTime( DateTimeFormatter.ISO_LOCAL_DATE_TIME.format( item.getUTime() ) );
        }

        return postDTO;
    }

    @Override
    public List<PostDTO> toPostDTO(List<PostDO> list) {
        if ( list == null ) {
            return null;
        }

        List<PostDTO> list1 = new ArrayList<PostDTO>( list.size() );
        for ( PostDO postDO : list ) {
            list1.add( toPostDTO( postDO ) );
        }

        return list1;
    }

    @Override
    public PostDO toPostDO(PostDTO item) {
        if ( item == null ) {
            return null;
        }

        PostDO postDO = new PostDO();

        if ( item.getCTime() != null ) {
            postDO.setCTime( LocalDateTime.parse( item.getCTime(), dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168 ) );
        }
        postDO.setCreator( item.getCreatorId() );
        if ( item.getId() != null ) {
            postDO.setId( item.getId() );
        }
        postDO.setNodeId( item.getNodeId() );
        postDO.setType( item.getType() );
        postDO.setContent( item.getContent() );
        postDO.setOnce( item.getOnce() );
        postDO.setTwice( item.getTwice() );
        postDO.setHelpful( item.getHelpful() );
        postDO.setCommentCount( item.getCommentCount() );
        postDO.setState( item.getState() );
        postDO.setScore( item.getScore() );
        if ( item.getUTime() != null ) {
            postDO.setUTime( LocalDateTime.parse( item.getUTime() ) );
        }

        return postDO;
    }

    @Override
    public List<FollowDTO> toFollowDTO(List<FollowDO> list) {
        if ( list == null ) {
            return null;
        }

        List<FollowDTO> list1 = new ArrayList<FollowDTO>( list.size() );
        for ( FollowDO followDO : list ) {
            list1.add( followDOToFollowDTO( followDO ) );
        }

        return list1;
    }

    @Override
    public UserDTO toUserDTO(UserDO userDO) {
        if ( userDO == null ) {
            return null;
        }

        UserDTO userDTO = new UserDTO();

        if ( userDO.getCTime() != null ) {
            userDTO.setCTime( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( userDO.getCTime() ) );
        }
        if ( userDO.getUTime() != null ) {
            userDTO.setUTime( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( userDO.getUTime() ) );
        }
        userDTO.setId( userDO.getId() );
        userDTO.setName( userDO.getName() );
        userDTO.setPassword( userDO.getPassword() );
        userDTO.setPhone( userDO.getPhone() );
        userDTO.setEmail( userDO.getEmail() );
        userDTO.setEmailValidated( userDO.isEmailValidated() );
        userDTO.setBiography( userDO.getBiography() );

        return userDTO;
    }

    @Override
    public UserDTOV1 toUserDTOV1(UserDO userDO) {
        if ( userDO == null ) {
            return null;
        }

        UserDTOV1 userDTOV1 = new UserDTOV1();

        userDTOV1.setId( userDO.getId() );
        userDTOV1.setName( userDO.getName() );
        userDTOV1.setBiography( userDO.getBiography() );

        return userDTOV1;
    }

    @Override
    public UserDTOV4 toUserDTOV4(UserDO userDO) {
        if ( userDO == null ) {
            return null;
        }

        UserDTOV4 userDTOV4 = new UserDTOV4();

        userDTOV4.setId( userDO.getId() );
        userDTOV4.setName( userDO.getName() );

        return userDTOV4;
    }

    @Override
    public List<UserDTOV1> toUserDTOV1(List<UserDO> userDOList) {
        if ( userDOList == null ) {
            return null;
        }

        List<UserDTOV1> list = new ArrayList<UserDTOV1>( userDOList.size() );
        for ( UserDO userDO : userDOList ) {
            list.add( toUserDTOV1( userDO ) );
        }

        return list;
    }

    @Override
    public List<UserDTOV4> toUserDTOV4(List<UserDO> userDOList) {
        if ( userDOList == null ) {
            return null;
        }

        List<UserDTOV4> list = new ArrayList<UserDTOV4>( userDOList.size() );
        for ( UserDO userDO : userDOList ) {
            list.add( toUserDTOV4( userDO ) );
        }

        return list;
    }

    @Override
    public UserDTOV2 toUserDTOV2(UserDO userDO) {
        if ( userDO == null ) {
            return null;
        }

        UserDTOV2 userDTOV2 = new UserDTOV2();

        userDTOV2.setId( userDO.getId() );
        userDTOV2.setName( userDO.getName() );

        return userDTOV2;
    }

    @Override
    public UserDTOV3 toUserDTOV3(UserDO userDO) {
        if ( userDO == null ) {
            return null;
        }

        UserDTOV3 userDTOV3 = new UserDTOV3();

        userDTOV3.setId( userDO.getId() );
        userDTOV3.setName( userDO.getName() );
        userDTOV3.setBiography( userDO.getBiography() );

        return userDTOV3;
    }

    @Override
    public UserDO toUserDO(UserDTO userDTO) {
        if ( userDTO == null ) {
            return null;
        }

        UserDO userDO = new UserDO();

        if ( userDTO.getCTime() != null ) {
            userDO.setCTime( LocalDateTime.parse( userDTO.getCTime(), dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168 ) );
        }
        if ( userDTO.getUTime() != null ) {
            userDO.setUTime( LocalDateTime.parse( userDTO.getUTime(), dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168 ) );
        }
        userDO.setId( userDTO.getId() );
        userDO.setPassword( userDTO.getPassword() );
        userDO.setEmail( userDTO.getEmail() );
        userDO.setPhone( userDTO.getPhone() );
        userDO.setName( userDTO.getName() );
        userDO.setEmailValidated( userDTO.isEmailValidated() );
        userDO.setBiography( userDTO.getBiography() );

        return userDO;
    }

    @Override
    public CommentDTO toCommentDTO(CommentDO item) {
        if ( item == null ) {
            return null;
        }

        CommentDTO commentDTO = new CommentDTO();

        if ( item.getCTime() != null ) {
            commentDTO.setCTime( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( item.getCTime() ) );
        }
        commentDTO.setId( item.getId() );
        commentDTO.setContent( item.getContent() );
        commentDTO.setType( item.getType() );
        commentDTO.setObjectId( item.getObjectId() );
        commentDTO.setReplyCount( item.getReplyCount() );
        commentDTO.setReplyTo( item.getReplyTo() );
        commentDTO.setFromUser( item.getFromUser() );
        commentDTO.setToUser( item.getToUser() );
        commentDTO.setUpvoteCount( item.getUpvoteCount() );

        return commentDTO;
    }

    @Override
    public CommentDTOV1 toCommentDTOV1(CommentDO item) {
        if ( item == null ) {
            return null;
        }

        CommentDTOV1 commentDTOV1 = new CommentDTOV1();

        if ( item.getCTime() != null ) {
            commentDTOV1.setCTime( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( item.getCTime() ) );
        }
        commentDTOV1.setId( item.getId() );
        commentDTOV1.setContent( item.getContent() );
        commentDTOV1.setType( item.getType() );
        commentDTOV1.setObjectId( item.getObjectId() );
        commentDTOV1.setReplyCount( item.getReplyCount() );
        commentDTOV1.setReplyTo( item.getReplyTo() );
        commentDTOV1.setFromUser( item.getFromUser() );
        commentDTOV1.setToUser( item.getToUser() );
        commentDTOV1.setUpvoteCount( item.getUpvoteCount() );
        commentDTOV1.setState( item.getState() );

        return commentDTOV1;
    }

    @Override
    public List<CommentDTO> toCommentDTO(List<CommentDO> list) {
        if ( list == null ) {
            return null;
        }

        List<CommentDTO> list1 = new ArrayList<CommentDTO>( list.size() );
        for ( CommentDO commentDO : list ) {
            list1.add( toCommentDTO( commentDO ) );
        }

        return list1;
    }

    @Override
    public List<CommentDTOV1> toCommentDTOV1(List<CommentDO> list) {
        if ( list == null ) {
            return null;
        }

        List<CommentDTOV1> list1 = new ArrayList<CommentDTOV1>( list.size() );
        for ( CommentDO commentDO : list ) {
            list1.add( toCommentDTOV1( commentDO ) );
        }

        return list1;
    }

    @Override
    public CommentDO toCommentDO(CommentDTO item) {
        if ( item == null ) {
            return null;
        }

        CommentDO commentDO = new CommentDO();

        if ( item.getCTime() != null ) {
            commentDO.setCTime( LocalDateTime.parse( item.getCTime(), dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168 ) );
        }
        commentDO.setId( item.getId() );
        commentDO.setContent( item.getContent() );
        commentDO.setType( item.getType() );
        commentDO.setObjectId( item.getObjectId() );
        commentDO.setReplyCount( item.getReplyCount() );
        commentDO.setReplyTo( item.getReplyTo() );
        commentDO.setFromUser( item.getFromUser() );
        commentDO.setToUser( item.getToUser() );
        commentDO.setUpvoteCount( item.getUpvoteCount() );

        return commentDO;
    }

    @Override
    public MessageDTO toMessageDTO(MessageDO item) {
        if ( item == null ) {
            return null;
        }

        MessageDTO messageDTO = new MessageDTO();

        if ( item.getCreatedAt() != null ) {
            messageDTO.setCreatedAt( dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168.format( item.getCreatedAt() ) );
        }
        messageDTO.setId( item.getId() );
        messageDTO.setType( item.getType() );
        messageDTO.setIsRead( item.getIsRead() );

        return messageDTO;
    }

    @Override
    public List<MessageDTO> toMessageDTO(List<MessageDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MessageDTO> list1 = new ArrayList<MessageDTO>( list.size() );
        for ( MessageDO messageDO : list ) {
            list1.add( toMessageDTO( messageDO ) );
        }

        return list1;
    }

    @Override
    public MessageDO toMessageDO(MessageDTO item) {
        if ( item == null ) {
            return null;
        }

        MessageDO messageDO = new MessageDO();

        if ( item.getCreatedAt() != null ) {
            messageDO.setCreatedAt( LocalDateTime.parse( item.getCreatedAt(), dateTimeFormatter_yyyy_MM_dd_HH_mm_ss_11333195168 ) );
        }
        messageDO.setId( item.getId() );
        messageDO.setType( item.getType() );
        messageDO.setIsRead( item.getIsRead() );

        return messageDO;
    }

    @Override
    public List<MessageDO> toMessageDO(List<MessageDTO> list) {
        if ( list == null ) {
            return null;
        }

        List<MessageDO> list1 = new ArrayList<MessageDO>( list.size() );
        for ( MessageDTO messageDTO : list ) {
            list1.add( toMessageDO( messageDTO ) );
        }

        return list1;
    }

    @Override
    public RoadmapDTO toRoadMapDTO(RoadmapDO item) {
        if ( item == null ) {
            return null;
        }

        RoadmapDTO roadmapDTO = new RoadmapDTO();

        roadmapDTO.setCreatedAt( item.getCreatedAt() );
        roadmapDTO.setUpdatedAt( item.getUpdatedAt() );
        roadmapDTO.setId( item.getId() );
        roadmapDTO.setContent( item.getContent() );
        roadmapDTO.setProfessionId( item.getProfessionId() );
        roadmapDTO.setDescription( item.getDescription() );
        roadmapDTO.setVote( item.getVote() );
        roadmapDTO.setComment( item.getComment() );

        return roadmapDTO;
    }

    @Override
    public List<RoadmapDTO> toRoadMapDTO(List<RoadmapDO> list) {
        if ( list == null ) {
            return null;
        }

        List<RoadmapDTO> list1 = new ArrayList<RoadmapDTO>( list.size() );
        for ( RoadmapDO roadmapDO : list ) {
            list1.add( toRoadMapDTO( roadmapDO ) );
        }

        return list1;
    }

    @Override
    public RoadmapDTOV2 toRoadmapDTOV2(RoadmapDO item) {
        if ( item == null ) {
            return null;
        }

        RoadmapDTOV2 roadmapDTOV2 = new RoadmapDTOV2();

        roadmapDTOV2.setCreatedAt( item.getCreatedAt() );
        roadmapDTOV2.setUpdatedAt( item.getUpdatedAt() );
        roadmapDTOV2.setId( item.getId() );
        roadmapDTOV2.setContent( item.getContent() );
        roadmapDTOV2.setDescription( item.getDescription() );
        roadmapDTOV2.setVote( item.getVote() );
        roadmapDTOV2.setComment( item.getComment() );

        return roadmapDTOV2;
    }

    @Override
    public List<RoadmapDTOV2> toRoadmapDTOV2(List<RoadmapDO> list) {
        if ( list == null ) {
            return null;
        }

        List<RoadmapDTOV2> list1 = new ArrayList<RoadmapDTOV2>( list.size() );
        for ( RoadmapDO roadmapDO : list ) {
            list1.add( toRoadmapDTOV2( roadmapDO ) );
        }

        return list1;
    }

    @Override
    public UserCourseDTO toUserCourseDTO(UserCourseDO item) {
        if ( item == null ) {
            return null;
        }

        UserCourseDTO userCourseDTO = new UserCourseDTO();

        userCourseDTO.setId( item.getId() );
        userCourseDTO.setUserId( item.getUserId() );
        userCourseDTO.setProgressPercent( item.getProgressPercent() );
        userCourseDTO.setStatus( item.getStatus() );
        userCourseDTO.setStartedAt( item.getStartedAt() );
        userCourseDTO.setCompletedAt( item.getCompletedAt() );
        userCourseDTO.setCreatedAt( item.getCreatedAt() );
        userCourseDTO.setUpdatedAt( item.getUpdatedAt() );

        return userCourseDTO;
    }

    @Override
    public List<UserCourseDTO> toUserCourseDTO(List<UserCourseDO> list) {
        if ( list == null ) {
            return null;
        }

        List<UserCourseDTO> list1 = new ArrayList<UserCourseDTO>( list.size() );
        for ( UserCourseDO userCourseDO : list ) {
            list1.add( toUserCourseDTO( userCourseDO ) );
        }

        return list1;
    }

    @Override
    public UserRoadmapDTO toUserRoadmapDTO(UserRoadmapDO item) {
        if ( item == null ) {
            return null;
        }

        UserRoadmapDTO userRoadmapDTO = new UserRoadmapDTO();

        userRoadmapDTO.setId( item.getId() );
        userRoadmapDTO.setUserId( item.getUserId() );
        userRoadmapDTO.setProgressPercent( item.getProgressPercent() );
        userRoadmapDTO.setStatus( item.getStatus() );
        userRoadmapDTO.setStartedAt( item.getStartedAt() );
        userRoadmapDTO.setCompletedAt( item.getCompletedAt() );
        userRoadmapDTO.setCreatedAt( item.getCreatedAt() );
        userRoadmapDTO.setUpdatedAt( item.getUpdatedAt() );

        return userRoadmapDTO;
    }

    @Override
    public ProfessionDTO toProfessionDTO(ProfessionDO item) {
        if ( item == null ) {
            return null;
        }

        ProfessionDTO professionDTO = new ProfessionDTO();

        professionDTO.setId( item.getId() );
        professionDTO.setName( item.getName() );
        professionDTO.setDescription( item.getDescription() );
        professionDTO.setPrice( item.getPrice() );
        professionDTO.setSkills( item.getSkills() );
        professionDTO.setMainCategory( item.getMainCategory() );
        professionDTO.setSubCategory( item.getSubCategory() );
        professionDTO.setState( item.getState() );
        professionDTO.setRejectedReason( item.getRejectedReason() );
        professionDTO.setIcon( item.getIcon() );
        professionDTO.setCreator( item.getCreator() );
        professionDTO.setCreatedAt( item.getCreatedAt() );
        professionDTO.setUpdatedAt( item.getUpdatedAt() );

        return professionDTO;
    }

    @Override
    public List<ProfessionDTO> toProfessionDTO(List<ProfessionDO> list) {
        if ( list == null ) {
            return null;
        }

        List<ProfessionDTO> list1 = new ArrayList<ProfessionDTO>( list.size() );
        for ( ProfessionDO professionDO : list ) {
            list1.add( toProfessionDTO( professionDO ) );
        }

        return list1;
    }

    @Override
    public ProfessionDO toProfessionDO(ProfessionDTO professionDTO) {
        if ( professionDTO == null ) {
            return null;
        }

        ProfessionDO professionDO = new ProfessionDO();

        if ( professionDTO.getId() != null ) {
            professionDO.setId( professionDTO.getId() );
        }
        professionDO.setName( professionDTO.getName() );
        professionDO.setDescription( professionDTO.getDescription() );
        professionDO.setPrice( professionDTO.getPrice() );
        professionDO.setSkills( professionDTO.getSkills() );
        if ( professionDTO.getMainCategory() != null ) {
            professionDO.setMainCategory( professionDTO.getMainCategory() );
        }
        if ( professionDTO.getSubCategory() != null ) {
            professionDO.setSubCategory( professionDTO.getSubCategory() );
        }
        professionDO.setState( professionDTO.getState() );
        professionDO.setRejectedReason( professionDTO.getRejectedReason() );
        professionDO.setIcon( professionDTO.getIcon() );
        professionDO.setCreator( professionDTO.getCreator() );
        professionDO.setCreatedAt( professionDTO.getCreatedAt() );
        professionDO.setUpdatedAt( professionDTO.getUpdatedAt() );

        return professionDO;
    }

    protected FollowDTO followDOToFollowDTO(FollowDO followDO) {
        if ( followDO == null ) {
            return null;
        }

        FollowDTO followDTO = new FollowDTO();

        followDTO.setFollowerId( followDO.getFollowerId() );
        followDTO.setFolloweeId( followDO.getFolloweeId() );
        if ( followDO.getCreateTime() != null ) {
            followDTO.setCreateTime( DateTimeFormatter.ISO_LOCAL_DATE_TIME.format( followDO.getCreateTime() ) );
        }

        return followDTO;
    }
}
