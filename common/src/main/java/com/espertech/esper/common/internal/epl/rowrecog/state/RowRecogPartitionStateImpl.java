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
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogMultimatchState;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategyImpl;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * All current state holding partial NFA matches.
 */
public class RowRecogPartitionStateImpl implements RowRecogPartitionState {
    private RowRecogStateRandomAccess randomAccess;
    private List<RowRecogNFAStateEntry> currentStates = new ArrayList<RowRecogNFAStateEntry>();
    private Object optionalKeys;

    /**
     * Ctor.
     *
     * @param randomAccess for handling "prev" functions, if any
     * @param optionalKeys keys for "partition", if any
     */
    public RowRecogPartitionStateImpl(RowRecogStateRandomAccess randomAccess, Object optionalKeys) {
        this.randomAccess = randomAccess;
        this.optionalKeys = optionalKeys;
    }

    /**
     * Ctor.
     *
     * @param getter        for "prev" access
     * @param currentStates existing state
     */
    public RowRecogPartitionStateImpl(RowRecogPreviousStrategyImpl getter,
                                      List<RowRecogNFAStateEntry> currentStates) {
        this(getter, currentStates, null);
    }

    /**
     * Ctor.
     *
     * @param getter        for "prev" access
     * @param currentStates existing state
     * @param optionalKeys  partition keys if any
     */
    public RowRecogPartitionStateImpl(RowRecogPreviousStrategyImpl getter,
                                      List<RowRecogNFAStateEntry> currentStates,
                                      Object optionalKeys) {
        if (getter != null) {
            randomAccess = new RowRecogStateRandomAccessImpl(getter);
        }
        this.currentStates = currentStates;
        this.optionalKeys = optionalKeys;
    }

    /**
     * Returns the random access for "prev".
     *
     * @return access
     */
    public RowRecogStateRandomAccess getRandomAccess() {
        return randomAccess;
    }

    /**
     * Returns partial matches.
     *
     * @return state
     */
    public Iterator<RowRecogNFAStateEntry> getCurrentStatesIterator() {
        return currentStates.iterator();
    }

    /**
     * Sets partial matches.
     *
     * @param currentStates state to set
     */
    public void setCurrentStates(List<RowRecogNFAStateEntry> currentStates) {
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
        List<RowRecogNFAStateEntry> keepList = removeEventFromState(oldEvent, currentStates.iterator());
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

    public List<RowRecogNFAStateEntry> getCurrentStatesForPrint() {
        return currentStates;
    }

    public boolean isEmptyCurrentState() {
        return currentStates.isEmpty();
    }

    public static List<RowRecogNFAStateEntry> removeEventFromState(EventBean oldEvent, Iterator<RowRecogNFAStateEntry> states) {
        List<RowRecogNFAStateEntry> keepList = new ArrayList<RowRecogNFAStateEntry>();
        for (; states.hasNext(); ) {
            RowRecogNFAStateEntry entry = states.next();
            boolean keep = true;

            EventBean[] state = entry.getEventsPerStream();
            for (EventBean aState : state) {
                if (aState != null && aState.equals(oldEvent)) {
                    keep = false;
                    break;
                }
            }

            if (keep) {
                RowRecogMultimatchState[] multimatch = entry.getOptionalMultiMatches();
                if (multimatch != null) {
                    for (RowRecogMultimatchState aMultimatch : multimatch) {
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
