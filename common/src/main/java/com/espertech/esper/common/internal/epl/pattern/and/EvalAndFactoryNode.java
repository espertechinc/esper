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
package com.espertech.esper.common.internal.epl.pattern.and;

import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.epl.pattern.core.*;


/**
 * This class represents an 'and' operator in the evaluation tree representing an event expressions.
 */
public class EvalAndFactoryNode extends EvalFactoryNodeBase {

    protected EvalFactoryNode[] children;

    public void setChildren(EvalFactoryNode[] children) {
        this.children = children;
    }

    public EvalFactoryNode[] getChildren() {
        return children;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode[] nodes = EvalNodeUtil.makeEvalNodeChildren(children, agentInstanceContext, parentNode);
        return new EvalAndNode(agentInstanceContext, this, nodes);
    }

    public final String toString() {
        return "EvalAndFactoryNode children=" + children.length;
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return true;
    }

    public void accept(EvalFactoryNodeVisitor visitor) {
        visitor.visit(this);
        for (EvalFactoryNode child : children) {
            child.accept(visitor);
        }
    }
}
