package com.twicemax.application.service;

import com.twicemax.analytics.ranking.service.CourseRankingDomainService;
import com.twicemax.application.assembler.CourseAssembler;
import com.twicemax.application.dto.request.CreateCourseRequest;
import com.twicemax.application.dto.response.KeysetPageResponse;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.dto.v2.Cursor;
import com.twicemax.application.dto.response.course.CourseAdminDTO;
import com.twicemax.application.dto.response.course.CourseFullDTO;
import com.twicemax.application.dto.response.course.CourseSummaryDTO;
import com.twicemax.content.course.CourseDomainService;
import com.twicemax.content.node.NodeDO;
import com.twicemax.content.course.CourseDO;
import com.twicemax.content.course.CourseDataService;
import com.twicemax.content.node.NodeDataService;
import com.twicemax.infrastructure.datasource.DataSourceContextHolder;
import com.twicemax.shared.common.utils.Utils;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.application.dto.request.UpdateCourseRequest;
import com.twicemax.shared.infrastructure.config.SystemDomainService;
import com.twicemax.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meilisearch.sdk.model.Searchable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.twicemax.shared.domain.Enums.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

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
     * 用于课程详情页面
     */
    public CourseFullDTO getCourseById(Long id, Long userId) {
        CourseDO course = courseDataService.validateAndGet(id);

        // 检查课程及其父课程的可见性
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
     * 用户端按名称搜索已发布的课程（基于 Meilisearch 全文搜索 + 数据库回查详情）
     * TODO: 全文搜索暂未启用，先用数据库 LIKE 查询
     */
    public List<CourseSummaryDTO> searchPublishedCourses(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 暂时直接走数据库 LIKE 查询
        List<CourseDO> fallback = courseDataService.searchPublishedByName(name, 20);
        return courseAssembler.toSummaryDTO(fallback);

        /*
        // 1. Meilisearch 检索命中 id
        Searchable searchable = meilisearchService.searchCourses(name, 20, 0);
        if (searchable == null) {
            // Meilisearch 不可用时降级到数据库 LIKE 查询
            List<CourseDO> fallback = courseDataService.searchPublishedByName(name, 20);
            return courseAssembler.toSummaryDTO(fallback);
        }

        ArrayList<HashMap<String, Object>> hits = searchable.getHits();
        if (hits == null || hits.isEmpty()) {
            return Collections.emptyList();
        }

        // 保留命中顺序
        List<Long> ids = hits.stream()
            .map(h -> h.get("id"))
            .filter(Objects::nonNull)
            .map(v -> v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString()))
            .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量回查 CourseDO，按 Meilisearch 命中顺序排序
        Map<Long, CourseDO> courseMap = courseDataService.getMapByIds(ids);
        List<CourseDO> ordered = new ArrayList<>(ids.size());
        for (Long id : ids) {
            CourseDO c = courseMap.get(id);
            if (c != null) {
                ordered.add(c);
            }
        }

        // 3. 转换为 CourseSummaryDTO（包含 rootNodeId）
        return courseAssembler.toSummaryDTO(ordered);
        */
    }

    /**
     * 获取子课程列表（仅包含已批准的子课程）, 用于展示在课程详情页面
     * @param parentCourseId
     * @return
     */
    public List<CourseSummaryDTO> getSubCourses(long parentCourseId) {
        return courseAssembler.toSummaryDTO(courseDataService.listByParentAndState(ContentState.PUBLISHED, parentCourseId));
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
            request.getSubCategory(),
            request.getIcon()
        );
    }

    // 根据状态和lastId获取课程列表
    public List<CourseFullDTO> getListByState(ContentState state, Long lastId, Long userId) {
        Byte stateValue = state != null ? state.value() : null;
        List<CourseDO> courseDOList = courseDataService.listByState(stateValue, lastId, DEFAULT_PAGE_SIZE);
        return courseAssembler.toFullDTOList(courseDOList, userId);
    }

    // 根据状态和lastId获取课程列表（分页版本）
    public CursorPage<CourseFullDTO> getListByStatePage(ContentState state, String cursor, Long userId) {
        Byte stateValue = state != null ? state.value() : null;
        List<CourseDO> courseDOList = courseDataService.listByState(stateValue, Cursor.decode(cursor).id(), DEFAULT_PAGE_SIZE);
        return buildPageResponse(courseDOList, userId);
    }

    // 根据分类获取已批准的课程列表（支持只传主分类，支持分页）
    public List<CourseFullDTO> getListByCategory(Integer mainCategory, Integer subCategory, Long lastId, Long userId) {
        List<CourseDO> courseDOList;

        // 如果传了子分类，按主分类+子分类查询
        if (subCategory != null) {
            courseDOList = courseDataService.listRootByCategory(mainCategory, subCategory, lastId);
        }
        // 只传了主分类，按主分类查询
        else {
            courseDOList = courseDataService.listRootByMainCategory(mainCategory, lastId);
        }

        return courseAssembler.toFullDTOList(courseDOList, userId);
    }

    // 根据分类获取已批准的课程列表（分页版本）
    public CursorPage<CourseFullDTO> getListByCategoryPage(Integer mainCategory, Integer subCategory, String cursor, Long userId) {
        Long lastId = Cursor.decode(cursor).id();
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

    // 根据父课程ID获取子课程列表（用户端）
    public List<CourseFullDTO> getListByParent(long parentId, ContentState state, Long userId) {
        List<CourseDO> courseDOList;
        if (state == null) { // null表示获取所有状态
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state, parentId);
        }
        return courseAssembler.toFullDTOList(courseDOList, userId);
    }

    // 根据父课程ID获取子课程列表（分页版本）
    public CursorPage<CourseFullDTO> getListByParentPage(long parentId, ContentState state, String cursor, Long userId) {
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
    private CursorPage<CourseFullDTO> buildPageResponse(List<CourseDO> courseDOList, Long userId) {
        int pageSize = 20;
        boolean hasMore = courseDOList.size() > pageSize;

        // 如果数据超过pageSize，只返回pageSize条
        List<CourseDO> actualCourses = hasMore ? courseDOList.subList(0, pageSize) : courseDOList;

        // 转换为 DTO
        List<CourseFullDTO> items = courseAssembler.toFullDTOList(actualCourses, userId);

        // 构建 nextCursor
        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            nextCursor = Cursor.of(items.get(items.size() - 1).getId()).encode();
        }

        return CursorPage.of(items, hasMore, nextCursor);
    }

    // 管理后台：根据状态获取课程列表（返回分页响应）
    public KeysetPageResponse<CourseAdminDTO> listByState(ContentState state, Long lastId) {
        Byte stateValue = state != null ? state.value() : null;
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
    public List<CourseAdminDTO> getListByParent(long parentId, ContentState state) {
        List<CourseDO> courseDOList;
        if (state == null) { // null表示获取所有状态
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state, parentId);
        }

        return courseAssembler.toAdminDTOList(courseDOList);
    }

    /**
     * 审核通过课程
     *
     * 应用层职责：编排领域服务 + 发送通知（跨Interaction域）
     */
    @Transactional
    public void approve(long id, UserDO operator) {
        CourseDO courseDO = courseDataService.validateAndGet(id);
        Utils.validateStateTransition(courseDO.getState(), ContentState.PUBLISHED);

        int rowsAffected = courseDataService.approve(id);
        if (rowsAffected == 0) {
            throw StatusCode.OPERATION_FAILED.exception();
        }

        // 如果是子课程，增加父课程的子课程数量
        if (courseDO.getParentCourseId() != null && courseDO.getParentCourseId() > 0) {
            courseDataService.incrementSubCourseCount(courseDO.getParentCourseId());
        }

        // 发布审核通过事件，触发消息通知
        eventPublisher.publishEvent(ContentApprovedEvent.forCourse(
            courseDO.getCreatorId(),
            courseDO.getId(),
            courseDO.getName()
        ));

        // 异步更新搜索索引
        courseDO.setState(ContentState.PUBLISHED.value());
        meilisearchService.indexCourse(courseDO, DataSourceContextHolder.getLanguage());
    }

    /**
     * 拒绝课程
     *
     * 应用层职责：编排领域服务 + 发送通知（跨Interaction域）
     */
    @Transactional
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

        // 异步更新搜索索引（从索引中移除）
        courseDO.setState(ContentState.REJECTED.value());
        meilisearchService.indexCourse(courseDO, DataSourceContextHolder.getLanguage());
    }

    /**
     * 封禁课程
     *
     * 应用层职责：编排领域服务，ban 不发送消息
     */
    @Transactional
    public void ban(long id, String reason, UserDO operator) {
        CourseDO courseDO = courseDataService.validateAndGet(id);
        Utils.validateStateTransition(courseDO.getState(), ContentState.BANNED);

        // 如果是已发布的子课程，减少父课程的子课程数量
        boolean wasPublished = courseDO.getState() == ContentState.PUBLISHED.value();

        int rowsAffected = courseDataService.ban(id, reason);
        if (rowsAffected == 0) {
            throw StatusCode.OPERATION_FAILED.exception();
        }

        // 只有之前是已发布状态的子课程，才减少父课程的计数
        if (wasPublished && courseDO.getParentCourseId() != null && courseDO.getParentCourseId() > 0) {
            courseDataService.decrementSubCourseCount(courseDO.getParentCourseId());
        }

        // ban 不发送任何消息或事件
        log.info("课程 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reason);

        // 异步更新搜索索引（从索引中移除）
        courseDO.setState(ContentState.BANNED.value());
        meilisearchService.indexCourse(courseDO, DataSourceContextHolder.getLanguage());
    }

    /**
     * 删除课程
     */
    @Transactional
    public void delete(long id, UserDO operator) {
        CourseDO courseDO = courseDataService.validateAndGet(id);

        // 如果是已发布的子课程，减少父课程的子课程数量
        boolean wasPublished = courseDO.getState() == ContentState.PUBLISHED.value();
        if (wasPublished && courseDO.getParentCourseId() != null && courseDO.getParentCourseId() > 0) {
            courseDataService.decrementSubCourseCount(courseDO.getParentCourseId());
        }

        courseDomainService.deleteCourse(id);

        // 异步从搜索索引中移除
        meilisearchService.deleteCourse(id, DataSourceContextHolder.getLanguage());
    }

    @Transactional
    public Long createCourse(CreateCourseRequest request, UserDO creator) {
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

        NodeDO nodeDO = new NodeDO(creator.getId(), course.getId(), course.getName(),
                course.getDescription(), ContentState.PUBLISHED.value(), Bool.TRUE.value());
        nodeDataService.insert(nodeDO);

        course.setRootNodeId(nodeDO.getId());
        courseDataService.update(course);

        return course.getId();
    }

    /**
     * 创建课程并自动审核通过（Admin专用）
     */
    @Transactional
    public Long createAndApprove(CreateCourseRequest request, UserDO creator) {
        // 先创建课程（状态为SUBMITTED）
        Long courseId = createCourse(request, creator);

        // 审核通过
        approve(courseId, creator);

        return courseId;
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

        NodeDO nodeDO = new NodeDO(creator.getId(), subCourse.getId(), subCourse.getName(),
                subCourse.getDescription(), ContentState.PUBLISHED.value(), Bool.TRUE.value());
        nodeDataService.insert(nodeDO);

        subCourse.setRootNodeId(nodeDO.getId());
        courseDataService.update(subCourse);
    }

    // 获取热门课程（使用Redis排行榜）
    public List<CourseFullDTO> getHotCourses(int limit) {
        try {
            // 从Redis获取2倍数量，以防过滤后不足limit个
            int fetchLimit = limit * 2;
            List<Long> hotCourseIds = courseRankingDomainService.getHotCourseIds(fetchLimit);

            if (hotCourseIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<CourseDO> courseDOList = courseDataService.getByIds(hotCourseIds);

            // 过滤已发布状态的课程
            List<CourseDO> publishedCourses = courseDOList.stream()
                .filter(c -> c.getState() == ContentState.PUBLISHED.value())
                .limit(limit)
                .collect(Collectors.toList());

            return courseAssembler.toFullDTOList(publishedCourses, null);

        } catch (Exception e) {
            throw StatusCode.COURSE_OPERATION_FAILED.exception(e);
        }
    }

    /**
     * 重新计算所有课程的子课程数量
     * 分批处理，只更新不一致的记录
     *
     * @param progressCallback 进度回调，传入当前进度信息
     * @return 包含 checked（检查的父课程数）、updated（更新的数量）、timeout（是否超时）
     */
    public Map<String, Integer> recalculateAllSubCourseCounts(java.util.function.Consumer<Object> progressCallback) {
        long startTime = System.currentTimeMillis();
        long timeout = 10 * 60 * 1000; // 10分钟超时

        int checked = 0;
        int updated = 0;
        Long lastId = null;

        while (true) {
            // 每批检查是否超时
            if ((System.currentTimeMillis() - startTime) > timeout) {
                log.warn("子课程数量重算任务超时，已处理 {} 个，更新 {} 个", checked, updated);
                return Map.of("checked", checked, "updated", updated, "timeout", 1);
            }

            // 分页查询所有课程
            List<CourseDO> courses = courseDataService.listByLastId(lastId);
            if (courses.isEmpty()) {
                break;
            }

            for (CourseDO course : courses) {
                // 跳过子课程
                if (course.getParentCourseId() != null && course.getParentCourseId() > 0) {
                    continue;
                }

                checked++;
                int actualCount = courseDataService.countPublishedSubCourses(course.getId());
                int currentCount = course.getSubCourseCount() != null ? course.getSubCourseCount() : 0;

                // 只有不一致时才更新
                if (actualCount != currentCount) {
                    courseDataService.updateSubCourseCount(course.getId(), actualCount);
                    updated++;
                    log.info("课程 {} 子课程数量从 {} 更新为 {}", course.getId(), currentCount, actualCount);
                }
            }

            lastId = courses.get(courses.size() - 1).getId();

            // 每批处理完后报告进度
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
