package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.service.Impl.TemplateBasedShapefileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 模板坐标转换测试类
 * 测试根据模板中的originalCoordinateSystem，targetCoordinateSystem，isZh字段判断是否进行坐标转换
 */
public class TemplateCoordinateTransformTest {

    @Mock
    private CoordinateTransformService coordinateTransformService;

    @InjectMocks
    private TemplateBasedShapefileServiceImpl templateService;

    private GisManageTemplate template;
    private final String TEST_GEOMETRY = "POINT(120.5 30.5)";
    private final String TRANSFORMED_GEOMETRY = "POINT(3337641.456 3379401.623)";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // 创建测试模板
        template = new GisManageTemplate();
        template.setId(1);
        template.setNameZh("测试模板");
        template.setTableName("test_table");

        // 设置默认的坐标系配置
        template.setIsZh(true);
        template.setOriginalCoordinateSystem("CGCS2000");
        template.setTargetCoordinateSystem("CGCS2000XY");

        // 配置模拟行为
        when(coordinateTransformService.isSupportedCoordSystem(anyString())).thenReturn(true);
        when(coordinateTransformService.transformGeometryWithCoordSystems(
                eq(TEST_GEOMETRY), eq("CGCS2000"), eq("CGCS2000XY")))
                .thenReturn(TRANSFORMED_GEOMETRY);
    }

    @Test
    public void testCoordinateTransformEnabled() {
        // 启用坐标转换
        template.setIsZh(true);
        template.setOriginalCoordinateSystem("CGCS2000");
        template.setTargetCoordinateSystem("CGCS2000XY");

        String result = templateService.applyCoordinateTransformWithTemplate(TEST_GEOMETRY, template);
        
        assertEquals(TRANSFORMED_GEOMETRY, result);
        verify(coordinateTransformService).transformGeometryWithCoordSystems(
                TEST_GEOMETRY, "CGCS2000", "CGCS2000XY");
    }

    @Test
    public void testCoordinateTransformDisabled() {
        // 禁用坐标转换
        template.setIsZh(false);

        String result = templateService.applyCoordinateTransformWithTemplate(TEST_GEOMETRY, template);
        
        assertEquals(TEST_GEOMETRY, result);
        verify(coordinateTransformService, never()).transformGeometryWithCoordSystems(
                anyString(), anyString(), anyString());
    }

    @Test
    public void testMissingSourceCoordinateSystem() {
        // 启用坐标转换但缺少源坐标系
        template.setIsZh(true);
        template.setOriginalCoordinateSystem(null);
        template.setTargetCoordinateSystem("CGCS2000XY");

        String result = templateService.applyCoordinateTransformWithTemplate(TEST_GEOMETRY, template);
        
        assertEquals(TEST_GEOMETRY, result);
        verify(coordinateTransformService, never()).transformGeometryWithCoordSystems(
                anyString(), anyString(), anyString());
    }

    @Test
    public void testMissingTargetCoordinateSystem() {
        // 启用坐标转换但缺少目标坐标系
        template.setIsZh(true);
        template.setOriginalCoordinateSystem("CGCS2000");
        template.setTargetCoordinateSystem(null);

        String result = templateService.applyCoordinateTransformWithTemplate(TEST_GEOMETRY, template);
        
        assertEquals(TEST_GEOMETRY, result);
        verify(coordinateTransformService, never()).transformGeometryWithCoordSystems(
                anyString(), anyString(), anyString());
    }

    @Test
    public void testSameSourceAndTargetCoordinateSystem() {
        // 源坐标系和目标坐标系相同
        template.setIsZh(true);
        template.setOriginalCoordinateSystem("CGCS2000");
        template.setTargetCoordinateSystem("CGCS2000");

        String result = templateService.applyCoordinateTransformWithTemplate(TEST_GEOMETRY, template);
        
        assertEquals(TEST_GEOMETRY, result);
        verify(coordinateTransformService, never()).transformGeometryWithCoordSystems(
                anyString(), anyString(), anyString());
    }

    @Test
    public void testUnsupportedSourceCoordinateSystem() {
        // 源坐标系不受支持
        template.setIsZh(true);
        template.setOriginalCoordinateSystem("UNSUPPORTED");
        template.setTargetCoordinateSystem("CGCS2000XY");

        when(coordinateTransformService.isSupportedCoordSystem("UNSUPPORTED")).thenReturn(false);

        String result = templateService.applyCoordinateTransformWithTemplate(TEST_GEOMETRY, template);
        
        assertEquals(TEST_GEOMETRY, result);
        verify(coordinateTransformService, never()).transformGeometryWithCoordSystems(
                anyString(), anyString(), anyString());
    }
}
