package com.prosper.learn.business.service.data;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.persistence.dataobject.UserCourseSrsSettingDO;
import com.prosper.learn.persistence.mapper.UserCourseSrsSettingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户课程SRS设置数据服务
 */
@Slf4j
@Service
public class UserCourseSrsSettingDataService extends AbstractDataService<UserCourseSrsSettingDO, UserCourseSrsSettingMapper, Long> {

    @Autowired
    private UserCourseSrsSettingMapper userCourseSrsSettingMapper;

    @Override
    protected UserCourseSrsSettingMapper mapper() {
        return userCourseSrsSettingMapper;
    }

    @Override
    protected String getCacheName() {
        return "user_course_srs_settings";
    }

    @Override
    protected String getEntityName() {
        return "UserCourseSrsSetting";
    }

    @Override
    protected Long getEntityId(UserCourseSrsSettingDO entity) {
        return entity.getId();
    }

    @Override
    protected UserCourseSrsSettingDO getByIdFromMapper(UserCourseSrsSettingMapper mapper, Long id) {
        return mapper.get(id);
    }

    @Override
    protected List<UserCourseSrsSettingDO> getByIdsFromMapper(UserCourseSrsSettingMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }

    @Override
    protected Map<Long, UserCourseSrsSettingDO> getMapByIdsFromMapper(UserCourseSrsSettingMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }

    @Override
    protected Duration getCacheTtl() {
        return Duration.ofHours(2);
    }

    @Override
    protected int deleteByIdFromMapper(UserCourseSrsSettingMapper mapper, Long id) {
        return 0;
    }

    /**
     * 插入课程设置
     */
    public int insert(UserCourseSrsSettingDO setting) {
        if (setting == null) {
            throw new IllegalArgumentException("Setting cannot be null");
        }

        try {
            return userCourseSrsSettingMapper.insert(setting);
        } catch (Exception e) {
            log.error("Error inserting course setting: userId={}, courseId={}", 
                     setting.getUserId(), setting.getCourseId(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新课程设置并清除缓存
     */
    @CacheEvict(value = "user_course_srs_settings", key = "#setting.id")
    public void update(UserCourseSrsSettingDO setting) {
        if (setting == null || setting.getId() == null) {
            throw new IllegalArgumentException("Setting or setting ID cannot be null");
        }

        try {
            userCourseSrsSettingMapper.update(setting);
            log.debug("Updated course setting {}", setting.getId());
        } catch (Exception e) {
            log.error("Error updating course setting: {}", setting.getId(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新复习频率设置
     */
    public boolean updateFrequencySetting(long userId, long courseId, int frequencySetting) {
        try {
            // 先获取现有记录以获得ID用于清除缓存
            UserCourseSrsSettingDO existingSetting = userCourseSrsSettingMapper.getByUserAndCourse(userId, courseId);
            
            int result = userCourseSrsSettingMapper.updateFrequencySetting(userId, courseId, frequencySetting);
            
            // 如果更新成功且存在记录，清除对应的缓存
            if (result > 0 && existingSetting != null) {
                evictCache(existingSetting.getId());
            }
            
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating frequency setting: userId={}, courseId={}", userId, courseId, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新学习状态
     */
    public boolean updateStatus(long userId, long courseId, int status) {
        try {
            // 先获取现有记录以获得ID用于清除缓存
            UserCourseSrsSettingDO existingSetting = userCourseSrsSettingMapper.getByUserAndCourse(userId, courseId);
            
            int result = userCourseSrsSettingMapper.updateState(userId, courseId, status);
            
            // 如果更新成功且存在记录，清除对应的缓存
            if (result > 0 && existingSetting != null) {
                evictCache(existingSetting.getId());
            }
            
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating status: userId={}, courseId={}", userId, courseId, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 根据用户和课程获取设置
     */
    public UserCourseSrsSettingDO getByUserAndCourse(long userId, long courseId) {
        return userCourseSrsSettingMapper.getByUserAndCourse(userId, courseId);
    }

    /**
     * 根据用户获取设置列表
     */
    public List<UserCourseSrsSettingDO> getByUser(long userId) {
        return userCourseSrsSettingMapper.getByUser(userId);
    }

    /**
     * 根据用户和状态获取设置列表
     */
    public List<UserCourseSrsSettingDO> getByUserAndStatus(long userId, int status) {
        return userCourseSrsSettingMapper.getByUserAndState(userId, status);
    }

    /**
     * 根据课程和状态获取设置列表
     */
    public List<UserCourseSrsSettingDO> getByCourseAndStatus(long courseId, int status) {
        return userCourseSrsSettingMapper.getByCourseAndState(courseId, status);
    }

    /**
     * 删除用户课程设置
     */
    public boolean deleteByUserAndCourse(long userId, long courseId) {
        try {
            // 先获取现有记录以获得ID用于清除缓存
            UserCourseSrsSettingDO existingSetting = userCourseSrsSettingMapper.getByUserAndCourse(userId, courseId);
            
            int result = userCourseSrsSettingMapper.deleteByUserAndCourse(userId, courseId);
            
            // 如果删除成功且存在记录，清除对应的缓存
            if (result > 0 && existingSetting != null) {
                evictCache(existingSetting.getId());
            }
            
            return result > 0;
        } catch (Exception e) {
            log.error("Error deleting course setting: userId={}, courseId={}", userId, courseId, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 统计用户的课程设置数量
     */
    public int countByUser(long userId) {
        return userCourseSrsSettingMapper.countByUser(userId);
    }

    /**
     * 统计用户指定状态的课程设置数量
     */
    public int countByUserAndStatus(long userId, int status) {
        return userCourseSrsSettingMapper.countByUserAndState(userId, status);
    }

}