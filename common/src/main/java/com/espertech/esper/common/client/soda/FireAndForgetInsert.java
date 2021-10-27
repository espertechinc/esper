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
package com.espertech.esper.common.client.soda;

import java.io.StringWriter;
import java.util.List;

/**
 * Fire-and-forget (on-demand) insert DML.
 * <p>
 * The insert-into clause holds the named window name and column names.
 * The select-clause list holds the values to be inserted.
 * </p>
 */
public class FireAndForgetInsert implements FireAndForgetClause {
    private static final long serialVersionUID = -3565886245820109541L;

    private boolean useValuesKeyword = true;
    private List<List<Expression>> rows;

    /**
     * Ctor.
     *
     * @param useValuesKeyword whether to use the "values" keyword or whether the syntax is based on select
     */
    public FireAndForgetInsert(boolean useValuesKeyword) {
        this.useValuesKeyword = useValuesKeyword;
    }

    /**
     * Ctor.
     */
    public FireAndForgetInsert() {
    }

    /**
     * Returns indicator whether to use the values keyword.
     *
     * @return indicator
     */
    public boolean isUseValuesKeyword() {
        return useValuesKeyword;
    }

    /**
     * Sets indicator whether to use the values keyword.
     *
     * @param useValuesKeyword indicator
     */
    public void setUseValuesKeyword(boolean useValuesKeyword) {
        this.useValuesKeyword = useValuesKeyword;
    }

    /**
     * Returns the rows. Only applicable when using the "values"-keyword i.e. "values (...row...), (...row...)".
     * @return rows wherein each row is a list of expressions
     */
    public List<List<Expression>> getRows() {
        return rows;
    }

    /**
     * Sets the rows. Only applicable when using the "values"-keyword i.e. "values (...row...), (...row...)".
     * @param rows rows wherein each row is a list of expressions
     */
    public void setRows(List<List<Expression>> rows) {
        this.rows = rows;
    }

    public void toEPL(StringWriter writer) {
        writer.append("values ");
        String delimiter = "";
        for (List<Expression> row : rows) {
            writer.write(delimiter);
            renderRow(writer, row);
            delimiter = ", ";
        }
    }

    private void renderRow(StringWriter writer, List<Expression> row) {
        writer.write("(");
        String delimiter = "";
        for (Expression param : row) {
            writer.write(delimiter);
            delimiter = ", ";
            param.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        writer.write(")");
    }
}
