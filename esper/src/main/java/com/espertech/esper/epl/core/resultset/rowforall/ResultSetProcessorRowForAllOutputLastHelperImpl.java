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
package com.espertech.esper.epl.core.resultset.rowforall;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil;

import java.util.Set;

public class ResultSetProcessorRowForAllOutputLastHelperImpl implements ResultSetProcessorRowForAllOutputLastHelper {
    private final ResultSetProcessorRowForAll processor;
    private EventBean[] lastEventRStreamForOutputLast;

    public ResultSetProcessorRowForAllOutputLastHelperImpl(ResultSetProcessorRowForAll processor) {
        this.processor = processor;
    }

    public void processView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        if (processor.isSelectRStream() && lastEventRStreamForOutputLast == null) {
            lastEventRStreamForOutputLast = processor.getSelectListEventsAsArray(false, isGenerateSynthetic, false);
        }

        EventBean[] eventsPerStream = new EventBean[1];
        ResultSetProcessorUtil.applyAggViewResult(processor.getAggregationService(), processor.getExprEvaluatorContext(), newData, oldData, eventsPerStream);
    }

    public void processJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic) {
        if (processor.isSelectRStream() && lastEventRStreamForOutputLast == null) {
            lastEventRStreamForOutputLast = processor.getSelectListEventsAsArray(false, isGenerateSynthetic, true);
        }

        ResultSetProcessorUtil.applyAggJoinResult(processor.getAggregationService(), processor.getExprEvaluatorContext(), newEvents, oldEvents);
    }

    public UniformPair<EventBean[]> outputView(boolean isSynthesize) {
        return continueOutputLimitedLastNonBuffered(isSynthesize);
    }

    public UniformPair<EventBean[]> outputJoin(boolean isSynthesize) {
        return continueOutputLimitedLastNonBuffered(isSynthesize);
    }

    public void destroy() {
        // no action required
    }

    private UniformPair<EventBean[]> continueOutputLimitedLastNonBuffered(boolean isSynthesize) {
        EventBean[] events = processor.getSelectListEventsAsArray(true, isSynthesize, false);
        UniformPair<EventBean[]> result = new UniformPair<>(events, null);

        if (processor.isSelectRStream() && lastEventRStreamForOutputLast == null) {
            lastEventRStreamForOutputLast = processor.getSelectListEventsAsArray(false, isSynthesize, false);
        }
        if (lastEventRStreamForOutputLast != null) {
            result.setSecond(lastEventRStreamForOutputLast);
            lastEventRStreamForOutputLast = null;
        }

        return result;
    }
}
