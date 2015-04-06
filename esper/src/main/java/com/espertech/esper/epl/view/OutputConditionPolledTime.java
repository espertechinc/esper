/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.view;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;

public final class OutputConditionPolledTime implements OutputConditionPolled
{
    private ExprTimePeriod timePeriod;
    private AgentInstanceContext context;
    private Long lastUpdate;

    /**
     * Constructor.
     * @param timePeriod is the number of minutes or seconds to batch events for, may include variables
     * @param context is the view context for time scheduling
     */
    public OutputConditionPolledTime(ExprTimePeriod timePeriod,
                               AgentInstanceContext context)
    {
        if (context == null)
        {
            String message = "OutputConditionTime requires a non-null view context";
            throw new NullPointerException(message);
        }

        this.context = context;
        this.timePeriod = timePeriod;

        double numSeconds = timePeriod.evaluateAsSeconds(null, true, context);
        if ((numSeconds < 0.001) && (!timePeriod.hasVariable())) {
            throw new IllegalArgumentException("Output condition by time requires a interval size of at least 1 msec or a variable");
        }
    }

    public boolean updateOutputCondition(int newEventsCount, int oldEventsCount)
    {
        // If we pull the interval from a variable, then we may need to reschedule
        long msecIntervalSize = timePeriod.nonconstEvaluator().deltaMillisecondsUseEngineTime(null, context);

        long current = context.getTimeProvider().getTime();
        if (lastUpdate == null || current - lastUpdate >= msecIntervalSize) {
            this.lastUpdate = current;
            return true;
        }
        return false;
    }

    public final String toString()
    {
        return this.getClass().getName();
    }
}