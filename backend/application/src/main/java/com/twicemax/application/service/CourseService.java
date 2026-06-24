package com.twicemax.application.service;

import com.twicemax.analytics.ranking.service.CourseRankingDomainService;
import com.twicemax.application.assembler.CourseAssembler;
import com.twicemax.application.dto.request.CreateCourseRequest;
import com.twicemax.application.dto.request.UpdateCourseRequest;
import com.twicemax.application.dto.response.KeysetPageResponse;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.dto.v2.Cursor;
import com.twicemax.application.dto.response.course.CourseAdminDTO;
import com.twicemax.application.dto.response.course.CourseFullDTO;
import com.twicemax.application.dto.response.course.CourseSummaryDTO;
import com.twicemax.content.course.CourseDO;
import com.twicemax.content.course.CourseDataService;
import com.twicemax.content.course.CourseDomainService;
import com.twicemax.content.node.NodeDO;
import com.twicemax.content.node.NodeDataService;
import com.twicemax.infrastructure.datasource.DataSourceContextHolder;
import com.twicemax.shared.domain.Enums.Bool;
import com.twicemax.shared.domain.Enums.ContentState;
import com.twicemax.shared.domain.Enums.ContentType;
import com.twicemax.shared.domain.Enums.NewContentState;
import com.twicemax.shared.domain.Enums.UserRole;
import com.twicemax.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemDomainService;
import com.twicemax.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private static final String DEFAULT_EMPTY_STRING = "";

    private final CourseDomainService courseDomainService;
    private final CourseDataService courseDataService;
    private final NodeDataService nodeDataService;
    private final CourseRankingDomainService courseRankingDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemDomainService systemDomainService;
    private final CourseAssembler courseAssembler;
    private final ContentVisibilityService contentVisibilityService;
    private final MeilisearchService meilisearchService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    // ========== 公共业务方法 ==========

    /**
     * 获取课程详情（管理后台专用）
     */
    public CourseAdminDTO getAdminCourseById(Long id) {
        CourseDO course = courseDataService.validateAndGet(id);
        return courseAssembler.toAdminDTO(course);
    }

    /**
     * 获取课程完整信息
     */
    public CourseFullDTO getCourseById(Long id, Long userId) {
        CourseDO course = courseDataService.validateAndGet(id);
        contentVisibilityService.validateVisibility(ContentType.course, id, userId);
        return courseAssembler.toFullDTO(course, userId);
    }

    /**
     * 管理后台按名称搜索课程（搜索所有状态，支持滚动分页）
     */
    public KeysetPageResponse<CourseAdminDTO> searchCoursesByName(String name, Long lastId) {
        List<CourseDO> courseList = courseDataService.searchByName(name, lastId, DEFAULT_PAGE_SIZE + 1);

        boolean hasMore = courseList.size() > DEFAULT_PAGE_SIZE;
        if (hasMore) {
            courseList = courseList.subList(0, DEFAULT_PAGE_SIZE);
        }

        List<CourseAdminDTO> dtoList = courseAssembler.toAdminDTOList(courseList);

        Long nextLastId = dtoList.isEmpty() ? null : dtoList.get(dtoList.size() - 1).getId();
        return KeysetPageResponse.of(dtoList, hasMore, null, nextLastId);
    }

    /**
     * 用户端按名称搜索已发布的课程
     */
    public List<CourseSummaryDTO> searchPublishedCourses(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<CourseDO> fallback = courseDataService.searchPublishedByName(name, 20);
        return courseAssembler.toSummaryDTO(fallback);
    }

    /**
     * 获取子课程列表（仅包含已发布的子课程）
     */
    public List<CourseSummaryDTO> getSubCourses(long parentCourseId) {
        return courseAssembler.toSummaryDTO(
                courseDataService.listByParentAndState(NewContentState.PUBLISHED_VALUE, parentCourseId));
    }

    /**
     * 管理员编辑课程：走 revision 留审计。
     * 仅 ADMIN 可调用；普通用户编辑走 resubmit。
     */
    @Transactional
    public void updateCourse(Long id, UpdateCourseRequest request, UserDO operator) {
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("课程更新请求不能为空");
        }
        if (!operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }
        courseDomainService.edit(
                id,
                operator.getId(),
                request.getName(),
                request.getDescription(),
                request.getIcon(),
                request.getMainCategory(),
                request.getSubCategory()
        );
    }

    // ========== 列表 / 分页查询 ==========

    /**
     * 按主体状态获取主课程列表（用户端简单分页）。
     * state 为 NewContentState；旧调用方传 null 表示不过滤。
     */
    public List<CourseFullDTO> getListByState(NewContentState state, Long lastId, Long userId) {
        String stateValue = state != null ? state.value() : null;
        List<CourseDO> courseDOList = courseDataService.listByState(stateValue, lastId, DEFAULT_PAGE_SIZE);
        return courseAssembler.toFullDTOList(courseDOList, userId);
    }

    /**
     * 按主体状态分页（CursorPage 版本）
     */
    public CursorPage<CourseFullDTO> getListByStatePage(NewContentState state, String cursor, Long userId) {
        String stateValue = state != null ? state.value() : null;
        List<CourseDO> courseDOList = courseDataService.listByState(stateValue, Cursor.decode(cursor).id(), DEFAULT_PAGE_SIZE);
        return buildPageResponse(courseDOList, userId);
    }

    public List<CourseFullDTO> getListByCategory(Integer mainCategory, Integer subCategory, Long lastId, Long userId) {
        List<CourseDO> courseDOList;
        if (subCategory != null) {
            courseDOList = courseDataService.listRootByCategory(mainCategory, subCategory, lastId);
        } else {
            courseDOList = courseDataService.listRootByMainCategory(mainCategory, lastId);
        }
        return courseAssembler.toFullDTOList(courseDOList, userId);
    }

    public CursorPage<CourseFullDTO> getListByCategoryPage(Integer mainCategory, Integer subCategory, String cursor, Long userId) {
        Long lastId = Cursor.decode(cursor).id();
        List<CourseDO> courseDOList;
        if (subCategory != null) {
            courseDOList = courseDataService.listRootByCategory(mainCategory, subCategory, lastId);
        } else {
            courseDOList = courseDataService.listRootByMainCategory(mainCategory, lastId);
        }
        return buildPageResponse(courseDOList, userId);
    }

    /**
     * 按父课程ID获取子课程列表（用户端）
     * @param state 可为 null（返回所有），或 NewContentState
     */
    public List<CourseFullDTO> getListByParent(long parentId, NewContentState state, Long userId) {
        List<CourseDO> courseDOList;
        if (state == null) {
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state.value(), parentId);
        }
        return courseAssembler.toFullDTOList(courseDOList, userId);
    }

    public CursorPage<CourseFullDTO> getListByParentPage(long parentId, NewContentState state, String cursor, Long userId) {
        List<CourseDO> courseDOList;
        if (state == null) {
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state.value(), parentId);
        }
        return buildPageResponse(courseDOList, userId);
    }

    private CursorPage<CourseFullDTO> buildPageResponse(List<CourseDO> courseDOList, Long userId) {
        int pageSize = 20;
        boolean hasMore = courseDOList.size() > pageSize;
        List<CourseDO> actualCourses = hasMore ? courseDOList.subList(0, pageSize) : courseDOList;
        List<CourseFullDTO> items = courseAssembler.toFullDTOList(actualCourses, userId);

        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            nextCursor = Cursor.of(items.get(items.size() - 1).getId()).encode();
        }

        return CursorPage.of(items, hasMore, nextCursor);
    }

    // 管理后台：根据状态获取主课程列表
    public KeysetPageResponse<CourseAdminDTO> listByState(NewContentState state, Long lastId) {
        String stateValue = state != null ? state.value() : null;
        List<CourseDO> courseDOList = courseDataService.listByState(stateValue, lastId, DEFAULT_PAGE_SIZE + 1);

        boolean hasMore = courseDOList.size() > DEFAULT_PAGE_SIZE;
        if (hasMore) {
            courseDOList = courseDOList.subList(0, DEFAULT_PAGE_SIZE);
        }

        List<CourseAdminDTO> dtoList = courseAssembler.toAdminDTOList(courseDOList);

        Long nextLastId = dtoList.isEmpty() ? null : dtoList.get(dtoList.size() - 1).getId();
        return KeysetPageResponse.of(dtoList, hasMore, null, nextLastId);
    }

    // 管理后台：根据父课程ID获取子课程列表
    public List<CourseAdminDTO> getListByParent(long parentId, NewContentState state) {
        List<CourseDO> courseDOList;
        if (state == null) {
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state.value(), parentId);
        }
        return courseAssembler.toAdminDTOList(courseDOList);
    }

    /**
     * 用户视角：分页查看自己创建的课程申请。
     * @param state 可为 null（默认排除 BANNED）；或 NEVER_PUBLISHED / PUBLISHED
     */
    public List<CourseFullDTO> getUserCourses(Long userId, String cursor, NewContentState state, int limit) {
        List<CourseDO> courseDOList = courseDomainService.listByCreator(
                userId, Cursor.decode(cursor).id(), limit, state);
        return courseAssembler.toFullDTOList(courseDOList, userId);
    }

    // ========== Command 方法（revision 模型）==========

    /**
     * 创建课程申请。parentCourseId=0 表示主课程，>0 表示子课程。
     * 申请创建后立即创建一个 PUBLISHED 的 rootNode（保留现状，rootNode 可见性级联问题留作 TODO）。
     */
    @Transactional
    public Long createCourse(CreateCourseRequest request, UserDO creator) {
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("课程创建请求不能为空");
        }

        long parentCourseId = 0L;
        Integer mainCategory = request.getMainCategory();
        Integer subCategory = request.getSubCategory();
        // 如果是子课程，从父课程继承分类（保持原 createSubcourse 的语义）
        if (request.getParentCourseId() != null && request.getParentCourseId() > 0) {
            CourseDO parentCourse = courseDataService.getById(request.getParentCourseId());
            if (parentCourse == null) {
                throw StatusCode.COURSE_PARENT_NOT_FOUND.exception();
            }
            parentCourseId = parentCourse.getId();
            mainCategory = parentCourse.getMainCategory();
            subCategory = parentCourse.getSubCategory();
        } else {
            systemDomainService.validateCourseCategory(mainCategory, subCategory);
        }

        Long courseId = courseDomainService.create(
                creator.getId(),
                request.getName(),
                request.getDescription(),
                null, // icon 由后续编辑/审核流程补充
                mainCategory,
                subCategory,
                parentCourseId
        );

        // 创建 rootNode（保留现状）
        NodeDO nodeDO = new NodeDO(creator.getId(), courseId, request.getName(),
                request.getDescription(), ContentState.PUBLISHED.value(), Bool.TRUE.value());
        nodeDataService.insert(nodeDO);

        courseDomainService.bindRootNode(courseId, nodeDO.getId());

        return courseId;
    }

    /**
     * 创建课程并自动审核通过（Admin 专用）。
     * 内部 = createCourse + approve；approve 走 revision 模型完成 PUBLISHED 切换。
     */
    @Transactional
    public Long createAndApprove(CreateCourseRequest request, UserDO creator) {
        Long courseId = createCourse(request, creator);
        approve(courseId, creator);
        return courseId;
    }

    /**
     * 用户被驳回 / 撤回后重新提交。
     */
    @Transactional
    public Long resubmit(Long courseId, UpdateCourseRequest request, UserDO author) {
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("更新请求不能为空");
        }
        return courseDomainService.resubmit(
                courseId,
                author.getId(),
                request.getName(),
                request.getDescription(),
                request.getIcon(),
                request.getMainCategory(),
                request.getSubCategory()
        );
    }

    /**
     * 作者撤回审核中的版本。
     */
    @Transactional
    public void withdraw(long courseId, UserDO author) {
        courseDomainService.withdraw(courseId, author.getId());
    }

    /**
     * 审核通过课程。
     * 维护父课程子课程数：若是首次发布的子课程，父课程 subCourseCount + 1。
     */
    @Transactional
    public void approve(long id, UserDO operator) {
        CourseDO before = courseDataService.validateAndGet(id);
        boolean wasPublished = NewContentState.PUBLISHED_VALUE.equals(before.getState());

        CourseDomainService.CourseContent content = courseDomainService.approve(id, operator.getId());

        // 仅在"从非 PUBLISHED 进入 PUBLISHED"时维护父课程计数
        if (!wasPublished && content.parentCourseId > 0) {
            courseDataService.incrementSubCourseCount(content.parentCourseId);
        }

        CourseDO afterDO = courseDataService.validateAndGet(id);

        eventPublisher.publishEvent(ContentApprovedEvent.forCourse(
                afterDO.getCreatorId(),
                afterDO.getId(),
                afterDO.getName()
        ));

        meilisearchService.indexCourse(afterDO, DataSourceContextHolder.getLanguage());
    }

    /**
     * 拒绝课程：revision → REJECTED。主体状态保持不变，无需维护子课程数。
     */
    @Transactional
    public void reject(long id, String reason, UserDO operator) {
        CourseDO course = courseDataService.validateAndGet(id);
        courseDomainService.reject(id, reason, operator.getId());

        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;
        eventPublisher.publishEvent(ContentRejectedEvent.forCourse(
                course.getCreatorId(),
                course.getId(),
                course.getName(),
                reasonValue
        ));

        // reject 不改变主体可见性，不需要重建索引
    }

    /**
     * 封禁课程。
     * 维护父课程子课程数：若被封禁前是 PUBLISHED 的子课程，父课程 subCourseCount - 1。
     */
    @Transactional
    public void ban(long id, String reason, UserDO operator) {
        CourseDO course = courseDataService.validateAndGet(id);
        boolean wasPublished = NewContentState.PUBLISHED_VALUE.equals(course.getState());
        Long parentCourseId = course.getParentCourseId();

        courseDomainService.ban(id, reason, operator.getId());

        if (wasPublished && parentCourseId != null && parentCourseId > 0) {
            courseDataService.decrementSubCourseCount(parentCourseId);
        }

        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;
        eventPublisher.publishEvent(ContentBannedEvent.forCourse(
                course.getCreatorId(),
                course.getId(),
                course.getName(),
                reasonValue
        ));

        log.info("课程 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reasonValue);

        meilisearchService.indexCourse(courseDataService.getById(id), DataSourceContextHolder.getLanguage());
    }

    /**
     * 解封课程。
     * 若解封后状态为 PUBLISHED 且是子课程，父课程 subCourseCount + 1。
     */
    @Transactional
    public void restore(long id, UserDO operator) {
        CourseDO course = courseDataService.validateAndGet(id);
        Long parentCourseId = course.getParentCourseId();

        NewContentState newState = courseDomainService.restore(id);

        if (NewContentState.PUBLISHED.equals(newState) && parentCourseId != null && parentCourseId > 0) {
            courseDataService.incrementSubCourseCount(parentCourseId);
        }

        meilisearchService.indexCourse(courseDataService.getById(id), DataSourceContextHolder.getLanguage());
    }

    /**
     * 删除课程。
     * 若删除前是 PUBLISHED 的子课程，父课程 subCourseCount - 1。
     */
    @Transactional
    public void delete(long id, UserDO operator) {
        CourseDO course = courseDataService.validateAndGet(id);
        boolean wasPublished = NewContentState.PUBLISHED_VALUE.equals(course.getState());
        if (wasPublished && course.getParentCourseId() != null && course.getParentCourseId() > 0) {
            courseDataService.decrementSubCourseCount(course.getParentCourseId());
        }

        courseDomainService.deleteCourse(id);

        meilisearchService.deleteCourse(id, DataSourceContextHolder.getLanguage());
    }

    // 获取热门课程（使用Redis排行榜）
    public List<CourseFullDTO> getHotCourses(int limit) {
        try {
            int fetchLimit = limit * 2;
            List<Long> hotCourseIds = courseRankingDomainService.getHotCourseIds(fetchLimit);

            if (hotCourseIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<CourseDO> courseDOList = courseDataService.getByIds(hotCourseIds);

            // 过滤已发布状态的课程
            List<CourseDO> publishedCourses = courseDOList.stream()
                    .filter(c -> NewContentState.PUBLISHED_VALUE.equals(c.getState()))
                    .limit(limit)
                    .collect(Collectors.toList());

            return courseAssembler.toFullDTOList(publishedCourses, null);

        } catch (Exception e) {
            throw StatusCode.COURSE_OPERATION_FAILED.exception(e);
        }
    }

    /**
     * 重新计算所有课程的子课程数量
     */
    public Map<String, Integer> recalculateAllSubCourseCounts(java.util.function.Consumer<Object> progressCallback) {
        long startTime = System.currentTimeMillis();
        long timeout = 10 * 60 * 1000;

        int checked = 0;
        int updated = 0;
        Long lastId = null;

        while (true) {
            if ((System.currentTimeMillis() - startTime) > timeout) {
                log.warn("子课程数量重算任务超时，已处理 {} 个，更新 {} 个", checked, updated);
                return Map.of("checked", checked, "updated", updated, "timeout", 1);
            }

            List<CourseDO> courses = courseDataService.listByLastId(lastId);
            if (courses.isEmpty()) {
                break;
            }

            for (CourseDO course : courses) {
                if (course.getParentCourseId() != null && course.getParentCourseId() > 0) {
                    continue;
                }

                checked++;
                int actualCount = courseDataService.countPublishedSubCourses(course.getId());
                int currentCount = course.getSubCourseCount() != null ? course.getSubCourseCount() : 0;

                if (actualCount != currentCount) {
                    courseDataService.updateSubCourseCount(course.getId(), actualCount);
                    updated++;
                    log.info("课程 {} 子课程数量从 {} 更新为 {}", course.getId(), currentCount, actualCount);
                }
            }

            lastId = courses.get(courses.size() - 1).getId();

            if (progressCallback != null) {
                final int currentChecked = checked;
                final int currentUpdated = updated;
                progressCallback.accept(Map.of("checked", currentChecked, "updated", currentUpdated));
            }
        }

        log.info("子课程数量重算完成: 检查 {} 个父课程, 更新 {} 个", checked, updated);
        return Map.of("checked", checked, "updated", updated, "timeout", 0);
    }
}
