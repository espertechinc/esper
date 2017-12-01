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
package com.espertech.esper.codegen.util;

public class CodegenClassUtil {
    public static Class getComponentTypeOutermost(Class clazz) {
        if (!clazz.isArray()) {
            return clazz;
        }
        return getComponentTypeOutermost(clazz.getComponentType());
    }

    public static int getNumberOfDimensions(Class clazz) {
        if (clazz.getComponentType() == null) {
            return 0;
        } else {
            return getNumberOfDimensions(clazz.getComponentType()) + 1;
        }
    }
}
