/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;

import java.util.ArrayList;

/**
 * State for when no partitions (single partition) is required.
 */
public class RegexPartitionStateRepoNoGroup implements RegexPartitionStateRepo
{
    private final RegexPartitionStateImpl singletonState;

    /**
     * Ctor.
     * @param singletonState state
     */
    public RegexPartitionStateRepoNoGroup(RegexPartitionStateImpl singletonState)
    {
        this.singletonState = singletonState;
    }

    /**
     * Ctor.
     * @param getter "prev" getter
     */
    public RegexPartitionStateRepoNoGroup(RegexPartitionStateRandomAccessGetter getter)
    {
        singletonState = new RegexPartitionStateImpl(getter, new ArrayList<RegexNFAStateEntry>());
    }

    public void removeState(Object partitionKey) {
        // not an operation
    }

    /**
     * Copy state for iteration.
     * @return copy
     */
    public RegexPartitionStateRepo copyForIterate()
    {
        RegexPartitionStateImpl state = new RegexPartitionStateImpl(singletonState.getRandomAccess(), null);
        return new RegexPartitionStateRepoNoGroup(state);
    }

    public void removeOld(EventBean[] oldEvents, boolean isEmpty, boolean[] found)
    {
        if (isEmpty)
        {
            singletonState.clearCurrentStates();
        }
        else
        {
            for (EventBean oldEvent : oldEvents)
            {
                singletonState.removeEventFromState(oldEvent);
            }
        }
        singletonState.removeEventFromPrev(oldEvents);
    }

    public RegexPartitionState getState(EventBean theEvent, boolean collect)
    {
        return singletonState;
    }

    public RegexPartitionState getState(Object key)
    {
        return singletonState;
    }

    public void accept(EventRowRegexNFAViewServiceVisitor visitor) {
        visitor.visitUnpartitioned(singletonState);
    }

    public boolean isPartitioned() {
        return false;
    }
}