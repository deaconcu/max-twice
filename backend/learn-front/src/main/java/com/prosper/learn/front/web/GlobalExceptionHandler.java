package com.prosper.learn.front.web;

import cn.dev33.satoken.exception.SaTokenException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获未登录异常
    @ExceptionHandler(SaTokenException.class)
    public ModelAndView handleUnauthorizedException(SaTokenException e) {
        // 打印错误信息
        System.out.println("未登录，跳转到登录页面");

        // 跳转到登录页面
        return new ModelAndView("redirect:/login");  // 重定向到登录页面
    }
}
