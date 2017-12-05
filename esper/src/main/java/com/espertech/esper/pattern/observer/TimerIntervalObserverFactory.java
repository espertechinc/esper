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
package com.espertech.esper.pattern.observer;

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.pattern.EvalStateNodeNumber;
import com.espertech.esper.pattern.MatchedEventConvertor;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.util.JavaClassHelper;

import java.io.Serializable;
import java.util.List;

/**
 * Factory for making observer instances.
 */
public class TimerIntervalObserverFactory implements ObserverFactory, Serializable {
    private static final long serialVersionUID = -2808651894497586884L;

    private final static String NAME = "Timer-interval observer";

    /**
     * Parameters.
     */
    protected ExprNode parameter;

    /**
     * Convertor to events-per-stream.
     */
    protected transient MatchedEventConvertor convertor;

    public void setObserverParameters(List<ExprNode> parameters, MatchedEventConvertor convertor, ExprValidationContext validationContext) throws ObserverParameterException {
        ObserverParameterUtil.validateNoNamedParameters(NAME, parameters);
        String errorMessage = NAME + " requires a single numeric or time period parameter";
        if (parameters.size() != 1) {
            throw new ObserverParameterException(errorMessage);
        }
        if (!(parameters.get(0) instanceof ExprTimePeriod)) {
            Class returnType = parameters.get(0).getForge().getEvaluationType();
            if (!(JavaClassHelper.isNumeric(returnType))) {
                throw new ObserverParameterException(errorMessage);
            }
        }

        parameter = parameters.get(0);
        this.convertor = convertor;
    }

    public long computeDelta(MatchedEventMap beginState, PatternAgentInstanceContext context) {
        if (parameter instanceof ExprTimePeriod) {
            ExprTimePeriod timePeriod = (ExprTimePeriod) parameter;
            return timePeriod.nonconstEvaluator().deltaUseEngineTime(convertor.convert(beginState), context.getAgentInstanceContext(), context.getAgentInstanceContext().getTimeProvider());
        } else {
            Object result = parameter.getForge().getExprEvaluator().evaluate(convertor.convert(beginState), true, context.getAgentInstanceContext());
            if (result == null) {
                throw new EPException("Null value returned for guard expression");
            }
            return context.getStatementContext().getTimeAbacus().deltaForSecondsNumber((Number) result);
        }
    }

    public EventObserver makeObserver(PatternAgentInstanceContext context, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator, EvalStateNodeNumber stateNodeId, Object observerState, boolean isFilterChildNonQuitting) {
        return new TimerIntervalObserver(computeDelta(beginState, context), beginState, observerEventEvaluator);
    }

    public boolean isNonRestarting() {
        return false;
    }
}
