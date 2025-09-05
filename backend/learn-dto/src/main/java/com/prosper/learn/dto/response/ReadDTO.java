package com.prosper.learn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 读取内容响应DTO
 */
@Data
@Builder
public class ReadDTO {
    
    /**
     * 节点信息
     */
    private NodeDTOV2 node;
    
    /**
     * 父课程信息
     */
    private CourseDTOV4 parentCourse;
    
    /**
     * 课程信息
     */
    private CourseDTOV4 course;
    
    /**
     * 子课程列表
     */
    private List<CourseDTOV2> subCourseList;
    
    /**
     * 选中的帖子
     */
    private PostDTO chosenPosting;
    
    /**
     * 固定的帖子列表
     */
    private List<PostDTO> fixedPostings;
    
    /**
     * 其他帖子列表
     */
    private List<PostDTO> otherPostings;
    
    /**
     * 最后一个ID
     */
    private Long lastId;
    
    /**
     * 目录内容
     */
    private List<Object> toc;
    
    /**
     * 目录节点信息
     */
    private Map<Long, NodeDTOV2> tocNodeInfos;
    
    /**
     * 路径
     */
    private String path;
    
    /**
     * 用户列表
     */
    private List<UserDTOV1> users;
    
    /**
     * 是否正在学习
     */
    private Boolean learning;
    
    /**
     * 帖子信息（可选）
     */
    private PostDTO post;
    
    /**
     * 评论ID（readByComment时返回）
     */
    private Long commentId;
    
    /**
     * 子评论ID（readByComment时返回）
     */
    private Long subCommentId;
}