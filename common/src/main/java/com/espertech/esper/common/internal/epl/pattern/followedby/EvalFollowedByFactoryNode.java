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
package com.espertech.esper.common.internal.epl.pattern.followedby;

import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.pattern.core.*;

/**
 * This class represents a followed-by operator in the evaluation tree representing any event expressions.
 */
public class EvalFollowedByFactoryNode extends EvalFactoryNodeBase implements StatementReadyCallback {
    protected EvalFactoryNode[] children;
    private ExprEvaluator[] maxPerChildEvals;

    protected EvalFollowedByNodeOpType opType;

    public void setChildren(EvalFactoryNode[] children) {
        this.children = children;
    }

    public void setMaxPerChildEvals(ExprEvaluator[] maxPerChildEvals) {
        this.maxPerChildEvals = maxPerChildEvals;
        if (maxPerChildEvals != null && maxPerChildEvals.length == 0) {
            throw new IllegalStateException("Invalid empty array");
        }
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        boolean hasMax = maxPerChildEvals != null;
        boolean hasEngineWidePatternCount = statementContext.getRuntimeSettingsService().getConfigurationRuntime().getPatterns().getMaxSubexpressions() != null;

        if (!hasMax) {
            opType = hasEngineWidePatternCount ? EvalFollowedByNodeOpType.NOMAX_POOL : EvalFollowedByNodeOpType.NOMAX_PLAIN;
        } else {
            opType = hasEngineWidePatternCount ? EvalFollowedByNodeOpType.MAX_POOL : EvalFollowedByNodeOpType.MAX_PLAIN;
        }
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode[] nodes = EvalNodeUtil.makeEvalNodeChildren(children, agentInstanceContext, parentNode);
        return new EvalFollowedByNode(agentInstanceContext, this, nodes);
    }

    public EvalFollowedByNodeOpType getOpType() {
        return opType;
    }

    public int getMax(int position) {
        ExprEvaluator cachedExpr = maxPerChildEvals[position];
        if (cachedExpr == null) {
            return -1;  // no limit defined for this sub-expression
        }

        Number result = (Number) cachedExpr.evaluate(null, true, null);
        if (result != null) {
            return result.intValue();
        }
        return -1;  // no limit
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
