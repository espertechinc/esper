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

import java.util.ArrayList;
import java.util.Collections;

/**
 * State for when no partitions (single partition) is required.
 */
public class RegexPartitionStateRepoNoGroup implements RegexPartitionStateRepo {
    private final RegexPartitionStateImpl singletonState;
    private final RegexPartitionStateRepoScheduleStateImpl optionalIntervalSchedules;
    private int eventSequenceNumber;

    /**
     * Ctor.
     *
     * @param singletonState state
     */
    public RegexPartitionStateRepoNoGroup(RegexPartitionStateImpl singletonState) {
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

    public RegexPartitionStateRepoScheduleState getScheduleState() {
        return optionalIntervalSchedules;
    }

    public RegexPartitionStateRepoNoGroup(RegexPartitionStateRandomAccessGetter getter, boolean keepScheduleState, RegexPartitionTerminationStateComparator terminationStateCompare) {
        singletonState = new RegexPartitionStateImpl(getter, new ArrayList<RegexNFAStateEntry>());
        optionalIntervalSchedules = keepScheduleState ? new RegexPartitionStateRepoScheduleStateImpl(terminationStateCompare) : null;
    }

    public void removeState(Object partitionKey) {
        // not an operation
    }

    /**
     * Copy state for iteration.
     *
     * @return copy
     */
    public RegexPartitionStateRepo copyForIterate(boolean forOutOfOrderReprocessing) {
        RegexPartitionStateImpl state = new RegexPartitionStateImpl(singletonState.getRandomAccess(), null);
        return new RegexPartitionStateRepoNoGroup(state);
    }

    public int removeOld(EventBean[] oldEvents, boolean isEmpty, boolean[] found) {
        int countRemoved = 0;
        if (isEmpty) {
            countRemoved = singletonState.getNumStates();
            singletonState.setCurrentStates(Collections.<RegexNFAStateEntry>emptyList());
        } else {
            for (EventBean oldEvent : oldEvents) {
                countRemoved += singletonState.removeEventFromState(oldEvent);
            }
        }
        singletonState.removeEventFromPrev(oldEvents);
        return countRemoved;
    }

    public RegexPartitionState getState(EventBean theEvent, boolean collect) {
        return singletonState;
    }

    public RegexPartitionState getState(Object key) {
        return singletonState;
    }

    public void accept(EventRowRegexNFAViewServiceVisitor visitor) {
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