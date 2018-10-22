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
package com.espertech.esper.common.internal.epl.rowrecog.nfa;

/**
 * Base for states.
 */
public abstract class RowRecogNFAStateBase implements RowRecogNFAState {
    private String nodeNumNested;
    private String variableName;
    private int streamNum;
    private boolean multiple;
    private RowRecogNFAState[] nextStates;
    private Boolean greedy;
    private int nodeNumFlat;
    private boolean exprRequiresMultimatchState;

    public String getNodeNumNested() {
        return nodeNumNested;
    }

    public void setNodeNumNested(String nodeNumNested) {
        this.nodeNumNested = nodeNumNested;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public void setStreamNum(int streamNum) {
        this.streamNum = streamNum;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public RowRecogNFAState[] getNextStates() {
        return nextStates;
    }

    public void setNextStates(RowRecogNFAState[] nextStates) {
        this.nextStates = nextStates;
    }

    public Boolean isGreedy() {
        return greedy;
    }

    public void setGreedy(Boolean greedy) {
        this.greedy = greedy;
    }

    public int getNodeNumFlat() {
        return nodeNumFlat;
    }

    public void setNodeNumFlat(int nodeNumFlat) {
        this.nodeNumFlat = nodeNumFlat;
    }

    public boolean isExprRequiresMultimatchState() {
        return exprRequiresMultimatchState;
    }

    public void setExprRequiresMultimatchState(boolean exprRequiresMultimatchState) {
        this.exprRequiresMultimatchState = exprRequiresMultimatchState;
    }
}
