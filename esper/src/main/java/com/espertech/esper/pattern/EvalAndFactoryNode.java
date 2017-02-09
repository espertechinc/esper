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

import java.io.StringWriter;


/**
 * This class represents an 'and' operator in the evaluation tree representing an event expressions.
 */
public class EvalAndFactoryNode extends EvalNodeFactoryBase {
    private static final long serialVersionUID = -7065465204506721233L;

    protected EvalAndFactoryNode() {
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode[] children = EvalNodeUtil.makeEvalNodeChildren(this.getChildNodes(), agentInstanceContext, parentNode);
        return new EvalAndNode(agentInstanceContext, this, children);
    }

    public final String toString() {
        return "EvalAndFactoryNode children=" + this.getChildNodes().size();
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return true;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        PatternExpressionUtil.toPrecedenceFreeEPL(writer, "and", getChildNodes(), getPrecedence());
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.AND;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalAndFactoryNode.class);
}
