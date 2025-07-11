package com.zjxy.gisdataimport.shap;

import java.io.InputStream;
import java.util.List;

/**
 * Shapefile读取服务接口
 */
public interface ShapefileReader {
    
    /**
     * 从ZIP文件输入流读取Shapefile并保存到数据库
     * 
     * @param zipInputStream ZIP文件输入流
     * @param fileName 原始文件名
     * @return 处理的要素数量
     */
    int processShapefileZip(InputStream zipInputStream, String fileName);
    
    /**
     * 从指定路径的ZIP文件读取Shapefile并保存到数据库
     * 
     * @param zipFilePath ZIP文件路径
     * @return 处理的要素数量
     */
    int processShapefileZipFromPath(String zipFilePath);
}
