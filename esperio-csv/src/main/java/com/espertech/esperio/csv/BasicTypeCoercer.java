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
package com.espertech.esperio.csv;

import net.sf.cglib.reflect.FastConstructor;

/**
 * Coercer for using the constructor to perform the coercion.
 */
public class BasicTypeCoercer extends AbstractTypeCoercer {

    public Object coerce(String property, String source) throws Exception {
        Object[] parameters = new Object[]{source};

        FastConstructor ctor = propertyConstructors.get(property);
        Object value;
        if (ctor != null) {
            value = ctor.newInstance(parameters);
        } else {
            value = Long.parseLong(source);
        }

        return value;
    }
}
