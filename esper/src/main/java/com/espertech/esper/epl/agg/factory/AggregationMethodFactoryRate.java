/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.agg.factory;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.aggregator.AggregatorRate;
import com.espertech.esper.epl.agg.aggregator.AggregatorRateEver;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;
import com.espertech.esper.epl.expression.methodagg.ExprRateAggNode;
import com.espertech.esper.schedule.TimeProvider;

public class AggregationMethodFactoryRate implements AggregationMethodFactory
{
    protected final ExprRateAggNode parent;
    protected final boolean isEver;
    protected final long intervalMSec;
    protected final TimeProvider timeProvider;

    public AggregationMethodFactoryRate(ExprRateAggNode parent, boolean isEver, long intervalMSec, TimeProvider timeProvider)
    {
        this.parent = parent;
        this.isEver = isEver;
        this.intervalMSec = intervalMSec;
        this.timeProvider = timeProvider;
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

    public AggregationMethod make() {
        if (isEver) {
            return new AggregatorRateEver(intervalMSec, timeProvider);
        }
        else {
            return new AggregatorRate();
        }
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationMethodFactoryUtil.validateAggregationType(this, intoTableAgg);
        AggregationMethodFactoryRate that = (AggregationMethodFactoryRate) intoTableAgg;
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