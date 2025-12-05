package com.prosper.learn.web.v1.resolver;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.business.service.data.UserDataService;
import com.prosper.learn.persistence.dataobject.UserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

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
                throw ErrorCode.USER_NOT_FOUND.exception();
            }

            return user;

        } catch (Exception e) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }
    }
}
