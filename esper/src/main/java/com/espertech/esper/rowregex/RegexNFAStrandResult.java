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
 * A result of computing a strand of one or more NFA states that has a list of start states and a list of all states in the strand.
 */
public class RegexNFAStrandResult {
    private List<RegexNFAState> startStates;
    private List<RegexNFAStateBase> allStates;

    /**
     * Ctor.
     *
     * @param startStates NFA start states
     * @param allStates   all states
     */
    public RegexNFAStrandResult(List<RegexNFAState> startStates, List<RegexNFAStateBase> allStates) {
        this.startStates = startStates;
        this.allStates = allStates;
    }

    /**
     * Returns start states.
     *
     * @return start states
     */
    public List<RegexNFAState> getStartStates() {
        return startStates;
    }

    /**
     * Returns all states.
     *
     * @return all states
     */
    public List<RegexNFAStateBase> getAllStates() {
        return allStates;
    }
}
