package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.config.TestConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试数据服务 - 用于测试前的数据准备和清理
 *
 * 注意：由于 GeoFeatureEntity 已重构为中间态对象，此服务不再操作 geo_features 表
 * 主要用于测试环境的数据准备和清理工作
 */
@Slf4j
@Service
public class TestDataService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private TestConfig testConfig;
    
    /**
     * 清空测试表数据
     *
     * 注意：由于 GeoFeatureEntity 不再映射到 geo_features 表，
     * 此方法现在用于清理测试环境中的其他相关表数据
     */
    @Transactional
    public void clearTestData() {
        try {
            long startTime = System.currentTimeMillis();

            log.info("开始清理测试数据...");

            // 清理可能存在的测试表（如果存在的话）
            try {
                // 检查 geo_features 表是否存在（向后兼容）
                Long countBefore = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'geo_features'",
                    Long.class);

                if (countBefore != null && countBefore > 0) {
                    // 表存在，获取记录数
                    Long recordCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM geo_features", Long.class);

                    if (recordCount != null && recordCount > 0) {
                        log.info("发现 geo_features 表中有 {} 条记录，开始清理", recordCount);
                        jdbcTemplate.execute("TRUNCATE TABLE geo_features");
                        log.info("geo_features 表清理完成");
                    } else {
                        log.info("geo_features 表中无数据，无需清理");
                    }
                } else {
                    log.info("geo_features 表不存在，跳过清理");
                }

            } catch (Exception e) {
                log.warn("清理 geo_features 表时出现问题（可能表不存在）: {}", e.getMessage());
            }

            long endTime = System.currentTimeMillis();
            log.info("测试数据清理完成，耗时: {}ms", (endTime - startTime));

        } catch (Exception e) {
            log.error("清理测试数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法清理测试数据", e);
        }
    }
    
    /**
     * 获取当前表中的记录数
     *
     * 注意：由于 GeoFeatureEntity 不再映射到数据库表，此方法现在检查 geo_features 表（如果存在）
     */
    public long getCurrentRecordCount() {
        try {
            // 检查表是否存在
            Long tableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'geo_features'",
                Long.class);

            if (tableExists != null && tableExists > 0) {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM geo_features", Long.class);
                return count != null ? count : 0;
            } else {
                log.info("geo_features 表不存在");
                return 0;
            }
        } catch (Exception e) {
            log.warn("获取记录数失败: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 检查测试前的准备工作
     */
    public void prepareForTest() {
        log.info("=== 测试准备检查 ===");

        // 显示当前配置
        if (testConfig.isEnabled()) {
            log.info("测试模式: 启用");
            log.info("测试数据量: {} 条", testConfig.getMaxRecords());
            log.info("处理方法: {}", testConfig.getMethod());
            log.info("详细日志: {}", testConfig.isVerboseLogging() ? "启用" : "禁用");
        } else {
            log.info("测试模式: 禁用 (将处理全部数据)");
        }

        // 显示当前数据状态
        long currentCount = getCurrentRecordCount();
        log.info("当前 geo_features 表记录数: {}", currentCount);

        // 根据配置决定是否清理数据
        if (testConfig.isClearTableBeforeTest() && currentCount > 0) {
            log.info("配置要求清理表数据...");
            clearTestData();
        }

        log.info("=== 准备工作完成 ===");
        log.info("注意：GeoFeatureEntity 现在是中间态对象，数据将通过模板配置插入到目标表");
    }
    
    /**
     * 测试完成后的统计信息
     */
    public void showTestResults() {
        long finalCount = getCurrentRecordCount();
        log.info("=== 测试结果统计 ===");
        log.info("geo_features 表最终记录数: {}", finalCount);

        if (testConfig.isEnabled()) {
            long expectedCount = testConfig.getMaxRecords();
            if (finalCount == expectedCount) {
                log.info("✓ geo_features 表数据量符合预期");
            } else {
                log.warn("⚠ geo_features 表数据量与预期不符，预期: {}, 实际: {}", expectedCount, finalCount);
            }
        }

        log.info("注意：由于 GeoFeatureEntity 现在是中间态对象，实际业务数据应该在目标表中");
        log.info("==================");
    }
}
