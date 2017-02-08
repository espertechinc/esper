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

import java.util.Map;

public class ObjectArrayEventBeanPropertyWriterMapProp extends ObjectArrayEventBeanPropertyWriter {

    private final String key;

    public ObjectArrayEventBeanPropertyWriterMapProp(int propertyIndex, String key) {
        super(propertyIndex);
        this.key = key;
    }

    @Override
    public void write(Object value, Object[] array) {
        Map mapEntry = (Map) array[index];
        if (mapEntry != null) {
            mapEntry.put(key, value);
        }
    }
}
