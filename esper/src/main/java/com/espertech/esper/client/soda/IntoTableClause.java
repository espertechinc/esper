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
 * Into-table clause.
 */
public class IntoTableClause implements Serializable {

    private static final long serialVersionUID = -6050352961217205980L;

    private String tableName;

    /**
     * Ctor.
     *
     * @param tableName table name
     */
    public IntoTableClause(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Ctor.
     */
    public IntoTableClause() {
    }

    /**
     * Returns the table name.
     *
     * @return table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the table name.
     *
     * @param tableName table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Renders the clause.
     *
     * @param writer to write to
     */
    public void toEPL(StringWriter writer) {
        writer.append("into table ");
        writer.append(tableName);
        writer.append(" ");
    }
}
