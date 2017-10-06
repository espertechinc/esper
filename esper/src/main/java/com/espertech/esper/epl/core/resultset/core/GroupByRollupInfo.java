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
package com.espertech.esper.epl.core.resultset.core;

import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.expression.core.ExprNode;

public class GroupByRollupInfo {
    private final ExprNode[] exprNodes;
    private final AggregationGroupByRollupDesc rollupDesc;

    public GroupByRollupInfo(ExprNode[] exprNodes, AggregationGroupByRollupDesc rollupDesc) {
        this.exprNodes = exprNodes;
        this.rollupDesc = rollupDesc;
    }

    public ExprNode[] getExprNodes() {
        return exprNodes;
    }

    public AggregationGroupByRollupDesc getRollupDesc() {
        return rollupDesc;
    }
}
