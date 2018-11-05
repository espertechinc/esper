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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

import java.util.HashMap;
import java.util.Map;

public class TableDeployment {
    private final Map<String, Table> tables = new HashMap<>(4);

    public void add(String tableName, TableMetaData metadata, EPStatementInitServices services) {
        Table existing = tables.get(tableName);
        if (existing != null) {
            throw new IllegalStateException("Table already found for name '" + tableName + "'");
        }
        Table table = services.getTableManagementService().allocateTable(metadata);
        tables.put(tableName, table);
    }

    public Table getTable(String tableName) {
        return tables.get(tableName);
    }

    public void remove(String tableName) {
        tables.remove(tableName);
    }

    public boolean isEmpty() {
        return tables.isEmpty();
    }

    public Map<String, Table> getTables() {
        return tables;
    }
}
