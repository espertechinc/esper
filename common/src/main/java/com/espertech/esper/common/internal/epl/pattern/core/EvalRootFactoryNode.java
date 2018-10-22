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

/**
 * This class is always the root node in the evaluation tree representing an event expression.
 * It hold the handle to the EPStatement implementation for notifying when matches are found.
 */
public class EvalRootFactoryNode extends EvalFactoryNodeBase {

    protected EvalFactoryNode childNode;

    public void setChildNode(EvalFactoryNode childNode) {
        this.childNode = childNode;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode child = childNode.makeEvalNode(agentInstanceContext, parentNode);
        return new EvalRootNode(agentInstanceContext, this, child);
    }

    public EvalFactoryNode getChildNode() {
        return childNode;
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return childNode.isStateful();
    }

    public void accept(EvalFactoryNodeVisitor visitor) {
        visitor.visit(this);
        childNode.accept(visitor);
    }
}
