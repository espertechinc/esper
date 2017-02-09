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
import java.util.List;

/**
 * Represents a single expression declaration that applies to a given statement.
 */
public class ExpressionDeclaration implements Serializable {

    private static final long serialVersionUID = 8445897497101986441L;

    private String name;
    private List<String> parameterNames;
    private Expression expression;
    private boolean alias;

    /**
     * Ctor.
     */
    public ExpressionDeclaration() {
    }

    /**
     * Ctor.
     *
     * @param name           of expression
     * @param parameterNames expression paramater names
     * @param expression     the expression body
     * @param alias          indicator whether this is an expression alias or not
     */
    public ExpressionDeclaration(String name, List<String> parameterNames, Expression expression, boolean alias) {
        this.name = name;
        this.parameterNames = parameterNames;
        this.expression = expression;
        this.alias = alias;
    }

    /**
     * Returns expression name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets expression name.
     *
     * @param name name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the expression body.
     *
     * @return expression body
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Sets the expression body.
     *
     * @param expression body to set
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    /**
     * Returns the paramater names.
     *
     * @return paramater names
     */
    public List<String> getParameterNames() {
        return parameterNames;
    }

    /**
     * Returns indicator whether the expression is an alias or not.
     *
     * @return alias indicator
     */
    public boolean isAlias() {
        return alias;
    }

    /**
     * Sets indicator whether the expression is an alias or not.
     *
     * @param alias alias indicator
     */
    public void setAlias(boolean alias) {
        this.alias = alias;
    }

    /**
     * Sets the paramater names.
     *
     * @param parameterNames paramater names to set
     */
    public void setParameterNames(List<String> parameterNames) {
        this.parameterNames = parameterNames;
    }

    /**
     * Print.
     *
     * @param writer                 to print to
     * @param expressionDeclarations expression declarations
     * @param formatter              for newline-whitespace formatting
     */
    public static void toEPL(StringWriter writer, List<ExpressionDeclaration> expressionDeclarations, EPStatementFormatter formatter) {
        if ((expressionDeclarations == null) || (expressionDeclarations.isEmpty())) {
            return;
        }

        for (ExpressionDeclaration part : expressionDeclarations) {
            if (part.getName() == null) {
                continue;
            }
            formatter.beginExpressionDecl(writer);
            part.toEPL(writer);
        }
    }

    /**
     * Print part.
     *
     * @param writer to write to
     */
    public void toEPL(StringWriter writer) {
        writer.append("expression ");
        writer.append(name);
        if (alias) {
            writer.append(" alias for");
        }
        writer.append(" {");
        if (!alias) {
            if (parameterNames != null && parameterNames.size() == 1) {
                writer.append(parameterNames.get(0));
            } else if (parameterNames != null && !parameterNames.isEmpty()) {
                String delimiter = "";
                writer.append("(");
                for (String name : parameterNames) {
                    writer.append(delimiter);
                    writer.append(name);
                    delimiter = ",";
                }
                writer.append(")");
            }

            if (parameterNames != null && !parameterNames.isEmpty()) {
                writer.append(" => ");
            }
        }
        if (expression != null) {
            expression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        writer.append("}");
    }
}
