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
 * Implement this interface when providing a callback for SQL row result processing for a statement,
 * converting each row's values into a POJO.
 * <p>
 * Rows can also be skipped via this callback, determined by the implementation returning a null value for a row.
 * <p>
 * An instance of the class implementating this interface exists typically per statement
 * that the callback has been registered for by means of EPL statement annotation.
 */
public interface SQLOutputRowConversion {
    /**
     * Return the POJO class that represents a row of the SQL query result.
     *
     * @param context receives the context information such as database name, query fired and types returned by query
     * @return class that represents a result row
     */
    public Class getOutputRowType(SQLOutputRowTypeContext context);

    /**
     * Returns the POJO object that represents a row of the SQL query result, or null to indicate to skip this row.
     *
     * @param context receives row result information
     * @return POJO or null value to skip the row
     */
    public Object getOutputRow(SQLOutputRowValueContext context);
}
