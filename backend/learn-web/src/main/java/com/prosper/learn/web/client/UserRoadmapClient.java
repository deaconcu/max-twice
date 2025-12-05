package com.prosper.learn.web.client;

import com.prosper.learn.dto.response.Response;
import com.prosper.learn.dto.response.UserRoadmapDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "userRoadmapProgress")
public interface UserRoadmapClient {

    /**
     * 用户开始学习路线图
     */
    //@PostMapping("/user/roadmap")
    Response<Object> startRoadmap(@RequestParam("roadmapId") Long roadmapId);

    /**
     * 获取用户路线图学习进度
     */
    //@GetMapping("/user/roadmap")
    Response<UserRoadmapDTO> getRoadmap(@RequestParam("roadmapId") Long roadmapId);

    /**
     * 获取用户所有路线图学习进度
     */
    //@GetMapping("/user/roadmap/list")
    Response<List<UserRoadmapDTO>> getAllRoadmap();

    /**
     * 更新路线图学习进度
     */
    //@PutMapping("/user/roadmap")
    Response<UserRoadmapDTO> updateRoadmap(@RequestParam("roadmapId") Long roadmapId,
                                           @RequestParam("progressPercent") Integer progressPercent);

    /**
     * 删除路线图学习记录
     */
    //@DeleteMapping("/user/roadmap")
    Response<Object> deleteRoadmap(@RequestParam("roadmapId") Long roadmapId);
}
