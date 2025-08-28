package com.prosper.learn.api.client;

import com.prosper.learn.dto.NodeDTO;
import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "node")
public interface NodeClient{

    // 创建一个node
    @PostMapping("/node")
    Response post(@RequestBody NodeDTO nodeDTO);

    // 获取一个对象
    @GetMapping("/node/{id}")
    NodeDTO get(@PathVariable Long id);

    // 根据多个id同时获取多个对象
    @GetMapping("/node/ids-{ids}")
    List<NodeDTO> getNodes(@PathVariable String ids);

    /*
    // 根据多个id同时获取多个对象
    @GetMapping("/node/tree/{id}")
    NodeDTO getNodeTree(@PathVariable Long id);
     */


}