package com.prosper.learn.web.v1.resolver;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.prosper.learn.shared.domain.exception.StatusCode.*;

/**
 * CurrentUser 参数解析器
 * 自动将当前登录用户注入到标注了 @CurrentUser 的参数
 *
 * 注意：此解析器必须和 @SaCheckLogin 一起使用
 * - @SaCheckLogin 确保用户已登录，否则在此之前就会抛出异常
 * - 此解析器仅负责加载用户数据
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private UserDataService userDataService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 判断参数是否标注了 @CurrentUser 注解
        return parameter.hasParameterAnnotation(CurrentUser.class)
            && parameter.getParameterType().equals(UserDO.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        try {
            // 从 Sa-Token 获取当前用户ID
            // 注意：如果未登录，会抛出 NotLoginException，但这种情况不应该发生，
            // 因为 @SaCheckLogin 应该已经在拦截器中检查过了
            Long userId = StpUtil.getLoginIdAsLong();

            // 从缓存/数据库获取用户信息（自动使用 Redis 缓存）
            UserDO user = userDataService.getById(userId);

            if (user == null) {
                // 用户ID存在但数据库中找不到用户（数据不一致）
                throw USER_NOT_FOUND.exception();
            }

            return user;

        } catch (NotLoginException e) {
            // 未登录异常：理论上不应该到这里（@SaCheckLogin 应该已拦截）
            // 如果到了这里，说明配置有问题，抛出异常让全局异常处理器处理
            throw e;
        } catch (Exception e) {
            // 其他异常（如数据库异常）：不应该静默处理，应该抛出
            // 让全局异常处理器统一处理
            throw e;
        }
    }
}
