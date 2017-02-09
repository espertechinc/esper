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

import java.util.*;

public class ResultSetProcessorRowPerGroupOutputLastHelperImpl implements ResultSetProcessorRowPerGroupOutputLastHelper {

    protected final ResultSetProcessorRowPerGroup processor;
    private final Map<Object, EventBean[]> groupReps = new LinkedHashMap<Object, EventBean[]>();
    private final Map<Object, EventBean> groupRepsOutputLastUnordRStream = new LinkedHashMap<Object, EventBean>();

    public ResultSetProcessorRowPerGroupOutputLastHelperImpl(ResultSetProcessorRowPerGroup processor) {
        this.processor = processor;
    }

    public void processView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        if (newData != null) {
            for (EventBean aNewData : newData) {
                EventBean[] eventsPerStream = new EventBean[]{aNewData};
                Object mk = processor.generateGroupKey(eventsPerStream, true);

                // if this is a newly encountered group, generate the remove stream event
                if (groupReps.put(mk, eventsPerStream) == null) {
                    if (processor.prototype.isSelectRStream()) {
                        EventBean event = processor.generateOutputBatchedNoSortWMap(false, mk, eventsPerStream, true, isGenerateSynthetic);
                        if (event != null) {
                            groupRepsOutputLastUnordRStream.put(mk, event);
                        }
                    }
                }
                processor.aggregationService.applyEnter(eventsPerStream, mk, processor.agentInstanceContext);
            }
        }
        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                EventBean[] eventsPerStream = new EventBean[]{anOldData};
                Object mk = processor.generateGroupKey(eventsPerStream, true);

                if (groupReps.put(mk, eventsPerStream) == null) {
                    if (processor.prototype.isSelectRStream()) {
                        EventBean event = processor.generateOutputBatchedNoSortWMap(false, mk, eventsPerStream, false, isGenerateSynthetic);
                        if (event != null) {
                            groupRepsOutputLastUnordRStream.put(mk, event);
                        }
                    }
                }

                processor.aggregationService.applyLeave(eventsPerStream, mk, processor.agentInstanceContext);
            }
        }
    }

    public void processJoin(Set<MultiKey<EventBean>> newData, Set<MultiKey<EventBean>> oldData, boolean isGenerateSynthetic) {
        if (newData != null) {
            for (MultiKey<EventBean> aNewData : newData) {
                Object mk = processor.generateGroupKey(aNewData.getArray(), true);
                if (groupReps.put(mk, aNewData.getArray()) == null) {
                    if (processor.prototype.isSelectRStream()) {
                        EventBean event = processor.generateOutputBatchedNoSortWMap(true, mk, aNewData.getArray(), false, isGenerateSynthetic);
                        if (event != null) {
                            groupRepsOutputLastUnordRStream.put(mk, event);
                        }
                    }
                }
                processor.aggregationService.applyEnter(aNewData.getArray(), mk, processor.agentInstanceContext);
            }
        }
        if (oldData != null) {
            for (MultiKey<EventBean> anOldData : oldData) {
                Object mk = processor.generateGroupKey(anOldData.getArray(), false);
                if (groupReps.put(mk, anOldData.getArray()) == null) {
                    if (processor.prototype.isSelectRStream()) {
                        EventBean event = processor.generateOutputBatchedNoSortWMap(true, mk, anOldData.getArray(), false, isGenerateSynthetic);
                        if (event != null) {
                            groupRepsOutputLastUnordRStream.put(mk, event);
                        }
                    }
                }
                processor.aggregationService.applyLeave(anOldData.getArray(), mk, processor.agentInstanceContext);
            }
        }
    }

    public UniformPair<EventBean[]> outputView(boolean isSynthesize) {
        return output(isSynthesize, false);
    }

    public UniformPair<EventBean[]> outputJoin(boolean isSynthesize) {
        return output(isSynthesize, true);
    }

    public void destroy() {
        // no action required
    }

    public void remove(Object key) {
        groupReps.remove(key);
    }

    private UniformPair<EventBean[]> output(boolean isSynthesize, boolean join) {
        List<EventBean> newEvents = new ArrayList<EventBean>(4);
        processor.generateOutputBatchedArr(join, groupReps.entrySet().iterator(), true, isSynthesize, newEvents, null);
        groupReps.clear();
        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);

        EventBean[] oldEventsArr = null;
        if (groupRepsOutputLastUnordRStream != null && !groupRepsOutputLastUnordRStream.isEmpty()) {
            Collection<EventBean> oldEvents = groupRepsOutputLastUnordRStream.values();
            oldEventsArr = oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (newEventsArr == null && oldEventsArr == null) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }
}
