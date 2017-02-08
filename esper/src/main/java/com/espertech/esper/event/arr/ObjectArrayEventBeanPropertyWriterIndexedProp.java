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
package com.espertech.esper.event.arr;

import java.lang.reflect.Array;

public class ObjectArrayEventBeanPropertyWriterIndexedProp extends ObjectArrayEventBeanPropertyWriter {

    private final int indexTarget;

    public ObjectArrayEventBeanPropertyWriterIndexedProp(int propertyIndex, int indexTarget) {
        super(propertyIndex);
        this.indexTarget = indexTarget;
    }

    @Override
    public void write(Object value, Object[] array) {
        Object arrayEntry = array[index];
        if (arrayEntry != null && arrayEntry.getClass().isArray() && Array.getLength(arrayEntry) > indexTarget) {
            Array.set(arrayEntry, indexTarget, value);
        }
    }
}
