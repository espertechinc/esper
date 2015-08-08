/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringWriter;

/**
 * This class represents an 'every' operator in the evaluation tree representing an event expression.
 */
public class EvalEveryFactoryNode extends EvalNodeFactoryBase
{
    private static final long serialVersionUID = 2307264285954941167L;

    /**
     * Ctor.
     */
    protected EvalEveryFactoryNode()
    {
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode child = EvalNodeUtil.makeEvalNodeSingleChild(this.getChildNodes(), agentInstanceContext, parentNode);
        return new EvalEveryNode(agentInstanceContext, this, child);
    }

    public final String toString()
    {
        return "EvalEveryNode children=" + this.getChildNodes().size();
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isStateful() {
        return true;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("every ");
        this.getChildNodes().get(0).toEPL(writer, getPrecedence());
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.UNARY;
    }

    private static final Log log = LogFactory.getLog(EvalEveryFactoryNode.class);
}
