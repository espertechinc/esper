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
import java.util.Map;

/**
 * For use with {@link SQLOutputRowConversion}, context of row conversion. Provides row number, column values after reading the row as well as
 * the result set itself for direct access.
 * <p>
 * Applications should not retain instances of this class as the engine may change and reuse values here.
 */
public class SQLOutputRowValueContext {
    private int rowNum;
    private Map<String, Object> values;
    private ResultSet resultSet;

    /**
     * Return row number, the number of the current output row.
     *
     * @return row number
     */
    public int getRowNum() {
        return rowNum;
    }

    /**
     * Set the row number.
     *
     * @param rowNum row number
     */
    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    /**
     * Returns column values.
     *
     * @return values for all columns
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * Set column values.
     *
     * @param values for all columns
     */
    public void setValues(Map<String, Object> values) {
        this.values = values;
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
