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

public abstract class SortedAccessStrategyRelOpBase {
    private final ExprEvaluator keyEval;
    private final EventBean[] events;
    private final int lookupStream;
    private final boolean isNWOnTrigger;

    protected SortedAccessStrategyRelOpBase(boolean isNWOnTrigger, int lookupStream, int numStreams, ExprEvaluator keyEval) {
        this.lookupStream = lookupStream;
        this.keyEval = keyEval;
        this.isNWOnTrigger = isNWOnTrigger;
        if (lookupStream != -1) {
            events = new EventBean[lookupStream + 1];
        } else {
            events = new EventBean[numStreams + 1];
        }
    }

    public Object evaluateLookup(EventBean theEvent, ExprEvaluatorContext context) {
        events[lookupStream] = theEvent;
        return keyEval.evaluate(events, true, context);
    }

    public Object evaluatePerStream(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (isNWOnTrigger) {
            return keyEval.evaluate(eventsPerStream, true, context);
        } else {
            System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);
            return keyEval.evaluate(events, true, context);
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " key " + keyEval.getClass().getSimpleName();
    }
}
