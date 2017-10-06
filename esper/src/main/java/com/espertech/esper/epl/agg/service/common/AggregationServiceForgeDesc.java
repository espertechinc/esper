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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeGroupKey;

import java.util.List;

public class AggregationServiceForgeDesc {
    private final AggregationServiceFactoryForge aggregationServiceFactoryForge;
    private final List<AggregationServiceAggExpressionDesc> expressions;
    private final List<ExprAggregateNodeGroupKey> groupKeyExpressions;

    public AggregationServiceForgeDesc(AggregationServiceFactoryForge aggregationServiceFactoryForge, List<AggregationServiceAggExpressionDesc> expressions, List<ExprAggregateNodeGroupKey> groupKeyExpressions) {
        this.aggregationServiceFactoryForge = aggregationServiceFactoryForge;
        this.expressions = expressions;
        this.groupKeyExpressions = groupKeyExpressions;
    }

    public AggregationServiceFactoryForge getAggregationServiceFactoryForge() {
        return aggregationServiceFactoryForge;
    }

    public List<AggregationServiceAggExpressionDesc> getExpressions() {
        return expressions;
    }

    public List<ExprAggregateNodeGroupKey> getGroupKeyExpressions() {
        return groupKeyExpressions;
    }
}
