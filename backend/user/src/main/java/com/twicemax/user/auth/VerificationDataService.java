package com.twicemax.user.auth;

import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 验证码数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationDataService {

    private final VerificationMapper verificationMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询验证码
     */
    public VerificationDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return verificationMapper.getById(id);
    }

    /**
     * 根据邮箱和类型获取验证码
     */
    public VerificationDO getByEmailAndType(String email, byte type, boolean used) {
        return verificationMapper.getByEmailAndType(email, type, used);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证并获取验证码
     */
    public VerificationDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("验证码ID无效");
        }
        VerificationDO verification = getById(id);
        if (verification == null) {
            throw StatusCode.USER_VERIFICATION_CODE_NOT_FOUND.exception();
        }
        return verification;
    }

    // ==================== 写入方法 ====================

    /**
     * 插入验证码
     */
    public int insert(VerificationDO verificationDO) {
        return verificationMapper.insert(verificationDO);
    }

    /**
     * 更新验证码
     */
    public void update(VerificationDO verificationDO) {
        verificationMapper.update(verificationDO);
    }
}
