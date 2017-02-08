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
package com.espertech.esper.epl.join.util;

public class QueryPlanIndexDescOnExpr extends QueryPlanIndexDescBase {

    private final String strategyName;
    private final String tableLookupStrategy;

    public QueryPlanIndexDescOnExpr(IndexNameAndDescPair[] tables, String strategyName, String tableLookupStrategy) {
        super(tables);
        this.strategyName = strategyName;
        this.tableLookupStrategy = tableLookupStrategy;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public String getTableLookupStrategy() {
        return tableLookupStrategy;
    }
}
