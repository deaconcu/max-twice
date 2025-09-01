package com.prosper.learn.dto.response;

import lombok.Data;

@Data
public class Response<T> {

    public static int SUCCESS = 200;
    public static int NOT_LOGIN = 401;
    public static int NOT_FOUND = 404;
    public static int BAD_REQUEST = 400;
    public static int CONFLICT = 409;
    public static int USER_NOT_EXIST = 1001;
    public static int PASSWORD_IS_WRONG = 1002;
    public static int FAILED = 1003;

    public static Response<Object> success = new Response<>(SUCCESS);
    public static Response<Object> notLogin = new Response<>(NOT_LOGIN);
    public static Response<Object> notFound = new Response<>(NOT_FOUND);
    public static Response<Object> badRequest = new Response<>(BAD_REQUEST);
    public static Response<Object> confilit = new Response<>(CONFLICT);
    public static Response<Object> userNotExist = new Response<>(USER_NOT_EXIST);
    public static Response<Object> passwordIsWrong = new Response<>(PASSWORD_IS_WRONG);
    public static Response<Object> failed = new Response<>(FAILED);

    public static <T> Response<T> instance(Integer code) {
        return new Response<>(code);
    }

    private Integer code;

    private String msg;

    private T data;

    public Response() {}

    public Response(Integer code) {
        this(code, "", null);
    }

    public Response(Integer code, String msg) {
        this(code, "", null);
    }

    public Response(T data) {
        this(200, "success", data);
    }

    public Response(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data  = data;
    }

    // 添加静态方法
    public static <T> Response<T> success(T data) {
        return new Response<>(SUCCESS, "success", data);
    }

    public static Response<Object> success() {
        return Response.success;
    }

    public static <T> Response<T> fail(int code, String msg) {
        return new Response<>(code, msg, null);
    }

    public static <T> Response<Object> fail() {
        return Response.failed;
    }
}
