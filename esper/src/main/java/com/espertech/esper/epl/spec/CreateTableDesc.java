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
 * Descriptor for create-table statements.
 */
public class CreateTableDesc implements Serializable {
    private static final long serialVersionUID = -2708705726609018664L;

    private final String tableName;
    private final List<CreateTableColumn> columns;

    public CreateTableDesc(String tableName, List<CreateTableColumn> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public List<CreateTableColumn> getColumns() {
        return columns;
    }
}
