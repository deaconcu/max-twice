package com.prosper.learn.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "learn-service", contextId = "system")
public interface SystemClient {

    // 获取单个对象
    //@GetMapping("/system")
    Response<JsonNode> get();

    // 获取单个对象
    //@GetMapping(value = "/system", params = "part")
    Response<JsonNode> getPart(String part);

    // 获取单个对象
    //@PostMapping("/system")
    Response<Object> post(String config);
}
