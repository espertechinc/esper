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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StatementResultListener;
import com.espertech.esper.core.service.UpdateDispatchView;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.join.base.JoinExecutionStrategy;
import com.espertech.esper.view.Viewable;

import java.util.Iterator;
import java.util.Set;

public class OutputStrategyUtil {
    public static void output(boolean forceUpdate, UniformPair<EventBean[]> result, UpdateDispatchView finalView) {
        EventBean[] newEvents = result != null ? result.getFirst() : null;
        EventBean[] oldEvents = result != null ? result.getSecond() : null;
        if (newEvents != null || oldEvents != null) {
            finalView.newResult(result);
        } else if (forceUpdate) {
            finalView.newResult(result);
        }
    }

    /**
     * Indicate statement result.
     *
     * @param newOldEvents     result
     * @param statementContext context
     */
    public static void indicateEarlyReturn(StatementContext statementContext, UniformPair<EventBean[]> newOldEvents) {
        if (newOldEvents == null) {
            return;
        }
        if ((statementContext.getMetricReportingService() != null) &&
                (statementContext.getMetricReportingService().getStatementOutputHooks() != null) &&
                (!statementContext.getMetricReportingService().getStatementOutputHooks().isEmpty())) {
            for (StatementResultListener listener : statementContext.getMetricReportingService().getStatementOutputHooks()) {
                listener.update(newOldEvents.getFirst(), newOldEvents.getSecond(), statementContext.getStatementName(), null, null);
            }
        }
    }

    public static Iterator<EventBean> getIterator(JoinExecutionStrategy joinExecutionStrategy, ResultSetProcessor resultSetProcessor, Viewable parentView, boolean distinct) {
        Iterator<EventBean> iterator;
        EventType eventType;
        if (joinExecutionStrategy != null) {
            Set<MultiKey<EventBean>> joinSet = joinExecutionStrategy.staticJoin();
            iterator = resultSetProcessor.getIterator(joinSet);
            eventType = resultSetProcessor.getResultEventType();
        } else if (resultSetProcessor != null) {
            iterator = resultSetProcessor.getIterator(parentView);
            eventType = resultSetProcessor.getResultEventType();
        } else {
            iterator = parentView.iterator();
            eventType = parentView.getEventType();
        }

        if (!distinct) {
            return iterator;
        }
        return new EventDistinctIterator(iterator, eventType);
    }
}
