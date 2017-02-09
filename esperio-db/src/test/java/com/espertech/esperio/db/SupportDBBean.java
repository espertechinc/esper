package com.espertech.esperio.db;

public class SupportDBBean {
    private String key1;
    private Integer key2;
    private String value1;
    private Double value2;

    public SupportDBBean(String key1, Integer key2, String value1, Double value2) {
        this.key1 = key1;
        this.key2 = key2;
        this.value1 = value1;
        this.value2 = value2;
    }

    public SupportDBBean() {
    }

    public String getKey1() {
        return key1;
    }

    public Integer getKey2() {
        return key2;
    }

    public String getValue1() {
        return value1;
    }

    public Double getValue2() {
        return value2;
    }

    public void setKey1(String key1) {
        this.key1 = key1;
    }

    public void setKey2(Integer key2) {
        this.key2 = key2;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public void setValue2(Double value2) {
        this.value2 = value2;
    }
}
