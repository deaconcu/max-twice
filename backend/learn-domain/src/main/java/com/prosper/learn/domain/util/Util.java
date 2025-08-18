package com.prosper.learn.domain.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Util {

    private static ObjectMapper objectMapper;

    public Util(ObjectMapper objectMapper) {
        Util.objectMapper = objectMapper;
    }

    public static String toJson(Object object) {
        try{
            return objectMapper.writeValueAsString(object);
        } catch ( Exception e ) {
            throw new RuntimeException("Parse json failed", e);
        }
    }

    public static Map<String, Object> readValueToMap(String json) {
        try{
            return objectMapper.readValue(json, Map.class);
        } catch ( Exception e ) {
            throw new RuntimeException("Parse json failed", e);
        }
    }
}
