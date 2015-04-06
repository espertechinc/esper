/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.expression.methodagg;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;

public class ExprRateAggNodeFactory implements AggregationMethodFactory
{
    private final ExprRateAggNode parent;
    private final boolean isEver;
    private final long intervalMSec;

    public ExprRateAggNodeFactory(ExprRateAggNode parent, boolean isEver, long intervalMSec)
    {
        this.parent = parent;
        this.isEver = isEver;
        this.intervalMSec = intervalMSec;
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public Class getResultType()
    {
        return Double.class;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public AggregationStateFactory getAggregationStateFactory(boolean isMatchRecognize) {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public AggregationAccessor getAccessor() {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public AggregationMethod make(MethodResolutionService methodResolutionService, int agentInstanceId, int groupId, int aggregationId) {
        if (isEver) {
            return methodResolutionService.makeRateEverAggregator(agentInstanceId, groupId, aggregationId, intervalMSec);
        }
        else {
            return methodResolutionService.makeRateAggregator(agentInstanceId, groupId, aggregationId);
        }
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationMethodFactoryUtil.validateAggregationType(this, intoTableAgg);
        ExprRateAggNodeFactory that = (ExprRateAggNodeFactory) intoTableAgg;
        if (intervalMSec != that.intervalMSec) {
            throw new ExprValidationException("The size is " +
                    intervalMSec +
                    " and provided is " +
                    that.intervalMSec);
        }
        AggregationMethodFactoryUtil.validateAggregationUnbound(!isEver, !that.isEver);
    }

    public AggregationAgent getAggregationStateAgent() {
        return null;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }
}