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
 * Case expression that act as a when-then-else.
 */
public class CaseWhenThenExpression extends ExpressionBase {
    private static final long serialVersionUID = -6478192972183336030L;

    /**
     * Ctor - for use to create an expression tree, without child expression.
     * <p>
     * Use add methods to add child expressions to acts upon.
     */
    public CaseWhenThenExpression() {
    }

    /**
     * Adds a when-then pair of expressions.
     *
     * @param when providings conditions to evaluate
     * @param then provides the result when a condition evaluates to true
     * @return expression
     */
    public CaseWhenThenExpression add(Expression when, Expression then) {
        int size = this.getChildren().size();
        if (size % 2 == 0) {
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
     * Sets the expression to provide a value when no when-condition matches.
     *
     * @param elseExpr expression providing default result
     * @return expression
     */
    public CaseWhenThenExpression setElse(Expression elseExpr) {
        int size = this.getChildren().size();
        // remove last node representing the else
        if (size % 2 != 0) {
            this.getChildren().remove(size - 1);
        }
        this.addChild(elseExpr);
        return this;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.CASE;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("case");
        int index = 0;
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
