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
package com.espertech.esper.client.soda;

import java.io.StringWriter;
import java.util.List;

/**
 * Followed-by for use in pattern expressions.
 */
public class PatternFollowedByExpr extends PatternExprBase {
    private static final long serialVersionUID = 1480442602208180240L;

    private List<Expression> optionalMaxPerSubexpression;

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     */
    public PatternFollowedByExpr() {
    }

    /**
     * Ctor.
     *
     * @param optionalMaxPerSubexpression if parameterized by a max-limits for each pattern sub-expressions
     */
    public PatternFollowedByExpr(List<Expression> optionalMaxPerSubexpression) {
        this.optionalMaxPerSubexpression = optionalMaxPerSubexpression;
    }

    /**
     * Ctor.
     *
     * @param first        a first pattern expression in the followed-by relationship
     * @param second       a second pattern expression in the followed-by relationship
     * @param patternExprs further optional pattern expressions in the followed-by relationship
     */
    public PatternFollowedByExpr(PatternExpr first, PatternExpr second, PatternExpr... patternExprs) {
        this.addChild(first);
        this.addChild(second);
        for (int i = 0; i < patternExprs.length; i++) {
            this.addChild(patternExprs[i]);
        }
    }

    /**
     * Adds a pattern expression to the followed-by relationship between patterns.
     *
     * @param expr to add
     * @return pattern expression
     */
    public PatternFollowedByExpr add(PatternExpr expr) {
        this.getChildren().add(expr);
        return this;
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.FOLLOWED_BY;
    }

    /**
     * Returns the instance limits, if any, for pattern-subexpressions.
     *
     * @return list of max-limit or null
     */
    public List<Expression> getOptionalMaxPerSubexpression() {
        return optionalMaxPerSubexpression;
    }

    /**
     * Sets the instance limits, if any, for pattern-subexpressions.
     *
     * @param optionalMaxPerSubexpression list of max-limit or null
     */
    public void setOptionalMaxPerSubexpression(List<Expression> optionalMaxPerSubexpression) {
        this.optionalMaxPerSubexpression = optionalMaxPerSubexpression;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter) {
        String delimiter = "";
        int childNum = 0;
        for (PatternExpr child : this.getChildren()) {
            writer.write(delimiter);
            child.toEPL(writer, getPrecedence(), formatter);

            delimiter = " -> ";
            if (optionalMaxPerSubexpression != null && optionalMaxPerSubexpression.size() > childNum) {
                Expression maxExpr = optionalMaxPerSubexpression.get(childNum);
                if (maxExpr != null) {
                    StringWriter inner = new StringWriter();
                    maxExpr.toEPL(inner, ExpressionPrecedenceEnum.MINIMUM);
                    delimiter = " -[" + inner.toString() + "]> ";
                }
            }
            childNum++;
        }
    }
}
