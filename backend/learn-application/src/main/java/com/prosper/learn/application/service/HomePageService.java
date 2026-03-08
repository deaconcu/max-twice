package com.prosper.learn.application.service;

import com.prosper.learn.analytics.dto.UserStatsDTO;
import com.prosper.learn.analytics.stats.service.UserStatsDomainService;
import com.prosper.learn.application.dto.response.PlatformStatsDTO;
import com.prosper.learn.application.dto.response.ReviewSummaryDTO;
import com.prosper.learn.application.dto.response.course.CourseWithStatsDTO;
import com.prosper.learn.application.dto.response.home.HomePageDTO;
import com.prosper.learn.application.dto.response.home.UserLearningStatsDTO;
import com.prosper.learn.application.dto.response.profession.ProfessionDTO;
import com.prosper.learn.application.dto.response.userlearning.UserLearningDTO;
import com.prosper.learn.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页数据聚合服务
 * 负责聚合首页所需的所有数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomePageService {

    private final PlatformStatsService platformStatsService;
    private final UserStatsDomainService userStatsDomainService;
    private final UserLearningService userLearningService;
    private final CourseService courseService;
    private final ProfessionService professionService;
    private final MemoryBankService memoryBankService;

    // 首页展示的数量限制
    private static final int LEARNING_CAREERS_LIMIT = 8;
    private static final int LEARNING_COURSES_LIMIT = 8;
    private static final int RECOMMENDED_CAREERS_LIMIT = 4;
    private static final int RECOMMENDED_COURSES_LIMIT = 30;

    /**
     * 获取首页聚合数据
     *
     * @param userId 当前用户ID
     * @return 首页聚合数据
     */
    public HomePageDTO getHomePageData(Long userId) {
        HomePageDTO homePageDTO = new HomePageDTO();

        // 1. 平台统计数据
        homePageDTO.setPlatformStats(platformStatsService.getPlatformStats());

        // 2. 用户学习统计
        homePageDTO.setUserStats(getUserLearningStats(userId));

        // 3. 正在学习的职业路线
        homePageDTO.setLearningCareers(getLearningCareers(userId));

        // 4. 正在学习的课程
        homePageDTO.setLearningCourses(getLearningCourses(userId));

        // 5. 复习概览
        homePageDTO.setReviewSummary(getReviewSummary(userId));

        // 6. 推荐职业
        homePageDTO.setRecommendedCareers(getRecommendedCareers());

        // 7. 推荐课程
        homePageDTO.setRecommendedCourses(getRecommendedCourses());

        return homePageDTO;
    }

    /**
     * 获取用户学习统计
     */
    private UserLearningStatsDTO getUserLearningStats(Long userId) {
        UserLearningStatsDTO stats = new UserLearningStatsDTO();

        try {
            UserStatsDTO userStats = userStatsDomainService.getUserStats(userId);
            if (userStats != null) {
                stats.setCoursesInProgress(userStats.getLearningCourseCount() != null
                        ? userStats.getLearningCourseCount() : 0);
                stats.setCareersInProgress(userStats.getInProgressProfessionCount() != null
                        ? userStats.getInProgressProfessionCount() : 0);
            }
            // TODO: 累计学习天数需要从其他地方获取
            stats.setLearningDays(0);
        } catch (Exception e) {
            log.error("获取用户学习统计失败, userId={}", userId, e);
        }

        return stats;
    }

    /**
     * 获取正在学习的职业路线
     */
    private List<UserLearningDTO<Object>> getLearningCareers(Long userId) {
        try {
            return userLearningService.getByUserWithObjects(
                    userId,
                    Enums.ContentType.roadmap,
                    Enums.UserProgressState.IN_PROGRESS.value(),
                    null,
                    LEARNING_CAREERS_LIMIT
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
            return userLearningService.getAllCoursesProgress(userId, null, LEARNING_COURSES_LIMIT);
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
     * 获取推荐职业
     */
    private List<ProfessionDTO> getRecommendedCareers() {
        try {
            return professionService.getHotProfessions(RECOMMENDED_CAREERS_LIMIT);
        } catch (Exception e) {
            log.error("获取推荐职业失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取推荐课程
     */
    private List<CourseWithStatsDTO> getRecommendedCourses() {
        try {
            return courseService.getHotCourses(RECOMMENDED_COURSES_LIMIT);
        } catch (Exception e) {
            log.error("获取推荐课程失败", e);
            return new ArrayList<>();
        }
    }
}
