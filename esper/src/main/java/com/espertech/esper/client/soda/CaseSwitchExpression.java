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
 * Case-expression that acts as a switch testing a value against other values.
 * <p>
 * The first child expression provides the value to switch on.
 * The following pairs of child expressions provide the "when expression then expression" results.
 * The last child expression provides the "else" result.
 */
public class CaseSwitchExpression extends ExpressionBase {
    private static final long serialVersionUID = -5843078556996307245L;

    /**
     * Ctor - for use to create an expression tree, without inner expression
     */
    public CaseSwitchExpression() {
    }

    /**
     * Ctor.
     *
     * @param switchValue is the expression providing the value to switch on
     */
    public CaseSwitchExpression(Expression switchValue) {
        // switch value expression is first
        this.addChild(switchValue);
    }

    /**
     * Adds a pair of expressions representing a "when" and a "then" in the switch.
     *
     * @param when expression to match on
     * @param then expression to return a conditional result when the when-expression matches
     * @return expression
     */
    public CaseSwitchExpression add(Expression when, Expression then) {
        int size = this.getChildren().size();
        if (size % 2 != 0) {
            this.addChild(when);
            this.addChild(then);
        } else {
            // add next to last as the last node is the else clause
            this.getChildren().add(this.getChildren().size() - 1, when);
            this.getChildren().add(this.getChildren().size() - 1, then);
        }
        return this;
    }

    /**
     * Sets the else-part of the case-switch. This result of this expression is returned
     * when no when-expression matched.
     *
     * @param elseExpr is the expression returning the no-match value
     * @return expression
     */
    public CaseSwitchExpression setElse(Expression elseExpr) {
        int size = this.getChildren().size();
        // remove last node representing the else
        if (size % 2 == 0) {
            this.getChildren().remove(size - 1);
        }
        this.addChild(elseExpr);
        return this;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.CASE;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("case ");
        getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        int index = 1;
        while (index < this.getChildren().size() - 1) {
            writer.write(" when ");
            getChildren().get(index).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            index++;
            if (index == this.getChildren().size()) {
                throw new IllegalStateException("Invalid case-when expression, count of when-to-then nodes not matching");
            }
            writer.write(" then ");
            getChildren().get(index).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            index++;
        }

        if (index < this.getChildren().size()) {
            writer.write(" else ");
            getChildren().get(index).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        writer.write(" end");
    }
}
