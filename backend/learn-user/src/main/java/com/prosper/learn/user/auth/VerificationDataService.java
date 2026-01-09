package com.prosper.learn.user.auth;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.StatusCode;
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
     * 验证并获取验证码
     *
     * @param id 验证码ID
     * @return 验证码实体
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当验证码不存在时抛出 USER_VERIFICATION_CODE_NOT_FOUND (1109)
     */
    @Override
    public VerificationDO validateAndGet(Long id) {
        if (id == null) {
            throw StatusCode.INVALID_PARAMETER.exception("验证码ID不能为空");
        }

        if (id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("验证码ID必须大于0");
        }

        VerificationDO verification = getById(id);
        if (verification == null) {
            throw StatusCode.USER_VERIFICATION_CODE_NOT_FOUND.exception();
        }

        return verification;
    }

    /**
     * 根据邮箱和类型获取验证码信息
     */
    @Cacheable(value = "verifications", key = "'email:' + #email + ':type:' + #type + ':used:' + #used")
    public VerificationDO getByEmailAndType(String email, byte type, boolean used) {
        return verificationMapper.getByEmailAndType(email, type, used);
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