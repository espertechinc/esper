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
package com.espertech.esper.common.internal.event.bean.core;

import com.espertech.esper.common.client.EventBean;

import java.lang.reflect.Method;

public class BeanEventPropertyWriterIndexedProp extends BeanEventPropertyWriter {

    private final int index;

    public BeanEventPropertyWriterIndexedProp(Class clazz, Method writerMethod, int index) {
        super(clazz, writerMethod);
        this.index = index;
    }

    public void write(Object value, EventBean target) {
        super.invoke(new Object[]{index, value}, target.getUnderlying());
    }
}
