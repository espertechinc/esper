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
package com.espertech.esper.common.internal.epl.pattern.every;

import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.epl.pattern.core.*;

/**
 * This class represents an 'every' operator in the evaluation tree representing an event expression.
 */
public class EvalEveryFactoryNode extends EvalFactoryNodeBase {

    private EvalFactoryNode childNode;

    public EvalFactoryNode getChildNode() {
        return childNode;
    }

    public void setChildNode(EvalFactoryNode childNode) {
        this.childNode = childNode;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode child = EvalNodeUtil.makeEvalNodeSingleChild(childNode, agentInstanceContext, parentNode);
        return new EvalEveryNode(agentInstanceContext, this, child);
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isStateful() {
        return true;
    }

    public void accept(EvalFactoryNodeVisitor visitor) {
        visitor.visit(this);
        childNode.accept(visitor);
    }
}
