/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.view;

import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Output limit condition that is satisfied when either
 * the total number of new events arrived or the total number
 * of old events arrived is greater than a preset value.
 */
public final class OutputConditionPolledCount implements OutputConditionPolled
{
    private long eventRate;
    private int newEventsCount;
    private int oldEventsCount;
    private final VariableReader variableReader;
    private boolean isFirst = true;

    /**
     * Constructor.
     * @param eventRate is the number of old or new events that
     * must arrive in order for the condition to be satisfied
     * @param variableReader is for reading the variable value, if a variable was supplied, else null
     */
    public OutputConditionPolledCount(int eventRate, VariableReader variableReader)
    {
        if ((eventRate < 1) && (variableReader == null))
        {
            throw new IllegalArgumentException("Limiting output by event count requires an event count of at least 1 or a variable name");
        }
        this.eventRate = eventRate;
        this.variableReader = variableReader;
        newEventsCount = eventRate;
        oldEventsCount = eventRate;
    }

    public final boolean updateOutputCondition(int newDataCount, int oldDataCount)
    {
        if (variableReader != null)
        {
            Object value = variableReader.getValue();
            if (value != null)
            {
                eventRate = ((Number) value).longValue();
            }
        }

        this.newEventsCount += newDataCount;
        this.oldEventsCount += oldDataCount;

        if (isSatisfied() || isFirst)
        {
        	if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()))
            {
                log.debug(".updateOutputCondition() condition satisfied");
            }
            this.isFirst = false;
            this.newEventsCount = 0;
            this.oldEventsCount = 0;
            return true;
        }
        return false;
    }

    public final String toString()
    {
        return this.getClass().getName() +
                " eventRate=" + eventRate;
    }

    private boolean isSatisfied()
    {
    	return (newEventsCount >= eventRate) || (oldEventsCount >= eventRate);
    }

    private static final Log log = LogFactory.getLog(OutputConditionPolledCount.class);
}
