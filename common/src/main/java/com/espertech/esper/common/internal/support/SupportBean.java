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
package com.espertech.esper.common.internal.support;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.*;

public class SupportBean implements Serializable {
    private static final long serialVersionUID = -5497659066725918444L;
    private String theString;

    private boolean boolPrimitive;
    private int intPrimitive;
    private long longPrimitive;
    private char charPrimitive;
    private short shortPrimitive;
    private byte bytePrimitive;
    private float floatPrimitive;
    private double doublePrimitive;

    private Boolean boolBoxed;
    private Integer intBoxed;
    private Long longBoxed;
    private Character charBoxed;
    private Short shortBoxed;
    private Byte byteBoxed;
    private Float floatBoxed;
    private Double doubleBoxed;
    private BigDecimal bigDecimal;
    private BigInteger bigInteger;

    private SupportEnum enumValue;

    public SupportBean() {
    }

    public SupportBean(String theString, int intPrimitive) {
        this.theString = theString;
        this.intPrimitive = intPrimitive;
    }

    public String getTheString() {
        return theString;
    }

    public boolean isBoolPrimitive() {
        return boolPrimitive;
    }

    public int getIntPrimitive() {
        return intPrimitive;
    }

    public long getLongPrimitive() {
        return longPrimitive;
    }

    public char getCharPrimitive() {
        return charPrimitive;
    }

    public short getShortPrimitive() {
        return shortPrimitive;
    }

    public byte getBytePrimitive() {
        return bytePrimitive;
    }

    public float getFloatPrimitive() {
        return floatPrimitive;
    }

    public double getDoublePrimitive() {
        return doublePrimitive;
    }

    public Boolean getBoolBoxed() {
        return boolBoxed;
    }

    public Integer getIntBoxed() {
        return intBoxed;
    }

    public Long getLongBoxed() {
        return longBoxed;
    }

    public Character getCharBoxed() {
        return charBoxed;
    }

    public Short getShortBoxed() {
        return shortBoxed;
    }

    public Byte getByteBoxed() {
        return byteBoxed;
    }

    public Float getFloatBoxed() {
        return floatBoxed;
    }

    public Double getDoubleBoxed() {
        return doubleBoxed;
    }

    public void setTheString(String theString) {
        this.theString = theString;
    }

    public void setBoolPrimitive(boolean boolPrimitive) {
        this.boolPrimitive = boolPrimitive;
    }

    public void setIntPrimitive(int intPrimitive) {
        this.intPrimitive = intPrimitive;
    }

    public void setLongPrimitive(long longPrimitive) {
        this.longPrimitive = longPrimitive;
    }

    public void setCharPrimitive(char charPrimitive) {
        this.charPrimitive = charPrimitive;
    }

    public void setShortPrimitive(short shortPrimitive) {
        this.shortPrimitive = shortPrimitive;
    }

    public void setBytePrimitive(byte bytePrimitive) {
        this.bytePrimitive = bytePrimitive;
    }

    public void setFloatPrimitive(float floatPrimitive) {
        this.floatPrimitive = floatPrimitive;
    }

    public void setDoublePrimitive(double doublePrimitive) {
        this.doublePrimitive = doublePrimitive;
    }

    public void setBoolBoxed(Boolean boolBoxed) {
        this.boolBoxed = boolBoxed;
    }

    public void setIntBoxed(Integer intBoxed) {
        this.intBoxed = intBoxed;
    }

    public void setLongBoxed(Long longBoxed) {
        this.longBoxed = longBoxed;
    }

    public void setCharBoxed(Character charBoxed) {
        this.charBoxed = charBoxed;
    }

    public void setShortBoxed(Short shortBoxed) {
        this.shortBoxed = shortBoxed;
    }

    public void setByteBoxed(Byte byteBoxed) {
        this.byteBoxed = byteBoxed;
    }

    public void setFloatBoxed(Float floatBoxed) {
        this.floatBoxed = floatBoxed;
    }

    public void setDoubleBoxed(Double doubleBoxed) {
        this.doubleBoxed = doubleBoxed;
    }

    public SupportEnum getEnumValue() {
        return enumValue;
    }

