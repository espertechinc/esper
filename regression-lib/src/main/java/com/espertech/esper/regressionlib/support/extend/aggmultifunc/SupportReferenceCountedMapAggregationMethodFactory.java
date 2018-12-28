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

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethod;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethodFactory;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethodFactoryContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class SupportReferenceCountedMapAggregationMethodFactory implements AggregationMultiFunctionAggregationMethodFactory {
    private ExprEvaluator eval;

    public AggregationMultiFunctionAggregationMethod newMethod(AggregationMultiFunctionAggregationMethodFactoryContext context) {
        return new SupportReferenceCountedMapAggregationMethod(this);
    }

    public ExprEvaluator getEval() {
        return eval;
    }

    public void setEval(ExprEvaluator eval) {
        this.eval = eval;
    }
}
