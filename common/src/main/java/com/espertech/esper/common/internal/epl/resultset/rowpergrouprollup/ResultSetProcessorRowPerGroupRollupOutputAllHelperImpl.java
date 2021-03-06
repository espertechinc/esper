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
package com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupLevel;

import java.util.*;

public class ResultSetProcessorRowPerGroupRollupOutputAllHelperImpl implements ResultSetProcessorRowPerGroupRollupOutputAllHelper {

    private final ResultSetProcessorRowPerGroupRollup processor;
    private final Map<Object, EventBean[]>[] outputLimitGroupRepsPerLevel;
    private final Map<Object, EventBean>[] groupRepsOutputLastUnordRStream;
    private boolean first;

    public ResultSetProcessorRowPerGroupRollupOutputAllHelperImpl(ResultSetProcessorRowPerGroupRollup processor, int levelCount) {
        this.processor = processor;

        outputLimitGroupRepsPerLevel = (LinkedHashMap<Object, EventBean[]>[]) new LinkedHashMap[levelCount];
        for (int i = 0; i < levelCount; i++) {
            outputLimitGroupRepsPerLevel[i] = new LinkedHashMap<>();
        }

        if (processor.isSelectRStream()) {
            groupRepsOutputLastUnordRStream = (LinkedHashMap<Object, EventBean>[]) new LinkedHashMap[levelCount];
            for (int i = 0; i < levelCount; i++) {
                groupRepsOutputLastUnordRStream[i] = new LinkedHashMap<>();
            }
        } else {
            groupRepsOutputLastUnordRStream = null;
        }
    }

    public void processView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        generateRemoveStreamJustOnce(isGenerateSynthetic, false);

