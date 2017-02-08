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
package com.espertech.esper.epl.table.mgmt;

public abstract class TableMetadataColumn {

    private final String columnName;
    private final boolean key;

    protected TableMetadataColumn(String columnName, boolean key) {
        this.columnName = columnName;
        this.key = key;
    }

    public boolean isKey() {
        return key;
    }

    public String getColumnName() {
        return columnName;
    }
}
