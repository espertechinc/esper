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
import com.espertech.esper.epl.agg.access.AggregationStateImpl;
import com.espertech.esper.epl.agg.access.AggregationStateJoinImpl;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.epl.expression.core.ExprNode;

public class AggregationStateFactoryLinear implements AggregationStateFactory {

    protected final ExprAggMultiFunctionLinearAccessNode expr;
    protected final int streamNum;

    public AggregationStateFactoryLinear(ExprAggMultiFunctionLinearAccessNode expr, int streamNum) {
        this.expr = expr;
        this.streamNum = streamNum;
    }

    public AggregationState createAccess(int agentInstanceId, boolean join, Object groupKey, AggregationServicePassThru passThru) {
        if (join) {
            return new AggregationStateJoinImpl(streamNum);
        }
        return new AggregationStateImpl(streamNum);
    }

    public ExprNode getAggregationExpression() {
        return expr;
    }
}
