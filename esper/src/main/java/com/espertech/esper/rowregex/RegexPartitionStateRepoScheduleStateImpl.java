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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class RegexPartitionStateRepoScheduleStateImpl implements RegexPartitionStateRepoScheduleState {

    private final RegexPartitionTerminationStateComparator terminationStateCompare;
    private final TreeMap<Long, Object> schedule = new TreeMap<Long, Object>();

    public RegexPartitionStateRepoScheduleStateImpl(RegexPartitionTerminationStateComparator terminationStateCompare) {
        this.terminationStateCompare = terminationStateCompare;
    }

    public boolean isEmpty() {
        return schedule.isEmpty();
    }

    public boolean putOrAdd(long matchBeginTime, RegexNFAStateEntry state) {
        Object value = schedule.get(matchBeginTime);
        if (value == null) {
            schedule.put(matchBeginTime, state);
            return true;
        }

        if (value instanceof RegexNFAStateEntry) {
            RegexNFAStateEntry valueEntry = (RegexNFAStateEntry) value;
            List<RegexNFAStateEntry> list = new ArrayList<RegexNFAStateEntry>();
            list.add(valueEntry);
            list.add(state);
            schedule.put(matchBeginTime, list);
        } else {
            List<RegexNFAStateEntry> list = (List<RegexNFAStateEntry>) value;
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

    public void removeAddRemoved(long matchBeginTime, List<RegexNFAStateEntry> foundStates) {
        Object found = schedule.remove(matchBeginTime);
        if (found == null) {
            return;
        }
        if (found instanceof RegexNFAStateEntry) {
            foundStates.add((RegexNFAStateEntry) found);
        } else {
            foundStates.addAll((List<RegexNFAStateEntry>) found);
        }
    }

    public boolean containsKey(long matchBeginTime) {
        return schedule.containsKey(matchBeginTime);
    }

    public boolean findRemoveAddToList(long matchBeginTime, RegexNFAStateEntry state, List<RegexNFAStateEntry> foundStates) {
        Object entry = schedule.get(matchBeginTime);
        if (entry == null) {
            return false;
        }
        if (entry instanceof RegexNFAStateEntry) {
            RegexNFAStateEntry single = (RegexNFAStateEntry) entry;
            if (terminationStateCompare.compareTerminationStateToEndState(state, single)) {
                schedule.remove(matchBeginTime);
                foundStates.add(single);
                return true;
            }
            return false;
        }

        List<RegexNFAStateEntry> entries = (List<RegexNFAStateEntry>) entry;
        Iterator<RegexNFAStateEntry> it = entries.iterator();
        boolean removed = false;
        for (; it.hasNext(); ) {
            RegexNFAStateEntry endState = it.next();
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
