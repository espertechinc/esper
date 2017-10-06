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

import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

public class AggregationRowStateEvalDesc {
    private final ExprEvaluator[] methodEvals;
    private final AggregationMethodFactory[] methodFactories;
    private final AggregationAccessorSlotPair[] accessAccessors;
    private final AggregationStateFactory[] accessFactories;

    public AggregationRowStateEvalDesc(ExprEvaluator[] methodEvals, AggregationMethodFactory[] methodFactories, AggregationAccessorSlotPair[] accessAccessors, AggregationStateFactory[] accessFactories) {
        this.methodEvals = methodEvals;
        this.methodFactories = methodFactories;
        this.accessAccessors = accessAccessors;
        this.accessFactories = accessFactories;
    }

    public ExprEvaluator[] getMethodEvals() {
        return methodEvals;
    }

    public AggregationMethodFactory[] getMethodFactories() {
        return methodFactories;
    }

    public AggregationAccessorSlotPair[] getAccessAccessors() {
        return accessAccessors;
    }

    public AggregationStateFactory[] getAccessFactories() {
        return accessFactories;
    }
}
