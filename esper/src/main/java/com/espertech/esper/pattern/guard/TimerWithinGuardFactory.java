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
package com.espertech.esper.pattern.guard;

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.*;
import com.espertech.esper.util.JavaClassHelper;

import java.io.Serializable;
import java.util.List;

/**
 * Factory for {@link TimerWithinGuard} instances.
 */
public class TimerWithinGuardFactory implements GuardFactory, Serializable {
    private static final long serialVersionUID = -1026320055174163611L;

    /**
     * Number of milliseconds.
     */
    protected ExprNode timeExpr;

    /**
     * For converting matched-events maps to events-per-stream.
     */
    protected transient MatchedEventConvertor convertor;

    public void setGuardParameters(List<ExprNode> parameters, MatchedEventConvertor convertor) throws GuardParameterException {
        String errorMessage = "Timer-within guard requires a single numeric or time period parameter";
        if (parameters.size() != 1) {
            throw new GuardParameterException(errorMessage);
        }

        if (!JavaClassHelper.isNumeric(parameters.get(0).getForge().getEvaluationType())) {
            throw new GuardParameterException(errorMessage);
        }

        this.convertor = convertor;
        this.timeExpr = parameters.get(0);
    }

    public long computeTime(MatchedEventMap beginState, PatternAgentInstanceContext context) {
        if (timeExpr instanceof ExprTimePeriod) {
            ExprTimePeriod timePeriod = (ExprTimePeriod) timeExpr;
            return timePeriod.nonconstEvaluator().deltaUseEngineTime(convertor.convert(beginState), context.getAgentInstanceContext(), context.getAgentInstanceContext().getTimeProvider());
        } else {
            Object time = PatternExpressionUtil.evaluate("Timer-within guard", beginState, timeExpr, convertor, context.getAgentInstanceContext());
            if (time == null) {
                throw new EPException("Timer-within guard expression returned a null-value");
            }
            return context.getStatementContext().getTimeAbacus().deltaForSecondsNumber((Number) time);
        }
    }

    public Guard makeGuard(PatternAgentInstanceContext context, MatchedEventMap matchedEventMap, Quitable quitable, EvalStateNodeNumber stateNodeId, Object guardState) {
        return new TimerWithinGuard(computeTime(matchedEventMap, context), quitable);
    }
}
