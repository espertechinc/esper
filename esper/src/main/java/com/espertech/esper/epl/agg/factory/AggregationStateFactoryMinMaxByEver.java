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
package com.espertech.esper.epl.agg.factory;

import com.espertech.esper.epl.agg.access.AggregationServicePassThru;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.access.AggregationStateMinMaxByEver;
import com.espertech.esper.epl.agg.access.AggregationStateMinMaxByEverSpec;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionSortedMinMaxByNode;
import com.espertech.esper.epl.expression.core.ExprNode;

public class AggregationStateFactoryMinMaxByEver implements AggregationStateFactory {

    protected final ExprAggMultiFunctionSortedMinMaxByNode expr;
    protected final AggregationStateMinMaxByEverSpec spec;

    public AggregationStateFactoryMinMaxByEver(ExprAggMultiFunctionSortedMinMaxByNode expr, AggregationStateMinMaxByEverSpec spec) {
        this.expr = expr;
        this.spec = spec;
    }

    public AggregationState createAccess(int agentInstanceId, boolean join, Object groupKey, AggregationServicePassThru passThru) {
        return new AggregationStateMinMaxByEver(spec);
    }

    public ExprNode getAggregationExpression() {
        return expr;
    }
}
