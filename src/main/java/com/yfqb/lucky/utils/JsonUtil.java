package com.yfqb.lucky.utils;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON 工具类 - 完全类似 Fastjson 的 API
 */
public class JsonUtil {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = JsonMapper.builder().build();
    }

    /**
     * 对象转 JSON 字符串（类似 Fastjson.toJSONString）
     */
    public static String toJSONString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对象转美化格式的 JSON 字符串
     */
    public static String toJSONPrettyString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 字符串转对象（类似 Fastjson.parseObject）
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 字符串转带泛型的对象
     */
    public static <T> T parseObject(String json, tools.jackson.core.type.TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 字符串转 List（类似 Fastjson.parseArray）
     */
    public static <T> java.util.List<T> parseArray(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, clazz));
        } catch (JacksonException e) {
            throw new RuntimeException("JSON反序列化失败: " + e.getMessage(), e);
        }
    }
}
