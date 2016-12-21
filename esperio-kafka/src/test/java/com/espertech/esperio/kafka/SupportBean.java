package com.espertech.esperio.kafka;

import java.io.Serializable;

public class SupportBean implements Serializable {

    private static final long serialVersionUID = 8446347792055857663L;

    private String stringProp;
    private int intProp;

    public SupportBean() {
    }

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

    public void setStringProp(String stringProp) {
        this.stringProp = stringProp;
    }

    public void setIntProp(int intProp) {
        this.intProp = intProp;
    }
}
