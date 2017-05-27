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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SupportBeanArrayCollMap {
    private int[] intArr;
    private Long[] longArr;
    private Collection<Integer> intCol;
    private Collection<Long> longCol;
    private Map<Integer, String> intMap;
    private Map<Long, String> longMap;
    private Long longBoxed;
    private Object[] objectArr;
    private Object anyObject;
    private Map<String, Object> otherMap;

    public SupportBeanArrayCollMap() {
        this.anyObject = anyObject;
    }

    public SupportBeanArrayCollMap(Object anyObject) {
        this.anyObject = anyObject;
    }

    public SupportBeanArrayCollMap(Object[] objectArr) {
        this.objectArr = objectArr;
    }

    public SupportBeanArrayCollMap(int[] intArr) {
        this.intArr = intArr;
    }

    public SupportBeanArrayCollMap(int[] intArr, Long[] longArr) {
        this.intArr = intArr;
        this.longArr = longArr;
    }

    public SupportBeanArrayCollMap(boolean makeCol, int[] intArr, Long[] longArr, Long longBoxed) {
        this(makeCol, intArr, longArr);
        this.longBoxed = longBoxed;
    }

    public SupportBeanArrayCollMap(boolean makeCol, int[] intArr, Long[] longArr) {
        if (makeCol) {
            intCol = convertCol(intArr);
            longCol = convertCol(longArr);
        } else {
            intMap = convertMap(intArr);
            longMap = convertMap(longArr);
        }
    }

    public SupportBeanArrayCollMap(Long longBoxed, int[] intArr, Long[] longColl, int[] intMap) {
        this.longBoxed = longBoxed;
        this.intArr = intArr;
        this.longMap = convertMap(longColl);
        this.intCol = convertCol(intMap);
    }

    public Long getLongBoxed() {
        return longBoxed;
    }

    public int[] getIntArr() {
        return intArr;
    }

    public Long[] getLongArr() {
        return longArr;
    }

    public Collection<Integer> getIntCol() {
        return intCol;
    }

    public Collection<Long> getLongCol() {
        return longCol;
    }

    public Map<Integer, String> getIntMap() {
        return intMap;
    }

    public Map<Long, String> getLongMap() {
        return longMap;
    }

    public Object[] getObjectArr() {
        return objectArr;
    }

    public void setIntArr(int[] intArr) {
        this.intArr = intArr;
    }

    public void setIntCol(Collection<Integer> intCol) {
        this.intCol = intCol;
    }

    public void setIntMap(Map<Integer, String> intMap) {
        this.intMap = intMap;
    }

    public void setLongArr(Long[] longArr) {
        this.longArr = longArr;
    }

    public void setLongBoxed(Long longBoxed) {
        this.longBoxed = longBoxed;
    }

    public void setLongCol(Collection<Long> longCol) {
        this.longCol = longCol;
    }

    public void setLongMap(Map<Long, String> longMap) {
        this.longMap = longMap;
    }

    public void setObjectArr(Object[] objectArr) {
        this.objectArr = objectArr;
    }

    public Object getAnyObject() {
        return anyObject;
    }

    public void setAnyObject(Object anyObject) {
        this.anyObject = anyObject;
    }

    public Map<String, Object> getOtherMap() {
        return otherMap;
    }

    public void setOtherMap(Map<String, Object> otherMap) {
        this.otherMap = otherMap;
    }

    private static HashMap<Long, String> convertMap(Long[] longArr) {
        if (longArr == null) {
            return null;
        }

        HashMap<Long, String> longMap = new HashMap<Long, String>();
        for (Long along : longArr) {
            longMap.put(along, "");
        }
        return longMap;
    }

    private static HashMap<Integer, String> convertMap(int[] intArr) {
        if (intArr == null) {
            return null;
        }

        HashMap<Integer, String> intMap = new HashMap<Integer, String>();
        for (int anIntArr : intArr) {
            intMap.put(anIntArr, "");
        }
        return intMap;
    }

    private static ArrayList<Long> convertCol(Long[] longArr) {
        if (longArr == null) {
            return null;
        }

        ArrayList<Long> longCol = new ArrayList<Long>();
        for (Long along : longArr) {
            longCol.add(along);
        }
        return longCol;
    }

    private static ArrayList<Integer> convertCol(int[] intArr) {
        if (intArr == null) {
            return null;
        }

        ArrayList<Integer> intCol = new ArrayList<Integer>();
        for (int anIntArr : intArr) {
            intCol.add(anIntArr);
        }
        return intCol;
    }

}
