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

import com.espertech.esper.epl.agg.service.common.AggregationServiceAggExpressionDesc;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.List;

public class AggregationGroupByLocalGroupLevel {
    private final ExprNode[] partitionExpr;
    private final List<AggregationServiceAggExpressionDesc> expressions;

    public AggregationGroupByLocalGroupLevel(ExprNode[] partitionExpr, List<AggregationServiceAggExpressionDesc> expressions) {
        this.partitionExpr = partitionExpr;
        this.expressions = expressions;
    }

    public ExprNode[] getPartitionExpr() {
        return partitionExpr;
    }

    public List<AggregationServiceAggExpressionDesc> getExpressions() {
        return expressions;
    }
}
