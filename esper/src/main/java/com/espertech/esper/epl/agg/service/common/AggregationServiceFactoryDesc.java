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

public class AggregationServiceFactoryDesc {
    private final AggregationServiceFactory aggregationServiceFactory;
    private final List<AggregationServiceAggExpressionDesc> expressions;
    private final List<ExprAggregateNodeGroupKey> groupKeyExpressions;

    public AggregationServiceFactoryDesc(AggregationServiceFactory aggregationServiceFactory, List<AggregationServiceAggExpressionDesc> expressions, List<ExprAggregateNodeGroupKey> groupKeyExpressions) {
        this.aggregationServiceFactory = aggregationServiceFactory;
        this.expressions = expressions;
        this.groupKeyExpressions = groupKeyExpressions;
    }

    public AggregationServiceFactory getAggregationServiceFactory() {
        return aggregationServiceFactory;
    }

    public List<AggregationServiceAggExpressionDesc> getExpressions() {
        return expressions;
    }

    public List<ExprAggregateNodeGroupKey> getGroupKeyExpressions() {
        return groupKeyExpressions;
    }
}
