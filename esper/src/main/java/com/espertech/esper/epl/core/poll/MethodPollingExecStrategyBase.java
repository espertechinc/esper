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
package com.espertech.esper.epl.core.poll;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.db.PollExecStrategy;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Viewable providing historical data from a database.
 */
public abstract class MethodPollingExecStrategyBase implements PollExecStrategy {
    private static final Logger log = LoggerFactory.getLogger(MethodPollingExecStrategyBase.class);

    protected final EventAdapterService eventAdapterService;
    protected final FastMethod method;
    protected final EventType eventType;
    protected final Object invocationTarget;
    protected final MethodPollingExecStrategyEnum strategy;
    protected final VariableReader variableReader;
    protected final String variableName;
    protected final VariableService variableService;

    public MethodPollingExecStrategyBase(EventAdapterService eventAdapterService, FastMethod method, EventType eventType, Object invocationTarget, MethodPollingExecStrategyEnum strategy, VariableReader variableReader, String variableName, VariableService variableService) {
        this.eventAdapterService = eventAdapterService;
        this.method = method;
        this.eventType = eventType;
        this.invocationTarget = invocationTarget;
        this.strategy = strategy;
        this.variableReader = variableReader;
        this.variableName = variableName;
        this.variableService = variableService;
    }

    protected abstract List<EventBean> handleResult(Object invocationResult);

    protected boolean checkNonNullArrayValue(Object value) {
        if (value == null) {
            log.warn("Expected non-null return result from method '" + method.getName() + "', but received null array element value");
            return false;
        }
        return true;
    }

    public void start() {
    }

    public void done() {
    }

    public void destroy() {
    }

    public List<EventBean> poll(Object[] lookupValues, ExprEvaluatorContext exprEvaluatorContext) {
        switch (strategy) {
            case TARGET_CONST:
                return invokeInternal(lookupValues, invocationTarget);
            case TARGET_VAR:
                return invokeInternalVariable(lookupValues, variableReader);
            case TARGET_VAR_CONTEXT:
                VariableReader reader = variableService.getReader(variableName, exprEvaluatorContext.getAgentInstanceId());
                if (reader == null) {
                    return null;
                }
                return invokeInternalVariable(lookupValues, reader);
            default:
                throw new UnsupportedOperationException("unrecognized strategy " + strategy);
        }
    }

    private List<EventBean> invokeInternalVariable(Object[] lookupValues, VariableReader variableReader) {
        Object target = variableReader.getValue();
        if (target == null) {
            return null;
        }
        if (target instanceof EventBean) {
            target = ((EventBean) target).getUnderlying();
        }
        return invokeInternal(lookupValues, target);
    }

    private List<EventBean> invokeInternal(Object[] lookupValues, Object invocationTarget) {
        try {
            Object invocationResult = method.invoke(invocationTarget, lookupValues);
            if (invocationResult != null) {
                return handleResult(invocationResult);
            }
            return null;
        } catch (InvocationTargetException ex) {
            throw new EPException("Method '" + method.getName() + "' of class '" + method.getJavaMethod().getDeclaringClass().getName() +
                    "' reported an exception: " + ex.getTargetException(), ex.getTargetException());
        }
    }


}
