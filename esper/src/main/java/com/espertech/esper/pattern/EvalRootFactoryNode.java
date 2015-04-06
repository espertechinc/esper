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
 * This class is always the root node in the evaluation tree representing an event expression.
 * It hold the handle to the EPStatement implementation for notifying when matches are found.
 */
public class EvalRootFactoryNode extends EvalNodeFactoryBase
{
    private static final long serialVersionUID = -4478876398666926782L;

    public EvalRootFactoryNode() {
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext) {
        return makeEvalNodeRoot(agentInstanceContext);
    }

    public EvalRootNode makeEvalNodeRoot(PatternAgentInstanceContext agentInstanceContext) {
        EvalNode child = EvalNodeUtil.makeEvalNodeSingleChild(this.getChildNodes(), agentInstanceContext);
        return new EvalRootNode(agentInstanceContext, this, child);
    }

    public final String toString()
    {
        return ("EvalRootNode children=" + this.getChildNodes().size());
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return this.getChildNodes().get(0).isStateful();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (!getChildNodes().isEmpty()) {
            getChildNodes().get(0).toEPL(writer, getPrecedence());
        }
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.MINIMUM;
    }

    private static final Log log = LogFactory.getLog(EvalRootFactoryNode.class);
}
