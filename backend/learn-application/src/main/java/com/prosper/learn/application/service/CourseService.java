package com.prosper.learn.application.service;

import com.prosper.learn.analytics.ranking.service.CourseRankingDomainService;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.application.dto.request.CreateCourseRequest;
import com.prosper.learn.application.dto.response.course.*;
import com.prosper.learn.content.course.CourseDomainService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.application.dto.request.UpdateCourseRequest;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
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
    private final CourseConverter courseConverter;


    // ========== DTO 转换方法 ==========

    /**
     * 转换为课程摘要 DTO（列表信息）
     */
    public CourseSummaryDTO toSummaryDTO(CourseDO courseDO) {
        CourseSummaryDTO dto = courseConverter.toSummaryDTO(courseDO);

        // 检查课程是否被屏蔽或拒绝
        if (courseDO.getState() == ContentState.REJECTED.value() ||
            courseDO.getState() == ContentState.BANNED.value()) {
            dto.setAvailable(false);
        }

        return dto;
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

    // ========== 公共业务方法 ==========

    public CourseDetailDTO getCourseById(Long id) {
        CourseDO course = courseDomainService.validateAndGet(id);
        return toDetailDTO(course);
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
            throw ErrorCode.INVALID_PARAMETER.exception("课程更新请求不能为空");
        }

        // 验证权限：只有所有者或管理员可以修改
        if (!courseDomainService.isCreator(id, operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw ErrorCode.PERMISSION_DENIED.exception();
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
        CourseDO courseDO = courseDomainService.validateAndGet(id);
        courseDomainService.validateStateForApproval(courseDO);

        int rowsAffected = courseDataService.approve(id);
        courseDomainService.validateOperationResult(rowsAffected);

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
        CourseDO courseDO = courseDomainService.validateAndGet(id);
        courseDomainService.validateStateForRejection(courseDO);

        int rowsAffected = courseDataService.reject(id, reason);
        courseDomainService.validateOperationResult(rowsAffected);

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
        CourseDO courseDO = courseDomainService.validateAndGet(id);
        courseDomainService.validateStateForBan(courseDO);

        int rowsAffected = courseDataService.ban(id, reason);
        courseDomainService.validateOperationResult(rowsAffected);

        // ban 不发送任何消息或事件
        log.info("课程 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reason);
    }

    /**
     * 删除课程
     */
    public void delete(long id, UserDO operator) {
        courseDomainService.deleteCourse(id);
    }

    public void createCourse(CreateCourseRequest request, UserDO creator) {
        // 先验证参数
        if (request == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("课程创建请求不能为空");
        }

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

    public void createSubcourse(String name, String description, long parentId, UserDO creator) {
        courseDomainService.validateParentExists(parentId);
        CourseDO parentCourse = courseDataService.getById(parentId);

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
            throw ErrorCode.COURSE_OPERATION_FAILED.exception(e);
        }
    }

    // 获取热门课程完整排行榜
    public List<CourseWithStatsDTO> getHotCoursesRanking() {
        try {
            int rankingLimit = systemProperties.getCourse().getHotCoursesRankingLimit();
            // 从Redis获取2倍数量，以防过滤后不足rankingLimit个
            int fetchLimit = rankingLimit * 2;
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

                // 达到rankingLimit个后停止
                if (result.size() >= rankingLimit) {
                    break;
                }
            }

            return result;

        } catch (Exception e) {
            throw ErrorCode.COURSE_OPERATION_FAILED.exception(e);
        }
    }
}
