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

import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPartitionTerminationStateComparator;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class RowRecogPartitionStateRepoScheduleStateImpl implements RowRecogPartitionStateRepoScheduleState {

    private final RowRecogPartitionTerminationStateComparator terminationStateCompare;
    private final TreeMap<Long, Object> schedule = new TreeMap<Long, Object>();

    public RowRecogPartitionStateRepoScheduleStateImpl(RowRecogPartitionTerminationStateComparator terminationStateCompare) {
        this.terminationStateCompare = terminationStateCompare;
    }

    public boolean isEmpty() {
        return schedule.isEmpty();
    }

    public boolean putOrAdd(long matchBeginTime, RowRecogNFAStateEntry state) {
        Object value = schedule.get(matchBeginTime);
        if (value == null) {
            schedule.put(matchBeginTime, state);
            return true;
        }

        if (value instanceof RowRecogNFAStateEntry) {
            RowRecogNFAStateEntry valueEntry = (RowRecogNFAStateEntry) value;
            List<RowRecogNFAStateEntry> list = new ArrayList<RowRecogNFAStateEntry>();
            list.add(valueEntry);
            list.add(state);
            schedule.put(matchBeginTime, list);
        } else {
            List<RowRecogNFAStateEntry> list = (List<RowRecogNFAStateEntry>) value;
            list.add(state);
        }

        return false;
    }

    public Object get(long matchBeginTime) {
        return schedule.get(matchBeginTime);
    }

    public long firstKey() {
        return schedule.firstKey();
    }

    public void removeAddRemoved(long matchBeginTime, List<RowRecogNFAStateEntry> foundStates) {
        Object found = schedule.remove(matchBeginTime);
        if (found == null) {
            return;
        }
        if (found instanceof RowRecogNFAStateEntry) {
            foundStates.add((RowRecogNFAStateEntry) found);
        } else {
            foundStates.addAll((List<RowRecogNFAStateEntry>) found);
        }
    }

    public boolean containsKey(long matchBeginTime) {
        return schedule.containsKey(matchBeginTime);
    }

    public boolean findRemoveAddToList(long matchBeginTime, RowRecogNFAStateEntry state, List<RowRecogNFAStateEntry> foundStates) {
        Object entry = schedule.get(matchBeginTime);
        if (entry == null) {
            return false;
        }
        if (entry instanceof RowRecogNFAStateEntry) {
            RowRecogNFAStateEntry single = (RowRecogNFAStateEntry) entry;
            if (terminationStateCompare.compareTerminationStateToEndState(state, single)) {
                schedule.remove(matchBeginTime);
                foundStates.add(single);
                return true;
            }
            return false;
        }

        List<RowRecogNFAStateEntry> entries = (List<RowRecogNFAStateEntry>) entry;
        Iterator<RowRecogNFAStateEntry> it = entries.iterator();
        boolean removed = false;
        for (; it.hasNext(); ) {
            RowRecogNFAStateEntry endState = it.next();
            if (terminationStateCompare.compareTerminationStateToEndState(state, endState)) {
                it.remove();
                foundStates.add(endState);
                removed = true;
            }
        }
        if (entries.isEmpty()) {
            schedule.remove(matchBeginTime);
        }
        return removed;
    }
}
