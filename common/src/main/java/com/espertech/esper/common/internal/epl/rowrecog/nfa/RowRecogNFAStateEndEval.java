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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;

/**
 * End state in the regex NFA states.
 */
public class RowRecogNFAStateEndEval extends RowRecogNFAStateBase {
    private final static RowRecogNFAState[] EMPTY_ARRAY = new RowRecogNFAState[0];

    public RowRecogNFAStateEndEval() {
        setNodeNumFlat(-1);
        setStreamNum(-1);
        setNodeNumNested("end-state");
    }

    public boolean matches(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext) {
        throw new UnsupportedOperationException();
    }

    public RowRecogNFAState[] getNextStates() {
        return EMPTY_ARRAY;
    }

    @Override
    public boolean isExprRequiresMultimatchState() {
        throw new UnsupportedOperationException();
    }
}
