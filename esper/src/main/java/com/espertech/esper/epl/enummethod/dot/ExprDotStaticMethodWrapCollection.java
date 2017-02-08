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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class ExprDotStaticMethodWrapCollection implements ExprDotStaticMethodWrap {
    private static final Logger log = LoggerFactory.getLogger(ExprDotStaticMethodWrapArrayScalar.class);

    private final String methodName;
    private final Class componentType;

    public ExprDotStaticMethodWrapCollection(String methodName, Class componentType) {
        this.methodName = methodName;
        this.componentType = componentType;
    }

    public EPType getTypeInfo() {
        return EPTypeHelper.collectionOfSingleValue(componentType);
    }

    public Collection convert(Object result) {
        if (result == null) {
            return null;
        }
        if (!(result instanceof Collection)) {
            log.warn("Expected collection-type input from method '" + methodName + "' but received " + result.getClass());
            return null;
        }
        return (Collection) result;
    }
}
