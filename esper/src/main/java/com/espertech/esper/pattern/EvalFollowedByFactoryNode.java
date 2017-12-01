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

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.StringWriter;
import java.util.List;

/**
 * This class represents a followed-by operator in the evaluation tree representing any event expressions.
 */
public class EvalFollowedByFactoryNode extends EvalNodeFactoryBase {
    private static final long serialVersionUID = 6255755581326049894L;
    private List<ExprNode> optionalMaxExpressions;
    private boolean hasEngineWidePatternCount;

    protected EvalFollowedByNodeOpType opType;
    private Integer[] cachedMaxPerChild;
    private transient ExprEvaluator[] cachedMaxEvaluatorPerChild;

    protected EvalFollowedByFactoryNode(List<ExprNode> optionalMaxExpressions, boolean hasEngineWidePatternCount) {
        this.optionalMaxExpressions = optionalMaxExpressions;
        this.hasEngineWidePatternCount = hasEngineWidePatternCount;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        if (opType == null) {
            initOpType();
        }

        EvalNode[] children = EvalNodeUtil.makeEvalNodeChildren(this.getChildNodes(), agentInstanceContext, parentNode);
        return new EvalFollowedByNode(agentInstanceContext, this, children);
    }

    public List<ExprNode> getOptionalMaxExpressions() {
        return optionalMaxExpressions;
    }

    public void setOptionalMaxExpressions(List<ExprNode> optionalMaxExpressions) {
        this.optionalMaxExpressions = optionalMaxExpressions;
    }

    public final String toString() {
        return "EvalFollowedByNode children=" + this.getChildNodes().size();
    }

    protected void initOpType() {
        boolean hasMax = optionalMaxExpressions != null && !optionalMaxExpressions.isEmpty();
        if (!hasMax) {
            opType = hasEngineWidePatternCount ? EvalFollowedByNodeOpType.NOMAX_POOL : EvalFollowedByNodeOpType.NOMAX_PLAIN;
            return;
        }

        cachedMaxPerChild = new Integer[this.getChildNodes().size() - 1];
        cachedMaxEvaluatorPerChild = new ExprEvaluator[this.getChildNodes().size() - 1];

        for (int i = 0; i < getChildNodes().size() - 1; i++) {
            if (optionalMaxExpressions.size() <= i) {
                continue;
            }
            ExprNode optionalMaxExpression = optionalMaxExpressions.get(i);
            if (optionalMaxExpression == null) {
                continue;
            }
            if (optionalMaxExpression.isConstantResult()) {
                Number result = (Number) optionalMaxExpression.getForge().getExprEvaluator().evaluate(null, true, null);
                if (result != null) {
                    cachedMaxPerChild[i] = result.intValue();
                }
            } else {
                cachedMaxEvaluatorPerChild[i] = optionalMaxExpressions.get(i).getForge().getExprEvaluator();
            }
        }

        opType = hasEngineWidePatternCount ? EvalFollowedByNodeOpType.MAX_POOL : EvalFollowedByNodeOpType.MAX_PLAIN;
    }

    public EvalFollowedByNodeOpType getOpType() {
        return opType;
    }

    public int getMax(int position) {
        Integer cached = cachedMaxPerChild[position];
        if (cached != null) {
            return cached;  // constant value cached
        }

        ExprEvaluator cachedExpr = cachedMaxEvaluatorPerChild[position];
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

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (optionalMaxExpressions == null || optionalMaxExpressions.isEmpty()) {
            PatternExpressionUtil.toPrecedenceFreeEPL(writer, "->", getChildNodes(), getPrecedence());
        } else {
            getChildNodes().get(0).toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
            for (int i = 1; i < getChildNodes().size(); i++) {
                ExprNode optionalMaxExpression = null;
                if (optionalMaxExpressions.size() > (i - 1)) {
                    optionalMaxExpression = optionalMaxExpressions.get(i - 1);
                }
                if (optionalMaxExpression == null) {
                    writer.append(" -> ");
                } else {
                    writer.append(" -[");
                    writer.append(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(optionalMaxExpression));
                    writer.append("]> ");
                }
                getChildNodes().get(i).toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
            }
        }
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.FOLLOWEDBY;
    }
}
