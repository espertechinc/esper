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

import java.util.Map;

/**
 * For use with {@link SQLOutputRowConversion}, context of row conversion.
 */
public class SQLOutputRowTypeContext {
    private final String db;
    private final String sql;
    private final Map<String, Object> fields;

    /**
     * Ctor.
     *
     * @param db     database
     * @param sql    sql
     * @param fields columns and their types
     */
    public SQLOutputRowTypeContext(String db, String sql, Map<String, Object> fields) {
        this.db = db;
        this.sql = sql;
        this.fields = fields;
    }

    /**
     * Returns the database name.
     *
     * @return database name
     */
    public String getDb() {
        return db;
    }

    /**
     * Returns the sql.
     *
     * @return sql
     */
    public String getSql() {
        return sql;
    }

    /**
     * Returns the column names and types.
     *
     * @return columns
     */
    public Map<String, Object> getFields() {
        return fields;
    }
}
