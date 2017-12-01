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
package com.espertech.esper.epl.spec;

import java.io.Serializable;
import java.util.List;

/**
 * Specification for creating a named window.
 */
public class CreateIndexDesc implements Serializable {
    private static final long serialVersionUID = -6758785746637089810L;

    private final boolean unique;
    private final String indexName;
    private final String windowName;
    private final List<CreateIndexItem> columns;

    /**
     * Ctor.
     *
     * @param unique     indicator whether unique or not
     * @param indexName  index name
     * @param windowName window name
     * @param columns    properties to index
     */
    public CreateIndexDesc(boolean unique, String indexName, String windowName, List<CreateIndexItem> columns) {
        this.unique = unique;
        this.indexName = indexName;
        this.windowName = windowName;
        this.columns = columns;
    }

    /**
     * Returns index name.
     *
     * @return index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Returns window name.
     *
     * @return window name
     */
    public String getWindowName() {
        return windowName;
    }

    /**
     * Returns columns.
     *
     * @return columns
     */
    public List<CreateIndexItem> getColumns() {
        return columns;
    }

    public boolean isUnique() {
        return unique;
    }
}
