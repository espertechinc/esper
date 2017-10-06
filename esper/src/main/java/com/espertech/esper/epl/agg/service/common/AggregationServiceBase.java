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

import com.espertech.esper.epl.expression.core.ExprEvaluator;

/**
 * All aggregation services require evaluation nodes which supply the value to be aggregated (summed, averaged, etc.)
 * and aggregation state factories to make new aggregation states.
 */
public abstract class AggregationServiceBase implements AggregationService {
    /**
     * Evaluation nodes under.
     */
    protected ExprEvaluator[] evaluators;

    /**
     * Ctor.
     *
     * @param evaluators - are the child node of each aggregation function used for computing the value to be aggregated
     */
    public AggregationServiceBase(ExprEvaluator[] evaluators) {
        this.evaluators = evaluators;
    }

    public ExprEvaluator[] getEvaluators() {
        return evaluators;
    }
}
