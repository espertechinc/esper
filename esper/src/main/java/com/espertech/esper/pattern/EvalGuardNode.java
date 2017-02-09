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
 * This class represents a guard in the evaluation tree representing an event expressions.
 */
public class EvalGuardNode extends EvalNodeBase {
    protected final EvalGuardFactoryNode factoryNode;
    private final EvalNode childNode;

    public EvalGuardNode(PatternAgentInstanceContext context, EvalGuardFactoryNode factoryNode, EvalNode childNode) {
        super(context);
        this.factoryNode = factoryNode;
        this.childNode = childNode;
    }

    public EvalGuardFactoryNode getFactoryNode() {
        return factoryNode;
    }

    public EvalNode getChildNode() {
        return childNode;
    }

    public EvalStateNode newState(Evaluator parentNode,
                                  EvalStateNodeNumber stateNodeNumber, long stateNodeId) {
        return new EvalGuardStateNode(parentNode, this);
    }

    private static final Logger log = LoggerFactory.getLogger(EvalGuardNode.class);
}
