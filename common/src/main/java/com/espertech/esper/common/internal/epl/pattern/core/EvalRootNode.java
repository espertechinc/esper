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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.filterspec.MatchedEventMapImpl;

/**
 * This class is always the root node in the evaluation tree representing an event expression.
 * It hold the handle to the EPStatement implementation for notifying when matches are found.
 */
public class EvalRootNode implements EvalNode {
    protected final EvalRootFactoryNode factoryNode;
    protected final EvalNode childNode;
    protected final AgentInstanceContext agentInstanceContext;

    public EvalRootNode(PatternAgentInstanceContext context, EvalRootFactoryNode factoryNode, EvalNode childNode) {
        this.factoryNode = factoryNode;
        this.childNode = childNode;
        this.agentInstanceContext = context.getAgentInstanceContext();
    }

    public EvalNode getChildNode() {
        return childNode;
    }

    public EvalRootFactoryNode getFactoryNode() {
        return factoryNode;
    }

    public EvalStateNode newState(Evaluator parentNode) {
        return new EvalRootStateNode(this, childNode);
    }

    public EvalRootState start(PatternMatchCallback callback,
                               PatternContext context,
                               boolean isRecoveringResilient) {
        MatchedEventMap beginState = new MatchedEventMapImpl(context.getMatchedEventMapMeta());
        return startInternal(callback, context, beginState, isRecoveringResilient);
    }

    public EvalRootState start(PatternMatchCallback callback,
                               PatternContext context,
                               MatchedEventMap beginState,
                               boolean isRecoveringResilient) {
        return startInternal(callback, context, beginState, isRecoveringResilient);
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    private EvalRootState startInternal(PatternMatchCallback callback,
                                        PatternContext context,
                                        MatchedEventMap beginState,
                                        boolean isRecoveringResilient) {
        if (beginState == null) {
            throw new IllegalArgumentException("No pattern begin-state has been provided");
        }
        EvalStateNode rootStateNode = newState(null);
        EvalRootState rootState = (EvalRootState) rootStateNode;
        rootState.setCallback(callback);
        rootState.startRecoverable(isRecoveringResilient, beginState);
        return rootState;
    }
}
