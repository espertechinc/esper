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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodTargetStrategyStaticMethod implements MethodTargetStrategy, MethodTargetStrategyFactory, StatementReadyCallback {
    private Class clazz;
    private String methodName;
    private Class[] methodParameters;
    private Method method;
    private MethodTargetStrategyStaticMethodInvokeType invokeType;

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        this.method = resolveMethod(clazz, methodName, methodParameters);
        this.invokeType = MethodTargetStrategyStaticMethodInvokeType.getInvokeType(method);
    }

    public MethodTargetStrategy make(AgentInstanceContext agentInstanceContext) {
        return this;
    }

    public Object invoke(Object lookupValues, AgentInstanceContext agentInstanceContext) {
        try {
            switch (invokeType) {
                case NOPARAM:
                    return method.invoke(null, null);
                case SINGLE:
                    return method.invoke(null, lookupValues);
                case MULTIKEY:
                    return method.invoke(null, (Object[]) lookupValues);
                default:
                    throw new IllegalStateException("Unrecognized value for " + invokeType);
            }
        } catch (InvocationTargetException ex) {
            throw new EPException("Method '" + method.getName() + "' of class '" + method.getDeclaringClass().getName() +
                "' reported an exception: " + ex.getTargetException(), ex.getTargetException());
        } catch (IllegalAccessException ex) {
            throw new EPException("Method '" + method.getName() + "' of class '" + method.getDeclaringClass().getName() +
                "' reported an exception: " + ex, ex);
        }
    }

    public void setInvokeType(MethodTargetStrategyStaticMethodInvokeType invokeType) {
        this.invokeType = invokeType;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setMethodParameters(Class[] methodParameters) {
        this.methodParameters = methodParameters;
    }

    public String getPlan() {
        return "method '" + methodName + "' of class '" + clazz.getName() + "'";
    }

    protected static Method resolveMethod(Class clazz, String methodName, Class[] methodParameters) {
        Method method;
        try {
            method = clazz.getMethod(methodName, methodParameters);
        } catch (NoSuchMethodException ex) {
            throw new EPException("Failed to find method '" + methodName + "' of class '" + clazz.getName() + "' with parameters " +
                JavaClassHelper.getParameterAsString(methodParameters));
        }
        return method;
    }
}
