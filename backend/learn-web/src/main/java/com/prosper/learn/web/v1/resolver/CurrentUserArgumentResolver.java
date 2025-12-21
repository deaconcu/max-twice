package com.prosper.learn.web.v1.resolver;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.shared.domain.exception.ErrorCode;
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

import static com.prosper.learn.shared.domain.exception.ErrorCode.*;

/**
 * CurrentUser 参数解析器
 * 自动将当前登录用户注入到标注了 @CurrentUser 的参数
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
            Long userId = StpUtil.getLoginIdAsLong();

            // 从缓存/数据库获取用户信息（自动使用 Redis 缓存）
            UserDO user = userDataService.getById(userId);

            if (user == null) {
                throw USER_NOT_FOUND.exception();
            }

            return user;

        } catch (Exception e) {
            // 未登录时返回 null
            // 注意：如果方法标注了 @SaCheckLogin，SaInterceptor 会在此之前就抛出异常
            return null;
        }
    }
}
