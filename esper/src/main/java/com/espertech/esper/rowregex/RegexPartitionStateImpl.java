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
import java.util.Iterator;
import java.util.List;

/**
 * All current state holding partial NFA matches.
 */
public class RegexPartitionStateImpl implements RegexPartitionState {
    private RegexPartitionStateRandomAccess randomAccess;
    private List<RegexNFAStateEntry> currentStates = new ArrayList<RegexNFAStateEntry>();
    private Object optionalKeys;

    /**
     * Ctor.
     *
     * @param randomAccess for handling "prev" functions, if any
     * @param optionalKeys keys for "partition", if any
     */
    public RegexPartitionStateImpl(RegexPartitionStateRandomAccess randomAccess, Object optionalKeys) {
        this.randomAccess = randomAccess;
        this.optionalKeys = optionalKeys;
    }

    /**
     * Ctor.
     *
     * @param getter        for "prev" access
     * @param currentStates existing state
     */
    public RegexPartitionStateImpl(RegexPartitionStateRandomAccessGetter getter,
                                   List<RegexNFAStateEntry> currentStates) {
        this(getter, currentStates, null);
    }

    /**
     * Ctor.
     *
     * @param getter        for "prev" access
     * @param currentStates existing state
     * @param optionalKeys  partition keys if any
     */
    public RegexPartitionStateImpl(RegexPartitionStateRandomAccessGetter getter,
                                   List<RegexNFAStateEntry> currentStates,
                                   Object optionalKeys) {
        if (getter != null) {
            randomAccess = new RegexPartitionStateRandomAccessImpl(getter);
        }
        this.currentStates = currentStates;
        this.optionalKeys = optionalKeys;
    }

    /**
     * Returns the random access for "prev".
     *
     * @return access
     */
    public RegexPartitionStateRandomAccess getRandomAccess() {
        return randomAccess;
    }

    /**
     * Returns partial matches.
     *
     * @return state
     */
    public Iterator<RegexNFAStateEntry> getCurrentStatesIterator() {
        return currentStates.iterator();
    }

    /**
     * Sets partial matches.
     *
     * @param currentStates state to set
     */
    public void setCurrentStates(List<RegexNFAStateEntry> currentStates) {
        this.currentStates = currentStates;
    }

    /**
     * Returns partition keys, if any.
     *
     * @return keys
     */
    public Object getOptionalKeys() {
        return optionalKeys;
    }

    /**
     * Remove an event from random access for "prev".
     *
     * @param oldEvents to remove
     */
    public void removeEventFromPrev(EventBean[] oldEvents) {
        if (randomAccess != null) {
            randomAccess.remove(oldEvents);
        }
    }

    /**
     * Remove an event from random access for "prev".
     *
     * @param oldEvent to remove
     */
    public void removeEventFromPrev(EventBean oldEvent) {
        if (randomAccess != null) {
            randomAccess.remove(oldEvent);
        }
    }

    /**
     * Remove an event from state.
     *
     * @param oldEvent to remove
     * @return true for removed, false for not found
     */
    public int removeEventFromState(EventBean oldEvent) {
        int currentSize = currentStates.size();
        List<RegexNFAStateEntry> keepList = removeEventFromState(oldEvent, currentStates.iterator());
        if (randomAccess != null) {
            randomAccess.remove(oldEvent);
        }
        currentStates = keepList;
        return currentSize - keepList.size();
    }

    public int getNumStates() {
        return currentStates.size();
    }

    public void clearCurrentStates() {
        currentStates.clear();
    }

    public List<RegexNFAStateEntry> getCurrentStatesForPrint() {
        return currentStates;
    }

    public boolean isEmptyCurrentState() {
        return currentStates.isEmpty();
    }

    public static List<RegexNFAStateEntry> removeEventFromState(EventBean oldEvent, Iterator<RegexNFAStateEntry> states) {
        List<RegexNFAStateEntry> keepList = new ArrayList<RegexNFAStateEntry>();
        for (; states.hasNext(); ) {
            RegexNFAStateEntry entry = states.next();
            boolean keep = true;

            EventBean[] state = entry.getEventsPerStream();
            for (EventBean aState : state) {
                if (aState != null && aState.equals(oldEvent)) {
                    keep = false;
                    break;
                }
            }

            if (keep) {
                MultimatchState[] multimatch = entry.getOptionalMultiMatches();
                if (multimatch != null) {
                    for (MultimatchState aMultimatch : multimatch) {
                        if ((aMultimatch != null) && (aMultimatch.containsEvent(oldEvent))) {
                            keep = false;
                            break;
                        }
                    }
                }
            }

            if (keep) {
                keepList.add(entry);
            }
        }
        return keepList;
    }
}
