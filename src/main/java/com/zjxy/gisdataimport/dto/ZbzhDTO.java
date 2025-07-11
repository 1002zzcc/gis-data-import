package com.zjxy.gisdataimport.dto;

import lombok.Data;
import java.util.List;

@Data
public class ZbzhDTO {
    private List<String> wkts;
    private String source;
    private String target;
    private boolean surveyCoord;
    private Double L1;
    private Double L2;
}
