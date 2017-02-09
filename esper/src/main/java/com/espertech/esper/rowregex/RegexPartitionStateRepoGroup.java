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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;

/**
 * Partition-by implementation for partition state.
 */
public class RegexPartitionStateRepoGroup implements RegexPartitionStateRepo {
    /**
     * Empty state collection initial threshold.
     */
    public final static int INITIAL_COLLECTION_MIN = 100;

    private final RegexPartitionStateRepoGroupMeta meta;
    private final RegexPartitionStateRandomAccessGetter getter;
    private final Map<Object, RegexPartitionStateImpl> states;
    private final RegexPartitionStateRepoScheduleStateImpl optionalIntervalSchedules;

    private int currentCollectionSize = INITIAL_COLLECTION_MIN;
    private int eventSequenceNumber;

    public RegexPartitionStateRepoGroup(RegexPartitionStateRandomAccessGetter getter,
                                        RegexPartitionStateRepoGroupMeta meta,
                                        boolean keepScheduleState,
                                        RegexPartitionTerminationStateComparator terminationStateCompare) {
        this.getter = getter;
        this.meta = meta;
        this.states = new HashMap<Object, RegexPartitionStateImpl>();
        this.optionalIntervalSchedules = keepScheduleState ? new RegexPartitionStateRepoScheduleStateImpl(terminationStateCompare) : null;
    }

    public int incrementAndGetEventSequenceNum() {
        ++eventSequenceNumber;
        return eventSequenceNumber;
    }

    public void setEventSequenceNum(int num) {
        this.eventSequenceNumber = num;
    }

    public RegexPartitionStateRepoScheduleState getScheduleState() {
        return optionalIntervalSchedules;
    }

    public void removeState(Object partitionKey) {
        states.remove(partitionKey);
    }

    public RegexPartitionStateRepo copyForIterate(boolean forOutOfOrderReprocessing) {
        RegexPartitionStateRepoGroup copy = new RegexPartitionStateRepoGroup(getter, meta, false, null);
        for (Map.Entry<Object, RegexPartitionStateImpl> entry : states.entrySet()) {
            copy.states.put(entry.getKey(), new RegexPartitionStateImpl(entry.getValue().getRandomAccess(), entry.getKey()));
        }
        return copy;
    }

    public int removeOld(EventBean[] oldData, boolean isEmpty, boolean[] found) {
        if (isEmpty) {
            int countRemoved;
            if (getter == null) {
                // no "prev" used, clear all state
                countRemoved = getStateCount();
                states.clear();
            } else {
                countRemoved = 0;
                for (Map.Entry<Object, RegexPartitionStateImpl> entry : states.entrySet()) {
                    countRemoved += entry.getValue().getNumStates();
                    entry.getValue().setCurrentStates(Collections.<RegexNFAStateEntry>emptyList());
                }
            }

            // clear "prev" state
            if (getter != null) {
                // we will need to remove event-by-event
                for (int i = 0; i < oldData.length; i++) {
                    RegexPartitionStateImpl partitionState = getState(oldData[i], true);
                    if (partitionState == null) {
                        continue;
                    }
                    partitionState.removeEventFromPrev(oldData);
                }
            }

            return countRemoved;
        }

        // we will need to remove event-by-event
        int countRemoved = 0;
        for (int i = 0; i < oldData.length; i++) {
            RegexPartitionStateImpl partitionState = getState(oldData[i], true);
            if (partitionState == null) {
                continue;
            }

            if (found[i]) {
                countRemoved += partitionState.removeEventFromState(oldData[i]);
                boolean cleared = partitionState.getNumStates() == 0;
                if (cleared) {
                    if (getter == null) {
                        states.remove(partitionState.getOptionalKeys());
                    }
                }
            }

            partitionState.removeEventFromPrev(oldData[i]);
        }
        return countRemoved;
    }

    public RegexPartitionState getState(Object key) {
        return states.get(key);
    }

    public RegexPartitionStateImpl getState(EventBean theEvent, boolean isCollect) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qRegExPartition(meta.getPartitionExpressionNodes());
        }

        // collect unused states
        if (isCollect && (states.size() >= currentCollectionSize)) {
            List<Object> removeList = new ArrayList<Object>();
            for (Map.Entry<Object, RegexPartitionStateImpl> entry : states.entrySet()) {
                if ((entry.getValue().isEmptyCurrentState()) &&
                        (entry.getValue().getRandomAccess() == null || entry.getValue().getRandomAccess().isEmpty())) {
                    removeList.add(entry.getKey());
                }
            }

            for (Object removeKey : removeList) {
                states.remove(removeKey);
            }

            if (removeList.size() < (currentCollectionSize / 5)) {
                currentCollectionSize *= 2;
            }
        }

        Object key = getKeys(theEvent, meta);

        RegexPartitionStateImpl state = states.get(key);
        if (state != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aRegExPartition(true, state);
            }
            return state;
        }

        state = new RegexPartitionStateImpl(getter, new ArrayList<RegexNFAStateEntry>(), key);
        states.put(key, state);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aRegExPartition(false, state);
        }
        return state;
    }

    public void accept(EventRowRegexNFAViewServiceVisitor visitor) {
        visitor.visitPartitioned((Map) states);
    }

    public boolean isPartitioned() {
        return true;
    }

    public Map<Object, RegexPartitionStateImpl> getStates() {
        return states;
    }

    public int getStateCount() {
        int total = 0;
        for (Map.Entry<Object, RegexPartitionStateImpl> entry : states.entrySet()) {
            total += entry.getValue().getNumStates();
        }
        return total;
    }

    public static Object getKeys(EventBean theEvent, RegexPartitionStateRepoGroupMeta meta) {
        EventBean[] eventsPerStream = meta.getEventsPerStream();
        eventsPerStream[0] = theEvent;

        ExprEvaluator[] partitionExpressions = meta.getPartitionExpressions();
        if (partitionExpressions.length == 1) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprValue(meta.getPartitionExpressionNodes()[0], eventsPerStream);
                Object value = partitionExpressions[0].evaluate(eventsPerStream, true, meta.getExprEvaluatorContext());
                InstrumentationHelper.get().aExprValue(value);
                return value;
            } else {
                return partitionExpressions[0].evaluate(eventsPerStream, true, meta.getExprEvaluatorContext());
            }
        }

        Object[] keys = new Object[partitionExpressions.length];
        int count = 0;
        ExprEvaluatorContext exprEvaluatorContext = meta.getExprEvaluatorContext();
        for (ExprEvaluator node : partitionExpressions) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprValue(meta.getPartitionExpressionNodes()[count], eventsPerStream);
            }
            keys[count] = node.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprValue(keys[count]);
            }
            count++;
        }
        return new MultiKeyUntyped(keys);
    }

    public void destroy() {
    }
}