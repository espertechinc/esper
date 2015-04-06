package com.espertech.esperio.db;

public class SupportBean {
    private String stringProp;
    private int intProp;

    public SupportBean(String stringProp, int intProp) {
        this.stringProp = stringProp;
        this.intProp = intProp;
    }

    public String getStringProp() {
        return stringProp;
    }

    public int getIntProp() {
        return intProp;
    }
}
