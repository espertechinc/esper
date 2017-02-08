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

public class TableMetadataColumnPairPlainCol extends TableMetadataColumnPairBase {
    private final int source;

    public TableMetadataColumnPairPlainCol(int dest, int source) {
        super(dest);
        this.source = source;
    }

    public int getSource() {
        return source;
    }
}
