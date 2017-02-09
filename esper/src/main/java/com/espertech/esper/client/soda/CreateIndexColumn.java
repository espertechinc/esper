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
import java.util.Locale;

/**
 * Create an index on a named window.
 */
public class CreateIndexColumn implements Serializable {
    private static final long serialVersionUID = 0L;

    private String columnName;
    private CreateIndexColumnType type = CreateIndexColumnType.HASH;

    /**
     * Ctor.
     */
    public CreateIndexColumn() {
    }

    /**
     * Ctor.
     *
     * @param columnName column name
     */
    public CreateIndexColumn(String columnName) {
        this.columnName = columnName;
    }

    /**
     * Ctor.
     *
     * @param columnName colum name
     * @param type       index type
     */
    public CreateIndexColumn(String columnName, CreateIndexColumnType type) {
        this.columnName = columnName;
        this.type = type;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        writer.write(columnName);
        if (type != CreateIndexColumnType.HASH) {
            writer.write(' ');
            writer.write(type.toString().toLowerCase(Locale.ENGLISH));
        }
    }

    /**
     * Returns the column name.
     *
     * @return column name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Set the column name.
     *
     * @param columnName name to set
     */
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * Returns the index type.
     *
     * @return index type
     */
    public CreateIndexColumnType getType() {
        return type;
    }

    /**
     * Sets the index type.
     *
     * @param type of index
     */
    public void setType(CreateIndexColumnType type) {
        this.type = type;
    }
}