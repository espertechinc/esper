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

import java.util.ArrayList;
import java.util.Collections;

/**
 * State for when no partitions (single partition) is required.
 */
public class RowRecogPartitionStateRepoNoGroup implements RowRecogPartitionStateRepo {
    private final RowRecogPartitionStateImpl singletonState;
    private final RowRecogPartitionStateRepoScheduleStateImpl optionalIntervalSchedules;
    private int eventSequenceNumber;

    /**
     * Ctor.
     *
     * @param singletonState state
     */
    public RowRecogPartitionStateRepoNoGroup(RowRecogPartitionStateImpl singletonState) {
        this.singletonState = singletonState;
        this.optionalIntervalSchedules = null;
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

    public RowRecogPartitionStateRepoNoGroup(RowRecogPreviousStrategyImpl getter, boolean keepScheduleState, RowRecogPartitionTerminationStateComparator terminationStateCompare) {
        singletonState = new RowRecogPartitionStateImpl(getter, new ArrayList<RowRecogNFAStateEntry>());
        optionalIntervalSchedules = keepScheduleState ? new RowRecogPartitionStateRepoScheduleStateImpl(terminationStateCompare) : null;
    }

    public void removeState(Object partitionKey) {
        // not an operation
    }

    /**
     * Copy state for iteration.
     *
     * @return copy
     */
    public RowRecogPartitionStateRepo copyForIterate(boolean forOutOfOrderReprocessing) {
        RowRecogPartitionStateImpl state = new RowRecogPartitionStateImpl(singletonState.getRandomAccess(), null);
        return new RowRecogPartitionStateRepoNoGroup(state);
    }

    public int removeOld(EventBean[] oldEvents, boolean isEmpty, boolean[] found) {
        int countRemoved = 0;
        if (isEmpty) {
            countRemoved = singletonState.getNumStates();
            singletonState.setCurrentStates(Collections.<RowRecogNFAStateEntry>emptyList());
        } else {
            for (EventBean oldEvent : oldEvents) {
                countRemoved += singletonState.removeEventFromState(oldEvent);
            }
        }
        singletonState.removeEventFromPrev(oldEvents);
        return countRemoved;
    }

    public RowRecogPartitionState getState(EventBean theEvent, boolean collect) {
        return singletonState;
    }

    public RowRecogPartitionState getState(Object key) {
        return singletonState;
    }

    public void accept(RowRecogNFAViewServiceVisitor visitor) {
        visitor.visitUnpartitioned(singletonState);
    }

    public boolean isPartitioned() {
        return false;
    }

    public int getStateCount() {
        return singletonState.getNumStates();
    }

    public void destroy() {
    }
}