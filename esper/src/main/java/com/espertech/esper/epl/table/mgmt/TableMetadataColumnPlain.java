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

public class TableMetadataColumnPlain extends TableMetadataColumn {

    private final int indexPlain;

    public TableMetadataColumnPlain(String columnName, boolean key, int indexPlain) {
        super(columnName, key);
        this.indexPlain = indexPlain;
    }

    public int getIndexPlain() {
        return indexPlain;
    }
}
