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
 * Represents a list-compare of the format "expression operator all/any (expressions)".
 */
public class CompareListExpression extends ExpressionBase {
    private String operator;
    private boolean all;
    private static final long serialVersionUID = 2915262248687901526L;

    /**
     * Ctor.
     */
    public CompareListExpression() {
    }

    /**
     * Ctor.
     *
     * @param all      is all, false if any
     * @param operator =, !=, &lt;, &gt;, &lt;=, &gt;=, &lt;&gt;
     */
    public CompareListExpression(boolean all, String operator) {
        this.all = all;
        this.operator = operator;
    }

    /**
     * Returns all flag, true for ALL and false for ANY.
     *
     * @return indicator if all or any
     */
    public boolean isAll() {
        return all;
    }

    /**
     * Returns all flag, true for ALL and false for ANY.
     *
     * @return indicator if all or any
     */
    public boolean getAll() {
        return all;
    }

    /**
     * Sets all flag, true for ALL and false for ANY.
     *
     * @param all indicator if all or any
     */
    public void setAll(boolean all) {
        this.all = all;
    }

    /**
     * Returns the operator.
     *
     * @return operator
     */
    public String getOperator() {
        return operator;
    }

    /**
     * Sets the operator.
     *
     * @param operator to set (=, !=, &lt;&gt;, &lt;, &gt;, &lt;=, &gt;=)
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {

        this.getChildren().get(0).toEPL(writer, this.getPrecedence());
        writer.write(operator);
        if (all) {
            writer.write("all(");
        } else {
            writer.write("any(");
        }

        String delimiter = "";
        for (int i = 1; i < this.getChildren().size(); i++) {
            writer.write(delimiter);
            this.getChildren().get(i).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
        writer.write(')');
    }
}
