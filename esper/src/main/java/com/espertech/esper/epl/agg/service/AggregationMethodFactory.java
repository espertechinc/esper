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
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;

/**
 * Factory for aggregation methods.
 */
public interface AggregationMethodFactory {
    public boolean isAccessAggregation();

    public AggregationMethod make();

    public Class getResultType();

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize);

    public AggregationStateFactory getAggregationStateFactory(boolean isMatchRecognize);

    public AggregationAccessor getAccessor();

    public ExprAggregateNodeBase getAggregationExpression();

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException;

    public AggregationAgent getAggregationStateAgent();

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException;
}