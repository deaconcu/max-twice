package com.twicemax.web.v2.resolver;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
import com.twicemax.web.v2.annotation.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.twicemax.shared.domain.exception.StatusCode.USER_NOT_FOUND;

/**
 * CurrentUser 参数解析器
 * 自动将当前登录用户注入到标注了 @CurrentUser 的参数
 *
 * 注意：此解析器要求用户必须已登录
 * - 如果未登录，会抛出 NotLoginException
 * - 使用此注解的接口必须配合 @SaCheckLogin 或确保有登录拦截
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
            // 如果未登录，会抛出 NotLoginException
            Long userId = StpUtil.getLoginIdAsLong();

            // 从缓存/数据库获取用户信息（自动使用 Redis 缓存）
            UserDO user = userDataService.getById(userId);

            if (user == null) {
                // 用户ID存在但数据库中找不到用户（数据不一致）
                throw USER_NOT_FOUND.exception();
            }

            return user;

        } catch (NotLoginException e) {
            // 未登录异常：抛出异常，由全局异常处理器处理
            throw e;
        } catch (Exception e) {
            // 其他异常（如数据库异常）：抛出异常，由全局异常处理器处理
            throw e;
        }
    }
}
