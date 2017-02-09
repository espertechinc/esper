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

import java.sql.ResultSet;

/**
 * For use with {@link SQLColumnTypeConversion}, context of column conversion. Contains the columns information
 * as well as the column result value after reading the value and the result set itself for direct access, if required.
 * <p>
 * Applications should not retain instances of this class as the engine may change and reuse values here.
 */
public class SQLColumnValueContext {
    private String columnName;
    private int columnNumber;
    private Object columnValue;
    private ResultSet resultSet;

    /**
     * Ctor.
     */
    public SQLColumnValueContext() {
    }

    /**
     * Returns column name.
     *
     * @return name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Returns column name
     *
     * @param columnName the name of the column
     */
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * Returns column number.
     *
     * @return column number
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Set column number
     *
     * @param columnNumber to set
     */
    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    /**
     * Returns column value
     *
     * @return value
     */
    public Object getColumnValue() {
        return columnValue;
    }

    /**
     * Set column value
     *
     * @param columnValue to set
     */
    public void setColumnValue(Object columnValue) {
        this.columnValue = columnValue;
    }

    /**
     * Sets the result set.
     *
     * @param resultSet to set
     */
    public void setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    /**
     * Returns the result set.
     *
     * @return result set
     */
    public ResultSet getResultSet() {
        return resultSet;
    }
}
