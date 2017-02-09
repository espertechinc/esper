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

import java.io.StringWriter;

/**
 * This class represents an 'or' operator in the evaluation tree representing any event expressions.
 */
public class EvalOrFactoryNode extends EvalNodeFactoryBase {
    private static final long serialVersionUID = -771361274781500482L;

    protected EvalOrFactoryNode() {
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode[] children = EvalNodeUtil.makeEvalNodeChildren(this.getChildNodes(), agentInstanceContext, parentNode);
        return new EvalOrNode(agentInstanceContext, this, children);
    }

    public final String toString() {
        return "EvalOrNode children=" + this.getChildNodes().size();
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        for (EvalFactoryNode child : this.getChildNodes()) {
            if (child.isStateful()) {
                return true;
            }
        }
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        PatternExpressionUtil.toPrecedenceFreeEPL(writer, "or", getChildNodes(), getPrecedence());
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.OR;
    }
}
