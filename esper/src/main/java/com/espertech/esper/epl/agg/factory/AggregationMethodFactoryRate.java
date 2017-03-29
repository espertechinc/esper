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

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.*;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.agg.service.AggregationValidationUtil;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;
import com.espertech.esper.epl.expression.methodagg.ExprRateAggNode;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.schedule.TimeProvider;

public class AggregationMethodFactoryRate implements AggregationMethodFactory {
    protected final ExprRateAggNode parent;
    protected final boolean isEver;
    protected final long intervalTime;
    protected final TimeProvider timeProvider;
    protected final TimeAbacus timeAbacus;

    public AggregationMethodFactoryRate(ExprRateAggNode parent, boolean isEver, long intervalTime, TimeProvider timeProvider, TimeAbacus timeAbacus) {
        this.parent = parent;
        this.isEver = isEver;
        this.intervalTime = intervalTime;
        this.timeProvider = timeProvider;
        this.timeAbacus = timeAbacus;
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public Class getResultType() {
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
            if (parent.getPositionalParams().length == 0) {
                return new AggregatorRateEver(intervalTime, timeAbacus.getOneSecond(), timeProvider);
            } else {
                return new AggregatorRateEverFilter(intervalTime, timeAbacus.getOneSecond(), timeProvider);
            }
        } else {
            if (parent.getOptionalFilter() != null) {
                return new AggregatorRateFilter(timeAbacus.getOneSecond());
            }
            return new AggregatorRate(timeAbacus.getOneSecond());
        }
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, intoTableAgg);
        AggregationMethodFactoryRate that = (AggregationMethodFactoryRate) intoTableAgg;
        if (intervalTime != that.intervalTime) {
            throw new ExprValidationException("The size is " +
                    intervalTime +
                    " and provided is " +
                    that.intervalTime);
        }
        AggregationValidationUtil.validateAggregationUnbound(!isEver, !that.isEver);
    }

    public AggregationAgent getAggregationStateAgent() {
        return null;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }
}