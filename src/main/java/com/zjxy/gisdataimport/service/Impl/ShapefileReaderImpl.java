package com.zjxy.gisdataimport.service.Impl;

import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import com.zjxy.gisdataimport.shap.ShapefileReader;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import com.zjxy.gisdataimport.service.PerformanceMonitor;
import com.zjxy.gisdataimport.service.TestDataService;
import com.zjxy.gisdataimport.service.BatchInsertService;
import com.zjxy.gisdataimport.service.CoordinateTransformService;
import com.zjxy.gisdataimport.config.BatchProcessingConfig;
import com.zjxy.gisdataimport.config.TestConfig;

@Service
public class ShapefileReaderImpl implements ShapefileReader {

    @Autowired
    private com.zjxy.gisdataimport.mapper.GeoFeatureMapper geoFeatureMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PerformanceMonitor performanceMonitor;

    @Autowired
    private TestConfig testConfig;

    @Autowired
    private TestDataService testDataService;

    @Autowired
    private BatchInsertService batchInsertService;

    @Autowired
    private CoordinateTransformService coordinateTransformService;

    private int totalFeaturesProcessed = 0;

    @Override
    public int processShapefileZip(InputStream zipInputStream, String fileName) {
        Long startTime = System.currentTimeMillis();
        totalFeaturesProcessed = 0;

        // 测试准备工作
        testDataService.prepareForTest();

        // 开始性能监控
        performanceMonitor.startMonitoring();

        try {
            // 创建临时目录来存放解压后的文件
            Path tempDir = Files.createTempDirectory("shapefile-temp");
            unzip(zipInputStream, tempDir.toFile(), Charset.forName("GBK")); // 尝试使用GBK编码

            processShapefileFromTempDir(tempDir);

            // 删除临时目录及其内容
            deleteDirectory(tempDir.toFile());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("处理Shapefile ZIP文件失败", e);
        }

        Long endTime = System.currentTimeMillis();

        // 完成性能监控
        performanceMonitor.finishMonitoring();

        // 显示测试结果
        testDataService.showTestResults();

        System.out.println("处理完成，总共导入 " + totalFeaturesProcessed + " 条记录，耗时：" + (endTime - startTime) + "毫秒");
        return totalFeaturesProcessed;
    }

    @Override
    public int processShapefileZipFromPath(String zipFilePath) {
        try (FileInputStream fis = new FileInputStream(new File(zipFilePath))) {
            return processShapefileZip(fis, new File(zipFilePath).getName());
        } catch (IOException e) {
            throw new RuntimeException("无法读取ZIP文件: " + zipFilePath, e);
        }
    }

