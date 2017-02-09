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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ResultSetProcessorAggregateGroupedOutputLastHelperImpl implements ResultSetProcessorAggregateGroupedOutputLastHelper {

    private final ResultSetProcessorAggregateGrouped processor;

    private Map<Object, EventBean> outputLastUnordGroupNew;
    private Map<Object, EventBean> outputLastUnordGroupOld;

    public ResultSetProcessorAggregateGroupedOutputLastHelperImpl(ResultSetProcessorAggregateGrouped processor) {
        this.processor = processor;
        outputLastUnordGroupNew = new LinkedHashMap<Object, EventBean>();
        outputLastUnordGroupOld = new LinkedHashMap<Object, EventBean>();
    }

    public void processView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        Object[] newDataMultiKey = processor.generateGroupKeys(newData, true);
        Object[] oldDataMultiKey = processor.generateGroupKeys(oldData, false);

        if (newData != null) {
            // apply new data to aggregates
            int count = 0;
            for (EventBean aNewData : newData) {
                Object mk = newDataMultiKey[count];
                processor.eventsPerStreamOneStream[0] = aNewData;
                processor.aggregationService.applyEnter(processor.eventsPerStreamOneStream, mk, processor.agentInstanceContext);
                count++;
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            int count = 0;
            for (EventBean anOldData : oldData) {
                processor.eventsPerStreamOneStream[0] = anOldData;
                processor.aggregationService.applyLeave(processor.eventsPerStreamOneStream, oldDataMultiKey[count], processor.agentInstanceContext);
                count++;
            }
        }

        if (processor.prototype.isSelectRStream()) {
            processor.generateOutputBatchedViewPerKey(oldData, oldDataMultiKey, false, isGenerateSynthetic, outputLastUnordGroupOld, null);
        }
        processor.generateOutputBatchedViewPerKey(newData, newDataMultiKey, false, isGenerateSynthetic, outputLastUnordGroupNew, null);
    }

    public void processJoin(Set<MultiKey<EventBean>> newData, Set<MultiKey<EventBean>> oldData, boolean isGenerateSynthetic) {
        Object[] newDataMultiKey = processor.generateGroupKeys(newData, true);
        Object[] oldDataMultiKey = processor.generateGroupKeys(oldData, false);

        if (newData != null) {
            // apply new data to aggregates
            int count = 0;
            for (MultiKey<EventBean> aNewData : newData) {
                Object mk = newDataMultiKey[count];
                processor.aggregationService.applyEnter(aNewData.getArray(), mk, processor.agentInstanceContext);
                count++;
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            int count = 0;
            for (MultiKey<EventBean> anOldData : oldData) {
                processor.aggregationService.applyLeave(anOldData.getArray(), oldDataMultiKey[count], processor.agentInstanceContext);
                count++;
            }
        }

        if (processor.prototype.isSelectRStream()) {
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
        if (processor.prototype.isSelectRStream()) {
            oldEventsArr = (outputLastUnordGroupOld.isEmpty()) ? null : outputLastUnordGroupOld.values().toArray(new EventBean[outputLastUnordGroupOld.size()]);
        }
        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        outputLastUnordGroupNew.clear();
        outputLastUnordGroupOld.clear();
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }
}
