package com.zjxy.gisdataimport.coordsystem;

import java.util.Objects;

public class Ellipsoid {
    private String m_Name;
    private EllipsoidType m_Type;
    private double m_SemiMajorAxis;
    private double m_SemiMinorAxis;
    private double m_FlatRate;
    
    public Ellipsoid(){
        m_Name = "";
        m_Type = EllipsoidType.Userdefined;
        m_SemiMajorAxis = 0;
        m_SemiMinorAxis = 0;
        m_FlatRate = 0;
    }
    
    public Ellipsoid(String type){
        if(type.equals("Beijing1954")){
            setSemiMajorAxis(6378245);
            setFlatRate(0.0033523299);
            setSemiMinorAxis(6356863.018576974);
            m_Type = EllipsoidType.Beijing1954;
            m_Name = type;
        }else if(type.equals("Xian1980")){
            setSemiMajorAxis(6378140);
            setFlatRate(0.003352813);
            setSemiMinorAxis(6356755.28929218);
            m_Type = EllipsoidType.Xian1980;
            m_Name = type;
        }else if(type.equals("WGS1984")){
            setSemiMajorAxis(6378137);
            setFlatRate(0.0033528107);
            setSemiMajorAxis(6356752.3140203338);
            m_Type = EllipsoidType.WGS1984;
            m_Name = type;
        }else if(type.equals("CGCS2000")){
            setSemiMajorAxis(6378137);
            setFlatRate(0.00335281068118232);
            setSemiMinorAxis(6356752.3141403561);
            m_Type = EllipsoidType.CGCS2000;
            m_Name = type;
        }
    }
    
    public String getName(){
        return m_Name;
    }
    public void setName(String name){
        this.m_Name = name;
    }
    public EllipsoidType getType(){
        return m_Type;
    }
    public void setType(EllipsoidType type){
        this.m_Type = type;
    }
    public double getSemiMajorAxis(){
        return m_SemiMajorAxis;
    }
    public void setSemiMajorAxis(double semiMajorAxis){
        m_SemiMajorAxis = semiMajorAxis;
        if (m_SemiMinorAxis > 0)
        {
            m_FlatRate = (m_SemiMajorAxis - m_SemiMinorAxis) / m_SemiMajorAxis;
        }
        else
        {
            if (m_FlatRate > 0)
            {
                m_SemiMinorAxis = m_SemiMajorAxis * (1 - m_FlatRate);
            }
        }
    }
    public double getSemiMinorAxis(){
        return m_SemiMinorAxis;
    }
    public void setSemiMinorAxis(double semiMinorAxis){
        m_SemiMinorAxis = semiMinorAxis;
        if (m_SemiMajorAxis > 0)
        {
            m_FlatRate = (m_SemiMajorAxis - m_SemiMinorAxis) / m_SemiMajorAxis;
        }
    }
    public double getFlatRate(){
        return m_FlatRate;
    }
    public void setFlatRate(double flatRate){
        m_FlatRate = flatRate;
        if (m_SemiMajorAxis > 0)
        {
            m_SemiMinorAxis = m_SemiMajorAxis * (1 - m_FlatRate);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ellipsoid ellipsoid = (Ellipsoid) o;
        return Double.compare(ellipsoid.m_SemiMajorAxis, m_SemiMajorAxis) == 0 &&
                Double.compare(ellipsoid.m_SemiMinorAxis, m_SemiMinorAxis) == 0 &&
                Double.compare(ellipsoid.m_FlatRate, m_FlatRate) == 0 &&
                Objects.equals(m_Name, ellipsoid.m_Name) &&
                m_Type == ellipsoid.m_Type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_Name, m_Type, m_SemiMajorAxis, m_SemiMinorAxis, m_FlatRate);
    }
}
