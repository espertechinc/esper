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
package com.espertech.esper.client.hook;

/**
 * Implement this interface when providing a callback for SQL input parameter and column result processing for a statement,
 * converting an input parameter or converting an output column value into any other value.
 * <p>
 * An instance of the class implementating this interface exists typically per statement
 * that the callback has been registered for by means of EPL statement annotation.
 */
public interface SQLColumnTypeConversion {
    /**
     * Return the new type of the column. To leave the type unchanged, return {@link SQLColumnTypeContext#getColumnClassType}
     * or null.
     *
     * @param context contains the database name, query fired, column name, column type and column number
     * @return type of column after conversion
     */
    public Class getColumnType(SQLColumnTypeContext context);

    /**
     * Return the new value of the column. To leave the value unchanged, return {@link SQLColumnValueContext#getColumnValue}.
     *
     * @param context contains the column name, column value and column number
     * @return value of column after conversion
     */
    public Object getColumnValue(SQLColumnValueContext context);

    /**
     * Return the new value of the input parameter. To leave the value unchanged, return {@link SQLInputParameterContext#getParameterValue}.
     *
     * @param context contains the parameter name and number
     * @return value of parameter after conversion
     */
    public Object getParameterValue(SQLInputParameterContext context);
}
