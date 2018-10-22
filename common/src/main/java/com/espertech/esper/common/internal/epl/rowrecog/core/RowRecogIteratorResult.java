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
package com.espertech.esper.common.internal.epl.rowrecog.core;

import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEntry;

import java.util.List;

/**
 * Iteration result for row regex.
 */
public class RowRecogIteratorResult {
    private List<RowRecogNFAStateEntry> endStates;
    private int eventSequenceNum;

    /**
     * Ctor.
     *
     * @param endStates        end states
     * @param eventSequenceNum seq num of event
     */
    public RowRecogIteratorResult(List<RowRecogNFAStateEntry> endStates, int eventSequenceNum) {
        this.endStates = endStates;
        this.eventSequenceNum = eventSequenceNum;
    }

    /**
     * Returns the end states
     *
     * @return end states
     */
    public List<RowRecogNFAStateEntry> getEndStates() {
        return endStates;
    }

    /**
     * Returns the event seq num.
     *
     * @return seq num
     */
    public int getEventSequenceNum() {
        return eventSequenceNum;
    }
}
