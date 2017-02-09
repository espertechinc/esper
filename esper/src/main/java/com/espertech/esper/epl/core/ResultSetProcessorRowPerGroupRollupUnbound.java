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
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.Viewable;

import java.util.Iterator;

public class ResultSetProcessorRowPerGroupRollupUnbound extends ResultSetProcessorRowPerGroupRollup {

    private final ResultSetProcessorRowPerGroupRollupUnboundHelper unboundHelper;

    public ResultSetProcessorRowPerGroupRollupUnbound(ResultSetProcessorRowPerGroupRollupFactory prototype, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        super(prototype, orderByProcessor, aggregationService, agentInstanceContext);
        unboundHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupRollupSnapshotUnbound(agentInstanceContext, prototype);
    }

    @Override
    public void stop() {
        super.stop();
        unboundHelper.destroy();
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        Object[][] newDataMultiKey = generateGroupKeysView(newData, unboundHelper.getBuffer(), true);
        Object[][] oldDataMultiKey = generateGroupKeysView(oldData, unboundHelper.getBuffer(), false);

        // update aggregates
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                aggregationService.applyEnter(eventsPerStream, newDataMultiKey[i], agentInstanceContext);
            }
        }
        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                eventsPerStream[0] = oldData[i];
                aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[i], agentInstanceContext);
            }
        }
    }

    @Override
    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerGroup();
        }

        Object[][] newDataMultiKey = generateGroupKeysView(newData, unboundHelper.getBuffer(), true);
        Object[][] oldDataMultiKey = generateGroupKeysView(oldData, unboundHelper.getBuffer(), false);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsView(unboundHelper.getBuffer(), false, isSynthesize);
        }

        // update aggregates
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                aggregationService.applyEnter(eventsPerStream, newDataMultiKey[i], agentInstanceContext);
            }
        }
        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                eventsPerStream[0] = oldData[i];
                aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[i], agentInstanceContext);
            }
        }

        // generate new events using select expressions
        EventBean[] selectNewEvents = generateOutputEventsView(unboundHelper.getBuffer(), true, isSynthesize);

        if ((selectNewEvents != null) || (selectOldEvents != null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(selectNewEvents, selectOldEvents);
            }
            return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(null, null);
        }
        return null;
    }

    @Override
    public Iterator<EventBean> getIterator(Viewable parent) {
        EventBean[] output = generateOutputEventsView(unboundHelper.getBuffer(), true, true);
        return new ArrayEventIterator(output);
    }
}
