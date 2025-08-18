package com.prosper.learn.api.client;

import com.prosper.learn.dto.CourseTocDTO;
import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "learn-service", contextId = "contents")
public interface ContentsClient {

    // 获取某个用户某个课程的目录
    @GetMapping("/contents")
    Response<CourseTocDTO> get(@RequestParam(value = "userId") int userId,
                               @RequestParam(value="courseId") int courseId,
                               @RequestParam(value="create") boolean create);

    // 提交或修改用户目录
    //@PostMapping("/contents")
    Response<Object> choose(@RequestParam(value = "userId") int userId,
                    @RequestParam(value = "path") String path,
                    @RequestParam(value = "courseId") int courseId,
                    @RequestParam(value = "PostingId") int postingId);

    // 取消选择某一个帖子作为目录
    @DeleteMapping("/contents")
    Response<Object> unchoose(@RequestParam(value = "userId") int userId,
                      @RequestParam(value = "courseId") int courseId,
                      @RequestParam(value = "path") String path);

    @PostMapping("/contents/pin")
    Response<Object> pin(@RequestParam(value = "userId") int userId,
                 @RequestParam(value = "courseId") int courseId,
                 @RequestParam(value = "path") String path,
                 @RequestParam(value = "PostingId") int postingId);

    @DeleteMapping("/contents/pin")
    Response<Object> unpin(@RequestParam(value = "userId") int userId,
                   @RequestParam(value = "courseId") int courseId,
                   @RequestParam(value = "path") String path,
                   @RequestParam(value = "PostingId") int postingId);
}
