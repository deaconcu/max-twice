package com.prosper.learn.memory.bank;

import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.memory.review.*;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 记忆库领域服务
 *
 * 只依赖 memory 域，处理记忆库的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryBankDomainService {

    private final UserCourseSrsSettingDataService courseSrsSettingDataService;
    private final UserCardInCourseDataService userCardInCourseDataService;
    private final UserCardSrsDataService userCardSrsDataService;
    private final MemoryCardDeckDataService deckDataService;

    // ========== Command 方法 ==========

    /**
     * 添加卡片组到记忆库（领域逻辑）
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param deckId 卡片组ID
     * @param cards 卡片列表（已验证存在）
     * @param deckVersion 卡片组版本
     * @param nodeId 节点ID
     */
    @Transactional
    public void addDeckToMemoryBank(Long userId, Long courseId, Long deckId,
                                     List<MemoryCardDO> cards, Integer deckVersion, Long nodeId) {
        // 1. 创建或更新课程学习设置
        UserCourseSrsSettingDO existingSetting = courseSrsSettingDataService.getByUserAndCourse(userId, courseId);
        if (existingSetting == null) {
            UserCourseSrsSettingDO setting = new UserCourseSrsSettingDO();
            setting.setUserId(userId);
            setting.setCourseId(courseId);
            setting.setFrequencySetting(FrequencySetting.NORMAL.value());
            setting.setState(DeckCourseStudyState.STUDYING.value());
            courseSrsSettingDataService.insert(setting);
        }

        // 2. 批量添加卡片到课程
        if (cards != null && !cards.isEmpty()) {
            log.info("Adding {} cards from deck: {} to memory bank for user: {} in course: {}",
                cards.size(), deckId, userId, courseId);

            List<Long> cardIds = cards.stream()
                    .map(MemoryCardDO::getId)
                    .collect(Collectors.toList());

            // 批量插入用户卡片课程关系（自动跳过已存在的）
            userCardInCourseDataService.batchInsertIgnore(userId, deckId, courseId, cardIds);

            // 3. 构建SRS状态对象
            List<UserCardSrsDO> srsList = new ArrayList<>();
            for (MemoryCardDO card : cards) {
                UserCardSrsDO srs = userCardSrsDataService.createNewSrsState(
                    userId, card.getId(), nodeId, deckId, deckVersion, card.getCurrentVersionId());
                srsList.add(srs);
            }

            // 批量创建SRS状态（自动跳过已存在的）
            userCardSrsDataService.batchInsertIgnoreSrsStates(srsList);
        } else {
            log.info("No cards found in deck: {} to add to memory bank for user: {} in course: {}",
                deckId, userId, courseId);
        }
    }

    /**
     * 更新课程复习策略（领域逻辑）
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param frequencySetting 频率设置（可选）
     * @param state 状态（可选）
     */
    @Transactional
    public void updateCourseSetting(Long userId, Long courseId,
                                     Integer frequencySetting, Byte state) {
        // 获取现有设置
        UserCourseSrsSettingDO existingSetting = courseSrsSettingDataService.getByUserAndCourse(userId, courseId);
        if (existingSetting == null) {
            throw StatusCode.MEMORY_BANK_COURSE_NOT_FOUND.exception("课程设置不存在");
        }

        // 更新设置
        if (frequencySetting != null) {
            existingSetting.setFrequencySetting(frequencySetting.byteValue());
        }
        if (state != null) {
            existingSetting.setState(state);
        }
        existingSetting.setUpdatedAt(LocalDateTime.now());
        courseSrsSettingDataService.update(existingSetting);

        log.info("Updated course setting for user: {} course: {}", userId, courseId);
    }

    /**
     * 移除课程中的卡片组（领域逻辑）
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param cardIds 卡片ID列表（已验证存在）
     */
    @Transactional
    public void removeDeckFromCourse(Long userId, Long courseId, List<Long> cardIds) {
        // 验证业务逻辑 - 课程设置存在
        UserCourseSrsSettingDO courseSetting = courseSrsSettingDataService.getByUserAndCourse(userId, courseId);
        if (courseSetting == null) {
            throw StatusCode.MEMORY_BANK_COURSE_NOT_FOUND.exception("课程设置不存在");
        }

        if (cardIds != null && !cardIds.isEmpty()) {
            // 批量删除用户卡片课程关系
            userCardInCourseDataService.batchDeleteByUserCourseAndCards(userId, courseId, cardIds);

            // 批量查询仍有课程关系的卡片ID（优化1+N查询）
            List<Long> existingCardIds = userCardInCourseDataService.getExistingCardIdsByUserAndCards(userId, cardIds);
            Set<Long> existingCardSet = new HashSet<>(existingCardIds);

            // 找出孤立的卡片ID（没有其他课程关系的卡片）
            List<Long> orphanedCardIds = cardIds.stream()
                    .filter(cardId -> !existingCardSet.contains(cardId))
                    .collect(Collectors.toList());

            // 批量删除孤立卡片的SRS状态
            if (!orphanedCardIds.isEmpty()) {
                userCardSrsDataService.batchDeleteByUserAndCards(userId, orphanedCardIds);
            }

            log.info("Removed {} cards from course: {} for user: {}, deleted {} orphaned SRS states",
                cardIds.size(), courseId, userId, orphanedCardIds.size());
        }
    }

    // ========== Query 方法 ==========

    /**
     * 获取用户的课程设置列表
     *
     * @param userId 用户ID
     * @param state 状态过滤（可选）
     * @return 课程设置列表
     */
    public List<UserCourseSrsSettingDO> getMemoryBankCourseSettings(Long userId, Integer state) {
        List<UserCourseSrsSettingDO> settings = courseSrsSettingDataService.getByUser(userId);

        if (state != null && !settings.isEmpty()) {
            settings = settings.stream()
                    .filter(setting -> setting.getState().equals(state.byteValue()))
                    .collect(Collectors.toList());
        }

        return settings;
    }

    /**
     * 获取课程的卡片统计信息
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 课程记忆库统计DO
     */
    public CourseMemoryBankDO getCourseCardStats(Long userId, Long courseId) {
        return userCardInCourseDataService.getCardStatsForCourses(userId, courseId);
    }

    /**
     * 批量获取课程的卡片统计信息
     *
     * @param userId 用户ID
     * @param courseIds 课程ID集合
     * @return 课程记忆库统计DO列表
     */
    public List<CourseMemoryBankDO> getBatchCourseCardStats(Long userId, Set<Long> courseIds) {
        return userCardInCourseDataService.getBatchCardStatsForCourses(userId, courseIds);
    }
}
