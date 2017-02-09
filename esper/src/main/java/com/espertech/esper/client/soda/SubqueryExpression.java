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
 * Subquery-expression returns values returned by a lookup modelled by a further {@link EPStatementObjectModel}.
 */
public class SubqueryExpression extends ExpressionBase {
    private EPStatementObjectModel model;
    private static final long serialVersionUID = 5210335236320516663L;

    /**
     * Ctor.
     */
    public SubqueryExpression() {
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     *
     * @param model is the lookup statement object model
     */
    public SubqueryExpression(EPStatementObjectModel model) {
        this.model = model;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write('(');
        writer.write(model.toEPL());
        writer.write(')');
    }

    /**
     * Returns the lookup statement object model.
     *
     * @return lookup model
     */
    public EPStatementObjectModel getModel() {
        return model;
    }

    /**
     * Sets the lookup statement object model.
     *
     * @param model is the lookup model to set
     */
    public void setModel(EPStatementObjectModel model) {
        this.model = model;
    }
}
