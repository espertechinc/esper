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
package com.espertech.esper.epl.join.exec.sorted;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.PropertySortedEventTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class SortedAccessStrategyLE extends SortedAccessStrategyRelOpBase implements SortedAccessStrategy {

    public SortedAccessStrategyLE(boolean isNWOnTrigger, int lookupStream, int numStreams, ExprEvaluator keyEval) {
        super(isNWOnTrigger, lookupStream, numStreams, keyEval);
    }

    public Set<EventBean> lookup(EventBean theEvent, PropertySortedEventTable index, ExprEvaluatorContext context) {
        return index.lookupLessEqual(super.evaluateLookup(theEvent, context));
    }

    public Set<EventBean> lookupCollectKeys(EventBean theEvent, PropertySortedEventTable index, ExprEvaluatorContext context, ArrayList<Object> keys) {
        Object point = super.evaluateLookup(theEvent, context);
        keys.add(point);
        return index.lookupLessEqual(point);
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, PropertySortedEventTable index, ExprEvaluatorContext context) {
        return index.lookupLessEqualColl(super.evaluatePerStream(eventsPerStream, context));
    }

    public Collection<EventBean> lookupCollectKeys(EventBean[] eventsPerStream, PropertySortedEventTable index, ExprEvaluatorContext context, ArrayList<Object> keys) {
        Object point = super.evaluatePerStream(eventsPerStream, context);
        keys.add(point);
        return index.lookupLessEqualColl(point);
    }
}

