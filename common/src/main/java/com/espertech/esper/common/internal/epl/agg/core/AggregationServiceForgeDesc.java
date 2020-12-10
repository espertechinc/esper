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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeGroupKey;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.List;

public class AggregationServiceForgeDesc {
    private final AggregationServiceFactoryForge aggregationServiceFactoryForge;
    private final List<AggregationServiceAggExpressionDesc> expressions;
    private final List<ExprAggregateNodeGroupKey> groupKeyExpressions;
    private final List<StmtClassForgeableFactory> additionalForgeables;
    private final FabricCharge fabricCharge;

    public AggregationServiceForgeDesc(AggregationServiceFactoryForge aggregationServiceFactoryForge, List<AggregationServiceAggExpressionDesc> expressions, List<ExprAggregateNodeGroupKey> groupKeyExpressions, List<StmtClassForgeableFactory> additionalForgeables, FabricCharge fabricCharge) {
        this.aggregationServiceFactoryForge = aggregationServiceFactoryForge;
        this.expressions = expressions;
        this.groupKeyExpressions = groupKeyExpressions;
        this.additionalForgeables = additionalForgeables;
        this.fabricCharge = fabricCharge;
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

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}
