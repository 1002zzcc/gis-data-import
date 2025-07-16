package com.zjxy.gisdataimport.util;

import com.zjxy.gisdataimport.coordsystem.CoordConvertParam;
import com.zjxy.gisdataimport.coordsystem.Ellipsoid;
import com.zjxy.gisdataimport.coordsystem.PlaneFourParam;
import com.zjxy.gisdataimport.dto.ZbzhDTO;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.Collections;

@Slf4j
public class ZbzhUtil {
    public static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);

    public static String createPoint(Double x, Double y){
        org.locationtech.jts.geom.Coordinate coordinate = new org.locationtech.jts.geom.Coordinate(x, y);
        Point point = geometryFactory.createPoint(coordinate);
        WKTWriter wktWriter = new WKTWriter();
        String write = wktWriter.write(point);
        return write;
    }

    public static String crateLine(List<Map<String,Double>> list){
        if(list != null && list.size() > 0){
            org.locationtech.jts.geom.Coordinate[] coordinates = new org.locationtech.jts.geom.Coordinate[list.size()];
            for(int i = 0;i < list.size(); i++){
                Map<String, Double> stringDoubleMap = list.get(i);

                org.locationtech.jts.geom.Coordinate coordinate = new org.locationtech.jts.geom.Coordinate(stringDoubleMap.get("x"), stringDoubleMap.get("y"));
                coordinates[i] = coordinate;
            }

            LineString lineString = geometryFactory.createLineString(coordinates);
            WKTWriter wktWriter = new WKTWriter();
            String write = wktWriter.write(lineString);
            return write;

        }
        return null;
    }

    public static String createPolygon(List<Map<String,Double>> list){
        if(list != null && list.size() > 0){
            org.locationtech.jts.geom.Coordinate[] coordinates = new org.locationtech.jts.geom.Coordinate[list.size()];
            for(int i = 0;i < list.size(); i++){
                Map<String, Double> stringDoubleMap = list.get(i);

                org.locationtech.jts.geom.Coordinate coordinate = new org.locationtech.jts.geom.Coordinate(stringDoubleMap.get("x"), stringDoubleMap.get("y"));
                coordinates[i] = coordinate;
            }
            LinearRing linearRing = new GeometryFactory().createLinearRing(coordinates);
            Polygon polygon = geometryFactory.createPolygon(linearRing);
            WKTWriter wktWriter = new WKTWriter();
            String write = wktWriter.write(polygon);
            return write;

        }
        return null;
    }

    public static String crateLine(Double x, Double y,Double x1,Double y1){
        if(x != null && y != null && x1 != null && y1 != null){
            LineString lineString = geometryFactory.createLineString(new org.locationtech.jts.geom.Coordinate[]{
                            new org.locationtech.jts.geom.Coordinate(x,y),
                            new org.locationtech.jts.geom.Coordinate(x1,y1)
                    }
            );
            WKTWriter wktWriter = new WKTWriter();
            String write = wktWriter.write(lineString);
            return write;

        }
        return null;
    }


    public static List<String> CoordConvert2D(@RequestBody ZbzhDTO dto) throws IOException {
        org.springframework.core.io.Resource resource = new ClassPathResource("CoordSystem.json");
        Map<String,Object> map = JsonUtil.readJson(resource.getInputStream(), Map.class, StandardCharsets.UTF_8);

        if(!map.keySet().contains(dto.getSource())){
            List<String> strings = new ArrayList<>();
            strings.add("未找到坐标系"+dto.getSource());
            return strings;
        }
        if(!map.keySet().contains(dto.getTarget())){
            List<String> strings = new ArrayList<>();
            strings.add("未找到坐标系"+dto.getTarget());
            return strings;
        }
        CoordConvertParam coordConvertParam = new CoordConvertParam();
        Map<String,Object> source = (Map<String, Object>) map.get(dto.getSource());
        Map<String,Object> target = (Map<String, Object>) map.get(dto.getTarget());
        String csName1 = (String) source.get("EllipsoidType");
        csName1 = dto.getSource();
        String csName2 = (String) target.get("EllipsoidType");
        csName2 = dto.getTarget();
        coordConvertParam.setCSName1(csName1);
        coordConvertParam.setCSName2(csName2);
        coordConvertParam.setIsBLCoord1((Boolean) source.get("IsBLCoord"));
        coordConvertParam.setIsBLCoord2((Boolean) target.get("IsBLCoord"));
        if(dto.getL1()==null){
            dto.setL1(Double.parseDouble(source.get("L0").toString()));
        }
        if(dto.getL2()==null){
            dto.setL2(Double.parseDouble(target.get("L0").toString()));
        }
        coordConvertParam.setL1(dto.getL1());
        coordConvertParam.setL2(dto.getL2());
        coordConvertParam.setEllipsoid1(new Ellipsoid((String) source.get("EllipsoidType")));
        coordConvertParam.setEllipsoid2(new Ellipsoid((String) target.get("EllipsoidType")));
        PlaneFourParam planeFourParam = new PlaneFourParam();
        //坐标系不同时才需要平面四参数
        boolean forward = true;
        if(!coordConvertParam.getCSName1().equals(coordConvertParam.getCSName2())){
            org.springframework.core.io.Resource resource2 = new ClassPathResource("PlaneFourParam.json");
            Map<String,Object> map2 = JsonUtil.readJson(resource2.getInputStream(), Map.class, StandardCharsets.UTF_8);
            Map<String,Object> m = null;
            if(map2.keySet().contains(dto.getSource()+"-"+dto.getTarget())){
                forward = true;
                m = (Map<String, Object>) map2.get(dto.getSource() + "-" + dto.getTarget());
            }
            else if(map2.keySet().contains(dto.getTarget()+"-"+dto.getSource())){
                forward = false;
                m = (Map<String, Object>) map2.get(dto.getTarget() + "-" + dto.getSource());
            }else {
                List<String> strings = new ArrayList<>();
                strings.add("缺少从"+dto.getSource()+"转换至"+dto.getTarget()+"的参数");
                return strings;
            }
            planeFourParam.setL1(Double.parseDouble(m.get("L1").toString()));
            planeFourParam.setL2(Double.parseDouble(m.get("L2").toString()));
            planeFourParam.setA(Double.parseDouble(m.get("A").toString()));
            planeFourParam.setB(Double.parseDouble(m.get("B").toString()));
            planeFourParam.setC(Double.parseDouble(m.get("C").toString()));
            planeFourParam.setD(Double.parseDouble(m.get("D").toString()));
            planeFourParam.setAlfa(Double.parseDouble(m.get("Alfa").toString()));
            planeFourParam.setDX(Double.parseDouble(m.get("dX").toString()));
            planeFourParam.setDY(Double.parseDouble(m.get("dY").toString()));
            planeFourParam.setK(Double.parseDouble(m.get("K").toString()));
        }
        List<Geometry> geometryList = ParseWKTToGeometry(dto.getWkts());
        List<String> result = new ArrayList<>();
        for (Geometry geometry:geometryList){
            for (Coordinate coordinate:geometry.getCoordinates()){
                if(dto.isSurveyCoord()){
                    double[] array = CoordConvert2D(coordinate.getX(), coordinate.getY(), coordConvertParam, planeFourParam,forward);
                    coordinate.setX(array[0]);
                    coordinate.setY(array[1]);
                }else {
                    double[] array = CoordConvert2D(coordinate.getY(), coordinate.getX(), coordConvertParam, planeFourParam,forward);
                    coordinate.setX(array[1]);
                    coordinate.setY(array[0]);
                }
            }
            result.add(geometry.toText().replace(" (","("));
        }
        return result;
    }

    public static Map<String,Object> GetCoordSystem() throws IOException {
        org.springframework.core.io.Resource resource = new ClassPathResource("" +
                "CoordSystem.json");
        Map<String,Object> map = JsonUtil.readJson(resource.getInputStream(), Map.class, StandardCharsets.UTF_8);
        return map;
    }



    public static List<Geometry> ParseWKTToGeometry(List<String> wkts) {
        GeometryFactory geometryFactory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(geometryFactory);
        List<Geometry> geometryList = new ArrayList<>();
        for (String wkt:wkts){
            try{
                Geometry geometry = wktReader.read(wkt);
                geometryList.add(geometry);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return geometryList;
    }

    public static Double getLineLength(Geometry geometry){
        try {
//            CoordinateReferenceSystem decode = CRS.decode("EPSG:4490");
//            CoordinateReferenceSystem decode1 = CRS.decode("EPSG:10001");
//            MathTransform mathTransform = CRS.findMathTransform(decode, decode1, false);
//            Geometry transform = JTS.transform(geometry, mathTransform);
            return geometry.getLength();
        }catch (Exception e){
            return 0.0;
        }
    }



    public static double[] CoordConvert2D(double X1, double Y1, CoordConvertParam coordConvertParam, PlaneFourParam planeFourParam, boolean isForward) {
        double[] result = new double[2];
        double X2 = 0;
        double Y2 = 0;
        Ellipsoid ellipsoid1 = coordConvertParam.getEllipsoid1();
        Ellipsoid ellipsoid2 = coordConvertParam.getEllipsoid2();
        if (!isForward)
        {
            ellipsoid2 = coordConvertParam.getEllipsoid1();
            ellipsoid1 = coordConvertParam.getEllipsoid2();
        }

        //同一个大地坐标系
        if (coordConvertParam.getCSName1().equals(coordConvertParam.getCSName2()))
        {
            //都是经纬度，则不用转换
            if (coordConvertParam.getIsBLCoord1() && coordConvertParam.getIsBLCoord2())
            {
                throw new RuntimeException("不能转换!");
            }
            //都是投影坐标且中央经线相同，则不用转换
            else if (!coordConvertParam.getIsBLCoord1() && !coordConvertParam.getIsBLCoord2() && coordConvertParam.getL1() .equals(coordConvertParam.getL2()))
            {
                throw new RuntimeException("不能转换!");
            }
            else
            {
                double b1 = X1;
                double l1 = Y1;
                //如果第一个不是经纬度，则把坐标转换成经纬度
                if (!coordConvertParam.getIsBLCoord1())
                {
                    double[] array = XY2BL(ellipsoid1, coordConvertParam.getL1(), X1, Y1);
                    b1 = array[0];
                    l1 = array[1];
                }
                //如果第二个是经纬度，则结果就是刚才转换出来的经纬度。
                if (coordConvertParam.getIsBLCoord2())
                {
                    X2 = b1;
                    Y2 = l1;
                }
                //如果第二个不是经纬度，则把刚才转换出来的经纬度输入，转换成投影坐标
                else
                {
                    double[] array = BL2XY(ellipsoid2, coordConvertParam.getL2(), b1, l1);
                    X2 = array[0];
                    Y2 = array[1];
                }
            }
        }
        //如果不是同一个大地坐标系
        else
        {
            //如果平面四参数不为空
            if (planeFourParam != null)
            {
                double x1 = X1;
                double y1 = Y1;
                double L1 = planeFourParam.getL1();
                if (!isForward)
                {
                    L1 = planeFourParam.getL2();
                }
                //如果不是经纬度
                if (!coordConvertParam.getIsBLCoord1())
                {
                    //如果坐标的中央经度和平面四参数的中央经度不一致，则需要转换至和平面四参数一样的经度，再通过平面四参数进行转换
                    if (coordConvertParam.getL1() != L1)
                    {
                        double b1 = 0;
                        double l1 = 0;
                        double[] array = XY2BL(ellipsoid1, coordConvertParam.getL1(), X1, Y1);
                        b1 = array[0];
                        l1 = array[1];
                        array = BL2XY(ellipsoid1, L1, b1, l1);
                        x1 = array[0];
                        y1 = array[1];
                    }
                }
                else
                {
                    //将输入经纬度转换成xy值
                    double[] array = BL2XY(ellipsoid1, L1, X1, Y1);
                    x1 = array[0];
                    y1 = array[1];
                }
                double x2 = 0;
                double y2 = 0;
                //通过平面四参数进行转换
                if (isForward)
                {
                    double[] array = CoordConvert2D(planeFourParam, x1, y1);
                    x2 = array[0];
                    y2 = array[1];
                }
                else
                {
                    double[] array = ReverseCoordConvert2D(planeFourParam, x1, y1);
                    x2 = array[0];
                    y2 = array[1];
                }

                double L2 = planeFourParam.getL2();
                if (!isForward)
                {
                    L2 = planeFourParam.getL1();
                }
                //如果目标坐标系不是经纬度
                if (!coordConvertParam.getIsBLCoord2())
                {
                    if (coordConvertParam.getL2() == L2)
                    {
                        X2 = x2;
                        Y2 = y2;
                    }
                    else
                    {
                        double b2 = 0;
                        double l2 = 0;

                        double[] array = XY2BL(ellipsoid2, L2, x2, y2);
                        b2 = array[0];
                        l2 = array[1];
                        array = BL2XY(ellipsoid2, coordConvertParam.getL2(), b2, l2);
                        X2 = array[0];
                        Y2 = array[1];
                    }
                }
                else
                {
                    double[] array = XY2BL(ellipsoid2, L2, x2, y2);
                    X2 = array[0];
                    Y2 = array[1];
                }
            }
            else
            {
                throw new RuntimeException("没有找到转换参数!");
            }
        }
        result[0] = X2;
        result[1] = Y2;
        return result;
    }

    public static double[] XY2BL(Ellipsoid ellipsoid, double L0, double X, double Y){
        double[] result = new double[2];
        double B = 0.0;
        double L = 0.0;
        L0 = L0 / 180 * Math.PI;
        int ProjNo = 0;
        if (Y > 0)
        {
            ProjNo = (int)(Y / 1000000L); //查找带号
            if (ProjNo <= 10)
            {
                ProjNo = 0;
            }
        }
        double Y0 = ProjNo * 1000000L + 500000L;
        double X0 = 0;
        double xval = X - X0;
        double yval = Y - Y0; //带内大地坐标
        double e2 = 2 * ellipsoid.getFlatRate() - ellipsoid.getFlatRate() * ellipsoid.getFlatRate();
        double e1 = (1.0 - Math.sqrt(1 - e2)) / (1.0 + Math.sqrt(1 - e2));
        double ee = e2 / (1 - e2);
        double M = xval;
        double u = M / (ellipsoid.getSemiMajorAxis() * (1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256));
        double fai = u + (3 * e1 / 2 - 27 * e1 * e1 * e1 / 32) * Math.sin(2 * u) + (21 * e1 * e1 / 16 - 55 * e1 * e1 * e1 * e1 / 32) * Math.sin(4 * u)
                + (151 * e1 * e1 * e1 / 96) * Math.sin(6 * u) + (1097 * e1 * e1 * e1 * e1 / 512) * Math.sin(8 * u);
        double C = ee * Math.cos(fai) * Math.cos(fai);
        double T = Math.tan(fai) * Math.tan(fai);
        double NN = ellipsoid.getSemiMajorAxis() / Math.sqrt(1.0 - e2 * Math.sin(fai) * Math.sin(fai));
        double R = ellipsoid.getSemiMajorAxis() * (1 - e2) / Math.sqrt((1 - e2 * Math.sin(fai) * Math.sin(fai)) * (1 - e2 * Math.sin(fai) * Math.sin(fai)) * (1 - e2 * Math.sin(fai) * Math.sin(fai)));
        double D = yval / NN;

        L = L0 + (D - (1 + 2 * T + C) * D * D * D / 6 + (5 - 2 * C + 28 * T - 3 * C * C + 8 * ee + 24 * T * T) * D * D * D * D * D / 120) / Math.cos(fai);
        B = fai - (NN * Math.tan(fai) / R) * (D * D / 2 - (5 + 3 * T + 10 * C - 4 * C * C - 9 * ee) * D * D * D * D / 24 + (61 + 90 * T + 298 * C + 45 * T * T - 256 * ee - 3 * C * C) * D * D * D * D * D * D / 720);

        L = L * 180 / Math.PI;
        B = B * 180 / Math.PI;
        result[0] = B;
        result[1] = L;
        return result;
    }
    public static double[] BL2XY(Ellipsoid ellipsoid, double L0, double B, double L){
        double[] result = new double[2];
        double X = 0.0;
        double Y = 0.0;
        L0 = L0 / 180 * Math.PI;
        B = B / 180 * Math.PI;
        L = L / 180 * Math.PI;

        double e2 = 2 * ellipsoid.getFlatRate() - ellipsoid.getFlatRate() * ellipsoid.getFlatRate();
        double ee = e2 * (1.0 - e2);
        double NN = ellipsoid.getSemiMajorAxis() / Math.sqrt(1.0 - e2 * Math.sin(B) * Math.sin(B));
        double T = Math.tan(B) * Math.tan(B);
        double C = ee * Math.cos(B) * Math.cos(B);
        double A = (L - L0) * Math.cos(B);
        double M = ellipsoid.getSemiMajorAxis() * ((1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256) * B - (3 * e2 / 8 + 3 * e2 * e2 / 32 + 45 * e2 * e2 * e2 / 1024) * Math.sin(2 * B) + (15 * e2 * e2 / 256 + 45 * e2 * e2 * e2 / 1024) * Math.sin(4 * B) - (35 * e2 * e2 * e2 / 3072) * Math.sin(6 * B));
        X = M + NN * Math.tan(B) * (A * A / 2 + (5 - T + 9 * C + 4 * C * C) * A * A * A * A / 24 + (61 - 58 * T + T * T + 600 * C - 330 * ee) * A * A * A * A * A * A / 720);
        Y = NN * (A + (1 - T + C) * A * A * A / 6 + (5 - 18 * T + T * T + 72 * C - 58 * ee) * A * A * A * A * A / 120) + 500000L;
        result[0] = X;
        result[1] = Y;
        return result;
    }
    public static double[] CoordConvert2D(PlaneFourParam param, double X1, double Y1) {
        double[] result = new double[2];
        double X2 = 0;
        double Y2 = 0;
        //由于没有填过ABCD，所以会采用下面的
        double alfa = param.getAlfa() / 180.0 * Math.PI;
        X2 = param.getDX() + (1 + param.getK()) * (X1 * Math.cos(alfa) + Y1 * Math.sin(alfa));
        Y2 = param.getDY() + (1 + param.getK()) * (Y1 * Math.cos(alfa) - X1 * Math.sin(alfa));
        result[0] = X2;
        result[1] = Y2;
        return result;
    }
    public static double[] ReverseCoordConvert2D(PlaneFourParam param, double X1, double Y1) {
        double[] result = new double[2];
        double X2 = 0;
        double Y2 = 0;
        double alfa = param.getAlfa() / 180.0 * Math.PI;
        X2 = ((X1 - param.getDX()) * Math.cos(alfa) - (Y1 - param.getDY()) * Math.sin(alfa)) / (1 + param.getK());
        Y2 = ((X1 - param.getDX()) * Math.sin(alfa) + (Y1 - param.getDY()) * Math.cos(alfa)) / (1 + param.getK());
        result[0] = X2;
        result[1] = Y2;
        return result;
    }

    public static Map<String,Object> getXY(String wkt){
        HashMap<String, Object> map = new HashMap<>();
        if(wkt.startsWith("L")){
            try{
                Geometry read = new WKTReader().read(wkt);
                List<Coordinate> coordinates = Arrays.asList(read.getCoordinates());

                for(int i = 0; i < coordinates.size(); i++){
                    if(i ==0) {
                        Coordinate coordinate = coordinates.get(i);
                        double x = coordinate.getX();
                        double y = coordinate.getY();
                        map.put("x0",x);
                        map.put("y0",y);
                    }
                    if(i ==1) {
                        Coordinate coordinate = coordinates.get(i);
                        double x = coordinate.getX();
                        double y = coordinate.getY();
                        map.put("x1",x);
                        map.put("y1",y);
                    }
                }
            }catch (Exception e){

            }
        }
        if(wkt.startsWith("P")){
            try{
                Geometry read = new WKTReader().read(wkt);
                List<Coordinate> coordinates = Arrays.asList(read.getCoordinates());

                for(int i = 0; i < coordinates.size(); i++){

                    Coordinate coordinate = coordinates.get(i);
                    double x = coordinate.getX();
                    double y = coordinate.getY();
                    map.put("x",x);
                    map.put("y",y);


                }
            }
            catch (Exception e){
            }
        }
        return map;
    }

    /**
     * 单个几何对象的坐标转换 - 便捷方法
     * @param wkt 几何对象的WKT字符串
     * @param sourceCoordSystem 源坐标系
     * @param targetCoordSystem 目标坐标系
     * @return 转换后的WKT字符串
     */
    public static String convertSingleGeometry(String wkt, String sourceCoordSystem, String targetCoordSystem) {
        try {
            ZbzhDTO dto = new ZbzhDTO();
            dto.setWkts(Arrays.asList(wkt));
            dto.setSource(sourceCoordSystem);
            dto.setTarget(targetCoordSystem);
            dto.setSurveyCoord(false); // 默认为非测绘坐标

            List<String> result = CoordConvert2D(dto);
            if (result != null && !result.isEmpty()) {
                return result.get(0);
            }
        } catch (Exception e) {
            log.error("坐标转换失败: " + wkt, e);
        }
        return wkt; // 转换失败时返回原始WKT
    }

    public static String createMutityPolygon(List<String> polygons) {

        // 创建一个WKTReader对象
        WKTReader reader = new WKTReader();

        try {
            // 读取两个多边形的WKT表示
            Polygon[] polygons1 = new Polygon[polygons.size()];

            for(int i = 0; i< polygons.size(); i++){
                Geometry polygon2 = reader.read(polygons.get(i));
                polygons1[i] = (Polygon)polygon2;

            }
            // 创建一个MultiPolygon对象，包含两个多边形
            MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(polygons1);

            // 输出MultiPolygon的WKT表示
            String s = multiPolygon.toText();

            return s;
        } catch ( org.locationtech.jts.io.ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


}
