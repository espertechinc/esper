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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import net.sf.cglib.reflect.FastMethod;

public class BeanEventPropertyWriterIndexedProp extends BeanEventPropertyWriter {

    private final int index;

    public BeanEventPropertyWriterIndexedProp(Class clazz, FastMethod writerMethod, int index) {
        super(clazz, writerMethod);
        this.index = index;
    }

    public void write(Object value, EventBean target) {
        super.invoke(new Object[]{index, value}, target.getUnderlying());
    }
}
