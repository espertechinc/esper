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
import java.util.List;

/**
 * Base for states.
 */
public abstract class RegexNFAStateBase implements RegexNFAState {
    private final String nodeNumNested;
    private final String variableName;
    private final int streamNum;
    private final boolean multiple;
    private final List<RegexNFAState> nextStates;
    private final Boolean isGreedy;

    private int nodeNumFlat;

    /**
     * Ctor.
     *
     * @param nodeNum      node num
     * @param variableName variable
     * @param streamNum    stream num
     * @param multiple     indicator
     * @param isGreedy     greedy indicator
     */
    public RegexNFAStateBase(String nodeNum, String variableName, int streamNum, boolean multiple, Boolean isGreedy) {
        this.nodeNumNested = nodeNum;
        this.variableName = variableName;
        this.streamNum = streamNum;
        this.multiple = multiple;
        this.isGreedy = isGreedy;
        nextStates = new ArrayList<RegexNFAState>();
    }

    public int getNodeNumFlat() {
        return nodeNumFlat;
    }

    /**
     * Assign a node number.
     *
     * @param nodeNumFlat flat number
     */
    public void setNodeNumFlat(int nodeNumFlat) {
        this.nodeNumFlat = nodeNumFlat;
    }

    public String getNodeNumNested() {
        return nodeNumNested;
    }

    public List<RegexNFAState> getNextStates() {
        return nextStates;
    }

    /**
     * Add a next state.
     *
     * @param next state to add
     */
    public void addState(RegexNFAState next) {
        nextStates.add(next);
    }

    public boolean isMultiple() {
        return multiple;
    }

    public String getVariableName() {
        return variableName;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public Boolean isGreedy() {
        return isGreedy;
    }
}
