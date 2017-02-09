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
import java.util.List;

/**
 * Represents a create-variable syntax for creating a new variable.
 */
public class CreateTableClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private String tableName;
    private List<CreateTableColumn> columns;

    /**
     * Ctor.
     */
    public CreateTableClause() {
    }

    /**
     * Ctor.
     *
     * @param tableName the table name
     */
    public CreateTableClause(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Ctor.
     *
     * @param tableName the table name
     * @param columns   table columns
     */
    public CreateTableClause(String tableName, List<CreateTableColumn> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    /**
     * Returns the table name
     *
     * @return table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the table name
     *
     * @param tableName table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the table columns
     *
     * @return table columns
     */
    public List<CreateTableColumn> getColumns() {
        return columns;
    }

    /**
     * Sets the table columns
     *
     * @param columns table columns
     */
    public void setColumns(List<CreateTableColumn> columns) {
        this.columns = columns;
    }

    /**
     * Render create-table clause
     *
     * @param writer to render to
     */
    public void toEPL(StringWriter writer) {
        writer.append("create table ");
        writer.append(tableName);
        writer.append(" (");
        String delimiter = "";
        for (CreateTableColumn col : columns) {
            writer.append(delimiter);
            col.toEPL(writer);
            delimiter = ", ";
        }
        writer.append(")");
    }
}
