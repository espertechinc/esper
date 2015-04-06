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

package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.type.RelationalOpEnum;
import com.espertech.esper.client.EventBean;

import java.util.Collection;

/**
 * Strategy for subselects with ">/</<=/>= ANY".
 */
public class SubselectEvalStrategyRelOpAny implements SubselectEvalStrategy
{
    private final RelationalOpEnum.Computer computer;
    private final ExprEvaluator valueExpr;
    private final ExprEvaluator selectClauseExpr;
    private final ExprEvaluator filterExpr;

    /**
     * Ctor.
     * @param computer operator
     * @param valueExpr LHS
     * @param selectClause select or null
     * @param filterExpr filter or null
     */
    public SubselectEvalStrategyRelOpAny(RelationalOpEnum.Computer computer, ExprEvaluator valueExpr, ExprEvaluator selectClause, ExprEvaluator filterExpr)
    {
        this.computer = computer;
        this.valueExpr = valueExpr;
        this.selectClauseExpr = selectClause;
        this.filterExpr = filterExpr;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext)
    {
        // Evaluate the value expression
        Object valueLeft = valueExpr.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

        if (matchingEvents == null)
        {
            return false;
        }
        if (matchingEvents.size() == 0)
        {
            return false;
        }

        // Evaluation event-per-stream
        EventBean[] events = new EventBean[eventsPerStream.length + 1];
        System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);

        // Filter and check each row.
        boolean hasNonNullRow = false;
        boolean hasRows = false;
        for (EventBean subselectEvent : matchingEvents)
        {
            // Prepare filter expression event list
            events[0] = subselectEvent;

            // Eval filter expression
            if (filterExpr != null)
            {
                Boolean pass = (Boolean) filterExpr.evaluate(events, true, exprEvaluatorContext);
                if ((pass == null) || (!pass))
                {
                    continue;
                }
            }
            hasRows = true;

            Object valueRight;
            if (selectClauseExpr != null)
            {
                valueRight = selectClauseExpr.evaluate(events, true, exprEvaluatorContext);
            }
            else
            {
                valueRight = events[0].getUnderlying();
            }

            if (valueRight != null)
            {
                hasNonNullRow = true;
            }

            if ((valueLeft != null) && (valueRight != null))
            {
                if (computer.compare(valueLeft, valueRight))
                {
                    return true;
                }
            }
        }

        if (!hasRows)
        {
            return false;
        }
        if ((!hasNonNullRow) || (valueLeft == null))
        {
            return null;
        }
        return false;
    }
}
