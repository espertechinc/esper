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
 * Specification object for a row limit.
 */
public class RowLimitClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private Integer numRows;
    private Integer optionalOffsetRows;
    private String numRowsVariable;
    private String optionalOffsetRowsVariable;

    /**
     * Ctor.
     */
    public RowLimitClause() {
    }

    /**
     * Creates a row limit clause.
     *
     * @param numRowsVariable name of the variable providing the maximum number of rows
     * @return clause
     */
    public static RowLimitClause create(String numRowsVariable) {
        return new RowLimitClause(null, null, numRowsVariable, null);
    }

    /**
     * Creates a row limit clause.
     *
     * @param numRowsVariable name of the variable providing the maximum number of rows
     * @param offsetVariable  name of the variable providing the offset
     * @return clause
     */
    public static RowLimitClause create(String numRowsVariable, String offsetVariable) {
        return new RowLimitClause(null, null, numRowsVariable, offsetVariable);
    }

    /**
     * Creates a row limit clause.
     *
     * @param numRows maximum number of rows
     * @return clause
     */
    public static RowLimitClause create(int numRows) {
        return new RowLimitClause(numRows, null, null, null);
    }

    /**
     * Creates a row limit clause.
     *
     * @param numRows maximum number of rows
     * @param offset  offset
     * @return clause
     */
    public static RowLimitClause create(int numRows, int offset) {
        return new RowLimitClause(numRows, offset, null, null);
    }

    /**
     * Ctor.
     *
     * @param numRows                    maximum number of rows
     * @param optionalOffsetRows         offset
     * @param numRowsVariable            name of the variable providing the maximum number of rows
     * @param optionalOffsetRowsVariable name of the variable providing the offset
     */
    public RowLimitClause(Integer numRows, Integer optionalOffsetRows, String numRowsVariable, String optionalOffsetRowsVariable) {
        this.numRows = numRows;
        this.optionalOffsetRows = optionalOffsetRows;
        this.numRowsVariable = numRowsVariable;
        this.optionalOffsetRowsVariable = optionalOffsetRowsVariable;
    }

    /**
     * Returns the maximum number of rows, or null if using variable.
     *
     * @return max num rows
     */
    public Integer getNumRows() {
        return numRows;
    }

    /**
     * Sets the maximum number of rows.
     *
     * @param numRows max num rows
     */
    public void setNumRows(Integer numRows) {
        this.numRows = numRows;
    }

    /**
     * Returns the offset, or null if using variable or not using offset.
     *
     * @return offset
     */
    public Integer getOptionalOffsetRows() {
        return optionalOffsetRows;
    }

    /**
     * Sets the offset.
     *
     * @param optionalOffsetRows offset
     */
    public void setOptionalOffsetRows(Integer optionalOffsetRows) {
        this.optionalOffsetRows = optionalOffsetRows;
    }

    /**
     * Returns the variable providing maximum number of rows, or null if using constant.
     *
     * @return max num rows variable
     */
    public String getNumRowsVariable() {
        return numRowsVariable;
    }

    /**
     * Sets the variable providing maximum number of rows.
     *
     * @param numRowsVariable max num rows variable
     */
    public void setNumRowsVariable(String numRowsVariable) {
        this.numRowsVariable = numRowsVariable;
    }

    /**
     * Returns the name of the variable providing offset values.
     *
     * @return variable name for offset
     */
    public String getOptionalOffsetRowsVariable() {
        return optionalOffsetRowsVariable;
    }

    /**
     * Sets the name of the variable providing offset values.
     *
     * @param optionalOffsetRowsVariable variable name for offset
     */
    public void setOptionalOffsetRowsVariable(String optionalOffsetRowsVariable) {
        this.optionalOffsetRowsVariable = optionalOffsetRowsVariable;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        if (numRowsVariable != null) {
            writer.write(numRowsVariable);
        } else {
            if (numRows != null) {
                writer.write(Integer.toString(numRows));
            } else {
                writer.write(Integer.toString(Integer.MAX_VALUE));
            }
        }

        if (optionalOffsetRowsVariable != null) {
            writer.write(" offset ");
            writer.write(optionalOffsetRowsVariable);
        } else if ((optionalOffsetRows != null) && (optionalOffsetRows != 0)) {
            writer.write(" offset ");
            writer.write(Integer.toString(optionalOffsetRows));
        }
    }
}
