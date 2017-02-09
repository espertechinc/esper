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

/**
 * This class represents an 'every-distinct' operator in the evaluation tree representing an event expression.
 */
public class EvalEveryDistinctNode extends EvalNodeBase {
    protected final EvalEveryDistinctFactoryNode factoryNode;
    private final EvalNode childNode;

    public EvalEveryDistinctNode(EvalEveryDistinctFactoryNode factoryNode, EvalNode childNode, PatternAgentInstanceContext agentInstanceContext) {
        super(agentInstanceContext);
        this.factoryNode = factoryNode;
        this.childNode = childNode;
    }

    public EvalEveryDistinctFactoryNode getFactoryNode() {
        return factoryNode;
    }

    public EvalNode getChildNode() {
        return childNode;
    }

    public EvalStateNode newState(Evaluator parentNode,
                                  EvalStateNodeNumber stateNodeNumber, long stateNodeId) {
        if (factoryNode.getTimeDeltaComputation() == null) {
            return new EvalEveryDistinctStateNode(parentNode, this);
        } else {
            return new EvalEveryDistinctStateExpireKeyNode(parentNode, this);
        }
    }
}
