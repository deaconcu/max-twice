package com.prosper.learn.memory.review;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户卡片课程归属数据服务
 */
@Slf4j
@Service
public class UserCardInCourseDataService extends AbstractDataService<UserCardInCourseDO, UserCardInCourseMapper, Long> {

    @Autowired
    private UserCardInCourseMapper userCardInCourseMapper;

    @Override
    protected UserCardInCourseMapper mapper() {
        return userCardInCourseMapper;
    }

    @Override
    protected String getCacheName() {
        return "user_card_in_courses";
    }

    @Override
    protected String getEntityName() {
        return "UserCardInCourse";
    }

    @Override
    protected Long getEntityId(UserCardInCourseDO entity) {
        return entity.getId();
    }

    @Override
    protected UserCardInCourseDO getByIdFromMapper(UserCardInCourseMapper mapper, Long id) {
        return mapper.get(id);
    }

    @Override
    protected List<UserCardInCourseDO> getByIdsFromMapper(UserCardInCourseMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }

    @Override
    protected Map<Long, UserCardInCourseDO> getMapByIdsFromMapper(UserCardInCourseMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }

    @Override
    protected int deleteByIdFromMapper(UserCardInCourseMapper mapper, Long id) {
        return mapper.deleteById(id);
    }

    @Override
    protected Duration getCacheTtl() {
        return Duration.ofHours(1);
    }

    /**
     * 验证并获取用户课程卡片关系
     *
     * @param id 关系ID
     * @return 用户课程卡片关系实体
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当关系不存在时抛出 USER_CARD_IN_COURSE_NOT_FOUND (2212)
     */
    @Override
    public UserCardInCourseDO validateAndGet(Long id) {
        if (id == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户课程卡片关系ID不能为空");
        }

        if (id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("用户课程卡片关系ID必须大于0");
        }

        UserCardInCourseDO relation = getById(id);
        if (relation == null) {
            throw StatusCode.USER_CARD_IN_COURSE_NOT_FOUND.exception();
        }

        return relation;
    }

