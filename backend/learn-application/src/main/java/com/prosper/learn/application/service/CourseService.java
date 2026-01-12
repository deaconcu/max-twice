package com.prosper.learn.application.service;

import com.prosper.learn.analytics.ranking.service.CourseRankingDomainService;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.application.dto.request.CreateCourseRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.course.*;
import com.prosper.learn.content.course.CourseDomainService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.learning.enrollment.UserCourseDO;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.application.dto.request.UpdateCourseRequest;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.shared.infrastructure.config.SystemDomainService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDomainService;
import com.prosper.learn.learning.enrollment.UserCourseDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseDomainService courseDomainService;
    private final CourseDataService courseDataService;
    private final NodeDataService nodeDataService;
    private final CourseRankingDomainService courseRankingDomainService;
    private final ContentStatsDataService contentStatsDataService;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemProperties systemProperties;
    private final SystemDomainService systemDomainService;
    private final CourseConverter courseConverter;
    private final UserCourseDomainService userCourseDomainService;
    private final UserDomainService userDomainService;


    // ========== DTO 转换方法 ==========

    /**
     * 转换为课程摘要 DTO（列表信息）
     */
    public CourseSummaryDTO toSummaryDTO(CourseDO courseDO) {
        return courseConverter.toSummaryDTO(courseDO);
    }

    public List<CourseSummaryDTO> toSummaryDTO(List<CourseDO> courseDOList) {
        return courseDOList.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为课程简要 DTO（仅 id + name）
     */
    public CourseBriefDTO toBriefDTO(CourseDO courseDO) {
        return courseConverter.toBriefDTO(courseDO);
    }

    public List<CourseBriefDTO> toBriefDTO(List<CourseDO> courseDOList) {
        return courseConverter.toBriefDTO(courseDOList);
    }

    /**
     * 转换为课程详情 DTO（完整信息 + 父课程）
     */
    public CourseDetailDTO toDetailDTO(CourseDO courseDO) {
        if (courseDO == null) return null;

        CourseDetailDTO dto = courseConverter.toDetailDTO(courseDO);

        // 填充父课程信息
        if (courseDO.getParentCourseId() != null && courseDO.getParentCourseId() > 0) {
            CourseDO parentCourseDO = courseDataService.getById(courseDO.getParentCourseId());
            if (parentCourseDO != null) {
                CourseBriefDTO parentDTO = courseConverter.toBriefDTO(parentCourseDO);
                dto.setParentCourse(parentDTO);
            }
        }
        return dto;
    }

    /**
     * 转换为带学习进度的课程 DTO（详情 + 进度）
     */
    public CourseWithProgressDTO toWithProgressDTO(CourseDO courseDO, boolean subscribed, int progress) {
        if (courseDO == null) return null;

        // 先转换为详情 DTO（含 parentCourse）
        CourseDetailDTO detailDTO = toDetailDTO(courseDO);

        // 再转换为带进度的 DTO
        CourseWithProgressDTO dto = courseConverter.toWithProgressDTO(courseDO);

        // 复制 parentCourse 信息
        dto.setParentCourse(detailDTO.getParentCourse());

        // 设置进度信息
        dto.setSubscribed(subscribed);
        dto.setProgress(progress);
        return dto;
    }

    /**
     * 转换为带统计信息的课程 DTO（摘要 + 统计）
     */
    private CourseWithStatsDTO toWithStatsDTO(CourseDO courseDO) {
        CourseWithStatsDTO courseDTO = courseConverter.toWithStatsDTO(courseDO);

        try {
            ContentStatsDO stats = contentStatsDataService.getByContent(ContentType.course, courseDO.getId())
                .orElse(null);

            if (stats != null) {
                courseDTO.setLearnerCount(stats.getInProgressUsers());
                courseDTO.setSubscriptionCount(stats.getBookmarks());
            } else {
                courseDTO.setLearnerCount(0);
                courseDTO.setSubscriptionCount(0);
            }
        } catch (Exception e) {
            // 统计信息获取失败时设置默认值
            courseDTO.setLearnerCount(0);
            courseDTO.setSubscriptionCount(0);
        }
        return courseDTO;
    }

    /**
     * 转换为课程摘要（含统计和进度）DTO
     * 用于课程详情页面，包含统计信息和用户个人数据
     *
     * @param courseDO 课程实体
     * @param userId 当前用户ID（未登录时为null）
     * @return CourseSummaryWithStatsAndProgressDTO
     */
    public CourseSummaryWithStatsAndProgressDTO toSummaryWithStatsAndProgressDTO(CourseDO courseDO, Long userId) {
        if (courseDO == null) return null;

        // 基础转换
        CourseSummaryWithStatsAndProgressDTO dto = courseConverter.toSummaryWithStatsAndProgressDTO(courseDO);

        // 填充统计信息（learnerCount, subscriptionCount）
        try {
            ContentStatsDO stats = contentStatsDataService.getByContent(ContentType.course, courseDO.getId())
                .orElse(null);

            if (stats != null) {
                dto.setLearnerCount(stats.getInProgressUsers());
                dto.setSubscriptionCount(stats.getBookmarks());
            } else {
                dto.setLearnerCount(0);
                dto.setSubscriptionCount(0);
            }
        } catch (Exception e) {
            log.error("获取课程统计信息失败, courseId={}", courseDO.getId(), e);
            dto.setLearnerCount(0);
            dto.setSubscriptionCount(0);
        }

        // 填充用户相关信息（subscribed, progress）
        if (userId != null) {
            // 检查订阅状态
            dto.setSubscribed(userDomainService.isSubscribed(userId, courseDO.getId()));

            // 获取学习进度
            Integer progress = userCourseDomainService.getCourseProgress(userId, courseDO.getId());
            dto.setProgress(progress != null ? progress : 0);
        } else {
            // 未登录用户
            dto.setSubscribed(false);
            dto.setProgress(0);
        }

        return dto;
    }

    /**
     * 批量转换为课程摘要（含统计和进度）DTO
     * 用于课程列表页面
     */
    public List<CourseSummaryWithStatsAndProgressDTO> toSummaryWithStatsAndProgressDTOList(List<CourseDO> courseDOList, Long userId) {
        if (courseDOList == null || courseDOList.isEmpty()) {
            return List.of();
        }

        // 批量查询所有课程的统计信息
        List<Long> courseIds = courseDOList.stream()
            .map(CourseDO::getId)
            .collect(Collectors.toList());

        // 批量查询统计信息（异常时返回空 Map）
        final Map<Long, ContentStatsDO> statsMap = getBatchStats(courseIds);

        // 批量查询用户订阅状态（如果已登录）
        final java.util.Set<Long> subscribedCourseIds;
        if (userId != null) {
            List<Long> userSubscriptions = userDomainService.getSubscriptionIds(userId);
            subscribedCourseIds = new java.util.HashSet<>(userSubscriptions);
        } else {
            subscribedCourseIds = new java.util.HashSet<>();
        }

        // 批量查询用户学习进度（如果已登录）
        final Map<Long, Integer> progressMap = getBatchProgress(userId, courseIds);

        // 转换每个课程
        return courseDOList.stream()
            .map(courseDO -> {
                CourseSummaryWithStatsAndProgressDTO dto = courseConverter.toSummaryWithStatsAndProgressDTO(courseDO);

                // 填充统计信息
                ContentStatsDO stats = statsMap.get(courseDO.getId());
                if (stats != null) {
                    dto.setLearnerCount(stats.getInProgressUsers());
                    dto.setSubscriptionCount(stats.getBookmarks());
                } else {
                    dto.setLearnerCount(0);
                    dto.setSubscriptionCount(0);
                }

                // 填充用户信息
                if (userId != null) {
                    dto.setSubscribed(subscribedCourseIds.contains(courseDO.getId()));
                    dto.setProgress(progressMap.getOrDefault(courseDO.getId(), 0));
                } else {
                    dto.setSubscribed(false);
                    dto.setProgress(0);
                }

                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * 批量获取课程统计信息（异常安全）
     */
    private Map<Long, ContentStatsDO> getBatchStats(List<Long> courseIds) {
        try {
            List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(ContentType.course, courseIds);
            return statsList.stream()
                .collect(Collectors.toMap(ContentStatsDO::getContentId, stats -> stats));
        } catch (Exception e) {
            log.error("批量获取课程统计信息失败", e);
            return new java.util.HashMap<>();
        }
    }

    /**
     * 批量获取用户学习进度
     */
    private Map<Long, Integer> getBatchProgress(Long userId, List<Long> courseIds) {
        if (userId == null) {
            return new java.util.HashMap<>();
        }

        // 使用批量查询避免 N+1 问题
        Map<Long, UserCourseDO> userCoursesMap = userCourseDomainService.getUserCoursesBatch(userId, courseIds);
        return userCoursesMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getProgressPercent() != null ?
                        entry.getValue().getProgressPercent() : 0
            ));
    }

    // ========== 公共业务方法 ==========

    public CourseDetailDTO getCourseById(Long id) {
        CourseDO course = courseDataService.validateAndGet(id);
        return toDetailDTO(course);
    }

    /**
     * 获取课程详情（含统计和用户数据）
     * 用于课程详情页面
     */
    public CourseSummaryWithStatsAndProgressDTO getCourseWithStatsAndProgress(Long id, Long userId) {
        CourseDO course = courseDataService.validateAndGet(id);
        return toSummaryWithStatsAndProgressDTO(course, userId);
    }

    public List<CourseBriefDTO> searchCoursesByName(String name) {
        int searchLimit = systemProperties.getCourse().getSearchLimit();
        List<CourseDO> courseList = courseDataService.searchByName(name, searchLimit);
        return toBriefDTO(courseList);
    }

    /**
     * 获取子课程列表（仅包含已批准的子课程）, 用于展示在课程详情页面
     * @param parentCourseId
     * @return
     */
    public List<CourseSummaryDTO> getSubCourses(long parentCourseId) {
        return toSummaryDTO(courseDataService.listByParentAndState(ContentState.PUBLISHED, parentCourseId));
    }

    /**
     * 更新课程信息
     *
     * 应用层职责：
     * 1. 权限验证（跨User域）
     * 2. 调用领域服务执行更新
     */
    @Transactional
    public void updateCourse(Long id, UpdateCourseRequest request, UserDO operator) {
        // 先验证参数
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("课程更新请求不能为空");
        }

        // 验证权限：只有所有者或管理员可以修改
        if (!courseDomainService.isCreator(id, operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }

        // 调用领域服务执行更新
        courseDomainService.updateCourse(
            id,
            request.getName(),
            request.getDescription(),
            request.getMainCategory(),
            request.getSubCategory()
        );
    }

    // 新增：根据状态和lastId获取课程列表
    public List<CourseSummaryWithStatsAndProgressDTO> getListByStateAndLastIdWithStats(ContentState state, Long lastId, Long userId) {
        List<CourseDO> courseDOList = courseDataService.listByStateAndLastId(state, lastId);
        return toSummaryWithStatsAndProgressDTOList(courseDOList, userId);
    }

    // 新增：根据状态和lastId获取课程列表（分页版本）
    public KeysetPageResponse<CourseSummaryWithStatsAndProgressDTO> getListByStateAndLastIdWithStatsPage(ContentState state, Long lastId, Long userId) {
        List<CourseDO> courseDOList = courseDataService.listByStateAndLastId(state, lastId);
        return buildPageResponse(courseDOList, userId);
    }

    // 新增：根据分类获取已批准的课程列表（支持只传主分类，支持分页）
    public List<CourseSummaryWithStatsAndProgressDTO> getListByCategoryWithStats(Integer mainCategory, Integer subCategory, Long lastId, Long userId) {
        List<CourseDO> courseDOList;

        // 如果传了子分类，按主分类+子分类查询
        if (subCategory != null) {
            courseDOList = courseDataService.listRootByCategory(mainCategory, subCategory, lastId);
        }
        // 只传了主分类，按主分类查询
        else {
            courseDOList = courseDataService.listRootByMainCategory(mainCategory, lastId);
        }

        return toSummaryWithStatsAndProgressDTOList(courseDOList, userId);
    }

    // 新增：根据分类获取已批准的课程列表（分页版本）
    public KeysetPageResponse<CourseSummaryWithStatsAndProgressDTO> getListByCategoryWithStatsPage(Integer mainCategory, Integer subCategory, Long lastId, Long userId) {
        List<CourseDO> courseDOList;

        // 如果传了子分类，按主分类+子分类查询
        if (subCategory != null) {
            courseDOList = courseDataService.listRootByCategory(mainCategory, subCategory, lastId);
        }
        // 只传了主分类，按主分类查询
        else {
            courseDOList = courseDataService.listRootByMainCategory(mainCategory, lastId);
        }

        return buildPageResponse(courseDOList, userId);
    }

    // 新增：根据父课程ID获取子课程列表
    public List<CourseSummaryWithStatsAndProgressDTO> getListByParentWithStats(long parentId, ContentState state, Long userId) {
        List<CourseDO> courseDOList;
        if (state == null) { // null表示获取所有状态
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state, parentId);
        }
        return toSummaryWithStatsAndProgressDTOList(courseDOList, userId);
    }

    // 新增：根据父课程ID获取子课程列表（分页版本）
    public KeysetPageResponse<CourseSummaryWithStatsAndProgressDTO> getListByParentWithStatsPage(long parentId, ContentState state, Long lastId, Long userId) {
        List<CourseDO> courseDOList;
        if (state == null) { // null表示获取所有状态
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state, parentId);
        }
        return buildPageResponse(courseDOList, userId);
    }

    /**
     * 构建分页响应
     * 课程使用简单的 ID 游标分页，不需要 score
     */
    private KeysetPageResponse<CourseSummaryWithStatsAndProgressDTO> buildPageResponse(List<CourseDO> courseDOList, Long userId) {
        int pageSize = 20;
        boolean hasMore = courseDOList.size() > pageSize;

        // 如果数据超过pageSize，只返回pageSize条
        List<CourseDO> actualCourses = hasMore ? courseDOList.subList(0, pageSize) : courseDOList;

        // 转换为 DTO
        List<CourseSummaryWithStatsAndProgressDTO> items = toSummaryWithStatsAndProgressDTOList(actualCourses, userId);

        // 构建 nextCursor
        Long nextLastId = null;
        if (hasMore && !items.isEmpty()) {
            nextLastId = items.get(items.size() - 1).getId();
        }

        return KeysetPageResponse.of(items, hasMore, null, nextLastId);
    }

    // 保留旧方法用于管理后台
    // 新增：根据状态和lastId获取课程列表
    public List<CourseDetailDTO> getListByStateAndLastId(ContentState state, Long lastId) {
        List<CourseDO> courseDOList = courseDataService.listByStateAndLastId(state, lastId);
        return courseDOList.stream()
                .map(this::toDetailDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    // 新增：根据分类获取已批准的课程列表（支持只传主分类，支持分页）
    public List<CourseDetailDTO> getListByCategory(Integer mainCategory, Integer subCategory, Long lastId) {
        List<CourseDO> courseDOList;

        // 如果传了子分类，按主分类+子分类查询
        if (subCategory != null) {
            courseDOList = courseDataService.listRootByCategory(mainCategory, subCategory, lastId);
        }
        // 只传了主分类，按主分类查询
        else {
            courseDOList = courseDataService.listRootByMainCategory(mainCategory, lastId);
        }

        return courseDOList.stream()
                .map(this::toDetailDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    // 新增：根据父课程ID获取子课程列表
    public List<CourseDetailDTO> getListByParent(long parentId, ContentState state) {
        List<CourseDO> courseDOList;
        if (state == null) { // null表示获取所有状态
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state, parentId);
        }
        return courseDOList.stream()
                .map(this::toDetailDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 审核通过课程
     *
     * 应用层职责：编排领域服务 + 发送通知（跨Interaction域）
     */
    public void approve(long id, UserDO operator) {
        CourseDO courseDO = courseDataService.validateAndGet(id);
        Utils.validateStateTransition(courseDO.getState(), ContentState.PUBLISHED);

        int rowsAffected = courseDataService.approve(id);
        if (rowsAffected == 0) {
            throw StatusCode.OPERATION_FAILED.exception();
        }

        // 发布审核通过事件，触发消息通知
        eventPublisher.publishEvent(ContentApprovedEvent.forCourse(
            courseDO.getCreatorId(),
            courseDO.getId(),
            courseDO.getName()
        ));
    }

    /**
     * 拒绝课程
     *
     * 应用层职责：编排领域服务 + 发送通知（跨Interaction域）
     */
    public void reject(long id, String reason, UserDO operator) {
        CourseDO courseDO = courseDataService.validateAndGet(id);
        Utils.validateStateTransition(courseDO.getState(), ContentState.REJECTED);

        int rowsAffected = courseDataService.reject(id, reason);
        if (rowsAffected == 0) {
            throw StatusCode.OPERATION_FAILED.exception();
        }

        // 发布审核拒绝事件，触发消息通知
        eventPublisher.publishEvent(ContentRejectedEvent.forCourse(
            courseDO.getCreatorId(),
            courseDO.getId(),
            courseDO.getName(),
            reason
        ));
    }

    /**
     * 封禁课程
     *
     * 应用层职责：编排领域服务，ban 不发送消息
     */
    public void ban(long id, String reason, UserDO operator) {
        CourseDO courseDO = courseDataService.validateAndGet(id);
        Utils.validateStateTransition(courseDO.getState(), ContentState.BANNED);

        int rowsAffected = courseDataService.ban(id, reason);
        if (rowsAffected == 0) {
            throw StatusCode.OPERATION_FAILED.exception();
        }

        // ban 不发送任何消息或事件
        log.info("课程 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reason);
    }

    /**
     * 删除课程
     */
    public void delete(long id, UserDO operator) {
        courseDomainService.deleteCourse(id);
    }

    @Transactional
    public void createCourse(CreateCourseRequest request, UserDO creator) {
        // 先验证参数
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("课程创建请求不能为空");
        }

        // 验证分类是否有效
        systemDomainService.validateCourseCategory(request.getMainCategory(), request.getSubCategory());

        // 验证通过后创建对象
        CourseDO course = new CourseDO();
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setCreatorId(creator.getId());
        course.setRootNodeId(0L);
        course.setParentCourseId(0L);
        course.setState(ContentState.SUBMITTED.value());
        course.setMainCategory(request.getMainCategory());
        course.setSubCategory(request.getSubCategory());
        courseDataService.insert(course);

        NodeDO nodeDO = NodeDO.createRoot(creator.getId(), course.getId());
        nodeDataService.insert(nodeDO);

        course.setRootNodeId(nodeDO.getId());
        courseDataService.update(course);
    }

    @Transactional
    public void createSubcourse(String name, String description, long parentId, UserDO creator) {
        CourseDO parentCourse = courseDataService.getById(parentId);
        if (parentCourse == null) {
            throw StatusCode.COURSE_PARENT_NOT_FOUND.exception();
        }

        CourseDO subCourse = new CourseDO();
        subCourse.setName(name);
        subCourse.setDescription(description);
        subCourse.setCreatorId(creator.getId());
        subCourse.setRootNodeId(0L);
        subCourse.setParentCourseId(parentId);
        subCourse.setState(ContentState.SUBMITTED.value());
        subCourse.setMainCategory(parentCourse.getMainCategory());
        subCourse.setSubCategory(parentCourse.getSubCategory());

        courseDataService.insert(subCourse);

        NodeDO nodeDO = NodeDO.createRoot(creator.getId(), subCourse.getId());
        nodeDataService.insert(nodeDO);

        subCourse.setRootNodeId(nodeDO.getId());
        courseDataService.update(subCourse);
    }

    // 获取热门课程（使用Redis排行榜）
    public List<CourseWithStatsDTO> getHotCourses(int limit) {
        try {
            // 从Redis获取2倍数量，以防过滤后不足limit个
            int fetchLimit = limit * 2;
            List<Long> hotCourseIds = courseRankingDomainService.getHotCourseIds(fetchLimit);

            if (hotCourseIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<CourseDO> courseDOList = courseDataService.getByIds(hotCourseIds);

            List<CourseWithStatsDTO> result = new ArrayList<>();
            for (CourseDO courseDO : courseDOList) {
                // 只返回已发布状态的课程，过滤屏蔽、拒绝等状态
                if (courseDO.getState() != ContentState.PUBLISHED.value()) {
                    continue;
                }
                result.add(toWithStatsDTO(courseDO));

                // 达到limit个后停止
                if (result.size() >= limit) {
                    break;
                }
            }

            return result;

        } catch (Exception e) {
            throw StatusCode.COURSE_OPERATION_FAILED.exception(e);
        }
    }
}
