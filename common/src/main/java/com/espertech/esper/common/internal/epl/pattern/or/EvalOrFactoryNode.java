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
package com.espertech.esper.common.internal.epl.pattern.or;

import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.epl.pattern.core.*;

/**
 * This class represents an 'or' operator in the evaluation tree representing any event expressions.
 */
public class EvalOrFactoryNode extends EvalFactoryNodeBase {

    protected EvalFactoryNode[] children;

    public EvalFactoryNode[] getChildren() {
        return children;
    }

    public void setChildren(EvalFactoryNode[] children) {
        this.children = children;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode[] nodes = EvalNodeUtil.makeEvalNodeChildren(children, agentInstanceContext, parentNode);
        return new EvalOrNode(agentInstanceContext, this, nodes);
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        for (EvalFactoryNode child : children) {
            if (child.isStateful()) {
                return true;
            }
        }
        return false;
    }

    public void accept(EvalFactoryNodeVisitor visitor) {
        visitor.visit(this);
        for (EvalFactoryNode child : children) {
            child.accept(visitor);
        }
    }
}
