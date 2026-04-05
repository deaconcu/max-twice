package com.prosper.learn.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.analytics.stats.mapper.UserStatsDO;
import com.prosper.learn.analytics.stats.service.UserStatsDomainService;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.ProfessionConverter;
import com.prosper.learn.application.converter.RoadmapConverter;
import com.prosper.learn.application.dto.response.ReviewSummaryDTO;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.course.CourseFullDTO;
import com.prosper.learn.application.dto.response.home.HomePageDTO;
import com.prosper.learn.application.dto.response.home.UserLearningStatsDTO;
import com.prosper.learn.application.dto.response.profession.ProfessionBriefDTO;
import com.prosper.learn.application.dto.response.profession.ProfessionDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.prosper.learn.application.dto.response.userlearning.UserLearningDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.interaction.bookmark.BookmarkDO;
import com.prosper.learn.interaction.bookmark.BookmarkDataService;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.infrastructure.config.SystemDataService;
import com.prosper.learn.user.profile.UserDO;
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
    private final ProfessionService professionService;
    private final MemoryBankService memoryBankService;
    private final BookmarkDataService bookmarkDataService;
    private final ProfessionDataService professionDataService;
    private final RoadmapDataService roadmapDataService;
    private final CourseDataService courseDataService;
    private final SystemDataService systemDataService;
    private final ProfessionConverter professionConverter;
    private final RoadmapConverter roadmapConverter;
    private final CourseConverter courseConverter;

    // 首页展示的数量限制
    private static final int LEARNING_PROFESSIONS_LIMIT = 8;
    private static final int LEARNING_COURSES_LIMIT = 8;
    private static final int HOT_PROFESSIONS_LIMIT = 10;
    private static final int HOT_COURSES_LIMIT = 30;
    private static final int BOOKMARKED_PROFESSIONS_LIMIT = 10;

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

        // 3. 收藏的职业（最新收藏的在前，最多10个）
        homePageDTO.setBookmarkedProfessions(getBookmarkedProfessions(userId));

        // 4. 正在学习的职业路线
        homePageDTO.setLearningProfessions(getLearningProfessions(userId));

        // 5. 正在学习的课程
        homePageDTO.setLearningCourses(getLearningCourses(userId));

        // 6. 复习概览
        homePageDTO.setReviewSummary(getReviewSummary(userId));

        // 7. 热门职业榜单
        homePageDTO.setHotProfessions(getHotProfessions());

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
                stats.setProfessionsInProgress(userStats.getInProgressProfessionCount() != null
                        ? userStats.getInProgressProfessionCount() : 0);
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
     * 获取正在学习的职业路线
     */
    private List<UserLearningDTO<Object>> getLearningProfessions(Long userId) {
        try {
            return userLearningService.getByUserWithObjects(
                    userId,
                    Enums.ContentType.roadmap,
                    Enums.UserProgressState.IN_PROGRESS.value(),
                    null,
                    LEARNING_PROFESSIONS_LIMIT
            );
        } catch (Exception e) {
            log.error("获取正在学习的职业路线失败, userId={}", userId, e);
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
     * 获取热门职业榜单
     */
    private List<ProfessionDTO> getHotProfessions() {
        try {
            return professionService.getHotProfessions(HOT_PROFESSIONS_LIMIT);
        } catch (Exception e) {
            log.error("获取热门职业失败", e);
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
     * 获取收藏的职业（最新收藏的在前，最多10个）
     */
    private List<ProfessionDTO> getBookmarkedProfessions(Long userId) {
        try {
            // 分页查询第一页（按收藏时间倒序）
            List<BookmarkDO> bookmarks = bookmarkDataService.listByUserAndLastId(
                    userId,
                    Enums.ContentType.profession.value(),
                    null,
                    BOOKMARKED_PROFESSIONS_LIMIT
            );
            if (bookmarks.isEmpty()) {
                return new ArrayList<>();
            }

            // 提取职业ID
            List<Long> professionIds = bookmarks.stream()
                    .map(BookmarkDO::getObjectId)
                    .toList();

            // 批量获取职业信息
            List<ProfessionDO> professions = professionDataService.getByIds(professionIds);
            Map<Long, ProfessionDO> professionMap = professions.stream()
                    .collect(Collectors.toMap(ProfessionDO::getId, p -> p));

            // 按收藏顺序返回
            return professionIds.stream()
                    .map(professionMap::get)
                    .filter(p -> p != null)
                    .map(professionConverter::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("获取收藏的职业失败, userId={}", userId, e);
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
                homePageDTO.setBeginnerProfessions(new ArrayList<>());
                homePageDTO.setBeginnerRoadmaps(new ArrayList<>());
                homePageDTO.setBeginnerCourses(new ArrayList<>());
                return;
            }

            // 解析配置 JSON
            Map<String, List<Long>> config = objectMapper.readValue(
                    configValue,
                    new TypeReference<Map<String, List<Long>>>() {}
            );

            // 获取新手推荐职业
            List<Long> professionIds = config.getOrDefault("professions", new ArrayList<>());
            homePageDTO.setBeginnerProfessions(getBeginnerProfessions(professionIds));

            // 获取新手推荐路线图
            List<Long> roadmapIds = config.getOrDefault("roadmaps", new ArrayList<>());
            homePageDTO.setBeginnerRoadmaps(getBeginnerRoadmaps(roadmapIds));

            // 获取新手推荐课程
            List<Long> courseIds = config.getOrDefault("courses", new ArrayList<>());
            homePageDTO.setBeginnerCourses(getBeginnerCourses(courseIds));

        } catch (Exception e) {
            log.error("加载新手推荐数据失败", e);
            homePageDTO.setBeginnerProfessions(new ArrayList<>());
            homePageDTO.setBeginnerRoadmaps(new ArrayList<>());
            homePageDTO.setBeginnerCourses(new ArrayList<>());
        }
    }

    /**
     * 获取新手推荐职业
     */
    private List<ProfessionBriefDTO> getBeginnerProfessions(List<Long> professionIds) {
        if (professionIds == null || professionIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProfessionDO> professions = professionDataService.getByIds(professionIds);
        Map<Long, ProfessionDO> professionMap = professions.stream()
                .collect(Collectors.toMap(ProfessionDO::getId, p -> p));
        // 按配置顺序返回
        return professionIds.stream()
                .map(professionMap::get)
                .filter(p -> p != null)
                .map(professionConverter::toBriefDTO)
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

        // 获取关联的职业信息
        List<Long> professionIds = roadmaps.stream()
                .map(RoadmapDO::getProfessionId)
                .distinct()
                .toList();
        Map<Long, ProfessionDO> professionMap = professionDataService.getByIds(professionIds).stream()
                .collect(Collectors.toMap(ProfessionDO::getId, p -> p));

        // 按配置顺序返回，并填充职业信息
        return roadmapIds.stream()
                .map(roadmapMap::get)
                .filter(r -> r != null)
                .map(r -> {
                    RoadmapSummaryDTO dto = roadmapConverter.toSummaryDTO(r);
                    ProfessionDO profession = professionMap.get(r.getProfessionId());
                    if (profession != null) {
                        dto.setProfession(professionConverter.toBriefDTO(profession));
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
