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
import java.util.Map;

public class SupportInKeywordBean implements Serializable {
    private int[] ints;
    private long[] longs;
    private Map<Integer, String> mapOfIntKey;
    private Collection<Integer> collOfInt;

    public SupportInKeywordBean(int[] ints) {
        this.ints = ints;
    }

    public SupportInKeywordBean(Map<Integer, String> mapOfIntKey) {
        this.mapOfIntKey = mapOfIntKey;
    }

    public SupportInKeywordBean(Collection<Integer> collOfInt) {
        this.collOfInt = collOfInt;
    }

    public SupportInKeywordBean(long[] longs) {
        this.longs = longs;
    }

    public int[] getInts() {
        return ints;
    }

    public Map<Integer, String> getMapOfIntKey() {
        return mapOfIntKey;
    }

    public Collection<Integer> getCollOfInt() {
        return collOfInt;
    }

    public long[] getLongs() {
        return longs;
    }
}
