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
import java.util.ArrayList;
import java.util.List;

/**
 * Dot-expresson is for use in "(inner_expression).dot_expression".
 */
public class DotExpression extends ExpressionBase {
    private static final long serialVersionUID = -7597133103354244332L;
    private List<DotExpressionItem> chain = new ArrayList<DotExpressionItem>();

    /**
     * Ctor.
     */
    public DotExpression() {
    }

    /**
     * Ctor.
     *
     * @param innerExpression the expression in parenthesis
     */
    public DotExpression(Expression innerExpression) {
        this.getChildren().add(innerExpression);
    }

    /**
     * Add a method to the chain of methods after the dot.
     *
     * @param methodName to add
     * @param parameters parameters to method
     */
    public void add(String methodName, List<Expression> parameters) {
        chain.add(new DotExpressionItem(methodName, parameters, false));
    }

    /**
     * Add a method to the chain of methods after the dot, indicating the this segment is a property and does not need parenthesis and won't have paramaters.
     *
     * @param methodName method name
     * @param parameters parameter expressions
     * @param isProperty property flag
     */
    public void add(String methodName, List<Expression> parameters, boolean isProperty) {
        chain.add(new DotExpressionItem(methodName, parameters, isProperty));
    }

    /**
     * Returns the method chain of all methods after the dot.
     *
     * @return method name ane list of parameters
     */
    public List<DotExpressionItem> getChain() {
        return chain;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.MINIMUM;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (!this.getChildren().isEmpty()) {
            this.getChildren().get(0).toEPL(writer, getPrecedence());
        }
        DotExpressionItem.render(chain, writer, !this.getChildren().isEmpty());
    }
}