    private void processShapefileFromTempDir(Path tempDir) throws Exception {
        // 查找解压后的SHP文件
        File shpFile = findShpFile(tempDir.toFile());
        if (shpFile == null) {
            throw new FileNotFoundException("在ZIP文件中未找到.shp文件");
        }

        // 创建参数映射并指定编码
        Map<String, Object> params = new HashMap<>();
        params.put("url", shpFile.toURI().toURL());
        params.put("charset", Charset.forName("GBK").name()); // 指定编码

        // 获取DataStore对象
        DataStore dataStore = null;
        try {
            dataStore = DataStoreFinder.getDataStore(params);
            if (dataStore == null) {
                throw new IOException("无法创建DataStore");
            }

            // 获取类型名称（通常是文件名）
            String typeName = dataStore.getTypeNames()[0];

            // 获取FeatureSource对象
            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);

            // 获取FeatureCollection对象
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();

            // 获取Schema
            SimpleFeatureType schema = source.getSchema();

            // ========== 性能测试选择 ==========
            // 根据配置选择处理方式

            if (testConfig.isEnabled()) {
                System.out.println("=== 性能测试模式 ===");
                System.out.println("测试数据量: " + testConfig.getMaxRecords() + " 条");
                System.out.println("处理方法: " + testConfig.getMethod());
                System.out.println("==================");

                if ("original".equals(testConfig.getMethod())) {
                    // 方式1：原始处理方法 (测试对比用)
                    processShapefileWithOriginalMethod(collection, schema);
                } else {
                    // 方式2：优化后的批量处理方案 (新方法)
                    processShapefileWithOptimizedBatching(collection, schema);
                }
            } else {
                // 生产模式：使用优化方法处理全部数据
                processShapefileWithOptimizedBatching(collection, schema);
            }

        } finally {
            // 关闭DataStore
            if (dataStore != null) {
                dataStore.dispose();
            }
        }
    }

    private void unzip(InputStream inputStream, File destDirectory, Charset charset) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        try (ZipInputStream zis = new ZipInputStream(inputStream, charset)) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(destDirectory, fileName);
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
        }
    }

    private File findShpFile(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".shp"));
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    private void deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }

    /**
     * 原始处理方法 - 保留用于性能对比测试
     */
    private void processShapefileWithOriginalMethod(FeatureCollection<SimpleFeatureType, SimpleFeature> collection,
                                                   SimpleFeatureType schema) throws Exception {
        // 使用多线程处理要素 (原始方法)
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final int batchSize = 100; // 原始批次大小
        final int maxRecords = testConfig.isEnabled() ? testConfig.getMaxRecords() : Integer.MAX_VALUE;

        int processedCount = 0; // 将变量声明移到try块外面

        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext() && processedCount < maxRecords) { // 根据配置限制数据量
                SimpleFeature[] batch = new SimpleFeature[batchSize];
                int count = 0;
                while (count < batchSize && features.hasNext() && processedCount < maxRecords) {
                    batch[count++] = features.next();
                    processedCount++;
                }
                if (count > 0) {
                    final SimpleFeature[] finalBatch = new SimpleFeature[count];
                    System.arraycopy(batch, 0, finalBatch, 0, count);
                    executor.submit(() -> processOriginalBatch(finalBatch, schema));

                    if (processedCount % 1000 == 0) {
                        System.out.println("原始方法已处理 " + processedCount + " 条记录");
                    }
                }
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }

        totalFeaturesProcessed = Math.min(maxRecords, processedCount);
        System.out.println("原始方法处理完成，实际处理: " + totalFeaturesProcessed + " 条记录");
    }

    /**
     * 原始批次处理方法 - 逐条保存到数据库
     */
    private void processOriginalBatch(SimpleFeature[] batch, SimpleFeatureType schema) {
        for (SimpleFeature feature : batch) {
            // 创建一个实体对象来保存要素数据
            GeoFeatureEntity geoFeature = new GeoFeatureEntity();

            // 设置要素ID
            geoFeature.setFeatureId(feature.getID());

            // 设置几何信息 (WKT格式) - 集成坐标转换功能
            if (feature.getDefaultGeometryProperty() != null && feature.getDefaultGeometryProperty().getValue() != null) {
                try {
                    String originalGeometryStr = feature.getDefaultGeometryProperty().getValue().toString();

                    // 进行坐标转换（使用配置的坐标系）
                    String transformedGeometryStr = coordinateTransformService.transformGeometry(originalGeometryStr);

                    geoFeature.setGeometry(transformedGeometryStr);
                } catch (Exception e) {
                    // 静默处理几何信息转换异常，避免输出详细参数
                    geoFeature.setGeometry("GEOMETRY_ERROR");
                }
            }

            // 处理属性
            Map<String, Object> attributes = new HashMap<>();
            for (int i = 0; i < schema.getAttributeCount(); i++) {
                String attributeName = schema.getDescriptor(i).getLocalName();
                Object attributeValue = feature.getAttribute(attributeName);
                if (attributeValue != null) {
                    attributes.put(attributeName, attributeValue.toString());
                }
            }

            // 设置属性JSON
            geoFeature.setAttributes(convertMapToJson(attributes));

            // 保存到数据库 (原始方法：逐条保存)
            geoFeatureMapper.insert(geoFeature);
        }
    }

    /**
     * 优化的批量处理方案 - 支持大数据量高性能处理
     */
    private void processShapefileWithOptimizedBatching(FeatureCollection<SimpleFeatureType, SimpleFeature> collection,
                                                      SimpleFeatureType schema) throws Exception {
        final int LARGE_BATCH_SIZE = BatchProcessingConfig.BatchConstants.LARGE_BATCH_SIZE;
        final int THREAD_COUNT = Math.min(Runtime.getRuntime().availableProcessors() * 2,
                                         BatchProcessingConfig.BatchConstants.MAX_THREAD_COUNT);
        final int maxRecords = testConfig.isEnabled() ? testConfig.getMaxRecords() : Integer.MAX_VALUE;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Integer>> futures = new ArrayList<>();

        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            int totalProcessedInTest = 0; // 测试计数器

            while (features.hasNext() && totalProcessedInTest < maxRecords) { // 根据配置限制数据量
                // 收集一个大批次的数据
                List<SimpleFeature> largeBatch = new ArrayList<>(LARGE_BATCH_SIZE);
                int count = 0;

                while (count < LARGE_BATCH_SIZE && features.hasNext() && totalProcessedInTest < maxRecords) {
                    largeBatch.add(features.next());
                    count++;
                    totalProcessedInTest++;
                }

                if (!largeBatch.isEmpty()) {
                    // 提交批次处理任务
                    Future<Integer> future = executor.submit(() -> processLargeBatch(largeBatch, schema));
                    futures.add(future);

                     System.out.println("优化方法 - 提交批次处理任务，批次大小: " + largeBatch.size() +
                                      ", 已提交批次数: " + futures.size() +
                                      ", 累计处理: " + totalProcessedInTest + " 条");
                }
            }

            // 等待所有任务完成并统计结果
            int totalProcessed = 0;
            for (Future<Integer> future : futures) {
                totalProcessed += future.get(); // 等待任务完成并获取结果
            }

            totalFeaturesProcessed = totalProcessed;
            System.out.println("所有批次处理完成，总计处理: " + totalProcessed + " 条记录");

        } finally {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 处理大批次数据 - 使用JDBC批量插入优化性能
     */
    private Integer processLargeBatch(List<SimpleFeature> features, SimpleFeatureType schema) {
        long startTime = System.currentTimeMillis();

        try {
            // 转换为实体对象列表
            List<GeoFeatureEntity> entities = new ArrayList<>(features.size());

            for (SimpleFeature feature : features) {
                GeoFeatureEntity geoFeature = convertFeatureToEntity(feature, schema);
                entities.add(geoFeature);
            }

            // 使用MyBatis-Plus批量插入
            batchInsertService.fastBatchInsert(entities);

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            // 记录性能监控数据
            performanceMonitor.recordBatchCompleted(entities.size(), processingTime);

            // System.out.println("批次处理完成 - 线程: " + Thread.currentThread().getName() +
            //                  ", 处理数量: " + entities.size() +
            //                  ", 耗时: " + processingTime + "ms");

            return entities.size();

        } catch (Exception e) {
            System.err.println("批次处理失败 - 线程: " + Thread.currentThread().getName() +
                             ", 错误: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将SimpleFeature转换为GeoFeatureEntity
     */
    private GeoFeatureEntity convertFeatureToEntity(SimpleFeature feature, SimpleFeatureType schema) {
        GeoFeatureEntity geoFeature = new GeoFeatureEntity();

        // 设置要素ID
        geoFeature.setFeatureId(feature.getID());

        // 设置几何信息 (WKT格式) - 集成坐标转换功能
        if (feature.getDefaultGeometryProperty() != null &&
            feature.getDefaultGeometryProperty().getValue() != null) {
            try {
                String originalGeometryStr = feature.getDefaultGeometryProperty().getValue().toString();

                // 进行坐标转换（使用配置的坐标系）
                String transformedGeometryStr = coordinateTransformService.transformGeometry(originalGeometryStr);

                geoFeature.setGeometry(transformedGeometryStr);

                // 可选：记录转换日志（仅在调试时启用）
                if (testConfig.isVerboseLogging() && !originalGeometryStr.equals(transformedGeometryStr)) {
                    System.out.println("坐标转换: " + originalGeometryStr + " -> " + transformedGeometryStr);
                }

            } catch (Exception e) {
                // 静默处理几何信息转换异常，避免输出详细参数
                System.err.println("几何信息处理失败: " + e.getMessage());
                geoFeature.setGeometry("GEOMETRY_ERROR");
            }
        }

        // 处理属性
        Map<String, Object> attributes = new HashMap<>();
        for (int i = 0; i < schema.getAttributeCount(); i++) {
            String attributeName = schema.getDescriptor(i).getLocalName();
            Object attributeValue = feature.getAttribute(attributeName);
            if (attributeValue != null) {
                attributes.put(attributeName, attributeValue.toString());
            }
        }

        // 设置属性JSON
        geoFeature.setAttributes(convertMapToJson(attributes));

        return geoFeature;
    }

    private String convertMapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":\"")
                .append(entry.getValue().toString().replace("\"", "\\\""))
                .append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

}
