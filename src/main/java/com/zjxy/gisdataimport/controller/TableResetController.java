package com.zjxy.gisdataimport.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 表重置控制器
 * 用于清空表数据并重置序列
 */
@Slf4j
@RestController
@RequestMapping("/api/table-reset")
public class TableResetController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 重置指定表（清空数据并重置序列）
     */
    @PostMapping("/reset/{tableName}")
    public ResponseEntity<Map<String, Object>> resetTable(@PathVariable String tableName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("开始重置表: {}", tableName);
            
            // 使用 TRUNCATE 清空表并重置序列
            String sql = "TRUNCATE TABLE " + tableName + " RESTART IDENTITY";
            jdbcTemplate.execute(sql);
            
            // 验证表已清空
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
            
            response.put("success", true);
            response.put("message", "表重置成功");
            response.put("tableName", tableName);
            response.put("recordCount", count);
            
            log.info("表 {} 重置成功，当前记录数: {}", tableName, count);
            
        } catch (Exception e) {
            log.error("重置表失败: {}", tableName, e);
            response.put("success", false);
            response.put("message", "重置失败: " + e.getMessage());
            response.put("tableName", tableName);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 重置 t_gas_point_cs 表的快捷方法
     */
    @PostMapping("/reset-gas-table")
    public ResponseEntity<Map<String, Object>> resetGasTable() {
        return resetTable("t_gas_point_cs");
    }

    /**
     * 检查表的记录数和序列状态
     */
    @GetMapping("/status/{tableName}")
    public ResponseEntity<Map<String, Object>> getTableStatus(@PathVariable String tableName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取记录数
            Integer recordCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
            
            // 尝试获取序列信息（假设序列名为 tableName_id_seq）
            String sequenceName = tableName + "_id_seq";
            Long nextVal = null;
            
            try {
                nextVal = jdbcTemplate.queryForObject("SELECT nextval('" + sequenceName + "')", Long.class);
                // 重置回去（因为 nextval 会增加序列值）
                jdbcTemplate.execute("SELECT setval('" + sequenceName + "', " + (nextVal - 1) + ")");
            } catch (Exception e) {
                log.debug("无法获取序列 {} 的信息: {}", sequenceName, e.getMessage());
            }
            
            response.put("success", true);
            response.put("tableName", tableName);
            response.put("recordCount", recordCount);
            response.put("sequenceName", sequenceName);
            response.put("nextSequenceValue", nextVal);
            
        } catch (Exception e) {
            log.error("获取表状态失败: {}", tableName, e);
            response.put("success", false);
            response.put("message", "获取状态失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 只重置序列（不清空数据）
     */
    @PostMapping("/reset-sequence/{tableName}")
    public ResponseEntity<Map<String, Object>> resetSequence(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") Integer startValue) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sequenceName = tableName + "_id_seq";
            
            // 重置序列
            String sql = "ALTER SEQUENCE " + sequenceName + " RESTART WITH " + startValue;
            jdbcTemplate.execute(sql);
            
            response.put("success", true);
            response.put("message", "序列重置成功");
            response.put("tableName", tableName);
            response.put("sequenceName", sequenceName);
            response.put("startValue", startValue);
            
            log.info("序列 {} 重置成功，起始值: {}", sequenceName, startValue);
            
        } catch (Exception e) {
            log.error("重置序列失败: {}", tableName, e);
            response.put("success", false);
            response.put("message", "重置序列失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
