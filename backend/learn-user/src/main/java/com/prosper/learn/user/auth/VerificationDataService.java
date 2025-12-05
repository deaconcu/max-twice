package com.prosper.learn.user.auth;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 验证码数据服务，提供缓存功能
 */
@Slf4j
@Service
public class VerificationDataService extends AbstractDataService<VerificationDO, VerificationMapper, Long> {
    
    @Autowired
    private VerificationMapper verificationMapper;
    
    @Override
    protected VerificationMapper mapper() {
        return verificationMapper;
    }
    
    @Override
    protected String getCacheName() {
        return "verifications";
    }
    
    @Override
    protected String getEntityName() {
        return "Verification";
    }
    
    @Override
    protected Long getEntityId(VerificationDO entity) {
        return entity.getId();
    }
    
    @Override
    protected VerificationDO getByIdFromMapper(VerificationMapper mapper, Long id) {
        return mapper.getById(id);
    }
    
    @Override
    protected List<VerificationDO> getByIdsFromMapper(VerificationMapper mapper, Collection<Long> ids) {
        throw new UnsupportedOperationException("VerificationMapper does not support batch query");
    }
    
    @Override
    protected Map<Long, VerificationDO> getMapByIdsFromMapper(VerificationMapper mapper, Collection<Long> ids) {
        throw new UnsupportedOperationException("VerificationMapper does not support batch query");
    }

    @Override
    protected int deleteByIdFromMapper(VerificationMapper mapper, Long id) {
        return 0;
    }

    /**
     * 根据邮箱获取验证码信息
     */
    @Cacheable(value = "verifications", key = "'email:' + #email + ':used:' + #used")
    public VerificationDO getByEmail(String email, boolean used) {
        return verificationMapper.getByEmail(email, used);
    }
    
    /**
     * 插入验证码信息
     */
    @CacheEvict(value = "verifications", allEntries = true)
    public int insert(VerificationDO verificationDO) {
        return verificationMapper.insert(verificationDO);
    }
    
    /**
     * 更新验证码信息
     */
    @CacheEvict(value = "verifications", allEntries = true)
    public void update(VerificationDO verificationDO) {
        verificationMapper.update(verificationDO);
    }


}