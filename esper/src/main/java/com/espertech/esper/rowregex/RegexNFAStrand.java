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
 * A strand of one or more NFA states that has a list of start states, end states and a list of all states in the strand.
 */
public class RegexNFAStrand {
    private List<RegexNFAStateBase> startStates;
    private List<RegexNFAStateBase> endStates;
    private List<RegexNFAStateBase> allStates;
    private boolean isPassthrough;

    /**
     * Ctor.
     *
     * @param startStates start states
     * @param endStates   end states
     * @param allStates   all states
     * @param passthrough true if this strand passes through (zero-or-more multiplicity for all NFA in strand)
     */
    public RegexNFAStrand(List<RegexNFAStateBase> startStates, List<RegexNFAStateBase> endStates, List<RegexNFAStateBase> allStates, boolean passthrough) {
        this.startStates = startStates;
        this.endStates = endStates;
        this.allStates = allStates;
        isPassthrough = passthrough;
    }

    /**
     * Returns the start states.
     *
     * @return start states
     */
    public List<RegexNFAStateBase> getStartStates() {
        return startStates;
    }

    /**
     * Returns the end states.
     *
     * @return end states
     */
    public List<RegexNFAStateBase> getEndStates() {
        return endStates;
    }

    /**
     * Returns all states.
     *
     * @return all states
     */
    public List<RegexNFAStateBase> getAllStates() {
        return allStates;
    }

    /**
     * Returns indicator if passing-through (zero-or-more multiplicity for all NFA states in strand).
     *
     * @return pass-through
     */
    public boolean isPassthrough() {
        return isPassthrough;
    }
}
