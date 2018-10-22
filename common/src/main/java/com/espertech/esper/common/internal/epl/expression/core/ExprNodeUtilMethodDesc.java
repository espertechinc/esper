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
package com.espertech.esper.common.internal.epl.expression.core;

import java.lang.reflect.Method;

public class ExprNodeUtilMethodDesc {
    private final boolean allConstants;
    private final ExprForge[] childForges;
    private final Method reflectionMethod;

    public ExprNodeUtilMethodDesc(boolean allConstants, ExprForge[] childForges, Method reflectionMethod) {
        this.allConstants = allConstants;
        this.childForges = childForges;
        this.reflectionMethod = reflectionMethod;
    }

    public boolean isAllConstants() {
        return allConstants;
    }

    public Method getReflectionMethod() {
        return reflectionMethod;
    }

    public ExprForge[] getChildForges() {
        return childForges;
    }
}
