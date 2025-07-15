package com.zjxy.gisdataimport.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GeoFeatureEntity 测试类
 * 验证重构后的中间态数据传输对象功能
 */
public class GeoFeatureEntityTest {

    private GeoFeatureEntity entity;

    @BeforeEach
    void setUp() {
        entity = new GeoFeatureEntity();
    }

    @Test
    void testBasicProperties() {
        // 测试基本属性设置和获取
        entity.setId(1L);
        entity.setFeatureId("feature_001");
        entity.setGeometry("POINT(116.3974 39.9093)");
        entity.setAttributes("{\"name\":\"北京\",\"type\":\"city\"}");
        
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);

        assertEquals(1L, entity.getId());
        assertEquals("feature_001", entity.getFeatureId());
        assertEquals("POINT(116.3974 39.9093)", entity.getGeometry());
        assertEquals("{\"name\":\"北京\",\"type\":\"city\"}", entity.getAttributes());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void testRawAttributesOperations() {
        // 测试原始属性操作
        assertNull(entity.getRawAttributes());
        assertFalse(entity.hasAttribute("name"));
        assertNull(entity.getAttribute("name"));

        // 设置属性
        entity.setAttribute("name", "测试点");
        entity.setAttribute("x", 116.3974);
        entity.setAttribute("y", 39.9093);

        assertTrue(entity.hasAttribute("name"));
        assertTrue(entity.hasAttribute("x"));
        assertTrue(entity.hasAttribute("y"));
        assertFalse(entity.hasAttribute("nonexistent"));

        assertEquals("测试点", entity.getAttribute("name"));
        assertEquals(116.3974, entity.getAttribute("x"));
        assertEquals(39.9093, entity.getAttribute("y"));
        assertNull(entity.getAttribute("nonexistent"));

        // 验证 rawAttributes 不为空
        assertNotNull(entity.getRawAttributes());
        assertEquals(3, entity.getRawAttributes().size());
    }

    @Test
    void testSetRawAttributes() {
        // 测试直接设置 rawAttributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("field1", "value1");
        attributes.put("field2", 123);
        attributes.put("field3", true);

        entity.setRawAttributes(attributes);

        assertEquals(attributes, entity.getRawAttributes());
        assertTrue(entity.hasAttribute("field1"));
        assertTrue(entity.hasAttribute("field2"));
        assertTrue(entity.hasAttribute("field3"));

        assertEquals("value1", entity.getAttribute("field1"));
        assertEquals(123, entity.getAttribute("field2"));
        assertEquals(true, entity.getAttribute("field3"));
    }

    @Test
    void testToString() {
        // 测试 toString 方法
        entity.setId(1L);
        entity.setFeatureId("test_feature");
        entity.setGeometry("POINT(0 0)");
        entity.setAttributes("{\"test\":\"value\"}");
        entity.setCreatedAt(LocalDateTime.of(2023, 1, 1, 12, 0, 0));
        
        entity.setAttribute("attr1", "value1");
        entity.setAttribute("attr2", "value2");

        String result = entity.toString();
        
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("featureId='test_feature'"));
        assertTrue(result.contains("geometry='POINT(0 0)...'"));
        assertTrue(result.contains("attributes='{\"test\":\"value\"}'"));
        assertTrue(result.contains("rawAttributesCount=2"));
    }

    @Test
    void testLongGeometryToString() {
        // 测试长几何字符串的 toString 处理（确保超过50个字符）
        String longGeometry = "POLYGON((0 0, 0 1, 1 1, 1 0, 0 0), (0.2 0.2, 0.2 0.8, 0.8 0.8, 0.8 0.2, 0.2 0.2), (0.3 0.3, 0.3 0.7, 0.7 0.7, 0.7 0.3, 0.3 0.3))";
        entity.setGeometry(longGeometry);

        String result = entity.toString();

        // 验证几何字符串被截断（包含省略号）
        assertTrue(result.contains("geometry='") && result.contains("...'"));
        // 验证包含前50个字符的开始部分
        assertTrue(result.contains("POLYGON((0 0, 0 1, 1 1, 1 0, 0 0), (0.2 0.2, 0.2"));
    }

    @Test
    void testNullGeometryToString() {
        // 测试空几何的 toString 处理
        entity.setGeometry(null);

        String result = entity.toString();
        
        assertTrue(result.contains("geometry='null'"));
    }

    @Test
    void testEmptyRawAttributesToString() {
        // 测试空原始属性的 toString 处理
        String result = entity.toString();
        
        assertTrue(result.contains("rawAttributesCount=0"));
    }
}
