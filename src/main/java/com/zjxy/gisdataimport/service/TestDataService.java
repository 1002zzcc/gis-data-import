package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.mapper.GeoFeatureMapper;
import com.zjxy.gisdataimport.config.TestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试数据服务 - 用于测试前的数据准备和清理
 */
@Service
public class TestDataService {
    
    @Autowired
    private GeoFeatureMapper geoFeatureMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private TestConfig testConfig;
    
    /**
     * 清空测试表数据
     */
    @Transactional
    public void clearTestData() {
        try {
            long startTime = System.currentTimeMillis();
            
            // 获取清理前的记录数
            long countBefore = geoFeatureMapper.selectCount(null);
            
            if (countBefore > 0) {
                System.out.println("开始清理测试数据，当前记录数: " + countBefore);
                
                // 使用TRUNCATE快速清空表（比DELETE快）
                jdbcTemplate.execute("TRUNCATE TABLE geo_features");
                
                long endTime = System.currentTimeMillis();
                System.out.println("测试数据清理完成，耗时: " + (endTime - startTime) + "ms");
            } else {
                System.out.println("表中无数据，无需清理");
            }
            
        } catch (Exception e) {
            System.err.println("清理测试数据失败: " + e.getMessage());
            // 如果TRUNCATE失败，尝试使用DELETE
            try {
                geoFeatureMapper.delete(null);
                System.out.println("使用DELETE方式清理数据完成");
            } catch (Exception ex) {
                System.err.println("DELETE方式清理也失败: " + ex.getMessage());
                throw new RuntimeException("无法清理测试数据", ex);
            }
        }
    }
    
    /**
     * 获取当前表中的记录数
     */
    public long getCurrentRecordCount() {
        return geoFeatureMapper.selectCount(null);
    }
    
    /**
     * 检查测试前的准备工作
     */
    public void prepareForTest() {
        System.out.println("=== 测试准备检查 ===");
        
        // 显示当前配置
        if (testConfig.isEnabled()) {
            System.out.println("测试模式: 启用");
            System.out.println("测试数据量: " + testConfig.getMaxRecords() + " 条");
            System.out.println("处理方法: " + testConfig.getMethod());
            System.out.println("详细日志: " + (testConfig.isVerboseLogging() ? "启用" : "禁用"));
        } else {
            System.out.println("测试模式: 禁用 (将处理全部数据)");
        }
        
        // 显示当前数据状态
        long currentCount = getCurrentRecordCount();
        System.out.println("当前表记录数: " + currentCount);
        
        // 根据配置决定是否清理数据
        if (testConfig.isClearTableBeforeTest() && currentCount > 0) {
            System.out.println("配置要求清理表数据...");
            clearTestData();
        }
        
        System.out.println("=== 准备工作完成 ===");
    }
    
    /**
     * 测试完成后的统计信息
     */
    public void showTestResults() {
        long finalCount = getCurrentRecordCount();
        System.out.println("=== 测试结果统计 ===");
        System.out.println("最终表记录数: " + finalCount);
        
        if (testConfig.isEnabled()) {
            long expectedCount = testConfig.getMaxRecords();
            if (finalCount == expectedCount) {
                System.out.println("✓ 数据量符合预期");
            } else {
                System.out.println("⚠ 数据量与预期不符，预期: " + expectedCount + ", 实际: " + finalCount);
            }
        }
        
        System.out.println("==================");
    }
}
