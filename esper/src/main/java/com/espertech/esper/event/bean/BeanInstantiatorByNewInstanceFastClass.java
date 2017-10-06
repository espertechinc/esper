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

import net.sf.cglib.reflect.FastClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class BeanInstantiatorByNewInstanceFastClass implements BeanInstantiator {
    private final static Logger log = LoggerFactory.getLogger(BeanInstantiatorByNewInstanceFastClass.class);

    private final FastClass fastClass;

    public BeanInstantiatorByNewInstanceFastClass(FastClass fastClass) {
        this.fastClass = fastClass;
    }

    public Object instantiate() {
        try {
            return fastClass.newInstance();
        } catch (InvocationTargetException e) {
            String message = "Unexpected exception encountered invoking newInstance on class '" + fastClass.getJavaClass().getName() + "': " + e.getTargetException().getMessage();
            log.error(message, e);
            return null;
        }
    }
}
