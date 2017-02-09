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

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Clause for creating an expression for use across one or more statements.
 * <p>
 * Both expressions and scripts can be created using this clause.
 * </p>
 */
public class CreateExpressionClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private ExpressionDeclaration expressionDeclaration;
    private ScriptExpression scriptExpression;

    /**
     * Ctor.
     */
    public CreateExpressionClause() {
    }

    /**
     * Ctor.
     *
     * @param expressionDeclaration expression
     */
    public CreateExpressionClause(ExpressionDeclaration expressionDeclaration) {
        this.expressionDeclaration = expressionDeclaration;
    }

    /**
     * Ctor.
     *
     * @param scriptExpression script
     */
    public CreateExpressionClause(ScriptExpression scriptExpression) {
        this.scriptExpression = scriptExpression;
    }

    /**
     * Returns the expression declaration or null if script instead.
     *
     * @return expression declaration
     */
    public ExpressionDeclaration getExpressionDeclaration() {
        return expressionDeclaration;
    }

    /**
     * Sets the expression declaration or null if script instead.
     *
     * @param expressionDeclaration expression declaration
     */
    public void setExpressionDeclaration(ExpressionDeclaration expressionDeclaration) {
        this.expressionDeclaration = expressionDeclaration;
    }

    /**
     * Returns the script expression or null if declaring an EPL expression.
     *
     * @return script expression
     */
    public ScriptExpression getScriptExpression() {
        return scriptExpression;
    }

    /**
     * Sets the script expression or null if declaring an EPL expression.
     *
     * @param scriptExpression script expression
     */
    public void setScriptExpression(ScriptExpression scriptExpression) {
        this.scriptExpression = scriptExpression;
    }

    /**
     * EPL output
     *
     * @param writer to write to
     */
    public void toEPL(StringWriter writer) {
        writer.append("create ");
        if (expressionDeclaration != null) {
            expressionDeclaration.toEPL(writer);
        } else {
            scriptExpression.toEPL(writer);
        }
    }
}
