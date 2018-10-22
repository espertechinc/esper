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
package com.espertech.esper.common.internal.epl.table.compiletime;

import com.espertech.esper.common.internal.epl.util.CompileTimeRegistry;

import java.util.Map;

public class TableCompileTimeRegistry implements CompileTimeRegistry {
    private final Map<String, TableMetaData> tables;

    public TableCompileTimeRegistry(Map<String, TableMetaData> tables) {
        this.tables = tables;
    }

    public void newTable(TableMetaData metaData) {
        if (!metaData.getTableVisibility().isModuleProvidedAccessModifier()) {
            throw new IllegalStateException("Invalid visibility for tables");
        }
        String tableName = metaData.getTableName();
        TableMetaData existing = tables.get(tableName);
        if (existing != null) {
            throw new IllegalStateException("Duplicate table encountered for name '" + tableName + "'");
        }
        tables.put(tableName, metaData);
    }

    public Map<String, TableMetaData> getTables() {
        return tables;
    }

    public TableMetaData getTable(String tableName) {
        return tables.get(tableName);
    }
}
