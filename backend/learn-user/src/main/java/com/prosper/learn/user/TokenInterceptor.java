package com.prosper.learn.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.common.JwtUtil;
import com.prosper.learn.dto.Response;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Slf4j
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    //在业务处理请求之前处理
    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        log.info("intercepted");
        response.setContentType("application/json;charset=utf-8");
        // 判断对象是否是映射到一个方法，如果不是则直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        Method method = ((HandlerMethod)handler).getMethod();
        //检查方法是否有PassLogin注解，有则跳过认证
        if (method.isAnnotationPresent(PassLogin.class)){
            return true;
        }

        // 从HTTP请求头中获取Authorization信息，
        String authorization = request.getHeader(jwtUtil.getHeader());
        if(!StringUtils.hasText(authorization)){
            log.info("token无效");
            response.getWriter().write(objectMapper.writeValueAsString(Response.notLogin));
            return false;
        }
        //获取TOKEN,注意要清除前缀"Bearer "
        String token = authorization.replace("Bearer ","");
        // HTTP请求头中TOKEN解析出的用户信息
        Claims claims = jwtUtil.parseToken(token);
        if(claims == null){
            log.info("token无效");
            response.getWriter().write(objectMapper.writeValueAsString(Response.notLogin));
            return false;
        }
        //校验是否过期
        boolean flag = jwtUtil.isExpired(claims.getExpiration());
        if(flag){
            log.error("token过期");
            response.getWriter().write(objectMapper.writeValueAsString(Response.notLogin));
            return false;
        }
        //token正常，获取用户信息，比如这里的subject存的是用户id
        String subject = claims.getSubject();
        //将用户信息存入request，以便后面处理请求使用
        request.setAttribute("subject",subject);
        return true;
    }
}

