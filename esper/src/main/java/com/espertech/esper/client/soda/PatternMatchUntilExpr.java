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

/**
 * Match-Until construct for use in pattern expressions.
 */
public class PatternMatchUntilExpr extends PatternExprBase {
    private static final long serialVersionUID = -427123340111619016L;

    private Expression low;
    private Expression high;
    private Expression single;

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     */
    public PatternMatchUntilExpr() {
    }

    /**
     * Ctor - for use when adding required child nodes later.
     *
     * @param low    - low number of matches, or null if no lower boundary
     * @param high   - high number of matches, or null if no high boundary
     * @param single - if a single bound is provided, this carries the single bound (all others should be null)
     */
    public PatternMatchUntilExpr(Expression low, Expression high, Expression single) {
        this.low = low;
        this.high = high;
        this.single = single;
    }

    /**
     * Ctor.
     *
     * @param single the single bound expression
     */
    public PatternMatchUntilExpr(Expression single) {
        this.single = single;
    }

    /**
     * Ctor.
     *
     * @param low   - low number of matches, or null if no lower boundary
     * @param high  - high number of matches, or null if no high boundary
     * @param match - the pattern expression that is sought to match repeatedly
     * @param until - the pattern expression that ends matching (optional, can be null)
     */
    public PatternMatchUntilExpr(Expression low, Expression high, PatternExpr match, PatternExpr until) {
        this.low = low;
        this.high = high;
        this.addChild(match);
        this.addChild(until);
    }

    /**
     * Returns the optional low endpoint for the repeat, or null if none supplied.
     *
     * @return low endpoint
     */
    public Expression getLow() {
        return low;
    }

    /**
     * Sets the optional low endpoint for the repeat, or null if none supplied.
     *
     * @param low - low endpoint to set
     */
    public void setLow(Expression low) {
        this.low = low;
    }

    /**
     * Returns the optional high endpoint for the repeat, or null if none supplied.
     *
     * @return high endpoint
     */
    public Expression getHigh() {
        return high;
    }

    /**
     * Returns the single-bounds expression.
     *
     * @return single-bound expression
     */
    public Expression getSingle() {
        return single;
    }

    /**
     * Sets the single-bound expression.
     *
     * @param single single-bound expression
     */
    public void setSingle(Expression single) {
        this.single = single;
    }

    /**
     * Sets the optional high endpoint for the repeat, or null if none supplied.
     *
     * @param high - high endpoint to set
     */
    public void setHigh(Expression high) {
        this.high = high;
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.MATCH_UNTIL;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter) {
        if (single != null) {
            writer.write("[");
            single.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.write("]");
        } else {
            if (low != null || high != null) {
                writer.write("[");
                if ((low != null) && (high != null)) {
                    low.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                    writer.write(":");
                    high.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                } else if (low != null) {
                    low.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                    writer.write(":");
                } else {
                    writer.write(":");
                    high.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                }
                writer.write("] ");
            }
        }

        PatternExprPrecedenceEnum precedence = getPrecedence();
        if (this.getChildren().get(0) instanceof PatternMatchUntilExpr) {
            precedence = PatternExprPrecedenceEnum.MAXIMIM;
        }
        this.getChildren().get(0).toEPL(writer, precedence, formatter);

        if (this.getChildren().size() > 1) {
            writer.write(" until ");
            this.getChildren().get(1).toEPL(writer, getPrecedence(), formatter);
        }
    }
}
