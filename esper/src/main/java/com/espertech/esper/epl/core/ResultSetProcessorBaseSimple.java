/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.event.EventBeanUtility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Set;

/**
 * Result set processor for the simplest case: no aggregation functions used in the select clause, and no group-by.
 * <p>
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 */
public abstract class ResultSetProcessorBaseSimple implements ResultSetProcessor
{
    private static final Log log = LogFactory.getLog(ResultSetProcessorBaseSimple.class);

    public void clear()
    {
        // No need to clear state, there is no state held
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType)
    {
        if (outputLimitLimitType != OutputLimitLimitType.LAST)
        {
            UniformPair<Set<MultiKey<EventBean>>> flattened = EventBeanUtility.flattenBatchJoin(joinEventsSet);
            return processJoinResult(flattened.getFirst(), flattened.getSecond(), generateSynthetic);
        }

        // Determine the last event of the insert and remove stream that matches having-criteria
        int index = joinEventsSet.size() - 1;
        EventBean lastNonEmptyNew = null;
        EventBean lastNonEmptyOld = null;
        while(index >= 0)
        {
            UniformPair<Set<MultiKey<EventBean>>> pair = joinEventsSet.get(index);
            if ( ((pair.getFirst() != null) && (!pair.getFirst().isEmpty()) && (lastNonEmptyNew == null)) ||
                 ((pair.getSecond() != null) && (!pair.getSecond().isEmpty()) && (lastNonEmptyOld == null)) )
            {
                UniformPair<EventBean[]> result = processJoinResult(pair.getFirst(), pair.getSecond(), generateSynthetic);

                if ((lastNonEmptyNew == null) && (result != null) && (result.getFirst() != null) && (result.getFirst().length > 0))
                {
                    lastNonEmptyNew = result.getFirst()[result.getFirst().length - 1];
                }
                if ((lastNonEmptyOld == null) && (result != null) && (result.getSecond() != null) && (result.getSecond().length > 0))
                {
                    lastNonEmptyOld = result.getSecond()[result.getSecond().length - 1];
                }
            }
            if ((lastNonEmptyNew != null) && (lastNonEmptyOld != null))
            {
                break;
            }
            index--;
        }

        EventBean[] lastNew = null;
        if (lastNonEmptyNew != null) {
            lastNew = new EventBean[] {lastNonEmptyNew};
        }
        EventBean[] lastOld = null;
        if (lastNonEmptyOld != null) {
            lastOld = new EventBean[] {lastNonEmptyOld};
        }

        return new UniformPair<EventBean[]>(lastNew, lastOld);
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType)
    {
        if (outputLimitLimitType != OutputLimitLimitType.LAST)
        {
            UniformPair<EventBean[]> pair = EventBeanUtility.flattenBatchStream(viewEventsList);
            return processViewResult(pair.getFirst(), pair.getSecond(), generateSynthetic);
        }

        // Determine the last event of the insert and remove stream that matches having-criteria
        int index = viewEventsList.size() - 1;
        EventBean lastNonEmptyNew = null;
        EventBean lastNonEmptyOld = null;
        while(index >= 0)
        {
            UniformPair<EventBean[]> pair = viewEventsList.get(index);
            if ( ((pair.getFirst() != null) && (pair.getFirst().length != 0) && (lastNonEmptyNew == null)) ||
                 ((pair.getSecond() != null) && (pair.getSecond().length != 0) && (lastNonEmptyOld == null)) )
            {
                UniformPair<EventBean[]> result = processViewResult(pair.getFirst(), pair.getSecond(), generateSynthetic);

                if ((lastNonEmptyNew == null) && (result != null) && (result.getFirst() != null) && (result.getFirst().length > 0))
                {
                    lastNonEmptyNew = result.getFirst()[result.getFirst().length - 1];
                }
                if ((lastNonEmptyOld == null) && (result != null) && (result.getSecond() != null) && (result.getSecond().length > 0))
                {
                    lastNonEmptyOld = result.getSecond()[result.getSecond().length - 1];
                }
            }
            if ((lastNonEmptyNew != null) && (lastNonEmptyOld != null))
            {
                break;
            }
            index--;
        }

        EventBean[] lastNew = null;
        if (lastNonEmptyNew != null) {
            lastNew = new EventBean[] {lastNonEmptyNew};
        }
        EventBean[] lastOld = null;
        if (lastNonEmptyOld != null) {
            lastOld = new EventBean[] {lastNonEmptyOld};
        }

        return new UniformPair<EventBean[]>(lastNew, lastOld);
    }
}
