/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.pattern.guard;

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.pattern.*;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.MetaDefItem;

import java.io.Serializable;
import java.util.List;

/**
 * Factory for {@link TimerWithinGuard} instances.
 */
public class TimerWithinGuardFactory implements GuardFactory, MetaDefItem, Serializable
{
    private static final long serialVersionUID = -1026320055174163611L;

    /**
     * Number of milliseconds.
     */
    protected ExprNode millisecondsExpr;

    /**
     * For converting matched-events maps to events-per-stream.
     */
    protected transient MatchedEventConvertor convertor;

    public void setGuardParameters(List<ExprNode> parameters, MatchedEventConvertor convertor) throws GuardParameterException
    {
        String errorMessage = "Timer-within guard requires a single numeric or time period parameter";
        if (parameters.size() != 1)
        {
            throw new GuardParameterException(errorMessage);
        }

        if (!JavaClassHelper.isNumeric(parameters.get(0).getExprEvaluator().getType()))
        {
            throw new GuardParameterException(errorMessage);
        }

        this.convertor = convertor;
        this.millisecondsExpr = parameters.get(0);
    }

    public long computeMilliseconds(MatchedEventMap beginState, PatternAgentInstanceContext context) {
        if (millisecondsExpr instanceof ExprTimePeriod) {
            ExprTimePeriod timePeriod = (ExprTimePeriod) millisecondsExpr;
            return timePeriod.nonconstEvaluator().deltaMillisecondsUseEngineTime(convertor.convert(beginState), context.getAgentInstanceContext());
        }
        else {
            Object millisecondVal = PatternExpressionUtil.evaluate("Timer-within guard", beginState, millisecondsExpr, convertor, context.getAgentInstanceContext());

            if (millisecondVal == null) {
                throw new EPException("Timer-within guard expression returned a null-value");
            }

            Number param = (Number) millisecondVal;
            return Math.round(1000d * param.doubleValue());
        }
    }

    public Guard makeGuard(PatternAgentInstanceContext context, MatchedEventMap matchedEventMap, Quitable quitable, EvalStateNodeNumber stateNodeId, Object guardState)
    {
        return new TimerWithinGuard(computeMilliseconds(matchedEventMap, context), quitable);
    }
}
