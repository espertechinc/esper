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

import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

import java.util.Map;

public class TableCollectorImpl implements TableCollector {
    private final Map<String, TableMetaData> moduleTables;

    public TableCollectorImpl(Map<String, TableMetaData> moduleTables) {
        this.moduleTables = moduleTables;
    }

    public void registerTable(String tableName, TableMetaData table) {
        moduleTables.put(tableName, table);
    }
}
