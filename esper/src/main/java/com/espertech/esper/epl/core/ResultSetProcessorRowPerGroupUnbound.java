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
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationRowRemovedCallback;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.view.Viewable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ResultSetProcessorRowPerGroupUnbound extends ResultSetProcessorRowPerGroup implements AggregationRowRemovedCallback {

    protected final ResultSetProcessorRowPerGroupUnboundGroupRep groupReps;

    public ResultSetProcessorRowPerGroupUnbound(ResultSetProcessorRowPerGroupFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        super(prototype, selectExprProcessor, orderByProcessor, aggregationService, agentInstanceContext);
        groupReps = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupUnboundGroupRep(agentInstanceContext, prototype);
        aggregationService.setRemovedCallback(groupReps);
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            for (EventBean aNewData : newData) {
                eventsPerStream[0] = aNewData;
                Object mk = generateGroupKey(eventsPerStream, true);
                groupReps.put(mk, eventsPerStream[0]);
                aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
            }
        }
        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                eventsPerStream[0] = anOldData;
                Object mk = generateGroupKey(eventsPerStream, false);
                aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
            }
        }
    }

    @Override
    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        // Generate group-by keys for all events, collect all keys in a set for later event generation
        Map<Object, EventBean> keysAndEvents = new HashMap<Object, EventBean>();
        Object[] newDataMultiKey = generateGroupKeys(newData, keysAndEvents, true);
        Object[] oldDataMultiKey = generateGroupKeys(oldData, keysAndEvents, false);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsView(keysAndEvents, false, isSynthesize);
        }

        // update aggregates
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            // apply new data to aggregates
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                groupReps.put(newDataMultiKey[i], eventsPerStream[0]);
                aggregationService.applyEnter(eventsPerStream, newDataMultiKey[i], agentInstanceContext);
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            for (int i = 0; i < oldData.length; i++) {
                eventsPerStream[0] = oldData[i];
                aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[i], agentInstanceContext);
            }
        }

        // generate new events using select expressions
        EventBean[] selectNewEvents = generateOutputEventsView(keysAndEvents, true, isSynthesize);

        if ((selectNewEvents != null) || (selectOldEvents != null)) {
            return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
        }
        return null;
    }

    @Override
    public Iterator<EventBean> getIterator(Viewable parent) {
        if (orderByProcessor == null) {
            Iterator<EventBean> it = groupReps.valueIterator();
            return new ResultSetRowPerGroupIterator(it, this, aggregationService, agentInstanceContext);
        }
        return getIteratorSorted(groupReps.valueIterator());
    }

    @Override
    public void stop() {
        super.stop();
        groupReps.destroy();
    }
}
