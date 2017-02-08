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
package com.espertech.esper.epl.agg.util;

public class AggregationGroupByLocalGroupDesc {
    private final int numColumns;
    private final AggregationGroupByLocalGroupLevel[] levels;

    public AggregationGroupByLocalGroupDesc(int numColumns, AggregationGroupByLocalGroupLevel[] levels) {
        this.numColumns = numColumns;
        this.levels = levels;
    }

    public AggregationGroupByLocalGroupLevel[] getLevels() {
        return levels;
    }

    public int getNumColumns() {
        return numColumns;
    }
}
