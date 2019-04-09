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

public final class SupportEventWithIntArray implements Serializable {
    private String id;
    private int[] array;
    private int value;

    public SupportEventWithIntArray(String id, int[] array, int value) {
        this.id = id;
        this.array = array;
        this.value = value;
    }

    public SupportEventWithIntArray(String id, int[] array) {
        this(id, array, 0);
    }

    public String getId() {
        return id;
    }

    public int[] getArray() {
        return array;
    }

    public int getValue() {
        return value;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setArray(int[] array) {
        this.array = array;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
