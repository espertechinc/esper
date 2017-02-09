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
 * The "new" operator is useful to format an event or event property from a list of column names and expressions.
 * <p>
 * Useful with enumeration methods and with case-when clauses that return multiple result values, for example.
 * <p>
 * Column names are part of the state and the number of column names must match the number of sub-expressions to the expression.
 */
public class NewOperatorExpression extends ExpressionBase {

    private static final long serialVersionUID = -7207726921338996912L;

    private List<String> columnNames;

    /**
     * Ctor.
     */
    public NewOperatorExpression() {
    }

    /**
     * Ctor.
     * <p>
     * The list of column names should match the number of expressions provided hereunder.
     *
     * @param columnNames list of column names
     */
    public NewOperatorExpression(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * Returns the column names.
     *
     * @return colum names
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Sets the column names.
     *
     * @param columnNames colum names to set
     */
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.NEGATED;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("new{");
        String delimiter = "";
        for (int i = 0; i < this.getChildren().size(); i++) {
            writer.append(delimiter);
            writer.append(columnNames.get(i));
            Expression expr = this.getChildren().get(i);

            boolean outputexpr = true;
            if (expr instanceof PropertyValueExpression) {
                PropertyValueExpression prop = (PropertyValueExpression) expr;
                if (prop.getPropertyName().equals(columnNames.get(i))) {
                    outputexpr = false;
                }
            }

            if (outputexpr) {
                writer.append("=");
                expr.toEPL(writer, this.getPrecedence());
            }
            delimiter = ",";
        }
        writer.write("}");
    }
}
