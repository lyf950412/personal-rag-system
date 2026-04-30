package com.rag.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

/**
 * JSON工具类 - 基于Jackson实现
 */
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 配置ObjectMapper
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    }

    /**
     * 获取ObjectMapper实例
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 对象转JSON字符串
     *
     * @param obj 要转换的对象
     * @return JSON字符串，转换失败返回null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转JSON失败", e);
        }
    }

    /**
     * 对象转格式化的JSON字符串（美化输出）
     *
     * @param obj 要转换的对象
     * @return 格式化的JSON字符串，转换失败返回null
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转格式化JSON失败", e);
        }
    }

    /**
     * JSON字符串转对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 转换后的对象，转换失败返回null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON转对象失败", e);
        }
    }

    /**
     * JSON字符串转复杂类型（如List、Map等）
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 转换后的对象，转换失败返回null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON转复杂类型失败", e);
        }
    }

    /**
     * JSON字符串转Map
     *
     * @param json JSON字符串
     * @return Map对象，转换失败返回null
     */
    public static Map<String, Object> jsonToMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * 对象转Map
     *
     * @param obj 要转换的对象
     * @return Map对象，转换失败返回null
     */
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * Map转对象
     *
     * @param map   Map对象
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 转换后的对象，转换失败返回null
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return objectMapper.convertValue(map, clazz);
    }

    /**
     * JSON字符串转节点树（用于动态解析）
     *
     * @param json JSON字符串
     * @return JsonNode，转换失败返回null
     */
    public static com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON转节点树失败", e);
        }
    }

    /**
     * 验证字符串是否为合法JSON
     *
     * @param json 待验证的JSON字符串
     * @return true-合法JSON，false-非法JSON
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
