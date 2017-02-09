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
 * Lambda-expression is an expression of the form "parameter =&gt; body" where-in the "=&gt;" reads as goes-to.
 * <p>
 * The form "x =&gt; x * x" reads as "x goes to x times x", for an example expression that yields x multiplied by x.
 * <p>
 * Used with expression declaration and with enumeration methods, for example, to parameterize by an expression.
 */
public class LambdaExpression extends ExpressionBase {
    private static final long serialVersionUID = 353451331713297154L;

    private List<String> parameters;

    /**
     * Ctor.
     */
    public LambdaExpression() {
    }

    /**
     * Ctor.
     *
     * @param parameters the lambda expression parameters
     */
    public LambdaExpression(List<String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns the lambda expression parameters.
     *
     * @return lambda expression parameters
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * Sets the lambda expression parameters.
     *
     * @param parameters lambda expression parameters
     */
    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.MINIMUM;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (parameters.size() > 1) {
            writer.append("(");
            String delimiter = "";
            for (String parameter : parameters) {
                writer.append(delimiter);
                writer.append(parameter);
                delimiter = ",";
            }
            writer.append(")");
        } else {
            writer.append(parameters.get(0));
        }
        writer.append(" => ");
        this.getChildren().get(0).toEPL(writer, getPrecedence());
    }
}