    /**
     * 插入关系记录
     */
    public int insert(UserCardInCourseDO relation) {
        if (relation == null) {
            throw new IllegalArgumentException("Relation cannot be null");
        }

        try {
            return userCardInCourseMapper.insert(relation);
        } catch (Exception e) {
            log.error("Error inserting relation: userId={}, cardId={}, courseId={}", 
                     relation.getUserId(), relation.getCardId(), relation.getCourseId(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

// --注释掉检查 START (2025/12/10 11:27):
//    /**
//     * 批量插入关系记录
//     */
//    public int batchInsert(List<UserCardInCourseDO> relations) {
//        if (relations == null || relations.isEmpty()) {
//            return 0;
//        }
//
//        try {
//            int result = userCardInCourseMapper.batchInsert(relations);
//            return result;
//        } catch (Exception e) {
//            log.error("Error batch inserting relations: count={}", relations.size(), e);
//            throw ErrorCode.DATABASE_ERROR.exception(e);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:27)

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 根据用户、卡片和课程获取关系
//     */
//    public UserCardInCourseDO getByUserCardAndCourse(long userId, long cardId, long courseId) {
//        return userCardInCourseMapper.getByUserCardAndCourse(userId, cardId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 根据用户和课程获取关系列表
//     */
//    public List<UserCardInCourseDO> getByUserAndCourse(long userId, long courseId) {
//        return userCardInCourseMapper.getByUserAndCourse(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 根据用户和卡片获取关系列表
//     */
//    public List<UserCardInCourseDO> getByUserAndCard(long userId, long cardId) {
//        return userCardInCourseMapper.getByUserAndCard(userId, cardId);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 根据课程获取关系列表
//     */
//    public List<UserCardInCourseDO> getByCourse(long courseId) {
//        return userCardInCourseMapper.getByCourse(courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 获取用户的课程ID列表
//     */
//    public List<Long> getCourseIdsByUser(long userId) {
//        return userCardInCourseMapper.getCourseIdsByUser(userId);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 获取用户指定课程的卡片ID列表
//     */
//    public List<Long> getCardIdsByUserAndCourse(long userId, long courseId) {
//        return userCardInCourseMapper.getCardIdsByUserAndCourse(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

// --注释掉检查 START (2025/12/10 11:27):
//    /**
//     * 删除指定的关系记录
//     */
//    public boolean deleteByUserCardAndCourse(long userId, long cardId, long courseId) {
//        try {
//            // 先获取现有记录以获得ID用于清除缓存
//            UserCardInCourseDO existingRelation = userCardInCourseMapper.getByUserCardAndCourse(userId, cardId, courseId);
//
//            int result = userCardInCourseMapper.deleteByUserCardAndCourse(userId, cardId, courseId);
//
//            // 如果删除成功且存在记录，清除对应的缓存
//            if (result > 0 && existingRelation != null) {
//                evictCache(existingRelation.getId());
//            }
//
//            return result > 0;
//        } catch (Exception e) {
//            log.error("Error deleting relation: userId={}, cardId={}, courseId={}",
//                     userId, cardId, courseId, e);
//            throw ErrorCode.DATABASE_ERROR.exception(e);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:27)

// --注释掉检查 START (2025/12/10 11:27):
//    /**
//     * 删除用户指定课程的所有关系
//     */
//    public boolean deleteByUserAndCourse(long userId, long courseId) {
//        try {
//            // 先获取要删除的关系记录以获得ID列表用于清除缓存
//            List<UserCardInCourseDO> relations = userCardInCourseMapper.getByUserAndCourse(userId, courseId);
//
//            int result = userCardInCourseMapper.deleteByUserAndCourse(userId, courseId);
//
//            // 如果删除成功，清除相关的缓存记录
//            if (result > 0 && !relations.isEmpty()) {
//                for (UserCardInCourseDO relation : relations) {
//                    evictCache(relation.getId());
//                }
//                log.debug("Evicted cache for {} relations of user {} in course {}", relations.size(), userId, courseId);
//            }
//
//            return result > 0;
//        } catch (Exception e) {
//            log.error("Error deleting relations by user and course: userId={}, courseId={}",
//                     userId, courseId, e);
//            throw ErrorCode.DATABASE_ERROR.exception(e);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:27)

// --注释掉检查 START (2025/12/10 11:27):
//    /**
//     * 删除用户指定卡片的所有关系
//     */
//    public boolean deleteByUserAndCard(long userId, long cardId) {
//        try {
//            // 先获取要删除的关系记录以获得ID列表用于清除缓存
//            List<UserCardInCourseDO> relations = userCardInCourseMapper.getByUserAndCard(userId, cardId);
//
//            int result = userCardInCourseMapper.deleteByUserAndCard(userId, cardId);
//
//            // 如果删除成功，清除相关的缓存记录
//            if (result > 0 && !relations.isEmpty()) {
//                for (UserCardInCourseDO relation : relations) {
//                    evictCache(relation.getId());
//                }
//                log.debug("Evicted cache for {} relations of user {} and card {}", relations.size(), userId, cardId);
//            }
//
//            return result > 0;
//        } catch (Exception e) {
//            log.error("Error deleting relations by user and card: userId={}, cardId={}",
//                     userId, cardId, e);
//            throw ErrorCode.DATABASE_ERROR.exception(e);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:27)

// --注释掉检查 START (2025/12/10 11:27):
//    /**
//     * 统计用户的关系数量
//     */
//    public int countByUser(long userId) {
//        return userCardInCourseMapper.countByUser(userId);
//    }
// --注释掉检查 STOP (2025/12/10 11:27)

// --注释掉检查 START (2025/12/10 11:27):
//    /**
//     * 统计用户指定课程的关系数量
//     */
//    public int countByUserAndCourse(long userId, long courseId) {
//        return userCardInCourseMapper.countByUserAndCourse(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:27)

// --注释掉检查 START (2025/12/10 11:27):
//    /**
//     * 统计课程的关系数量
//     */
//    public int countByCourse(long courseId) {
//        return userCardInCourseMapper.countByCourse(courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:27)

// --注释掉检查 START (2025/12/10 11:27):
//    /**
//     * 统计用户指定课程的卡片数量
//     */
//    public long countCardsByUserAndCourse(long userId, long courseId) {
//        return userCardInCourseMapper.countByUserAndCourse(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:27)


    public List<CourseMemoryBankDO> getBatchCardStatsForCourses(Long userId, Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        return userCardInCourseMapper.getBatchCardStatsForCourses(userId, courseIds, LocalDateTime.now());
    }

    public CourseMemoryBankDO getCardStatsForCourses(Long userId, Long courseId) {
        return userCardInCourseMapper.getCardStatsForCourses(userId, courseId, LocalDateTime.now());
    }

    /**
     * 批量插入用户卡片课程关系（使用INSERT IGNORE自动跳过重复）
     */
    public int batchInsertIgnore(Long userId, Long deckId, Long courseId, List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return 0;
        }
        try {
            int result = userCardInCourseMapper.batchInsertIgnore(userId, deckId, courseId, cardIds);
            log.debug("Batch inserted {} card-course relations for user: {} course: {}",
                     result, userId, courseId);
            return result;
        } catch (Exception e) {
            log.error("Error batch inserting card-course relations: userId={}, courseId={}, cardCount={}",
                     userId, courseId, cardIds.size(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 批量删除用户指定课程的特定卡片关系
     */
    //@CacheEvict(value = "courseStats", key = "{#userId, #courseId}")
    public int batchDeleteByUserCourseAndCards(Long userId, Long courseId, List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return 0;
        }
        try {
            int result = userCardInCourseMapper.batchDeleteByUserCourseAndCards(userId, courseId, cardIds);
            log.debug("Batch deleted {} card-course relations for user: {} course: {}", 
                     result, userId, courseId);
            return result;
        } catch (Exception e) {
            log.error("Error batch deleting card-course relations: userId={}, courseId={}, cardCount={}", 
                     userId, courseId, cardIds.size(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 获取用户在指定卡片中仍有课程关系的卡片ID列表
     */
    public List<Long> getExistingCardIdsByUserAndCards(Long userId, List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return List.of();
        }
        return userCardInCourseMapper.getExistingCardIdsByUserAndCards(userId, cardIds);
    }

}