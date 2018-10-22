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
package com.espertech.esper.common.internal.event.bean.instantiator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

import static com.espertech.esper.common.internal.event.bean.instantiator.BeanInstantiatorForgeByCtor.instantiateSunJVMCtor;

public class BeanInstantiatorByCtor implements BeanInstantiator {
    private final static Logger log = LoggerFactory.getLogger(BeanInstantiatorByCtor.class);

    private final Constructor ctor;

    public BeanInstantiatorByCtor(Constructor ctor) {
        this.ctor = ctor;
    }

    public Object instantiate() {
        return instantiateSunJVMCtor(ctor);
    }
}