        // apply to aggregates
        Object[] groupKeysPerLevel = new Object[processor.getGroupByRollupDesc().getLevels().length];
        EventBean[] eventsPerStream;
        if (newData != null) {
            for (EventBean aNewData : newData) {
                eventsPerStream = new EventBean[]{aNewData};
                Object groupKeyComplete = processor.generateGroupKeySingle(eventsPerStream, true);
                for (AggregationGroupByRollupLevel level : processor.getGroupByRollupDesc().getLevels()) {
                    Object groupKey = level.computeSubkey(groupKeyComplete);
                    groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    if (outputLimitGroupRepsPerLevel[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                        if (processor.isSelectRStream()) {
                            processor.generateOutputBatchedMapUnsorted(false, groupKey, level, eventsPerStream, true, isGenerateSynthetic, groupRepsOutputLastUnordRStream[level.getLevelNumber()]);
                        }
                    }
                }
                processor.getAggregationService().applyEnter(eventsPerStream, groupKeysPerLevel, processor.getExprEvaluatorContext());
            }
        }
        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                eventsPerStream = new EventBean[]{anOldData};
                Object groupKeyComplete = processor.generateGroupKeySingle(eventsPerStream, false);
                for (AggregationGroupByRollupLevel level : processor.getGroupByRollupDesc().getLevels()) {
                    Object groupKey = level.computeSubkey(groupKeyComplete);
                    groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    if (outputLimitGroupRepsPerLevel[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                        if (processor.isSelectRStream()) {
                            processor.generateOutputBatchedMapUnsorted(true, groupKey, level, eventsPerStream, false, isGenerateSynthetic, groupRepsOutputLastUnordRStream[level.getLevelNumber()]);
                        }
                    }
                }
                processor.getAggregationService().applyLeave(eventsPerStream, groupKeysPerLevel, processor.getExprEvaluatorContext());
            }
        }
    }

    public void processJoin(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, boolean isGenerateSynthetic) {
        generateRemoveStreamJustOnce(isGenerateSynthetic, true);

        // apply to aggregates
        Object[] groupKeysPerLevel = new Object[processor.getGroupByRollupDesc().getLevels().length];
        if (newEvents != null) {
            for (MultiKeyArrayOfKeys<EventBean> newEvent : newEvents) {
                EventBean[] aNewData = newEvent.getArray();
                Object groupKeyComplete = processor.generateGroupKeySingle(aNewData, true);
                for (AggregationGroupByRollupLevel level : processor.getGroupByRollupDesc().getLevels()) {
                    Object groupKey = level.computeSubkey(groupKeyComplete);
                    groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    if (outputLimitGroupRepsPerLevel[level.getLevelNumber()].put(groupKey, aNewData) == null) {
                        if (processor.isSelectRStream()) {
                            processor.generateOutputBatchedMapUnsorted(false, groupKey, level, aNewData, true, isGenerateSynthetic, groupRepsOutputLastUnordRStream[level.getLevelNumber()]);
                        }
                    }
                }
                processor.getAggregationService().applyEnter(aNewData, groupKeysPerLevel, processor.getExprEvaluatorContext());
            }
        }
        if (oldEvents != null) {
            for (MultiKeyArrayOfKeys<EventBean> oldEvent : oldEvents) {
                EventBean[] aOldData = oldEvent.getArray();
                Object groupKeyComplete = processor.generateGroupKeySingle(aOldData, false);
                for (AggregationGroupByRollupLevel level : processor.getGroupByRollupDesc().getLevels()) {
                    Object groupKey = level.computeSubkey(groupKeyComplete);
                    groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    if (outputLimitGroupRepsPerLevel[level.getLevelNumber()].put(groupKey, aOldData) == null) {
                        if (processor.isSelectRStream()) {
                            processor.generateOutputBatchedMapUnsorted(true, groupKey, level, aOldData, false, isGenerateSynthetic, groupRepsOutputLastUnordRStream[level.getLevelNumber()]);
                        }
                    }
                }
                processor.getAggregationService().applyLeave(aOldData, groupKeysPerLevel, processor.getExprEvaluatorContext());
            }
        }
    }

    public UniformPair<EventBean[]> outputView(boolean isSynthesize) {
        generateRemoveStreamJustOnce(isSynthesize, false);
        return output(isSynthesize, false);
    }

    public UniformPair<EventBean[]> outputJoin(boolean isSynthesize) {
        generateRemoveStreamJustOnce(isSynthesize, true);
        return output(isSynthesize, true);
    }

    public void destroy() {
        // no action required
    }

    private UniformPair<EventBean[]> output(boolean isSynthesize, boolean isJoin) {

        List<EventBean> newEvents = new ArrayList<>(4);
        for (AggregationGroupByRollupLevel level : processor.getGroupByRollupDesc().getLevels()) {
            Map<Object, EventBean[]> groupGenerators = outputLimitGroupRepsPerLevel[level.getLevelNumber()];
            for (Map.Entry<Object, EventBean[]> entry : groupGenerators.entrySet()) {
                processor.generateOutputBatched(entry.getKey(), level, entry.getValue(), true, isSynthesize, newEvents, null);
            }
        }
        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);

        EventBean[] oldEventsArr = null;
        if (processor.isSelectRStream()) {
            List<EventBean> oldEventList = new ArrayList<>(4);
            for (Map<Object, EventBean> entry : groupRepsOutputLastUnordRStream) {
                oldEventList.addAll(entry.values());
                entry.clear();
            }
            if (!oldEventList.isEmpty()) {
                oldEventsArr = oldEventList.toArray(new EventBean[oldEventList.size()]);
            }
        }

        first = true;

        if (newEventsArr == null && oldEventsArr == null) {
            return null;
        }
        return new UniformPair<>(newEventsArr, oldEventsArr);
    }

    private void generateRemoveStreamJustOnce(boolean isSynthesize, boolean join) {
        if (first && processor.isSelectRStream()) {
            for (AggregationGroupByRollupLevel level : processor.getGroupByRollupDesc().getLevels()) {
                for (Map.Entry<Object, EventBean[]> groupRep : outputLimitGroupRepsPerLevel[level.getLevelNumber()].entrySet()) {
                    Object groupKeyPartial = processor.generateGroupKeySingle(groupRep.getValue(), false);
                    Object groupKey = level.computeSubkey(groupKeyPartial);
                    processor.generateOutputBatchedMapUnsorted(join, groupKey, level, groupRep.getValue(), false, isSynthesize, groupRepsOutputLastUnordRStream[level.getLevelNumber()]);
                }
            }
        }
        first = false;
    }
}
