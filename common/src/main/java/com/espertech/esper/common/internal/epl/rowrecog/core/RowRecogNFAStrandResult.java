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

import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateForge;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateForgeBase;

import java.util.List;

/**
 * A result of computing a strand of one or more NFA states that has a list of start states and a list of all states in the strand.
 */
public class RowRecogNFAStrandResult {
    private List<RowRecogNFAStateForge> startStates;
    private List<RowRecogNFAStateForgeBase> allStates;

    /**
     * Ctor.
     *
     * @param startStates NFA start states
     * @param allStates   all states
     */
    public RowRecogNFAStrandResult(List<RowRecogNFAStateForge> startStates, List<RowRecogNFAStateForgeBase> allStates) {
        this.startStates = startStates;
        this.allStates = allStates;
    }

    /**
     * Returns start states.
     *
     * @return start states
     */
    public List<RowRecogNFAStateForge> getStartStates() {
        return startStates;
    }

    /**
     * Returns all states.
     *
     * @return all states
     */
    public List<RowRecogNFAStateForgeBase> getAllStates() {
        return allStates;
    }
}
