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
 * This class represents an 'or' operator in the evaluation tree representing any event expressions.
 */
public class EvalAuditNode extends EvalNodeBase {
    private final EvalAuditFactoryNode factoryNode;
    private final EvalNode childNode;

    public EvalAuditNode(PatternAgentInstanceContext context, EvalAuditFactoryNode factoryNode, EvalNode childNode) {
        super(context);
        this.factoryNode = factoryNode;
        this.childNode = childNode;
    }

    public EvalAuditFactoryNode getFactoryNode() {
        return factoryNode;
    }

    public EvalNode getChildNode() {
        return childNode;
    }

    public EvalStateNode newState(Evaluator parentNode,
                                  EvalStateNodeNumber stateNodeNumber,
                                  long stateNodeId) {
        return new EvalAuditStateNode(parentNode, this, stateNodeNumber, stateNodeId);
    }
}
