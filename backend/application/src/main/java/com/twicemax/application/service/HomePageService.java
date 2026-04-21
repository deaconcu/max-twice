package com.twicemax.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.analytics.stats.mapper.UserStatsDO;
import com.twicemax.analytics.stats.service.UserStatsDomainService;
import com.twicemax.application.converter.CourseConverter;
import com.twicemax.application.converter.RoleConverter;
import com.twicemax.application.converter.RoadmapConverter;
import com.twicemax.application.dto.response.ReviewSummaryDTO;
import com.twicemax.application.dto.response.course.CourseBriefDTO;
import com.twicemax.application.dto.response.course.CourseFullDTO;
import com.twicemax.application.dto.response.home.HomePageDTO;
import com.twicemax.application.dto.response.home.UserLearningStatsDTO;
import com.twicemax.application.dto.response.role.RoleBriefDTO;
import com.twicemax.application.dto.response.role.RoleDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.twicemax.application.dto.response.userlearning.UserLearningDTO;
import com.twicemax.content.course.CourseDO;
import com.twicemax.content.course.CourseDataService;
import com.twicemax.content.role.RoleDO;
import com.twicemax.content.role.RoleDataService;
import com.twicemax.content.roadmap.RoadmapDO;
import com.twicemax.content.roadmap.RoadmapDataService;
import com.twicemax.interaction.bookmark.BookmarkDO;
import com.twicemax.interaction.bookmark.BookmarkDataService;
import com.twicemax.shared.common.util.TimeZoneUtil;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.infrastructure.config.SystemDataService;
import com.twicemax.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页数据聚合服务
 * 负责聚合首页所需的所有数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomePageService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CONFIG_KEY_HOMEPAGE_RECOMMENDATIONS = "homepage_recommendations";

    private final PlatformStatsService platformStatsService;
    private final UserStatsDomainService userStatsDomainService;
    private final UserLearningService userLearningService;
    private final CourseService courseService;
    private final RoleService roleService;
    private final MemoryBankService memoryBankService;
    private final BookmarkDataService bookmarkDataService;
    private final RoleDataService roleDataService;
    private final RoadmapDataService roadmapDataService;
    private final CourseDataService courseDataService;
    private final SystemDataService systemDataService;
    private final RoleConverter roleConverter;
    private final RoadmapConverter roadmapConverter;
    private final CourseConverter courseConverter;

    // 首页展示的数量限制
    private static final int LEARNING_ROLES_LIMIT = 8;
    private static final int LEARNING_COURSES_LIMIT = 8;
    private static final int HOT_ROLES_LIMIT = 10;
    private static final int HOT_COURSES_LIMIT = 30;
    private static final int BOOKMARKED_ROLES_LIMIT = 10;

    /**
     * 获取首页聚合数据
     *
     * @param user 当前用户
     * @return 首页聚合数据
     */
    public HomePageDTO getHomePageData(UserDO user) {
        Long userId = user.getId();
        HomePageDTO homePageDTO = new HomePageDTO();

        // 1. 平台统计数据
        homePageDTO.setPlatformStats(platformStatsService.getPlatformStats());

        // 2. 用户学习统计
        homePageDTO.setUserStats(getUserLearningStats(user));

        // 3. 收藏的角色（最新收藏的在前，最多10个）
        homePageDTO.setBookmarkedRoles(getBookmarkedRoles(userId));

        // 4. 正在学习的角色路线
        homePageDTO.setLearningRoles(getLearningRoles(userId));

        // 5. 正在学习的课程
        homePageDTO.setLearningCourses(getLearningCourses(userId));

        // 6. 复习概览
        homePageDTO.setReviewSummary(getReviewSummary(userId));

        // 7. 热门角色榜单
        homePageDTO.setHotRoles(getHotRoles());

        // 8. 热门课程榜单
        homePageDTO.setHotCourses(getHotCourses());

        // 9. 新手推荐（从系统配置读取）
        loadBeginnerRecommendations(homePageDTO);

        return homePageDTO;
    }

    /**
     * 获取用户学习统计
     */
    private UserLearningStatsDTO getUserLearningStats(UserDO user) {
        Long userId = user.getId();
        UserLearningStatsDTO stats = new UserLearningStatsDTO();

        try {
            UserStatsDO userStats = userStatsDomainService.getUserStats(userId);
            if (userStats != null) {
                stats.setCoursesInProgress(userStats.getLearningCourseCount() != null
                        ? userStats.getLearningCourseCount() : 0);
                stats.setRolesInProgress(userStats.getInProgressRoleCount() != null
                        ? userStats.getInProgressRoleCount() : 0);
            }
            // 获取连续学习天数
            LocalDate userToday = TimeZoneUtil.getUserToday(user.getTimezone());
            int streakDays = userStatsDomainService.getLearningStreakDays(userId, userToday);
            stats.setLearningDays(streakDays);
        } catch (Exception e) {
            log.error("获取用户学习统计失败, userId={}", userId, e);
        }

        return stats;
    }

    /**
     * 获取正在学习的角色路线
     */
    private List<UserLearningDTO<Object>> getLearningRoles(Long userId) {
        try {
            return userLearningService.getByUserWithObjects(
                    userId,
                    Enums.ContentType.roadmap,
                    Enums.UserProgressState.IN_PROGRESS.value(),
                    null,
                    LEARNING_ROLES_LIMIT
            );
        } catch (Exception e) {
            log.error("获取正在学习的角色路线失败, userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取正在学习的课程
     */
    private List<UserLearningDTO<Object>> getLearningCourses(Long userId) {
        try {
            return userLearningService.getAllCoursesProgress(userId, null, null, LEARNING_COURSES_LIMIT);
        } catch (Exception e) {
            log.error("获取正在学习的课程失败, userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取复习概览
     */
    private ReviewSummaryDTO getReviewSummary(Long userId) {
        try {
            return memoryBankService.getReviewSummary(userId, null);
        } catch (Exception e) {
            log.error("获取复习概览失败, userId={}", userId, e);
            return new ReviewSummaryDTO();
        }
    }

    /**
     * 获取热门角色榜单
     */
    private List<RoleDTO> getHotRoles() {
        try {
            return roleService.getHotRoles(HOT_ROLES_LIMIT);
        } catch (Exception e) {
            log.error("获取热门角色失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取热门课程榜单
     */
    private List<CourseFullDTO> getHotCourses() {
        try {
            return courseService.getHotCourses(HOT_COURSES_LIMIT);
        } catch (Exception e) {
            log.error("获取热门课程失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取收藏的角色（最新收藏的在前，最多10个）
     */
    private List<RoleDTO> getBookmarkedRoles(Long userId) {
        try {
            // 分页查询第一页（按收藏时间倒序）
            List<BookmarkDO> bookmarks = bookmarkDataService.listByUserAndLastId(
                    userId,
                    Enums.ContentType.role.value(),
                    null,
                    BOOKMARKED_ROLES_LIMIT
            );
            if (bookmarks.isEmpty()) {
                return new ArrayList<>();
            }

            // 提取角色ID
            List<Long> roleIds = bookmarks.stream()
                    .map(BookmarkDO::getObjectId)
                    .toList();

            // 批量获取角色信息
            List<RoleDO> roleDOList = roleDataService.getByIds(roleIds);
            Map<Long, RoleDO> roleDOMap = roleDOList.stream()
                    .collect(Collectors.toMap(RoleDO::getId, p -> p));

            // 按收藏顺序返回
            return roleIds.stream()
                    .map(roleDOMap::get)
                    .filter(p -> p != null)
                    .map(roleConverter::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("获取收藏的角色失败, userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 加载新手推荐数据（从系统配置读取）
     */
    private void loadBeginnerRecommendations(HomePageDTO homePageDTO) {
        try {
            String configValue = systemDataService.getValue(CONFIG_KEY_HOMEPAGE_RECOMMENDATIONS);
            if (configValue == null || configValue.isBlank()) {
                homePageDTO.setBeginnerRoles(new ArrayList<>());
                homePageDTO.setBeginnerRoadmaps(new ArrayList<>());
                homePageDTO.setBeginnerCourses(new ArrayList<>());
                return;
            }

            // 解析配置 JSON
            Map<String, List<Long>> config = objectMapper.readValue(
                    configValue,
                    new TypeReference<Map<String, List<Long>>>() {}
            );

            // 获取新手推荐角色
            List<Long> roleIds = config.getOrDefault("roles", new ArrayList<>());
            homePageDTO.setBeginnerRoles(getBeginnerRoles(roleIds));

            // 获取新手推荐路线图
            List<Long> roadmapIds = config.getOrDefault("roadmaps", new ArrayList<>());
            homePageDTO.setBeginnerRoadmaps(getBeginnerRoadmaps(roadmapIds));

            // 获取新手推荐课程
            List<Long> courseIds = config.getOrDefault("courses", new ArrayList<>());
            homePageDTO.setBeginnerCourses(getBeginnerCourses(courseIds));

        } catch (Exception e) {
            log.error("加载新手推荐数据失败", e);
            homePageDTO.setBeginnerRoles(new ArrayList<>());
            homePageDTO.setBeginnerRoadmaps(new ArrayList<>());
            homePageDTO.setBeginnerCourses(new ArrayList<>());
        }
    }

    /**
     * 获取新手推荐角色
     */
    private List<RoleBriefDTO> getBeginnerRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<RoleDO> roleDOList = roleDataService.getByIds(roleIds);
        Map<Long, RoleDO> roleDOMap = roleDOList.stream()
                .collect(Collectors.toMap(RoleDO::getId, p -> p));
        // 按配置顺序返回
        return roleIds.stream()
                .map(roleDOMap::get)
                .filter(p -> p != null)
                .map(roleConverter::toBriefDTO)
                .toList();
    }

    /**
     * 获取新手推荐路线图
     */
    private List<RoadmapSummaryDTO> getBeginnerRoadmaps(List<Long> roadmapIds) {
        if (roadmapIds == null || roadmapIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<RoadmapDO> roadmaps = roadmapDataService.getByIds(roadmapIds);
        Map<Long, RoadmapDO> roadmapMap = roadmaps.stream()
                .collect(Collectors.toMap(RoadmapDO::getId, r -> r));

        // 获取关联的角色信息
        List<Long> roleIds = roadmaps.stream()
                .map(RoadmapDO::getRoleId)
                .distinct()
                .toList();
        Map<Long, RoleDO> roleMap = roleDataService.getByIds(roleIds).stream()
                .collect(Collectors.toMap(RoleDO::getId, p -> p));

        // 按配置顺序返回，并填充角色信息
        return roadmapIds.stream()
                .map(roadmapMap::get)
                .filter(r -> r != null)
                .map(r -> {
                    RoadmapSummaryDTO dto = roadmapConverter.toSummaryDTO(r);
                    RoleDO roleDO = roleMap.get(r.getRoleId());
                    if (roleDO != null) {
                        dto.setRole(roleConverter.toBriefDTO(roleDO));
                    }
                    return dto;
                })
                .toList();
    }

    /**
     * 获取新手推荐课程
     */
    private List<CourseBriefDTO> getBeginnerCourses(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<CourseDO> courses = courseDataService.getByIds(courseIds);
        Map<Long, CourseDO> courseMap = courses.stream()
                .collect(Collectors.toMap(CourseDO::getId, c -> c));
        // 按配置顺序返回
        return courseIds.stream()
                .map(courseMap::get)
                .filter(c -> c != null)
                .map(courseConverter::toBriefDTO)
                .toList();
    }
}
