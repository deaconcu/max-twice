package com.twicemax.web.v2.aspect;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.twicemax.shared.domain.Enums;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
import com.twicemax.web.v2.annotation.CurrentUser;
import com.twicemax.web.v2.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 权限检查切面
 * 自动检查 @RequireRole 注解（支持方法级和类级）
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionAspect {

    private final UserDataService userDataService;

    /**
     * 检查角色权限（方法级注解）
     */
    @Before("@annotation(requireRole)")
    public void checkRoleOnMethod(JoinPoint joinPoint, RequireRole requireRole) {
        checkRolePermission(joinPoint, requireRole);
    }

    /**
     * 检查角色权限（类级注解）
     */
    @Before("@within(requireRole)")
    public void checkRoleOnClass(JoinPoint joinPoint, RequireRole requireRole) {
        // 先检查方法上是否有注解，如果有则跳过（避免重复检查）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.isAnnotationPresent(RequireRole.class)) {
            return;
        }
        checkRolePermission(joinPoint, requireRole);
    }

    /**
     * 执行权限检查
     */
    private void checkRolePermission(JoinPoint joinPoint, RequireRole requireRole) {
        // 获取当前用户
        UserDO currentUser = getCurrentUser(joinPoint);

        // 用户不存在时抛出异常
        if (currentUser == null) {
            log.error("权限检查失败：无法获取当前用户信息");
            throw new NotLoginException("用户未登录或会话已失效", StpUtil.getLoginType(), NotLoginException.NOT_TOKEN);
        }

        // 获取需要的角色
        Enums.UserRole requiredRole = requireRole.value();
        Enums.UserRole currentRole = currentUser.getRoleEnum();

        // 检查权限级别
        if (!currentUser.hasRole(requiredRole)) {
            log.warn("用户 {} (角色:{}) 尝试访问需要 {} 权限的接口",
                currentUser.getId(), currentRole, requiredRole);
            throw new NotPermissionException("需要 " + requiredRole.getDescription() + " 权限");
        }

        log.debug("用户 {} 通过角色检查: {}", currentUser.getId(), requiredRole);
    }

    /**
     * 从方法参数中获取当前用户
     */
    private UserDO getCurrentUser(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        // 查找标注了 @CurrentUser 的参数
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(CurrentUser.class)) {
                Object arg = args[i];
                if (arg instanceof UserDO) {
                    return (UserDO) arg;
                }
            }
        }

        // 如果没有 @CurrentUser 参数，手动获取
        Long userId = StpUtil.getLoginIdAsLong();
        return userDataService.getById(userId);
    }
}
