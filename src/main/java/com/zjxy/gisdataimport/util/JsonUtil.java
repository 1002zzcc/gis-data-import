package com.zjxy.gisdataimport.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T readJson(InputStream inputStream, Class<T> tClass, Charset charset) throws IOException {
        try{
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            String s = result.toString(charset.name());
            return objectMapper.readValue(s, tClass);
        }finally {
            inputStream.close();
        }
    }

    public static <T> T readJson(String json, Class<T> tClass) throws IOException {
        return objectMapper.readValue(json, tClass);
    }

    /**
     * 将对象转换为JSON字符串
     */
    public static String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("对象转JSON失败", e);
        }
    }

    /**
     * 将JSON字符串转换为对象
     */
    public static <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON转对象失败", e);
        }
    }

    /**
     * 将JSON字符串转换为对象（支持泛型）
     */
    public static <T> T fromJsonString(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("JSON转对象失败", e);
        }
    }
}
