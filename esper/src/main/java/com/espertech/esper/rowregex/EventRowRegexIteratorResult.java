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

import java.util.List;

/**
 * Iteration result for row regex.
 */
public class EventRowRegexIteratorResult {
    private List<RegexNFAStateEntry> endStates;
    private int eventSequenceNum;

    /**
     * Ctor.
     *
     * @param endStates        end states
     * @param eventSequenceNum seq num of event
     */
    public EventRowRegexIteratorResult(List<RegexNFAStateEntry> endStates, int eventSequenceNum) {
        this.endStates = endStates;
        this.eventSequenceNum = eventSequenceNum;
    }

    /**
     * Returns the end states
     *
     * @return end states
     */
    public List<RegexNFAStateEntry> getEndStates() {
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