    public void setEnumValue(SupportEnum enumValue) {
        this.enumValue = enumValue;
    }

    public String toString() {
        return this.getClass().getSimpleName() + "(" + theString + ", " + intPrimitive + ")";
    }

    public BigDecimal getBigDecimal() {
        return bigDecimal;
    }

    public void setBigDecimal(BigDecimal bigDecimal) {
        this.bigDecimal = bigDecimal;
    }

    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    public static SupportBean[] getBeansPerIndex(SupportBean[] beans, int[] indexes) {
        if (indexes == null) {
            return null;
        }
        SupportBean[] array = new SupportBean[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            array[i] = beans[indexes[i]];
        }
        return array;
    }

    public static Object[] getOAStringAndIntPerIndex(SupportBean[] beans, int[] indexes) {
        SupportBean[] arr = getBeansPerIndex(beans, indexes);
        if (arr == null) {
            return null;
        }
        return toOAStringAndInt(arr);
    }

    private static Object[] toOAStringAndInt(SupportBean[] arr) {
        Object[][] values = new Object[arr.length][];
        for (int i = 0; i < values.length; i++) {
            values[i] = new Object[]{arr[i].getTheString(), arr[i].getIntPrimitive()};
        }
        return values;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SupportBean that = (SupportBean) o;

        if (boolPrimitive != that.boolPrimitive) return false;
        if (intPrimitive != that.intPrimitive) return false;
        if (longPrimitive != that.longPrimitive) return false;
        if (charPrimitive != that.charPrimitive) return false;
        if (shortPrimitive != that.shortPrimitive) return false;
        if (bytePrimitive != that.bytePrimitive) return false;
        if (Float.compare(that.floatPrimitive, floatPrimitive) != 0) return false;
        if (Double.compare(that.doublePrimitive, doublePrimitive) != 0) return false;
        if (theString != null ? !theString.equals(that.theString) : that.theString != null) return false;
        if (boolBoxed != null ? !boolBoxed.equals(that.boolBoxed) : that.boolBoxed != null) return false;
        if (intBoxed != null ? !intBoxed.equals(that.intBoxed) : that.intBoxed != null) return false;
        if (longBoxed != null ? !longBoxed.equals(that.longBoxed) : that.longBoxed != null) return false;
        if (charBoxed != null ? !charBoxed.equals(that.charBoxed) : that.charBoxed != null) return false;
        if (shortBoxed != null ? !shortBoxed.equals(that.shortBoxed) : that.shortBoxed != null) return false;
        if (byteBoxed != null ? !byteBoxed.equals(that.byteBoxed) : that.byteBoxed != null) return false;
        if (floatBoxed != null ? !floatBoxed.equals(that.floatBoxed) : that.floatBoxed != null) return false;
        if (doubleBoxed != null ? !doubleBoxed.equals(that.doubleBoxed) : that.doubleBoxed != null) return false;
        if (bigDecimal != null ? !bigDecimal.equals(that.bigDecimal) : that.bigDecimal != null) return false;
        if (bigInteger != null ? !bigInteger.equals(that.bigInteger) : that.bigInteger != null) return false;
        return enumValue == that.enumValue;
    }

    public int hashCode() {
        int result;
        long temp;
        result = theString != null ? theString.hashCode() : 0;
        result = 31 * result + (boolPrimitive ? 1 : 0);
        result = 31 * result + intPrimitive;
        result = 31 * result + (int) (longPrimitive ^ (longPrimitive >>> 32));
        result = 31 * result + (int) charPrimitive;
        result = 31 * result + (int) shortPrimitive;
        result = 31 * result + (int) bytePrimitive;
        result = 31 * result + (floatPrimitive != +0.0f ? Float.floatToIntBits(floatPrimitive) : 0);
        temp = Double.doubleToLongBits(doublePrimitive);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (boolBoxed != null ? boolBoxed.hashCode() : 0);
        result = 31 * result + (intBoxed != null ? intBoxed.hashCode() : 0);
        result = 31 * result + (longBoxed != null ? longBoxed.hashCode() : 0);
        result = 31 * result + (charBoxed != null ? charBoxed.hashCode() : 0);
        result = 31 * result + (shortBoxed != null ? shortBoxed.hashCode() : 0);
        result = 31 * result + (byteBoxed != null ? byteBoxed.hashCode() : 0);
        result = 31 * result + (floatBoxed != null ? floatBoxed.hashCode() : 0);
        result = 31 * result + (doubleBoxed != null ? doubleBoxed.hashCode() : 0);
        result = 31 * result + (bigDecimal != null ? bigDecimal.hashCode() : 0);
        result = 31 * result + (bigInteger != null ? bigInteger.hashCode() : 0);
        result = 31 * result + (enumValue != null ? enumValue.hashCode() : 0);
        return result;
    }

