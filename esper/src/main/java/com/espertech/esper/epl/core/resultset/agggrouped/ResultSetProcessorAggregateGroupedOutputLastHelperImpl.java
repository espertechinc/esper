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
package com.espertech.esper.epl.core.resultset.agggrouped;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ResultSetProcessorAggregateGroupedOutputLastHelperImpl implements ResultSetProcessorAggregateGroupedOutputLastHelper {

    private final ResultSetProcessorAggregateGrouped processor;

    private Map<Object, EventBean> outputLastUnordGroupNew;
    private Map<Object, EventBean> outputLastUnordGroupOld;

    public ResultSetProcessorAggregateGroupedOutputLastHelperImpl(ResultSetProcessorAggregateGrouped processor) {
        this.processor = processor;
        outputLastUnordGroupNew = new LinkedHashMap<>();
        outputLastUnordGroupOld = new LinkedHashMap<>();
    }

    public void processView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        Object[] newDataMultiKey = processor.generateGroupKeyArrayView(newData, true);
        Object[] oldDataMultiKey = processor.generateGroupKeyArrayView(oldData, false);
        EventBean[] eventsPerStream = new EventBean[1];

        if (newData != null) {
            // apply new data to aggregates
            int count = 0;
            for (EventBean aNewData : newData) {
                Object mk = newDataMultiKey[count];
                eventsPerStream[0] = aNewData;
                processor.getAggregationService().applyEnter(eventsPerStream, mk, processor.getAgentInstanceContext());
                count++;
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            int count = 0;
            for (EventBean anOldData : oldData) {
                eventsPerStream[0] = anOldData;
                processor.getAggregationService().applyLeave(eventsPerStream, oldDataMultiKey[count], processor.getAgentInstanceContext());
                count++;
            }
        }

        if (processor.isSelectRStream()) {
            processor.generateOutputBatchedViewPerKey(oldData, oldDataMultiKey, false, isGenerateSynthetic, outputLastUnordGroupOld, null, eventsPerStream);
        }
        processor.generateOutputBatchedViewPerKey(newData, newDataMultiKey, false, isGenerateSynthetic, outputLastUnordGroupNew, null, eventsPerStream);
    }

    public void processJoin(Set<MultiKey<EventBean>> newData, Set<MultiKey<EventBean>> oldData, boolean isGenerateSynthetic) {
        Object[] newDataMultiKey = processor.generateGroupKeyArrayJoin(newData, true);
        Object[] oldDataMultiKey = processor.generateGroupKeyArrayJoin(oldData, false);

        if (newData != null) {
            // apply new data to aggregates
            int count = 0;
            for (MultiKey<EventBean> aNewData : newData) {
                Object mk = newDataMultiKey[count];
                processor.getAggregationService().applyEnter(aNewData.getArray(), mk, processor.getAgentInstanceContext());
                count++;
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            int count = 0;
            for (MultiKey<EventBean> anOldData : oldData) {
                processor.getAggregationService().applyLeave(anOldData.getArray(), oldDataMultiKey[count], processor.getAgentInstanceContext());
                count++;
            }
        }

        if (processor.isSelectRStream()) {
            processor.generateOutputBatchedJoinPerKey(oldData, oldDataMultiKey, false, isGenerateSynthetic, outputLastUnordGroupOld, null);
        }
        processor.generateOutputBatchedJoinPerKey(newData, newDataMultiKey, false, isGenerateSynthetic, outputLastUnordGroupNew, null);
    }

    public UniformPair<EventBean[]> outputView(boolean isSynthesize) {
        return continueOutputLimitedLastNonBuffered();
    }

    public UniformPair<EventBean[]> outputJoin(boolean isSynthesize) {
        return continueOutputLimitedLastNonBuffered();
    }

    public void remove(Object key) {
        // no action required
    }

    public void destroy() {
        // no action required
    }

    private UniformPair<EventBean[]> continueOutputLimitedLastNonBuffered() {
        EventBean[] newEventsArr = (outputLastUnordGroupNew.isEmpty()) ? null : outputLastUnordGroupNew.values().toArray(new EventBean[outputLastUnordGroupNew.size()]);
        EventBean[] oldEventsArr = null;
        if (processor.isSelectRStream()) {
            oldEventsArr = (outputLastUnordGroupOld.isEmpty()) ? null : outputLastUnordGroupOld.values().toArray(new EventBean[outputLastUnordGroupOld.size()]);
        }
        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        outputLastUnordGroupNew.clear();
        outputLastUnordGroupOld.clear();
        return new UniformPair<>(newEventsArr, oldEventsArr);
    }
}
