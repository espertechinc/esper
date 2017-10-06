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
package com.espertech.esper.epl.agg.service.groupby;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByReclaimAgedEvalFuncConstant implements AggSvcGroupByReclaimAgedEvalFunc {
    private final double longValue;

    public AggSvcGroupByReclaimAgedEvalFuncConstant(double longValue) {
        this.longValue = longValue;
    }

    public Double getLongValue() {
        return longValue;
    }
}