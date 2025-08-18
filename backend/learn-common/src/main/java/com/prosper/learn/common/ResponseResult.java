package com.prosper.learn.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Getter
public class ResponseResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    private static <T> ResponseResult<T> build(HttpStatus httpStatus, T data) {
        return new ResponseResult<T>().setCode(httpStatus.value()).setMsg(httpStatus.getReasonPhrase()).setData(data);
    }

    public static <T> ResponseResult<T> ok(){
        return build(HttpStatus.OK, null);
    }
    public static <T> ResponseResult<T> ok(T data){
        return build(HttpStatus.OK, data);
    }
    public static <T> ResponseResult<T> created(){
        return build(HttpStatus.CREATED, null);
    }
    public static <T> ResponseResult<T> invalid_request(){
        return build(HttpStatus.BAD_REQUEST, null);
    }
    public static <T> ResponseResult<T> notFound(){
        return build(HttpStatus.NOT_FOUND, null);
    }
    public static <T> ResponseResult<T> unauthorized(){
        return build(HttpStatus.UNAUTHORIZED, null);
    }
    public static <T> ResponseResult<T> forbidden(){
        return build(HttpStatus.FORBIDDEN, null);
    }
    public static <T> ResponseResult<T> internal_server_error(){
        return build(HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    public ResponseResult<T> setCode(int code) {
        this.code = code;
        return this;
    }

    public ResponseResult<T> setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public ResponseResult<T> setData(T data) {
        this.data = data;
        return this;
    }
}
