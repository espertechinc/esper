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
 * For use with {@link SQLColumnTypeConversion}, context of column conversion.
 */
public class SQLColumnTypeContext {
    private final String db;
    private final String sql;
    private final String columnName;
    private final Class columnClassType;
    private final int columnSqlType;
    private final int columnNumber;

    /**
     * Ctor.
     *
     * @param db              database
     * @param sql             sql
     * @param columnName      column name
     * @param columnClassType column type
     * @param columnSqlType   sql type
     * @param columnNumber    column number starting at 1
     */
    public SQLColumnTypeContext(String db, String sql, String columnName, Class columnClassType, int columnSqlType, int columnNumber) {
        this.db = db;
        this.sql = sql;
        this.columnName = columnName;
        this.columnClassType = columnClassType;
        this.columnSqlType = columnSqlType;
        this.columnNumber = columnNumber;
    }

    /**
     * Get database name.
     *
     * @return db name
     */
    public String getDb() {
        return db;
    }

    /**
     * Returns sql.
     *
     * @return sql
     */
    public String getSql() {
        return sql;
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
     * Returns column type.
     *
     * @return column type
     */
    public Class getColumnClassType() {
        return columnClassType;
    }

    /**
     * Returns column sql type.
     *
     * @return sql type
     */
    public int getColumnSqlType() {
        return columnSqlType;
    }

    /**
     * Returns column number starting at 1.
     *
     * @return column number
     */
    public int getColumnNumber() {
        return columnNumber;
    }
}
