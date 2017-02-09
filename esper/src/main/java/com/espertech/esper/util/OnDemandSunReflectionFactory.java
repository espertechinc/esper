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
package com.espertech.esper.util;

import java.lang.reflect.Constructor;

public class OnDemandSunReflectionFactory {

    private OnDemandSunReflectionFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getConstructor(Class<T> clazz,
                                                    Constructor<Object> constructor) {
        return (Constructor<T>) sun.reflect.ReflectionFactory.getReflectionFactory()
                .newConstructorForSerialization(clazz, constructor);
    }
}
