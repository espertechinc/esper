/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.ResultSetProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;

public abstract class OutputProcessViewBaseWAfter extends OutputProcessViewBase
{
    private static final Log log = LogFactory.getLog(OutputProcessViewBaseWAfter.class);

    private final Long afterConditionTime;
    private final Integer afterConditionNumberOfEvents;
    protected boolean isAfterConditionSatisfied;

    private int afterConditionEventsFound;

    protected OutputProcessViewBaseWAfter(ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied) {
        super(resultSetProcessor);
        this.afterConditionTime = afterConditionTime;
        this.afterConditionNumberOfEvents = afterConditionNumberOfEvents;
        isAfterConditionSatisfied = afterConditionSatisfied;
    }

    /**
     * Returns true if the after-condition is satisfied.
     * @param newEvents is the view new events
     * @return indicator for output condition
     */
    public boolean checkAfterCondition(EventBean[] newEvents, StatementContext statementContext)
    {
        return isAfterConditionSatisfied || checkAfterCondition(newEvents == null ? 0 : newEvents.length, statementContext);
    }

    /**
     * Returns true if the after-condition is satisfied.
     * @param newEvents is the join new events
     * @return indicator for output condition
     */
    public boolean checkAfterCondition(Set<MultiKey<EventBean>> newEvents, StatementContext statementContext)
    {
        return isAfterConditionSatisfied || checkAfterCondition(newEvents == null ? 0 : newEvents.size(), statementContext);
    }

    /**
     * Returns true if the after-condition is satisfied.
     * @param newOldEvents is the new and old events pair
     * @return indicator for output condition
     */
    public boolean checkAfterCondition(UniformPair<EventBean[]> newOldEvents, StatementContext statementContext)
    {
        return isAfterConditionSatisfied || checkAfterCondition(newOldEvents == null ? 0 : (newOldEvents.getFirst() == null ? 0 : newOldEvents.getFirst().length), statementContext);
    }

    private boolean checkAfterCondition(int numOutputEvents, StatementContext statementContext)
    {
        if (afterConditionTime != null)
        {
            long time = statementContext.getTimeProvider().getTime();
            if (time < afterConditionTime)
            {
                return false;
            }

            isAfterConditionSatisfied = true;
            return true;
        }
        else if (afterConditionNumberOfEvents != null)
        {
            afterConditionEventsFound += numOutputEvents;
            if (afterConditionEventsFound <= afterConditionNumberOfEvents)
            {
                return false;
            }

            isAfterConditionSatisfied = true;
            return true;
        }
        else
        {
            isAfterConditionSatisfied = true;
            return true;
        }
    }
}
