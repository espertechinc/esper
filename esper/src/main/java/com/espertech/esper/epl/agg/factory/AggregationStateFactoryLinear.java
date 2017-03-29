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

import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;

public class AggregationStateFactoryLinear implements AggregationStateFactory {

    protected final ExprAggMultiFunctionLinearAccessNode expr;
    protected final int streamNum;
    protected final ExprEvaluator optionalFilter;

    public AggregationStateFactoryLinear(ExprAggMultiFunctionLinearAccessNode expr, int streamNum, ExprEvaluator optionalFilter) {
        this.expr = expr;
        this.streamNum = streamNum;
        this.optionalFilter = optionalFilter;
    }

    public AggregationState createAccess(int agentInstanceId, boolean join, Object groupKey, AggregationServicePassThru passThru) {
        if (join) {
            if (optionalFilter != null) {
                return new AggregationStateLinearJoinWFilter(streamNum, optionalFilter);
            }
            return new AggregationStateLinearJoinImpl(streamNum);
        }
        if (optionalFilter != null) {
            return new AggregationStateLinearWFilter(streamNum, optionalFilter);
        }
        return new AggregationStateLinearImpl(streamNum);
    }

    public ExprNode getAggregationExpression() {
        return expr;
    }
}
