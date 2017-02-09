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
import java.util.ArrayList;
import java.util.List;

/**
 * Create an index on a named window.
 */
public class CreateIndexClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private String indexName;
    private String windowName;
    private List<CreateIndexColumn> columns = new ArrayList<CreateIndexColumn>();
    private boolean unique;

    /**
     * Ctor.
     */
    public CreateIndexClause() {
    }

    /**
     * Creates a clause to create a named window.
     *
     * @param windowName is the name of the named window
     * @param properties properties to index
     * @param indexName  name of index
     * @return create variable clause
     */
    public static CreateIndexClause create(String indexName, String windowName, String... properties) {
        return new CreateIndexClause(indexName, windowName, properties);
    }

    /**
     * Creates a clause to create a named window.
     *
     * @param windowName is the name of the named window
     * @param properties properties to index
     * @param indexName  name of index
     * @param unique     for unique index
     * @return create variable clause
     */
    public static CreateIndexClause create(boolean unique, String indexName, String windowName, String... properties) {
        return new CreateIndexClause(indexName, windowName, properties, unique);
    }

    /**
     * Ctor.
     *
     * @param indexName  index name
     * @param windowName named window name
     * @param columns    columns indexed
     */
    public CreateIndexClause(String indexName, String windowName, List<CreateIndexColumn> columns) {
        this(indexName, windowName, columns, false);
    }

    /**
     * Ctor.
     *
     * @param indexName  index name
     * @param windowName named window name
     * @param columns    columns indexed
     * @param unique     unique indicator
     */
    public CreateIndexClause(String indexName, String windowName, List<CreateIndexColumn> columns, boolean unique) {
        this.indexName = indexName;
        this.windowName = windowName;
        this.columns = columns;
        this.unique = unique;
    }

    /**
     * Ctor.
     *
     * @param windowName is the name of the window to create
     * @param indexName  index name
     * @param properties properties to index
     */
    public CreateIndexClause(String indexName, String windowName, String[] properties) {
        this(indexName, windowName, properties, false);
    }

    /**
     * Ctor.
     *
     * @param windowName is the name of the window to create
     * @param indexName  index name
     * @param properties properties to index
     * @param unique     for unique index
     */
    public CreateIndexClause(String indexName, String windowName, String[] properties, boolean unique) {
        this.indexName = indexName;
        this.windowName = windowName;
        for (String prop : properties) {
            columns.add(new CreateIndexColumn(prop));
        }
        this.unique = unique;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        writer.write("create ");
        if (unique) {
            writer.write("unique ");
        }
        writer.write("index ");
        writer.write(indexName);
        writer.write(" on ");
        writer.write(windowName);
        writer.write('(');
        String delimiter = "";
        for (CreateIndexColumn prop : columns) {
            writer.write(delimiter);
            prop.toEPL(writer);
            delimiter = ", ";
        }
        writer.write(')');
    }

    /**
     * Returns index name.
     *
     * @return name of index
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Set index name.
     *
     * @param indexName name of index
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Returns window name.
     *
     * @return name of window
     */
    public String getWindowName() {
        return windowName;
    }

    /**
     * Sets window.
     *
     * @param windowName to index
     */
    public void setWindowName(String windowName) {
        this.windowName = windowName;
    }

    /**
     * Returns columns.
     *
     * @return columns
     */
    public List<CreateIndexColumn> getColumns() {
        return columns;
    }

    /**
     * Sets columns.
     *
     * @param columns to index
     */
    public void setColumns(List<CreateIndexColumn> columns) {
        this.columns = columns;
    }

    /**
     * Returns unique indicator.
     *
     * @return unique indicator
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Sets unique indicator.
     *
     * @param unique unique indicator
     */
    public void setUnique(boolean unique) {
        this.unique = unique;
    }
}