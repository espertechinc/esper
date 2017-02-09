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
 * This class represents an 'and' operator in the evaluation tree representing an event expressions.
 */
public class EvalAndNode extends EvalNodeBase {
    protected final EvalAndFactoryNode factoryNode;
    protected final EvalNode[] childNodes;

    public EvalAndNode(PatternAgentInstanceContext context, EvalAndFactoryNode factoryNode, EvalNode[] childNodes) {
        super(context);
        this.factoryNode = factoryNode;
        this.childNodes = childNodes;
    }

    public EvalAndFactoryNode getFactoryNode() {
        return factoryNode;
    }

    public EvalNode[] getChildNodes() {
        return childNodes;
    }

    public EvalStateNode newState(Evaluator parentNode,
                                  EvalStateNodeNumber stateNodeNumber, long stateNodeId) {
        return new EvalAndStateNode(parentNode, this);
    }
}
