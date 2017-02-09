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
 * Exists-expression for a set of values returned by a lookup.
 */
public class SubqueryExistsExpression extends ExpressionBase {
    private EPStatementObjectModel model;
    private static final long serialVersionUID = 2615782942153556969L;

    /**
     * Ctor.
     */
    public SubqueryExistsExpression() {
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     *
     * @param model is the lookup statement object model
     */
    public SubqueryExistsExpression(EPStatementObjectModel model) {
        this.model = model;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("exists (");
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
