/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression.core;

import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.Method;

public class ExprNodeUtilMethodDesc {
    private final boolean allConstants;
    private final Class[] paramTypes;
    private final ExprEvaluator[] childEvals;
    private final Method reflectionMethod;
    private final FastMethod fastMethod;

    public ExprNodeUtilMethodDesc(boolean allConstants, Class[] paramTypes, ExprEvaluator[] childEvals, Method reflectionMethod, FastMethod fastMethod) {
        this.allConstants = allConstants;
        this.paramTypes = paramTypes;
        this.childEvals = childEvals;
        this.reflectionMethod = reflectionMethod;
        this.fastMethod = fastMethod;
    }

    public boolean isAllConstants() {
        return allConstants;
    }

    public Class[] getParamTypes() {
        return paramTypes;
    }

    public ExprEvaluator[] getChildEvals() {
        return childEvals;
    }

    public Method getReflectionMethod() {
        return reflectionMethod;
    }

    public FastMethod getFastMethod() {
        return fastMethod;
    }
}
