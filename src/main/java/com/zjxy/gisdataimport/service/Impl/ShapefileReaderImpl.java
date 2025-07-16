package com.zjxy.gisdataimport.service.Impl;

import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.shap.ShapefileReader;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
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
import com.zjxy.gisdataimport.service.TemplateBasedDatabaseInsertService;
import com.zjxy.gisdataimport.config.BatchProcessingConfig;
import com.zjxy.gisdataimport.config.TestConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ShapefileReaderImpl implements ShapefileReader {

    // 注意：GeoFeatureMapper 已移除，GeoFeatureEntity 现在是中间态对象
    // private com.zjxy.gisdataimport.mapper.GeoFeatureMapper geoFeatureMapper;

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

    @Autowired
    private TemplateBasedDatabaseInsertService templateBasedDatabaseInsertService;

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

    /**
     * 使用模板处理Shapefile ZIP文件
     */
    public int processShapefileZipWithTemplate(InputStream zipInputStream, String fileName,
                                              com.zjxy.gisdataimport.entity.GisManageTemplate template) {
        Long startTime = System.currentTimeMillis();
        totalFeaturesProcessed = 0;

        // 测试准备工作
        testDataService.prepareForTest();

        // 开始性能监控
        performanceMonitor.startMonitoring();

        try {
            // 创建临时目录来存放解压后的文件
            Path tempDir = Files.createTempDirectory("shapefile-temp");
            unzip(zipInputStream, tempDir.toFile(), Charset.forName("GBK"));

            processShapefileFromTempDirWithTemplate(tempDir, template);

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

        System.out.println("模板化处理完成，总共导入 " + totalFeaturesProcessed + " 条记录，耗时：" + (endTime - startTime) + "毫秒");
        return totalFeaturesProcessed;
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

    private void processShapefileFromTempDirWithTemplate(Path tempDir, GisManageTemplate template) throws Exception {
        // 查找解压后的SHP文件
        File shpFile = findShpFile(tempDir.toFile());
        if (shpFile == null) {
            throw new FileNotFoundException("在ZIP文件中未找到.shp文件");
        }

        // 创建参数映射并指定编码
        Map<String, Object> params = new HashMap<>();
        params.put("url", shpFile.toURI().toURL());
        params.put("charset", Charset.forName("GBK").name());

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

            // 使用模板化的优化批量处理
            System.out.println("=== 模板化处理模式 ===");
            System.out.println("模板ID: " + template.getId());
            System.out.println("模板名称: " + template.getNameZh());
            System.out.println("源坐标系: " + template.getOriginalCoordinateSystem());
            System.out.println("目标坐标系: " + template.getTargetCoordinateSystem());
            System.out.println("坐标转换: " + (template.getIsZh() != null && template.getIsZh() ? "启用" : "禁用"));
            System.out.println("==================");

            processShapefileWithOptimizedBatching(collection, schema, template);

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

            // 注意：不再直接保存到数据库，GeoFeatureEntity 现在是中间态对象
            // 数据应该通过模板配置插入到目标表
            log.debug("处理了地理要素: {}", geoFeature.getFeatureId());
        }
    }

    /**
     * 优化的批量处理方案 - 支持大数据量高性能处理
     */
    private void processShapefileWithOptimizedBatching(FeatureCollection<SimpleFeatureType, SimpleFeature> collection,
                                                      SimpleFeatureType schema) throws Exception {
        processShapefileWithOptimizedBatching(collection, schema, null);
    }

    /**
     * 优化的批量处理方案 - 支持模板化坐标转换
     */
    private void processShapefileWithOptimizedBatching(FeatureCollection<SimpleFeatureType, SimpleFeature> collection,
                                                      SimpleFeatureType schema,
                                                      GisManageTemplate template) throws Exception {
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
                    Future<Integer> future = executor.submit(() -> processLargeBatch(largeBatch, schema, template));
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
        return processLargeBatch(features, schema, null);
    }

    /**
     * 处理大批次数据 - 支持模板化处理
     */
    private Integer processLargeBatch(List<SimpleFeature> features, SimpleFeatureType schema,
                                     com.zjxy.gisdataimport.entity.GisManageTemplate template) {
        long startTime = System.currentTimeMillis();

        try {
            // 转换为实体对象列表
            List<GeoFeatureEntity> entities = new ArrayList<>(features.size());

            // 逐个处理要素，确保坐标转换准确性
            for (SimpleFeature feature : features) {
                GeoFeatureEntity geoFeature = convertFeatureToEntity(feature, schema, template);
                entities.add(geoFeature);
            }

            // 预处理地理要素数据（GeoFeatureEntity 现在是中间态）
            batchInsertService.batchPreprocessGeoFeatures(entities);

            // 如果有模板配置，使用模板化插入到目标表
            if (template != null) {
                try {
                    Map<String, Object> insertResult = templateBasedDatabaseInsertService.batchInsertWithTemplate(entities, template);
                    boolean success = (Boolean) insertResult.getOrDefault("success", false);
                    if (success) {
                        int insertedCount = (Integer) insertResult.getOrDefault("insertedCount", 0);
                        log.info("成功插入 {} 条记录到目标表: {}", insertedCount, template.getTableName());
                    } else {
                        String message = (String) insertResult.getOrDefault("message", "未知错误");
                        log.warn("插入到目标表失败: {}", message);
                    }
                } catch (Exception e) {
                    log.error("使用模板插入数据失败", e);
                }
            } else {
                // 没有模板配置，只是预处理数据
                log.warn("ShapefileReader 处理了 {} 条记录，但未插入到数据库（没有模板配置）", entities.size());
            }

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            // 记录性能监控数据
            performanceMonitor.recordBatchCompleted(entities.size(), processingTime);

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
        return convertFeatureToEntity(feature, schema, null);
    }

    /**
     * 将SimpleFeature转换为GeoFeatureEntity - 支持模板化转换
     */
    private GeoFeatureEntity convertFeatureToEntity(SimpleFeature feature, SimpleFeatureType schema,
                                                   com.zjxy.gisdataimport.entity.GisManageTemplate template) {
        GeoFeatureEntity geoFeature = new GeoFeatureEntity();

        // 设置要素ID
        geoFeature.setFeatureId(feature.getID());

        // 设置几何信息 (WKT格式) - 集成坐标转换功能
        if (feature.getDefaultGeometryProperty() != null &&
            feature.getDefaultGeometryProperty().getValue() != null) {
            try {
                String originalGeometryStr = feature.getDefaultGeometryProperty().getValue().toString();
                String transformedGeometryStr;

                // 根据是否有模板选择转换方式
                if (template != null) {
                    // 使用模板化坐标转换
                    transformedGeometryStr = applyTemplateCoordinateTransform(originalGeometryStr, template);

                    if (testConfig.isVerboseLogging() && !originalGeometryStr.equals(transformedGeometryStr)) {
                        System.out.println("模板坐标转换: " + template.getOriginalCoordinateSystem() +
                                         " -> " + template.getTargetCoordinateSystem() +
                                         ": " + originalGeometryStr + " -> " + transformedGeometryStr);
                    }
                } else {
                    // 使用默认坐标转换
                    transformedGeometryStr = coordinateTransformService.transformGeometry(originalGeometryStr);

                    if (testConfig.isVerboseLogging() && !originalGeometryStr.equals(transformedGeometryStr)) {
                        System.out.println("默认坐标转换: " + originalGeometryStr + " -> " + transformedGeometryStr);
                    }
                }

                geoFeature.setGeometry(transformedGeometryStr);

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
                attributes.put(attributeName, attributeValue);  // 保持原始类型
            }
        }

        // 设置原始属性（保持原始数据类型）
        geoFeature.setRawAttributes(attributes);

        // 设置属性JSON（转换为字符串格式，用于兼容）
        Map<String, Object> stringAttributes = new HashMap<>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            stringAttributes.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
        }
        geoFeature.setAttributes(convertMapToJson(stringAttributes));

        return geoFeature;
    }

    /**
     * 应用模板化坐标转换
     * 根据模板中的originalCoordinateSystem，targetCoordinateSystem，isZh字段判断是否进行坐标转换
     */
    private String applyTemplateCoordinateTransform(String geometryWkt, com.zjxy.gisdataimport.entity.GisManageTemplate template) {
        try {
            // 检查几何数据是否有效
            if (geometryWkt == null || geometryWkt.trim().isEmpty()) {
                log.warn("几何数据为空，无法进行坐标转换");
                return geometryWkt;
            }

            // 检查模板是否启用坐标转换
            if (template.getIsZh() == null || !template.getIsZh()) {
                log.debug("模板未启用坐标转换 (isZh={}), 返回原始几何数据", template.getIsZh());
                return geometryWkt; // 不进行转换
            }

            // 获取模板中的坐标系配置
            String sourceCoordSystem = template.getOriginalCoordinateSystem();
            String targetCoordSystem = template.getTargetCoordinateSystem();

            // 验证坐标系配置
            if (sourceCoordSystem == null || sourceCoordSystem.trim().isEmpty()) {
                log.warn("模板中源坐标系(originalCoordinateSystem)未配置，无法进行坐标转换");
                return geometryWkt;
            }

            if (targetCoordSystem == null || targetCoordSystem.trim().isEmpty()) {
                log.warn("模板中目标坐标系(targetCoordinateSystem)未配置，无法进行坐标转换");
                return geometryWkt;
            }

            // 检查源坐标系和目标坐标系是否相同
            if (sourceCoordSystem.equals(targetCoordSystem)) {
                log.debug("源坐标系和目标坐标系相同 ({})，无需转换", sourceCoordSystem);
                return geometryWkt;
            }

            // 检查坐标系是否受支持
            if (!coordinateTransformService.isSupportedCoordSystem(sourceCoordSystem)) {
                log.warn("源坐标系 {} 不受支持，无法进行坐标转换", sourceCoordSystem);
                return geometryWkt;
            }

            if (!coordinateTransformService.isSupportedCoordSystem(targetCoordSystem)) {
                log.warn("目标坐标系 {} 不受支持，无法进行坐标转换", targetCoordSystem);
                return geometryWkt;
            }

            log.debug("执行坐标转换: {} -> {}", sourceCoordSystem, targetCoordSystem);

            // 使用模板配置进行坐标转换
            String transformedWkt = coordinateTransformService.transformGeometryWithCoordSystems(
                geometryWkt, sourceCoordSystem, targetCoordSystem);

            // 检查转换结果
            if (transformedWkt == null) {
                log.warn("坐标转换返回null，使用原始几何数据");
                return geometryWkt;
            }

            return transformedWkt;

        } catch (Exception e) {
            log.error("模板化坐标转换失败: {}", e.getMessage(), e);
            return geometryWkt; // 返回原始数据
        }
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

    @Override
    public List<SimpleFeature> readFeatures(String shapefilePath) throws IOException {
        List<SimpleFeature> features = new ArrayList<>();

        try {
            // 创建文件对象
            File shapeFile = new File(shapefilePath);
            if (!shapeFile.exists()) {
                throw new IOException("Shapefile不存在: " + shapefilePath);
            }

            // 创建数据存储
            FileDataStore dataStore = FileDataStoreFinder.getDataStore(shapeFile);
            if (dataStore == null) {
                throw new IOException("无法创建数据存储，可能不是有效的Shapefile: " + shapefilePath);
            }

            try {
                // 获取要素源
                SimpleFeatureSource featureSource = dataStore.getFeatureSource();

                // 获取要素集合
                SimpleFeatureCollection featureCollection = featureSource.getFeatures();

                // 转换为列表
                try (SimpleFeatureIterator iterator = featureCollection.features()) {
                    while (iterator.hasNext()) {
                        features.add(iterator.next());
                    }
                }

                log.info("成功读取Shapefile: {}, 要素数量: {}", shapefilePath, features.size());

            } finally {
                dataStore.dispose();
            }

        } catch (Exception e) {
            log.error("读取Shapefile失败: {}", shapefilePath, e);
            throw new IOException("读取Shapefile失败: " + e.getMessage(), e);
        }

        return features;
    }

    @Override
    public SimpleFeatureType getSchema(String shapefilePath) throws IOException {
        try {
            // 创建文件对象
            File shapeFile = new File(shapefilePath);
            if (!shapeFile.exists()) {
                throw new IOException("Shapefile不存在: " + shapefilePath);
            }

            // 创建数据存储
            FileDataStore dataStore = FileDataStoreFinder.getDataStore(shapeFile);
            if (dataStore == null) {
                throw new IOException("无法创建数据存储，可能不是有效的Shapefile: " + shapefilePath);
            }

            try {
                // 获取要素源
                SimpleFeatureSource featureSource = dataStore.getFeatureSource();

                // 获取模式
                SimpleFeatureType schema = featureSource.getSchema();

                log.info("成功获取Shapefile模式: {}, 字段数量: {}", shapefilePath, schema.getAttributeCount());

                return schema;

            } finally {
                dataStore.dispose();
            }

        } catch (Exception e) {
            log.error("获取Shapefile模式失败: {}", shapefilePath, e);
            throw new IOException("获取Shapefile模式失败: " + e.getMessage(), e);
        }
    }


}
