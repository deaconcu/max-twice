package com.prosper.learn.api.util;

import cn.dev33.satoken.exception.NotLoginException;
import com.prosper.learn.dto.Response;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Response<Object> handleNotLoginException(NotLoginException e) {
        return Response.notLogin;
    }
}