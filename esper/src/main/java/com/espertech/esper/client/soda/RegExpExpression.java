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
 * Regular expression evaluates a "regexp" regular expression.
 */
public class RegExpExpression extends ExpressionBase {
    private boolean not;
    private static final long serialVersionUID = -3147750744100550487L;

    /**
     * Ctor - for use to create an expression tree, without child expression.
     *
     * @param isNot true for negated regex
     */
    public RegExpExpression(boolean isNot) {
        this.not = isNot;
    }

    /**
     * Ctor.
     *
     * @param left  provides values to match against regexp string
     * @param right provides the regexp string
     * @param isNot true for negated regex
     */
    public RegExpExpression(Expression left, Expression right, boolean isNot) {
        this(left, right, null, isNot);
    }

    /**
     * Ctor.
     *
     * @param left   provides values to match against regexp string
     * @param right  provides the regexp string
     * @param escape provides the escape character
     * @param isNot  true for negated regex
     */
    public RegExpExpression(Expression left, Expression right, Expression escape, boolean isNot) {
        this.getChildren().add(left);
        this.getChildren().add(right);
        if (escape != null) {
            this.getChildren().add(escape);
        }
        this.not = isNot;
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     */
    public RegExpExpression() {
        not = false;
    }

    /**
     * Ctor.
     *
     * @param left  provides values to match against regexp string
     * @param right provides the regexp string
     */
    public RegExpExpression(Expression left, Expression right) {
        this(left, right, null);
    }

    /**
     * Ctor.
     *
     * @param left   provides values to match against regexp string
     * @param right  provides the regexp string
     * @param escape provides the escape character
     */
    public RegExpExpression(Expression left, Expression right, Expression escape) {
        this.getChildren().add(left);
        this.getChildren().add(right);
        if (escape != null) {
            this.getChildren().add(escape);
        }
        not = false;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildren().get(0).toEPL(writer, getPrecedence());
        if (not) {
            writer.write(" not");
        }
        writer.write(" regexp ");
        this.getChildren().get(1).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);

        if (this.getChildren().size() > 2) {
            writer.write(" escape ");
            this.getChildren().get(2).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
    }

    /**
     * Returns true if negated.
     *
     * @return indicator whether negated
     */
    public boolean isNot() {
        return not;
    }
}
