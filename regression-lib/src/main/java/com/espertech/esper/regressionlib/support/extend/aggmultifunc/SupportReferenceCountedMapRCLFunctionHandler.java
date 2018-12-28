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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.hook.aggmultifunc.*;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

public class SupportReferenceCountedMapRCLFunctionHandler implements AggregationMultiFunctionHandler {
    private final ExprNode eval;

    public SupportReferenceCountedMapRCLFunctionHandler(ExprNode eval) {
        this.eval = eval;
    }

    public EPType getReturnType() {
        return EPTypeHelper.singleValue(Integer.class);
    }

    public AggregationMultiFunctionStateKey getAggregationStateUniqueKey() {
        throw new UnsupportedOperationException("The lookup function is only for table-column-reads");
    }

    public AggregationMultiFunctionStateMode getStateMode() {
        throw new UnsupportedOperationException("The lookup function is only for table-column-reads");
    }

    public AggregationMultiFunctionAccessorMode getAccessorMode() {
        throw new UnsupportedOperationException("The lookup function is only for table-column-reads");
    }

    public AggregationMultiFunctionAgentMode getAgentMode() {
        throw new UnsupportedOperationException("The lookup function is only for table-column-reads");
    }

    public AggregationMultiFunctionAggregationMethodMode getAggregationMethodMode(AggregationMultiFunctionAggregationMethodContext ctx) {
        return new AggregationMultiFunctionAggregationMethodModeManaged().setInjectionStrategyAggregationMethodFactory(
            new InjectionStrategyClassNewInstance(SupportReferenceCountedMapAggregationMethodFactory.class)
                .addExpression("eval", eval));
    }
}
