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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Create an index on a named window.
 */
public class CreateIndexColumn implements Serializable {
    private static final long serialVersionUID = 0L;

    private List<Expression> columns;
    private String type;
    private List<Expression> parameters;

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
        this(columnName, CreateIndexColumnType.HASH);
    }

    /**
     * Ctor.
     *
     * @param columnName colum name
     * @param type       index type
     */
    public CreateIndexColumn(String columnName, CreateIndexColumnType type) {
        this.columns = Collections.<Expression>singletonList(Expressions.property(columnName));
        this.type = type.name();
    }

    /**
     * Ctor
     * @param columns columns
     * @param type index type
     * @param parameters index parameters
     */
    public CreateIndexColumn(List<Expression> columns, String type, List<Expression> parameters) {
        this.columns = columns;
        this.type = type;
        this.parameters = parameters;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        if (columns.size() > 1) {
            writer.write("(");
        }

        ExpressionBase.toPrecedenceFreeEPL(columns, writer);
        if (columns.size() > 1) {
            writer.write(")");
        }

        if (type != null && !type.toLowerCase(Locale.ENGLISH).equals(CreateIndexColumnType.HASH.getNameLower())) {
            writer.write(' ');
            writer.write(type.toLowerCase(Locale.ENGLISH));
        }

        if (!parameters.isEmpty()) {
            writer.write("(");
            ExpressionBase.toPrecedenceFreeEPL(parameters, writer);
            writer.write(")");
        }
    }

    /**
     * Returns index column expressions
     *
     * @return columns to be indexed
     */
    public List<Expression> getColumns() {
        return columns;
    }

    /**
     * Sets index column expressions
     *
     * @param columns to be indexed
     */
    public void setColumns(List<Expression> columns) {
        this.columns = columns;
    }

    /**
     * Sets the index type
     *
     * @param type index type name
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the index type name
     *
     * @return index type name
     */
    public String getType() {
        return type;
    }

    /**
     * Returns index parameters
     *
     * @return parameters
     */
    public List<Expression> getParameters() {
        return parameters;
    }

    /**
     * Sets index parameters
     *
     * @param parameters to ser
     */
    public void setParameters(List<Expression> parameters) {
        this.parameters = parameters;
    }

}