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

public class AggregationLocalGroupByPlan {

    private final int numMethods;
    private final int numAccess;
    private final AggregationLocalGroupByColumn[] columns;
    private final AggregationLocalGroupByLevel optionalLevelTop;
    private final AggregationLocalGroupByLevel[] allLevels;

    public AggregationLocalGroupByPlan(int numMethods, int numAccess, AggregationLocalGroupByColumn[] columns, AggregationLocalGroupByLevel optionalLevelTop, AggregationLocalGroupByLevel[] allLevels) {
        this.numMethods = numMethods;
        this.numAccess = numAccess;
        this.columns = columns;
        this.optionalLevelTop = optionalLevelTop;
        this.allLevels = allLevels;
    }

    public AggregationLocalGroupByColumn[] getColumns() {
        return columns;
    }

    public AggregationLocalGroupByLevel getOptionalLevelTop() {
        return optionalLevelTop;
    }

    public AggregationLocalGroupByLevel[] getAllLevels() {
        return allLevels;
    }

    public int getNumMethods() {
        return numMethods;
    }

    public int getNumAccess() {
        return numAccess;
    }
}
