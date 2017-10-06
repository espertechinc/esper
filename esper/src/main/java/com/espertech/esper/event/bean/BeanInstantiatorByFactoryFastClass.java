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

import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class BeanInstantiatorByFactoryFastClass implements BeanInstantiator {
    private final static Logger log = LoggerFactory.getLogger(BeanInstantiatorByFactoryFastClass.class);

    private final FastMethod method;

    public BeanInstantiatorByFactoryFastClass(FastMethod method) {
        this.method = method;
    }

    public Object instantiate() {
        try {
            return method.invoke(null, null);
        } catch (InvocationTargetException e) {
            String message = "Unexpected exception encountered invoking factory method '" + method.getName() + "' on class '" + method.getJavaMethod().getDeclaringClass().getName() + "': " + e.getTargetException().getMessage();
            log.error(message, e);
            return null;
        }
    }
}
