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
package com.espertech.esper.common.internal.epl.agg.groupbylocal;

public class AggregationLocalGroupByPlanForge {

    private final int numMethods;
    private final int numAccess;
    private final AggregationLocalGroupByColumnForge[] columnsForges;
    private final AggregationLocalGroupByLevelForge optionalLevelTopForge;
    private final AggregationLocalGroupByLevelForge[] allLevelsForges;

    public AggregationLocalGroupByPlanForge(int numMethods, int numAccess, AggregationLocalGroupByColumnForge[] columns, AggregationLocalGroupByLevelForge optionalLevelTop, AggregationLocalGroupByLevelForge[] allLevels) {
        this.numMethods = numMethods;
        this.numAccess = numAccess;
        this.columnsForges = columns;
        this.optionalLevelTopForge = optionalLevelTop;
        this.allLevelsForges = allLevels;
    }

    public AggregationLocalGroupByColumnForge[] getColumnsForges() {
        return columnsForges;
    }

    public AggregationLocalGroupByLevelForge getOptionalLevelTopForge() {
        return optionalLevelTopForge;
    }

    public AggregationLocalGroupByLevelForge[] getAllLevelsForges() {
        return allLevelsForges;
    }

    public int getNumMethods() {
        return numMethods;
    }

    public int getNumAccess() {
        return numAccess;
    }
}
