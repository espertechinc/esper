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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.event.EventBeanUtility;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

public class ResultSetProcessorRowForAllOutputAllHelperImpl implements ResultSetProcessorRowForAllOutputAllHelper {
    private final ResultSetProcessorRowForAll processor;
    private final Deque<EventBean> eventsOld = new ArrayDeque<EventBean>(2);
    private final Deque<EventBean> eventsNew = new ArrayDeque<EventBean>(2);

    public ResultSetProcessorRowForAllOutputAllHelperImpl(ResultSetProcessorRowForAll processor) {
        this.processor = processor;
    }

    public void processView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        if (processor.prototype.isSelectRStream()) {
            EventBean[] events = processor.getSelectListEvents(false, isGenerateSynthetic, false);
            EventBeanUtility.addToCollection(events, eventsOld);
        }

        EventBean[] eventsPerStream = new EventBean[1];
        ResultSetProcessorUtil.applyAggViewResult(processor.aggregationService, processor.exprEvaluatorContext, newData, oldData, eventsPerStream);

        EventBean[] events = processor.getSelectListEvents(true, isGenerateSynthetic, false);
        EventBeanUtility.addToCollection(events, eventsNew);
    }

    public void processJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic) {
        if (processor.prototype.isSelectRStream()) {
            EventBean[] events = processor.getSelectListEvents(false, isGenerateSynthetic, true);
            EventBeanUtility.addToCollection(events, eventsOld);
        }

        ResultSetProcessorUtil.applyAggJoinResult(processor.aggregationService, processor.exprEvaluatorContext, newEvents, oldEvents);

        EventBean[] events = processor.getSelectListEvents(true, isGenerateSynthetic, true);
        EventBeanUtility.addToCollection(events, eventsNew);
    }

    public UniformPair<EventBean[]> outputView(boolean isGenerateSynthetic) {
        return output(isGenerateSynthetic, false);
    }

    public UniformPair<EventBean[]> outputJoin(boolean isGenerateSynthetic) {
        return output(isGenerateSynthetic, true);
    }

    public void destroy() {
        // no action required
    }

    private UniformPair<EventBean[]> output(boolean isGenerateSynthetic, boolean isJoin) {
        EventBean[] oldEvents = EventBeanUtility.toArrayNullIfEmpty(eventsOld);
        EventBean[] newEvents = EventBeanUtility.toArrayNullIfEmpty(eventsNew);

        if (newEvents == null) {
            newEvents = processor.getSelectListEvents(true, isGenerateSynthetic, isJoin);
        }
        if (oldEvents == null && processor.prototype.isSelectRStream()) {
            oldEvents = processor.getSelectListEvents(false, isGenerateSynthetic, isJoin);
        }

        UniformPair<EventBean[]> result = null;
        if (oldEvents != null || newEvents != null) {
            result = new UniformPair<EventBean[]>(newEvents, oldEvents);
        }

        eventsOld.clear();
        eventsNew.clear();
        return result;
    }
}
