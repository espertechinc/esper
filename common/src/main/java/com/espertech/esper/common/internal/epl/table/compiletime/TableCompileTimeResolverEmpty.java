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

import com.espertech.esper.common.client.EventType;

public class TableCompileTimeResolverEmpty implements TableCompileTimeResolver {
    public final static TableCompileTimeResolverEmpty INSTANCE = new TableCompileTimeResolverEmpty();

    private TableCompileTimeResolverEmpty() {
    }

    public TableMetaData resolve(String tableName) {
        return null;
    }

    public TableMetaData resolveTableFromEventType(EventType containedType) {
        return null;
    }
}
