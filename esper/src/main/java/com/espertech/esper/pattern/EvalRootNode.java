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
package com.espertech.esper.pattern;

import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.filterspec.MatchedEventMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is always the root node in the evaluation tree representing an event expression.
 * It hold the handle to the EPStatement implementation for notifying when matches are found.
 */
public class EvalRootNode extends EvalNodeBase implements PatternStarter {
    protected final EvalRootFactoryNode factoryNode;
    protected final EvalNode childNode;

    public EvalRootNode(PatternAgentInstanceContext context, EvalRootFactoryNode factoryNode, EvalNode childNode) {
        super(context);
        this.factoryNode = factoryNode;
        this.childNode = childNode;
    }

    public EvalNode getChildNode() {
        return childNode;
    }

    public EvalRootFactoryNode getFactoryNode() {
        return factoryNode;
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

    protected EvalRootState startInternal(PatternMatchCallback callback,
                                          PatternContext context,
                                          MatchedEventMap beginState,
                                          boolean isRecoveringResilient) {
        if (beginState == null) {
            throw new IllegalArgumentException("No pattern begin-state has been provided");
        }
        EvalStateNode rootStateNode = newState(null, null, 0L);
        EvalRootState rootState = (EvalRootState) rootStateNode;
        rootState.setCallback(callback);
        rootState.startRecoverable(isRecoveringResilient, beginState);
        return rootState;
    }

    public EvalStateNode newState(Evaluator parentNode,
                                  EvalStateNodeNumber stateNodeNumber, long stateNodeId) {
        return new EvalRootStateNode(childNode);
    }

    private static final Logger log = LoggerFactory.getLogger(EvalRootNode.class);
}
