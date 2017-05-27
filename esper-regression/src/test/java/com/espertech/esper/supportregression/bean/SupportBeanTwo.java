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
package com.espertech.esper.supportregression.bean;

import java.io.Serializable;

public class SupportBeanTwo implements Serializable {
    private String stringTwo;

    private boolean boolPrimitiveTwo;
    private int intPrimitiveTwo;
    private long longPrimitiveTwo;
    private char charPrimitiveTwo;
    private short shortPrimitiveTwo;
    private byte bytePrimitiveTwo;
    private float floatPrimitiveTwo;
    private double doublePrimitiveTwo;

    private Boolean boolBoxedTwo;
    private Integer intBoxedTwo;
    private Long longBoxedTwo;
    private Character charBoxedTwo;
    private Short shortBoxedTwo;
    private Byte byteBoxedTwo;
    private Float floatBoxedTwo;
    private Double doubleBoxedTwo;

    private SupportEnum enumValueTwo;

    public SupportBeanTwo() {
    }

    public SupportBeanTwo(String stringTwo, int intPrimitiveTwo) {
        this.stringTwo = stringTwo;
        this.intPrimitiveTwo = intPrimitiveTwo;
    }

    public String getStringTwo() {
        return stringTwo;
    }

    public void setStringTwo(String stringTwo) {
        this.stringTwo = stringTwo;
    }

    public boolean isBoolPrimitiveTwo() {
        return boolPrimitiveTwo;
    }

    public void setBoolPrimitiveTwo(boolean boolPrimitiveTwo) {
        this.boolPrimitiveTwo = boolPrimitiveTwo;
    }

    public int getIntPrimitiveTwo() {
        return intPrimitiveTwo;
    }

    public void setIntPrimitiveTwo(int intPrimitiveTwo) {
        this.intPrimitiveTwo = intPrimitiveTwo;
    }

    public long getLongPrimitiveTwo() {
        return longPrimitiveTwo;
    }

    public void setLongPrimitiveTwo(long longPrimitiveTwo) {
        this.longPrimitiveTwo = longPrimitiveTwo;
    }

    public char getCharPrimitiveTwo() {
        return charPrimitiveTwo;
    }

    public void setCharPrimitiveTwo(char charPrimitiveTwo) {
        this.charPrimitiveTwo = charPrimitiveTwo;
    }

    public short getShortPrimitiveTwo() {
        return shortPrimitiveTwo;
    }

    public void setShortPrimitiveTwo(short shortPrimitiveTwo) {
        this.shortPrimitiveTwo = shortPrimitiveTwo;
    }

    public byte getBytePrimitiveTwo() {
        return bytePrimitiveTwo;
    }

    public void setBytePrimitiveTwo(byte bytePrimitiveTwo) {
        this.bytePrimitiveTwo = bytePrimitiveTwo;
    }

    public float getFloatPrimitiveTwo() {
        return floatPrimitiveTwo;
    }

    public void setFloatPrimitiveTwo(float floatPrimitiveTwo) {
        this.floatPrimitiveTwo = floatPrimitiveTwo;
    }

    public double getDoublePrimitiveTwo() {
        return doublePrimitiveTwo;
    }

    public void setDoublePrimitiveTwo(double doublePrimitiveTwo) {
        this.doublePrimitiveTwo = doublePrimitiveTwo;
    }

    public Boolean getBoolBoxedTwo() {
        return boolBoxedTwo;
    }

    public void setBoolBoxedTwo(Boolean boolBoxedTwo) {
        this.boolBoxedTwo = boolBoxedTwo;
    }

    public Integer getIntBoxedTwo() {
        return intBoxedTwo;
    }

    public void setIntBoxedTwo(Integer intBoxedTwo) {
        this.intBoxedTwo = intBoxedTwo;
    }

    public Long getLongBoxedTwo() {
        return longBoxedTwo;
    }

    public void setLongBoxedTwo(Long longBoxedTwo) {
        this.longBoxedTwo = longBoxedTwo;
    }

    public Character getCharBoxedTwo() {
        return charBoxedTwo;
    }

    public void setCharBoxedTwo(Character charBoxedTwo) {
        this.charBoxedTwo = charBoxedTwo;
    }

    public Short getShortBoxedTwo() {
        return shortBoxedTwo;
    }

    public void setShortBoxedTwo(Short shortBoxedTwo) {
        this.shortBoxedTwo = shortBoxedTwo;
    }

    public Byte getByteBoxedTwo() {
        return byteBoxedTwo;
    }

    public void setByteBoxedTwo(Byte byteBoxedTwo) {
        this.byteBoxedTwo = byteBoxedTwo;
    }

    public Float getFloatBoxedTwo() {
        return floatBoxedTwo;
    }

    public void setFloatBoxedTwo(Float floatBoxedTwo) {
        this.floatBoxedTwo = floatBoxedTwo;
    }

    public Double getDoubleBoxedTwo() {
        return doubleBoxedTwo;
    }

    public void setDoubleBoxedTwo(Double doubleBoxedTwo) {
        this.doubleBoxedTwo = doubleBoxedTwo;
    }

    public SupportEnum getEnumValueTwo() {
        return enumValueTwo;
    }

    public void setEnumValueTwo(SupportEnum enumValueTwo) {
        this.enumValueTwo = enumValueTwo;
    }

    public String toString() {
        return this.getClass().getSimpleName() + "(" + stringTwo + ", " + intPrimitiveTwo + ")";
    }
}
