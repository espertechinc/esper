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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class SupportBeanParameterizedWFieldSingleIndexed<T> {
    private final T[] indexedArrayProperty;
    public final T[] indexedArrayField;
    private final List<T> indexedListProperty;
    public final List<T> indexedListField;

    public SupportBeanParameterizedWFieldSingleIndexed(Class<T> clazz, T value) {
        T[] array = (T[]) Array.newInstance(clazz, 1);
        array[0] = value;
        this.indexedArrayProperty = array;
        this.indexedArrayField = array;
        List<T> list = new ArrayList<>();
        list.add(value);
        this.indexedListProperty = list;
        this.indexedListField = list;
    }

    public T[] indexedArrayProperty() {
        return indexedArrayProperty;
    }

    public List<T> indexedListProperty() {
        return indexedListProperty;
    }

    public T indexedArrayAtIndex(int index) {
        return indexedArrayProperty[index];
    }
}
