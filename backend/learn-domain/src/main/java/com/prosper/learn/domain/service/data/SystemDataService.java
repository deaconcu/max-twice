package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.SystemDO;
import com.prosper.learn.persistence.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置数据服务 - Key-Value模式
 */
@Service
public class SystemDataService {

    @Autowired
    private SystemMapper systemMapper;

    /**
     * 根据key获取配置值
     */
    @Cacheable(value = "system", key = "#key")
    public String getValue(String key) {
        SystemDO systemDO = systemMapper.getByKey(key);
        return systemDO != null ? systemDO.getValue() : null;
    }

    /**
     * 获取所有配置
     */
    @Cacheable(value = "system", key = "'all'")
    public Map<String, String> getAllConfigs() {
        List<SystemDO> configs = systemMapper.getAll();
        return configs.stream()
                .collect(Collectors.toMap(SystemDO::getKey, SystemDO::getValue));
    }

    /**
     * 设置配置值（如果不存在则插入，存在则更新）
     */
    @CacheEvict(value = "system", key = "#key")
    public void setValue(String key, String value) {
        if (systemMapper.existsByKey(key) > 0) {
            SystemDO systemDO = new SystemDO();
            systemDO.setKey(key);
            systemDO.setValue(value);
            systemMapper.updateByKey(systemDO);
        } else {
            SystemDO systemDO = new SystemDO();
            systemDO.setKey(key);
            systemDO.setValue(value);
            systemMapper.insert(systemDO);
        }
    }

    /**
     * 删除配置
     */
    @CacheEvict(value = "system", key = "#key")
    public void deleteConfig(String key) {
        systemMapper.deleteByKey(key);
    }

    /**
     * 检查配置是否存在
     */
    public boolean exists(String key) {
        return systemMapper.existsByKey(key) > 0;
    }

    /**
     * 判断是否只读模式
     * @return true=只读模式，false=正常模式
     */
    public boolean isReadOnlyMode() {
        String value = getValue("readonly_mode");
        return "1".equals(value);
    }

    /**
     * 开启只读模式
     */
    public void enableReadOnlyMode() {
        setValue("readonly_mode", "1");
    }

    /**
     * 关闭只读模式
     */
    public void disableReadOnlyMode() {
        setValue("readonly_mode", "0");
    }
}