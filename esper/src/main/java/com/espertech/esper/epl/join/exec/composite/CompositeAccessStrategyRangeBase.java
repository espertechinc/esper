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
package com.espertech.esper.epl.join.exec.composite;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public abstract class CompositeAccessStrategyRangeBase {
    protected final ExprEvaluator start;
    protected final boolean includeStart;

    protected final ExprEvaluator end;
    protected final boolean includeEnd;

    private final EventBean[] events;
    private final int lookupStream;

    protected final Class coercionType;
    private final boolean isNWOnTrigger;

    protected CompositeAccessStrategyRangeBase(boolean isNWOnTrigger, int lookupStream, int numStreams, ExprEvaluator start, boolean includeStart, ExprEvaluator end, boolean includeEnd, Class coercionType) {
        this.start = start;
        this.includeStart = includeStart;
        this.end = end;
        this.includeEnd = includeEnd;
        this.coercionType = coercionType;
        this.isNWOnTrigger = isNWOnTrigger;

        if (lookupStream != -1) {
            events = new EventBean[lookupStream + 1];
        } else {
            events = new EventBean[numStreams + 1];
        }
        this.lookupStream = lookupStream;
    }

    public Object evaluateLookupStart(EventBean theEvent, ExprEvaluatorContext context) {
        events[lookupStream] = theEvent;
        return start.evaluate(events, true, context);
    }

    public Object evaluateLookupEnd(EventBean theEvent, ExprEvaluatorContext context) {
        events[lookupStream] = theEvent;
        return end.evaluate(events, true, context);
    }

    public Object evaluatePerStreamStart(EventBean[] eventPerStream, ExprEvaluatorContext context) {
        if (isNWOnTrigger) {
            return start.evaluate(eventPerStream, true, context);
        } else {
            System.arraycopy(eventPerStream, 0, events, 1, eventPerStream.length);
            return start.evaluate(events, true, context);
        }
    }

    public Object evaluatePerStreamEnd(EventBean[] eventPerStream, ExprEvaluatorContext context) {
        if (isNWOnTrigger) {
            return end.evaluate(eventPerStream, true, context);
        } else {
            System.arraycopy(eventPerStream, 0, events, 1, eventPerStream.length);
            return end.evaluate(events, true, context);
        }
    }

}
