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
package com.espertech.esper.common.internal.epl.output.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.join.base.JoinExecutionStrategy;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.util.EventDistinctIterator;

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
        // no action
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param joinExecutionStrategy join strategy
     * @param resultSetProcessor    processor
     * @param parentView            view
     * @param distinct              flag
     * @param distinctKeyGetter getter
     * @return iterator
     */
    public static Iterator<EventBean> getIterator(JoinExecutionStrategy joinExecutionStrategy, ResultSetProcessor resultSetProcessor, Viewable parentView, boolean distinct, EventPropertyValueGetter distinctKeyGetter) {
        Iterator<EventBean> iterator;
        EventType eventType;
        if (joinExecutionStrategy != null) {
            Set<MultiKeyArrayOfKeys<EventBean>> joinSet = joinExecutionStrategy.staticJoin();
            iterator = resultSetProcessor.getIterator(joinSet);
        } else if (resultSetProcessor != null) {
            iterator = resultSetProcessor.getIterator(parentView);
        } else {
            iterator = parentView.iterator();
        }

        if (!distinct) {
            return iterator;
        }
        return new EventDistinctIterator(iterator, distinctKeyGetter);
    }
}
