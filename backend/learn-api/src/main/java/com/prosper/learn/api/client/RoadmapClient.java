package com.prosper.learn.api.client;

import com.prosper.learn.dto.response.Response;
import com.prosper.learn.dto.response.RoadmapDTO;
import com.prosper.learn.dto.response.old.RoadmapDTOV1;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "roadmap")
public interface RoadmapClient {

    /**
     * 根据职业ID获取课程列表
     */
    //@GetMapping("/roadmap/list/{professionId}")
    Response<List<RoadmapDTOV1>> getListByProfession(
            @PathVariable("professionId") Long professionId, @RequestParam("lastId") Long lastId);

    /**
     * 更新课程信息
     */
    //@PutMapping("/roadmap/{id}")
    Response<Void> putById(@PathVariable("id") Long id, @RequestParam("content") String content);

    //@PutMapping("/roadmap/{id}/upvote")
    Response<Object> upvote(@PathVariable("id") Long id);
    /**
     * 创建新课程
     */
    //@PostMapping("/roadmap")
    Response<Long> post(@RequestParam Long professionId, @RequestParam String content, @RequestParam String description);

    /**
     * 获取课程详情
     */
    //@GetMapping("/roadmap/{id}")
    Response<RoadmapDTO> getById(@PathVariable("id") Long id);

    /**
     * 置顶用户课程
     */
    //@PostMapping("/roadmap/pin")
    Response<Object> pin(@RequestParam Long professionId, @RequestParam Long roadmapId);

}
