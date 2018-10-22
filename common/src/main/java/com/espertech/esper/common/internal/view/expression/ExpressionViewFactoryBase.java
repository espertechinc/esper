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
package com.espertech.esper.common.internal.view.expression;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationResultFutureAssignableWEval;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.view.core.DataWindowViewFactory;
import com.espertech.esper.common.internal.view.core.DataWindowViewWithPrevious;
import com.espertech.esper.common.internal.view.core.ViewFactoryContext;

/**
 * Base factory for expression-based window and batch view.
 */
public abstract class ExpressionViewFactoryBase implements DataWindowViewFactory, DataWindowViewWithPrevious {
    protected EventType eventType;
    protected Variable[] variables;
    protected EventType builtinMapType;
    protected ExprEvaluator expiryEval;
    protected int scheduleCallbackId;
    protected AggregationServiceFactory aggregationServiceFactory;
    protected AggregationResultFutureAssignableWEval aggregationResultFutureAssignable;

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public EventType getBuiltinMapType() {
        return builtinMapType;
    }

    public ExprEvaluator getExpiryEval() {
        return expiryEval;
    }

    public void setVariables(Variable[] variables) {
        this.variables = variables;
    }

    public void setBuiltinMapType(EventType builtinMapType) {
        this.builtinMapType = builtinMapType;
    }

    public void setExpiryEval(ExprEvaluator expiryEval) {
        this.expiryEval = expiryEval;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public AggregationServiceFactory getAggregationServiceFactory() {
        return aggregationServiceFactory;
    }

    public void setAggregationServiceFactory(AggregationServiceFactory aggregationServiceFactory) {
        this.aggregationServiceFactory = aggregationServiceFactory;
    }

    public AggregationResultFutureAssignableWEval getAggregationResultFutureAssignable() {
        return aggregationResultFutureAssignable;
    }

    public void setAggregationResultFutureAssignable(AggregationResultFutureAssignableWEval aggregationResultFutureAssignable) {
        this.aggregationResultFutureAssignable = aggregationResultFutureAssignable;
    }

    public Variable[] getVariables() {
        return variables;
    }
}
