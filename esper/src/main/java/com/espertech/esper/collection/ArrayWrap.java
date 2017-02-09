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
package com.espertech.esper.collection;

import java.lang.reflect.Array;

public class ArrayWrap<T> {
    private final Class componentType;
    private T[] handles;

    public ArrayWrap(Class componentType, int currentSize) {
        this.componentType = componentType;
        this.handles = (T[]) Array.newInstance(componentType, currentSize);
    }

    public void expand(int size) {
        int newSize = handles.length + size;
        T[] newHandles = (T[]) Array.newInstance(componentType, newSize);
        System.arraycopy(handles, 0, newHandles, 0, handles.length);
        handles = newHandles;
    }

    public T[] getArray() {
        return handles;
    }
}
