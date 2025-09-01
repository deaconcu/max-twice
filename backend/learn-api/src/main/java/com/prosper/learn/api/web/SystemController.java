package com.prosper.learn.api.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.api.client.SystemClient;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.dto.response.Response;
import com.prosper.learn.persistence.dataobject.SystemDO;
import com.prosper.learn.persistence.mapper.SystemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

//@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class SystemController implements SystemClient {

    private final SystemMapper systemMapper;
    private final ObjectMapper mapper;

    @Override
    public Response<JsonNode> get() {
        try {
            return new Response<>(mapper.readTree(systemMapper.get(0).getConfig()));
        } catch (IOException e) {
            throw ErrorCode.JSON_PARSE_ERROR.exception(e);
        }
    }

    @Override
    public Response<JsonNode> getPart(String part) {
        if (part == null || part.isEmpty()) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        try {
            String config = systemMapper.get(1).getConfig();
            JsonNode jsonNode = mapper.readTree(config);
            JsonNode partNode = jsonNode.get(part);
            if (partNode == null) {
                throw ErrorCode.SYSTEM_ERROR.exception();
            }
            return new Response<>(partNode);
        } catch (IOException e) {
            throw ErrorCode.JSON_PARSE_ERROR.exception(e);
        }
    }

    @Override
    public Response<Object> post(String config) {
        SystemDO systemDO = new SystemDO();
        systemDO.setId(1L);
        systemDO.setConfig(config);
        systemMapper.update(systemDO);
        return Response.success;
    }
}
