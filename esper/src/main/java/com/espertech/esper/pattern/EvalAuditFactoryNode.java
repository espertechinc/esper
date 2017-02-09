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
public class EvalAuditFactoryNode extends EvalNodeFactoryBase {
    private static final long serialVersionUID = 6585271287552353699L;
    private final boolean auditPattern;
    private final boolean auditPatternInstance;
    private final String patternExpr;
    private final transient EvalAuditInstanceCount instanceCount;
    private final boolean filterChildNonQuitting;

    public EvalAuditFactoryNode(boolean auditPattern, boolean auditPatternInstance, String patternExpr, EvalAuditInstanceCount instanceCount, boolean filterChildNonQuitting) {
        this.auditPattern = auditPattern;
        this.auditPatternInstance = auditPatternInstance;
        this.patternExpr = patternExpr;
        this.instanceCount = instanceCount;
        this.filterChildNonQuitting = filterChildNonQuitting;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode child = EvalNodeUtil.makeEvalNodeSingleChild(this.getChildNodes(), agentInstanceContext, parentNode);
        return new EvalAuditNode(agentInstanceContext, this, child);
    }

    public boolean isAuditPattern() {
        return auditPattern;
    }

    public String getPatternExpr() {
        return patternExpr;
    }

    public final String toString() {
        return "EvalAuditFactoryNode children=" + this.getChildNodes().size();
    }

    public void decreaseRefCount(EvalAuditStateNode current, PatternContext patternContext) {
        if (!auditPatternInstance) {
            return;
        }
        instanceCount.decreaseRefCount(this.getChildNodes().get(0), current, patternExpr, patternContext.getStatementName(), patternContext.getEngineURI());
    }

    public void increaseRefCount(EvalAuditStateNode current, PatternContext patternContext) {
        if (!auditPatternInstance) {
            return;
        }
        instanceCount.increaseRefCount(this.getChildNodes().get(0), current, patternExpr, patternContext.getStatementName(), patternContext.getEngineURI());
    }

    public boolean isFilterChildNonQuitting() {
        return filterChildNonQuitting;
    }

    public boolean isStateful() {
        return getChildNodes().get(0).isStateful();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes().get(0).toEPL(writer, getPrecedence());
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return this.getChildNodes().get(0).getPrecedence();
    }
}
