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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.type.RelationalOpEnum;

import java.util.Collection;

/**
 * Strategy for subselects with "&gt;/&lt;/&lt;=/&gt;= ALL".
 */
public class SubselectEvalStrategyRelOpAll implements SubselectEvalStrategy
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
    public SubselectEvalStrategyRelOpAll(RelationalOpEnum.Computer computer, ExprEvaluator valueExpr, ExprEvaluator selectClause, ExprEvaluator filterExpr)
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
            return true;
        }
        if (matchingEvents.size() == 0)
        {
            return true;
        }

        // Evaluation event-per-stream
        EventBean[] events = new EventBean[eventsPerStream.length + 1];
        System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);

        // Filter and check each row.
        boolean hasRows = false;
        boolean hasNullRow = false;
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

            if (valueRight == null)
            {
                hasNullRow = true;
            }
            else
            {
                if (valueLeft != null)
                {
                    if (!computer.compare(valueLeft, valueRight))
                    {
                        return false;
                    }
                }
            }

        }

        if (!hasRows)
        {
            return true;
        }
        if (valueLeft == null)
        {
            return null;
        }
        if (hasNullRow)
        {
            return null;
        }
        return true;
    }
}
