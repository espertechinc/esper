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
 * This class represents an 'or' operator in the evaluation tree representing any event expressions.
 */
public class EvalOrNode extends EvalNodeBase {
    protected final EvalOrFactoryNode factoryNode;
    private final EvalNode[] childNodes;

    public EvalOrNode(PatternAgentInstanceContext context, EvalOrFactoryNode factoryNode, EvalNode[] childNodes) {
        super(context);
        this.factoryNode = factoryNode;
        this.childNodes = childNodes;
    }

    public EvalOrFactoryNode getFactoryNode() {
        return factoryNode;
    }

    public EvalNode[] getChildNodes() {
        return childNodes;
    }

    public EvalStateNode newState(Evaluator parentNode,
                                  EvalStateNodeNumber stateNodeNumber, long stateNodeId) {
        return new EvalOrStateNode(parentNode, this);
    }

    private static final Logger log = LoggerFactory.getLogger(EvalOrNode.class);
}
