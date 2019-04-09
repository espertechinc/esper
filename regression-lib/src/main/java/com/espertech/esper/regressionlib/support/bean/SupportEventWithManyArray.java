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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;
import java.util.Collection;

public final class SupportEventWithManyArray implements Serializable {
    private String id;
    private int[] intOne;
    private int[] intTwo;
    private Integer[] intBoxedOne;
    private Integer[] intBoxedTwo;
    private int[][] int2DimOne;
    private int[][] int2DimTwo;
    private Object[] objectOne;
    private Object[] objectTwo;
    private boolean[] booleanOne;
    private boolean[] booleanTwo;
    private short[] shortOne;
    private short[] shortTwo;
    private float[] floatOne;
    private float[] floatTwo;
    private double[] doubleOne;
    private double[] doubleTwo;
    private char[] charOne;
    private char[] charTwo;
    private byte[] byteOne;
    private byte[] byteTwo;
    private long[] longOne;
    private long[] longTwo;
    private String[] stringOne;
    private String[] stringTwo;
    private int value;
    private Collection<int[]> intArrayCollection;

    public SupportEventWithManyArray() {
    }

    public SupportEventWithManyArray(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public int[] getIntOne() {
        return intOne;
    }

    public SupportEventWithManyArray withIntOne(int[] intOne) {
        this.intOne = intOne;
        return this;
    }

    public int[] getIntTwo() {
        return intTwo;
    }

    public SupportEventWithManyArray withIntTwo(int[] intTwo) {
        this.intTwo = intTwo;
        return this;
    }

    public int getValue() {
        return value;
    }

    public SupportEventWithManyArray withValue(int value) {
        this.value = value;
        return this;
    }

    public Integer[] getIntBoxedOne() {
        return intBoxedOne;
    }

    public SupportEventWithManyArray withIntBoxedOne(Integer[] intBoxedOne) {
        this.intBoxedOne = intBoxedOne;
        return this;
    }

    public Integer[] getIntBoxedTwo() {
        return intBoxedTwo;
    }

    public SupportEventWithManyArray withIntBoxedTwo(Integer[] intBoxedTwo) {
        this.intBoxedTwo = intBoxedTwo;
        return this;
    }

    public int[][] getInt2DimOne() {
        return int2DimOne;
    }

    public SupportEventWithManyArray withInt2DimOne(int[][] int2DimOne) {
        this.int2DimOne = int2DimOne;
        return this;
    }

    public int[][] getInt2DimTwo() {
        return int2DimTwo;
    }

    public SupportEventWithManyArray withInt2DimTwo(int[][] int2DimTwo) {
        this.int2DimTwo = int2DimTwo;
        return this;
    }

    public Object[] getObjectOne() {
        return objectOne;
    }

    public SupportEventWithManyArray withObjectOne(Object[] objectOne) {
        this.objectOne = objectOne;
        return this;
    }

    public Object[] getObjectTwo() {
        return objectTwo;
    }

    public SupportEventWithManyArray withObjectTwo(Object[] objectTwo) {
        this.objectTwo = objectTwo;
        return this;
    }

    public boolean[] getBooleanOne() {
        return booleanOne;
    }

    public SupportEventWithManyArray withBooleanOne(boolean[] booleanOne) {
        this.booleanOne = booleanOne;
        return this;
    }

    public boolean[] getBooleanTwo() {
        return booleanTwo;
    }

    public SupportEventWithManyArray withBooleanTwo(boolean[] booleanTwo) {
        this.booleanTwo = booleanTwo;
        return this;
    }

    public short[] getShortOne() {
        return shortOne;
    }

    public SupportEventWithManyArray withShortOne(short[] shortOne) {
        this.shortOne = shortOne;
        return this;
    }

    public short[] getShortTwo() {
        return shortTwo;
    }

    public SupportEventWithManyArray withShortTwo(short[] shortTwo) {
        this.shortTwo = shortTwo;
        return this;
    }

    public float[] getFloatOne() {
        return floatOne;
    }

    public SupportEventWithManyArray withFloatOne(float[] floatOne) {
        this.floatOne = floatOne;
        return this;
    }

    public float[] getFloatTwo() {
        return floatTwo;
    }

    public SupportEventWithManyArray WithFloatTwo(float[] floatTwo) {
        this.floatTwo = floatTwo;
        return this;
    }

    public double[] getDoubleOne() {
        return doubleOne;
    }

    public SupportEventWithManyArray withDoubleOne(double[] doubleOne) {
        this.doubleOne = doubleOne;
        return this;
    }

    public double[] getDoubleTwo() {
        return doubleTwo;
    }

    public SupportEventWithManyArray withDoubleTwo(double[] doubleTwo) {
        this.doubleTwo = doubleTwo;
        return this;
    }

    public char[] getCharOne() {
        return charOne;
    }

    public SupportEventWithManyArray withCharOne(char[] charOne) {
        this.charOne = charOne;
        return this;
    }

    public char[] getCharTwo() {
        return charTwo;
    }

    public SupportEventWithManyArray withCharTwo(char[] charTwo) {
        this.charTwo = charTwo;
        return this;
    }

    public byte[] getByteOne() {
        return byteOne;
    }

    public SupportEventWithManyArray withByteOne(byte[] byteOne) {
        this.byteOne = byteOne;
        return this;
    }

    public byte[] getByteTwo() {
        return byteTwo;
    }

    public SupportEventWithManyArray withByteTwo(byte[] byteTwo) {
        this.byteTwo = byteTwo;
        return this;
    }

    public long[] getLongOne() {
        return longOne;
    }

    public SupportEventWithManyArray withLongOne(long[] longOne) {
        this.longOne = longOne;
        return this;
    }

    public long[] getLongTwo() {
        return longTwo;
    }

    public SupportEventWithManyArray withLongTwo(long[] longTwo) {
        this.longTwo = longTwo;
        return this;
    }

    public void setIntOne(int[] intOne) {
        this.intOne = intOne;
    }

    public void setIntTwo(int[] intTwo) {
        this.intTwo = intTwo;
    }

    public void setIntBoxedOne(Integer[] intBoxedOne) {
        this.intBoxedOne = intBoxedOne;
    }

    public void setIntBoxedTwo(Integer[] intBoxedTwo) {
        this.intBoxedTwo = intBoxedTwo;
    }

    public void setInt2DimOne(int[][] int2DimOne) {
        this.int2DimOne = int2DimOne;
    }

    public void setInt2DimTwo(int[][] int2DimTwo) {
        this.int2DimTwo = int2DimTwo;
    }

    public void setObjectOne(Object[] objectOne) {
        this.objectOne = objectOne;
    }

    public void setObjectTwo(Object[] objectTwo) {
        this.objectTwo = objectTwo;
    }

    public void setBooleanOne(boolean[] booleanOne) {
        this.booleanOne = booleanOne;
    }

    public void setBooleanTwo(boolean[] booleanTwo) {
        this.booleanTwo = booleanTwo;
    }

    public void setShortOne(short[] shortOne) {
        this.shortOne = shortOne;
    }

    public void setShortTwo(short[] shortTwo) {
        this.shortTwo = shortTwo;
    }

    public void setFloatOne(float[] floatOne) {
        this.floatOne = floatOne;
    }

    public void setFloatTwo(float[] floatTwo) {
        this.floatTwo = floatTwo;
    }

    public void setDoubleOne(double[] doubleOne) {
        this.doubleOne = doubleOne;
    }

    public void setDoubleTwo(double[] doubleTwo) {
        this.doubleTwo = doubleTwo;
    }

    public void setCharOne(char[] charOne) {
        this.charOne = charOne;
    }

    public void setCharTwo(char[] charTwo) {
        this.charTwo = charTwo;
    }

    public void setByteOne(byte[] byteOne) {
        this.byteOne = byteOne;
    }

    public void setByteTwo(byte[] byteTwo) {
        this.byteTwo = byteTwo;
    }

    public void setLongOne(long[] longOne) {
        this.longOne = longOne;
    }

    public void setLongTwo(long[] longTwo) {
        this.longTwo = longTwo;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String[] getStringOne() {
        return stringOne;
    }

    public void setStringOne(String[] stringOne) {
        this.stringOne = stringOne;
    }

    public SupportEventWithManyArray withStringOne(String[] stringOne) {
        this.stringOne = stringOne;
        return this;
    }

    public String[] getStringTwo() {
        return stringTwo;
    }

    public void setStringTwo(String[] stringTwo) {
        this.stringTwo = stringTwo;
    }

    public SupportEventWithManyArray withStringTwo(String[] stringTwo) {
        this.stringTwo = stringTwo;
        return this;
    }

    public Collection<int[]> getIntArrayCollection() {
        return intArrayCollection;
    }

    public SupportEventWithManyArray withIntArrayCollection(Collection<int[]> intArrays) {
        this.intArrayCollection = intArrays;
        return this;
    }

    public void setIntArrayCollection(Collection<int[]> intArrayCollection) {
        this.intArrayCollection = intArrayCollection;
    }
}
