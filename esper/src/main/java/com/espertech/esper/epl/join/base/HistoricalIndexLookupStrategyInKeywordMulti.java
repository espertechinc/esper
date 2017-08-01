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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.plan.InKeywordTableLookupUtil;
import com.espertech.esper.epl.join.table.EventTable;

import java.util.Iterator;
import java.util.Set;

/**
 * Index lookup strategy into a poll-based cache result.
 */
public class HistoricalIndexLookupStrategyInKeywordMulti implements HistoricalIndexLookupStrategy {
    private final EventBean[] eventsPerStream;
    private final ExprEvaluator evaluator;
    private final int lookupStream;

    public HistoricalIndexLookupStrategyInKeywordMulti(int lookupStream, ExprNode expression) {
        this.eventsPerStream = new EventBean[lookupStream + 1];
        this.evaluator = expression.getForge().getExprEvaluator();
        this.lookupStream = lookupStream;
    }

    public Iterator<EventBean> lookup(EventBean lookupEvent, EventTable[] indexTable, ExprEvaluatorContext exprEvaluatorContext) {
        eventsPerStream[lookupStream] = lookupEvent;
        Set<EventBean> result = InKeywordTableLookupUtil.multiIndexLookup(evaluator, eventsPerStream, exprEvaluatorContext, indexTable);
        if (result == null) {
            return null;
        }
        return result.iterator();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
