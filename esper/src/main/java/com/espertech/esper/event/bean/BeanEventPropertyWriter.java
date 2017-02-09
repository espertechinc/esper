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
import com.espertech.esper.event.EventPropertyWriter;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * Writer for a property to an event.
 */
public class BeanEventPropertyWriter implements EventPropertyWriter {
    private static final Logger log = LoggerFactory.getLogger(BeanEventPropertyWriter.class);

    private final Class clazz;
    private final FastMethod writerMethod;

    /**
     * Ctor.
     *
     * @param clazz        to write to
     * @param writerMethod write method
     */
    public BeanEventPropertyWriter(Class clazz, FastMethod writerMethod) {
        this.clazz = clazz;
        this.writerMethod = writerMethod;
    }

    public void write(Object value, EventBean target) {
        invoke(new Object[]{value}, target.getUnderlying());
    }

    public void writeValue(Object value, Object target) {
        invoke(new Object[]{value}, target);
    }

    protected void invoke(Object[] values, Object target) {
        try {
            writerMethod.invoke(target, values);
        } catch (InvocationTargetException e) {
            String message = "Unexpected exception encountered invoking setter-method '" + writerMethod.getJavaMethod() + "' on class '" +
                    clazz.getName() + "' : " + e.getTargetException().getMessage();
            log.error(message, e);
        }
    }
}
