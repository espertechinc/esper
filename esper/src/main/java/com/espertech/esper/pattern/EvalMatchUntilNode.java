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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a match-until observer in the evaluation tree representing any event expressions.
 */
public class EvalMatchUntilNode extends EvalNodeBase {
    protected final EvalMatchUntilFactoryNode factoryNode;
    private final EvalNode childNodeSub;
    private final EvalNode childNodeUntil;

    public EvalMatchUntilNode(PatternAgentInstanceContext context, EvalMatchUntilFactoryNode factoryNode, EvalNode childNodeSub, EvalNode childNodeUntil) {
        super(context);
        this.factoryNode = factoryNode;
        this.childNodeSub = childNodeSub;
        this.childNodeUntil = childNodeUntil;
    }

    public EvalMatchUntilFactoryNode getFactoryNode() {
        return factoryNode;
    }

    public EvalNode getChildNodeSub() {
        return childNodeSub;
    }

    public EvalNode getChildNodeUntil() {
        return childNodeUntil;
    }

    public EvalStateNode newState(Evaluator parentNode,
                                  EvalStateNodeNumber stateNodeNumber, long stateNodeId) {
        return new EvalMatchUntilStateNode(parentNode, this);
    }

    private static final Logger log = LoggerFactory.getLogger(EvalMatchUntilNode.class);
}
