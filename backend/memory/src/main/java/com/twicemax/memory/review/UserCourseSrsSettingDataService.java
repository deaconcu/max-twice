package com.twicemax.memory.review;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户课程SRS设置数据服务
 * 负责用户课程SRS设置数据的 CRUD
 *
 * 无缓存：设置查询不频繁，主要是写操作
 */
@Service
@RequiredArgsConstructor
public class UserCourseSrsSettingDataService {

    private final UserCourseSrsSettingMapper userCourseSrsSettingMapper;

    // ==================== 查询方法 ====================

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

    // ==================== 写入方法 ====================

    /**
     * 插入课程设置
     */
    public int insert(UserCourseSrsSettingDO setting) {
        if (setting == null) {
            throw new IllegalArgumentException("Setting cannot be null");
        }
        return userCourseSrsSettingMapper.insert(setting);
    }

    /**
     * 更新课程设置
     */
    public void update(UserCourseSrsSettingDO setting) {
        if (setting == null || setting.getId() == null) {
            throw new IllegalArgumentException("Setting or setting ID cannot be null");
        }
        userCourseSrsSettingMapper.update(setting);
    }
}
