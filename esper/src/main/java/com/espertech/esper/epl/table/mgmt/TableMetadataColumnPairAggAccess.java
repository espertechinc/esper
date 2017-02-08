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

import com.espertech.esper.epl.agg.access.AggregationAccessor;

public class TableMetadataColumnPairAggAccess extends TableMetadataColumnPairBase {
    private final AggregationAccessor accessor;

    public TableMetadataColumnPairAggAccess(int dest, AggregationAccessor accessor) {
        super(dest);
        this.accessor = accessor;
    }

    public AggregationAccessor getAccessor() {
        return accessor;
    }
}
