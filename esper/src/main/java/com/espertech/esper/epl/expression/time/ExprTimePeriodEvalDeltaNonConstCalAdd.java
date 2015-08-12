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

package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Calendar;
import java.util.TimeZone;

public class ExprTimePeriodEvalDeltaNonConstCalAdd implements ExprTimePeriodEvalDeltaNonConst
{
    private final Calendar cal;
    private final ExprTimePeriodImpl parent;

    public ExprTimePeriodEvalDeltaNonConstCalAdd(TimeZone timeZone, ExprTimePeriodImpl parent) {
        this.parent = parent;
        this.cal = Calendar.getInstance(timeZone);
    }

    public synchronized long deltaMillisecondsAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        cal.setTimeInMillis(currentTime);
        addSubtract(parent, cal, 1, eventsPerStream, isNewData, context);
        return cal.getTimeInMillis() - currentTime;
    }

    public synchronized long deltaMillisecondsSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        cal.setTimeInMillis(currentTime);
        addSubtract(parent, cal, -1, eventsPerStream, isNewData, context);
        return cal.getTimeInMillis() - currentTime;
    }

    public synchronized long deltaMillisecondsUseEngineTime(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext) {
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        cal.setTimeInMillis(currentTime);
        addSubtract(parent, cal, 1, eventsPerStream, true, agentInstanceContext);
        return cal.getTimeInMillis() - currentTime;
    }

    public synchronized ExprTimePeriodEvalDeltaResult deltaMillisecondsAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // find the next-nearest reference higher then the current time, compute delta, return reference one lower
        if (reference > current) {
            while(reference > current) {
                reference = reference - deltaMillisecondsSubtract(reference, eventsPerStream, isNewData, context);
            }
        }

        long next = reference;
        long last;
        do {
            last = next;
            next = next + deltaMillisecondsAdd(last, eventsPerStream, isNewData, context);
        }
        while (next <= current);
        return new ExprTimePeriodEvalDeltaResult(next - current, last);
    }

    private void addSubtract(ExprTimePeriodImpl parent, Calendar cal, int factor, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        ExprTimePeriodImpl.TimePeriodAdder[] adders = parent.getAdders();
        ExprEvaluator[] evaluators = parent.getEvaluators();
        for (int i = 0; i < adders.length; i++) {
            int value = ((Number) evaluators[i].evaluate(eventsPerStream, newData, context)).intValue();
            adders[i].add(cal, factor * value);
        }
    }
}
