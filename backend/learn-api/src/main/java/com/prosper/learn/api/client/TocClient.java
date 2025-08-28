package com.prosper.learn.api.client;

import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "learn-service", contextId = "contents")
public interface TocClient {

    // 获取某个用户某个课程的目录
    @GetMapping("/toc/size")
    Response<Object> get(@RequestParam(value="courseId") Long courseId);

    // 提交或修改用户目录
    @PostMapping("/toc")
    Response<Object> post(@RequestParam(value = "courseId") Long courseId,
                          @RequestParam(value = "indexArray") String indexArray);

}
