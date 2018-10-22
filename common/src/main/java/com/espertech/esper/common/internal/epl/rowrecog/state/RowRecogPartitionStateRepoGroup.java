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
package com.espertech.esper.common.internal.epl.rowrecog.state;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogNFAViewServiceVisitor;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPartitionTerminationStateComparator;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategyImpl;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEntry;

import java.util.*;

/**
 * Partition-by implementation for partition state.
 */
public class RowRecogPartitionStateRepoGroup implements RowRecogPartitionStateRepo {
    /**
     * Empty state collection initial threshold.
     */
    public final static int INITIAL_COLLECTION_MIN = 100;

    private final RowRecogPartitionStateRepoGroupMeta meta;
    private final RowRecogPreviousStrategyImpl getter;
    private final Map<Object, RowRecogPartitionStateImpl> states;
    private final RowRecogPartitionStateRepoScheduleStateImpl optionalIntervalSchedules;

    private int currentCollectionSize = INITIAL_COLLECTION_MIN;
    private int eventSequenceNumber;

    public RowRecogPartitionStateRepoGroup(RowRecogPreviousStrategyImpl getter,
                                           RowRecogPartitionStateRepoGroupMeta meta,
                                           boolean keepScheduleState,
                                           RowRecogPartitionTerminationStateComparator terminationStateCompare) {
        this.getter = getter;
        this.meta = meta;
        this.states = new HashMap<Object, RowRecogPartitionStateImpl>();
        this.optionalIntervalSchedules = keepScheduleState ? new RowRecogPartitionStateRepoScheduleStateImpl(terminationStateCompare) : null;
    }

    public int incrementAndGetEventSequenceNum() {
        ++eventSequenceNumber;
        return eventSequenceNumber;
    }

    public void setEventSequenceNum(int num) {
        this.eventSequenceNumber = num;
    }

    public RowRecogPartitionStateRepoScheduleState getScheduleState() {
        return optionalIntervalSchedules;
    }

    public void removeState(Object partitionKey) {
        states.remove(partitionKey);
    }

    public RowRecogPartitionStateRepo copyForIterate(boolean forOutOfOrderReprocessing) {
        RowRecogPartitionStateRepoGroup copy = new RowRecogPartitionStateRepoGroup(getter, meta, false, null);
        for (Map.Entry<Object, RowRecogPartitionStateImpl> entry : states.entrySet()) {
            copy.states.put(entry.getKey(), new RowRecogPartitionStateImpl(entry.getValue().getRandomAccess(), entry.getKey()));
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
                for (Map.Entry<Object, RowRecogPartitionStateImpl> entry : states.entrySet()) {
                    countRemoved += entry.getValue().getNumStates();
                    entry.getValue().setCurrentStates(Collections.<RowRecogNFAStateEntry>emptyList());
                }
            }

            // clear "prev" state
            if (getter != null) {
                // we will need to remove event-by-event
                for (int i = 0; i < oldData.length; i++) {
                    RowRecogPartitionStateImpl partitionState = getState(oldData[i], true);
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
            RowRecogPartitionStateImpl partitionState = getState(oldData[i], true);
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

    public RowRecogPartitionState getState(Object key) {
        return states.get(key);
    }

    public RowRecogPartitionStateImpl getState(EventBean theEvent, boolean isCollect) {
        meta.getAgentInstanceContext().getInstrumentationProvider().qRegExPartition(theEvent);

        // collect unused states
        if (isCollect && (states.size() >= currentCollectionSize)) {
            List<Object> removeList = new ArrayList<Object>();
            for (Map.Entry<Object, RowRecogPartitionStateImpl> entry : states.entrySet()) {
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

        RowRecogPartitionStateImpl state = states.get(key);
        if (state != null) {
            meta.getAgentInstanceContext().getInstrumentationProvider().aRegExPartition(true, key, state);
            return state;
        }

        state = new RowRecogPartitionStateImpl(getter, new ArrayList<RowRecogNFAStateEntry>(), key);
        states.put(key, state);

        meta.getAgentInstanceContext().getInstrumentationProvider().aRegExPartition(false, key, state);
        return state;
    }

    public void accept(RowRecogNFAViewServiceVisitor visitor) {
        visitor.visitPartitioned((Map) states);
    }

    public boolean isPartitioned() {
        return true;
    }

    public Map<Object, RowRecogPartitionStateImpl> getStates() {
        return states;
    }

    public int getStateCount() {
        int total = 0;
        for (Map.Entry<Object, RowRecogPartitionStateImpl> entry : states.entrySet()) {
            total += entry.getValue().getNumStates();
        }
        return total;
    }

    public static Object getKeys(EventBean theEvent, RowRecogPartitionStateRepoGroupMeta meta) {
        EventBean[] eventsPerStream = meta.getEventsPerStream();
        eventsPerStream[0] = theEvent;
        return meta.getPartitionExpression().evaluate(eventsPerStream, true, meta.getAgentInstanceContext());
    }

    public void destroy() {
    }
}