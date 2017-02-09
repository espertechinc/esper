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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Calendar;
import java.util.TimeZone;

public class ExprTimePeriodEvalDeltaNonConstCalAdd implements ExprTimePeriodEvalDeltaNonConst {
    private final Calendar cal;
    private final ExprTimePeriodImpl parent;
    private final int indexMicroseconds;

    public ExprTimePeriodEvalDeltaNonConstCalAdd(TimeZone timeZone, ExprTimePeriodImpl parent) {
        this.parent = parent;
        this.cal = Calendar.getInstance(timeZone);
        this.indexMicroseconds = ExprTimePeriodUtil.findIndexMicroseconds(parent.getAdders());
    }

    public synchronized long deltaAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return addSubtract(currentTime, 1, eventsPerStream, isNewData, context);
    }

    public synchronized long deltaSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return addSubtract(currentTime, -1, eventsPerStream, isNewData, context);
    }

    public synchronized long deltaUseEngineTime(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext) {
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        return addSubtract(currentTime, 1, eventsPerStream, true, agentInstanceContext);
    }

    public synchronized ExprTimePeriodEvalDeltaResult deltaAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // find the next-nearest reference higher then the current time, compute delta, return reference one lower
        if (reference > current) {
            while (reference > current) {
                reference = reference - deltaSubtract(reference, eventsPerStream, isNewData, context);
            }
        }

        long next = reference;
        long last;
        do {
            last = next;
            next = next + deltaAdd(last, eventsPerStream, isNewData, context);
        }
        while (next <= current);
        return new ExprTimePeriodEvalDeltaResult(next - current, last);
    }

    private long addSubtract(long currentTime, int factor, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        long remainder = parent.getTimeAbacus().calendarSet(currentTime, cal);

        ExprTimePeriodImpl.TimePeriodAdder[] adders = parent.getAdders();
        ExprEvaluator[] evaluators = parent.getEvaluators();
        int usec = 0;
        for (int i = 0; i < adders.length; i++) {
            int value = ((Number) evaluators[i].evaluate(eventsPerStream, newData, context)).intValue();
            if (i == indexMicroseconds) {
                usec = value;
            } else {
                adders[i].add(cal, factor * value);
            }
        }

        long result = parent.getTimeAbacus().calendarGet(cal, remainder);
        if (indexMicroseconds != -1) {
            result += factor * usec;
        }
        return result - currentTime;
    }
}
