package com.zjxy.gisdataimport.coordsystem;

import lombok.Data;

@Data
public class CoordConvertParam {
    private String CSName1;
    private Ellipsoid ellipsoid1;
    private Boolean isBLCoord1;
    private Double L1;
    private String CSName2;
    private Ellipsoid ellipsoid2;
    private Boolean isBLCoord2;
    private Double L2;
}
