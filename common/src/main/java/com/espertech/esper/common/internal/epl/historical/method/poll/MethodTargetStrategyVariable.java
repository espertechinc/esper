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
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;

import java.lang.reflect.InvocationTargetException;

public class MethodTargetStrategyVariable implements MethodTargetStrategy {
    private final MethodTargetStrategyVariableFactory factory;
    private final VariableReader reader;

    public MethodTargetStrategyVariable(MethodTargetStrategyVariableFactory factory, VariableReader reader) {
        this.factory = factory;
        this.reader = reader;
    }

    public Object invoke(Object lookupValues, AgentInstanceContext agentInstanceContext) {
        Object target = reader.getValue();
        if (target == null) {
            return null;
        }
        if (target instanceof EventBean) {
            target = ((EventBean) target).getUnderlying();
        }
        try {
            switch (factory.invokeType) {
                case NOPARAM:
                    return factory.method.invoke(target, null);
                case SINGLE:
                    return factory.method.invoke(target, lookupValues);
                case MULTIKEY:
                    return factory.method.invoke(target, (Object[]) lookupValues);
                default:
                    throw new IllegalStateException("Unrecognized value for " + factory.invokeType);
            }
        } catch (InvocationTargetException ex) {
            throw new EPException("Method '" + factory.method.getName() + "' of class '" + factory.method.getDeclaringClass().getName() +
                "' reported an exception: " + ex.getTargetException(), ex.getTargetException());
        } catch (IllegalAccessException ex) {
            throw new EPException("Method '" + factory.method.getName() + "' of class '" + factory.method.getDeclaringClass().getName() +
                "' reported an exception: " + ex, ex);
        }
    }

    public String getPlan() {
        return "Variable '" + reader.getMetaData().getVariableName() + "'";
    }
}
