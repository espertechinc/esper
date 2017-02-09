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

import java.util.*;

public class ResultSetProcessorAggregateGroupedOutputAllHelperImpl implements ResultSetProcessorAggregateGroupedOutputAllHelper {

    private final ResultSetProcessorAggregateGrouped processor;

    private final List<EventBean> eventsOld = new ArrayList<EventBean>(2);
    private final List<EventBean> eventsNew = new ArrayList<EventBean>(2);
    private final Map<Object, EventBean[]> repsPerGroup = new LinkedHashMap<Object, EventBean[]>();
    private final Set<Object> lastSeenKeys = new HashSet<Object>();

    public ResultSetProcessorAggregateGroupedOutputAllHelperImpl(ResultSetProcessorAggregateGrouped processor) {
        this.processor = processor;
    }

    public void processView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        Object[] newDataMultiKey = processor.generateGroupKeys(newData, true);
        Object[] oldDataMultiKey = processor.generateGroupKeys(oldData, false);
        Set<Object> keysSeenRemoved = new HashSet<Object>();

        if (newData != null) {
            // apply new data to aggregates
            int count = 0;
            for (EventBean aNewData : newData) {
                EventBean[] eventsPerStream = new EventBean[]{aNewData};
                Object mk = newDataMultiKey[count];
                repsPerGroup.put(mk, eventsPerStream);
                lastSeenKeys.add(mk);
                processor.eventsPerStreamOneStream[0] = aNewData;
                processor.aggregationService.applyEnter(eventsPerStream, mk, processor.agentInstanceContext);
                count++;
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            int count = 0;
            for (EventBean anOldData : oldData) {
                Object mk = oldDataMultiKey[count];
                lastSeenKeys.add(mk);
                keysSeenRemoved.add(mk);
                processor.eventsPerStreamOneStream[0] = anOldData;
                processor.aggregationService.applyLeave(processor.eventsPerStreamOneStream, oldDataMultiKey[count], processor.agentInstanceContext);
                count++;
            }
        }

        if (processor.prototype.isSelectRStream()) {
            processor.generateOutputBatchedViewUnkeyed(oldData, oldDataMultiKey, false, isGenerateSynthetic, eventsOld, null);
        }
        processor.generateOutputBatchedViewUnkeyed(newData, newDataMultiKey, true, isGenerateSynthetic, eventsNew, null);

        for (Object keySeen : keysSeenRemoved) {
            EventBean newEvent = processor.generateOutputBatchedSingle(keySeen, repsPerGroup.get(keySeen), true, isGenerateSynthetic);
            if (newEvent != null) {
                eventsNew.add(newEvent);
            }
        }
    }

    public void processJoin(Set<MultiKey<EventBean>> newData, Set<MultiKey<EventBean>> oldData, boolean isGenerateSynthetic) {
        Object[] newDataMultiKey = processor.generateGroupKeys(newData, true);
        Object[] oldDataMultiKey = processor.generateGroupKeys(oldData, false);
        Set<Object> keysSeenRemoved = new HashSet<Object>();

        if (newData != null) {
            // apply new data to aggregates
            int count = 0;
            for (MultiKey<EventBean> aNewData : newData) {
                Object mk = newDataMultiKey[count];
                repsPerGroup.put(mk, aNewData.getArray());
                lastSeenKeys.add(mk);
                processor.aggregationService.applyEnter(aNewData.getArray(), mk, processor.agentInstanceContext);
                count++;
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            int count = 0;
            for (MultiKey<EventBean> anOldData : oldData) {
                Object mk = oldDataMultiKey[count];
                lastSeenKeys.add(mk);
                keysSeenRemoved.add(mk);
                processor.aggregationService.applyLeave(anOldData.getArray(), oldDataMultiKey[count], processor.agentInstanceContext);
                count++;
            }
        }

        if (processor.prototype.isSelectRStream()) {
            processor.generateOutputBatchedJoinUnkeyed(oldData, oldDataMultiKey, false, isGenerateSynthetic, eventsOld, null);
        }
        processor.generateOutputBatchedJoinUnkeyed(newData, newDataMultiKey, false, isGenerateSynthetic, eventsNew, null);

        for (Object keySeen : keysSeenRemoved) {
            EventBean newEvent = processor.generateOutputBatchedSingle(keySeen, repsPerGroup.get(keySeen), true, isGenerateSynthetic);
            if (newEvent != null) {
                eventsNew.add(newEvent);
            }
        }
    }

    public UniformPair<EventBean[]> outputView(boolean isSynthesize) {
        return output(isSynthesize);
    }

    public UniformPair<EventBean[]> outputJoin(boolean isSynthesize) {
        return output(isSynthesize);
    }

    public void remove(Object key) {
        repsPerGroup.remove(key);
    }

    public void destroy() {
        // no action required
    }

    private UniformPair<EventBean[]> output(boolean isSynthesize) {
        // generate remaining key events
        for (Map.Entry<Object, EventBean[]> entry : repsPerGroup.entrySet()) {
            if (lastSeenKeys.contains(entry.getKey())) {
                continue;
            }
            EventBean newEvent = processor.generateOutputBatchedSingle(entry.getKey(), entry.getValue(), true, isSynthesize);
            if (newEvent != null) {
                eventsNew.add(newEvent);
            }
        }
        lastSeenKeys.clear();

        EventBean[] newEventsArr = EventBeanUtility.toArray(eventsNew);
        EventBean[] oldEventsArr = null;
        if (processor.prototype.isSelectRStream()) {
            oldEventsArr = EventBeanUtility.toArray(eventsOld);
        }
        eventsNew.clear();
        eventsOld.clear();
        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }
}
