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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeanInstantiatorByNewInstanceReflection implements BeanInstantiator {
    private final static Logger log = LoggerFactory.getLogger(BeanInstantiatorByNewInstanceReflection.class);

    private final Class clazz;

    public BeanInstantiatorByNewInstanceReflection(Class clazz) {
        this.clazz = clazz;
    }

    public Object instantiate() {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException e) {
            return handle(e);
        } catch (InstantiationException e) {
            return handle(e);
        }
    }

    private Object handle(Exception e) {
        String message = "Unexpected exception encountered invoking newInstance on class '" + clazz.getName() + "': " + e.getMessage();
        log.error(message, e);
        return null;
    }
}
