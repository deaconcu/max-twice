package com.prosper.learn.api.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.api.client.SystemClient;
import com.prosper.learn.dto.Response;
import com.prosper.learn.persistence.dataobject.SystemDO;
import com.prosper.learn.persistence.mapper.SystemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class SystemControll implements SystemClient {

    private final SystemMapper systemMapper;
    private final ObjectMapper mapper;

    @Override
    public Response<JsonNode> get() {
        try {
            return new Response<>(mapper.readTree(systemMapper.get(0).getConfig()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response<JsonNode> getPart(String part) {
        if (part == null || part.isEmpty()) {
            return new Response<>(Response.BAD_REQUEST, "参数错误: part不能为空", null);
        }

        try {
            String config = systemMapper.get(0).getConfig();
            JsonNode jsonNode = mapper.readTree(config);
            JsonNode partNode = jsonNode.get(part);
            if (partNode == null) {
                return new Response<>(Response.NOT_FOUND, "未找到指定部分: " + part, null);
            }
            return new Response<>(partNode);
        } catch (IOException e) {
            log.error("获取系统配置部分失败", e);
            return new Response<>(Response.FAILED, "获取系统配置部分失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Response<Object> post(String config) {
        SystemDO systemDO = new SystemDO();
        systemDO.setId(0);
        systemDO.setConfig(config);
        systemMapper.update(systemDO);
        return Response.success;
    }
}