    public static SupportBean makeBean(String string, int intPrimitive, long longPrimitive) {
        return makeBean(string, intPrimitive, longPrimitive, 0);
    }

    public static SupportBean makeBean(String string, int intPrimitive, long longPrimitive, double doublePrimitive) {
        return makeBean(string, intPrimitive, longPrimitive, doublePrimitive, false);
    }

    public static SupportBean makeBean(String string, int intPrimitive, long longPrimitive, double doublePrimitive, boolean boolPrimitive) {
        SupportBean event = new SupportBean(string, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        event.setDoublePrimitive(doublePrimitive);
        event.setBoolPrimitive(boolPrimitive);
        return event;
    }

    public static SupportBean makeBean(String string) {
        return new SupportBean(string, -1);
    }

    public static SupportBean makeBean(String string, int intPrimitive) {
        return new SupportBean(string, intPrimitive);
    }

    public static SupportBean makeBeanWBoxed(String string, int intPrimitive, Double doubleBoxed, Long longBoxed) {
        SupportBean bean = new SupportBean(string, intPrimitive);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        return bean;
    }

    public static void compare(Object[] others, String[] split, Object[][] objects) {
        assertEquals(others.length, objects.length);
        for (int i = 0; i < others.length; i++) {
            compare((SupportBean) others[i], split, objects[i]);
        }
    }

    public static void compare(Object other, String theString, int intPrimitive) {
        SupportBean that = (SupportBean) other;
        assertEquals(that.getTheString(), theString);
        assertEquals(that.getIntPrimitive(), intPrimitive);
    }

    public static void compare(SupportBean received, String[] split, Object[] objects) {
        assertEquals(split.length, objects.length);
        for (int i = 0; i < split.length; i++) {
            compare(received, split[i], objects[i]);
        }
    }

    public static void compare(SupportBean received, String property, Object expected) {
        switch (property) {
            case "intPrimitive":
                assertEquals(expected, received.getIntPrimitive());
                break;
            case "intBoxed":
                assertEquals(expected, received.getIntBoxed());
                break;
            case "boolPrimitive":
                assertEquals(expected, received.isBoolPrimitive());
                break;
            case "boolBoxed":
                assertEquals(expected, received.getBoolBoxed());
                break;
            case "shortPrimitive":
                assertEquals(expected, received.getShortPrimitive());
                break;
            case "shortBoxed":
                assertEquals(expected, received.getShortBoxed());
                break;
            case "longPrimitive":
                assertEquals(expected, received.getLongPrimitive());
                break;
            case "longBoxed":
                assertEquals(expected, received.getLongBoxed());
                break;
            case "charPrimitive":
                assertEquals(expected, received.getCharPrimitive());
                break;
            case "charBoxed":
                assertEquals(expected, received.getCharBoxed());
                break;
            case "bytePrimitive":
                assertEquals(expected, received.getBytePrimitive());
                break;
            case "byteBoxed":
                assertEquals(expected, received.getByteBoxed());
                break;
            case "floatPrimitive":
                assertEquals(expected, received.getFloatPrimitive());
                break;
            case "floatBoxed":
                assertEquals(expected, received.getFloatBoxed());
                break;
            case "doublePrimitive":
                assertEquals(expected, received.getDoublePrimitive());
                break;
            case "doubleBoxed":
                assertEquals(expected, received.getDoubleBoxed());
                break;
            case "enumValue":
                assertEquals(expected, received.getEnumValue());
                break;
            case "theString":
                assertEquals(expected, received.getTheString());
                break;
            default:
                fail("Assertion not found for '" + property + "'");
        }
    }
}
