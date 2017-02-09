/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.supportunit.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

public class SupportBeanNumeric implements Serializable {
    private Integer intOne;
    private Integer intTwo;
    private BigInteger bigint;
    private BigDecimal bigdec;
    private BigDecimal bigdecTwo;
    private double doubleOne;
    private double doubleTwo;
    private float floatOne;
    private float floatTwo;

    public SupportBeanNumeric(Integer intOne, Integer intTwo, BigInteger bigint, BigDecimal bigdec, double doubleOne, double doubleTwo) {
        this.intOne = intOne;
        this.intTwo = intTwo;
        this.bigint = bigint;
        this.bigdec = bigdec;
        this.doubleOne = doubleOne;
        this.doubleTwo = doubleTwo;
    }

    public SupportBeanNumeric(Integer intOne, Integer intTwo) {
        this.intOne = intOne;
        this.intTwo = intTwo;
    }

    public SupportBeanNumeric(BigInteger bigint, BigDecimal bigdec) {
        this.bigint = bigint;
        this.bigdec = bigdec;
    }

    public SupportBeanNumeric(boolean floatDummy, float floatOne, float floatTwo) {
        this.floatOne = floatOne;
        this.floatTwo = floatTwo;
    }

    public Integer getIntOne() {
        return intOne;
    }

    public Integer getIntTwo() {
        return intTwo;
    }

    public BigInteger getBigint() {
        return bigint;
    }

    public BigDecimal getBigdec() {
        return bigdec;
    }

    public double getDoubleOne() {
        return doubleOne;
    }

    public double getDoubleTwo() {
        return doubleTwo;
    }

    public void setIntOne(Integer intOne) {
        this.intOne = intOne;
    }

    public void setIntTwo(Integer intTwo) {
        this.intTwo = intTwo;
    }

    public void setBigint(BigInteger bigint) {
        this.bigint = bigint;
    }

    public void setBigdec(BigDecimal bigdec) {
        this.bigdec = bigdec;
    }

    public void setDoubleOne(double doubleOne) {
        this.doubleOne = doubleOne;
    }

    public void setDoubleTwo(double doubleTwo) {
        this.doubleTwo = doubleTwo;
    }

    public BigDecimal getBigdecTwo() {
        return bigdecTwo;
    }

    public void setBigdecTwo(BigDecimal bigdecTwo) {
        this.bigdecTwo = bigdecTwo;
    }

    public float getFloatOne() {
        return floatOne;
    }

    public void setFloatOne(float floatOne) {
        this.floatOne = floatOne;
    }

    public float getFloatTwo() {
        return floatTwo;
    }

    public void setFloatTwo(float floatTwo) {
        this.floatTwo = floatTwo;
    }
}
