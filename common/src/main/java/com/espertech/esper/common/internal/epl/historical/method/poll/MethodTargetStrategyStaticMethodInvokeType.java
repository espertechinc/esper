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
package com.espertech.esper.common.internal.epl.historical.method.poll;

import java.lang.reflect.Method;

public enum MethodTargetStrategyStaticMethodInvokeType {
    NOPARAM,
    SINGLE,
    MULTIKEY;

    protected static MethodTargetStrategyStaticMethodInvokeType getInvokeType(Method method) {
        if (method.getParameterTypes().length == 0) {
            return MethodTargetStrategyStaticMethodInvokeType.NOPARAM;
        } else if (method.getParameterTypes().length == 1) {
            return MethodTargetStrategyStaticMethodInvokeType.SINGLE;
        } else {
            return MethodTargetStrategyStaticMethodInvokeType.MULTIKEY;
        }
    }
}
