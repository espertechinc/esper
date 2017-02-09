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
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.aggregator.AggregatorStddev;
import com.espertech.esper.epl.agg.aggregator.AggregatorStddevFilter;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;
import com.espertech.esper.epl.expression.methodagg.ExprStddevNode;

public class AggregationMethodFactoryStddev implements AggregationMethodFactory {
    protected final ExprStddevNode parent;
    protected final Class aggregatedValueType;

    public AggregationMethodFactoryStddev(ExprStddevNode parent, Class aggregatedValueType) {
        this.parent = parent;
        this.aggregatedValueType = aggregatedValueType;
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
        AggregationMethod method = makeStddevAggregator(parent.isHasFilter());
        if (!parent.isDistinct()) {
            return method;
        }
        return AggregationMethodFactoryUtil.makeDistinctAggregator(method, parent.isHasFilter());
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationType(this, intoTableAgg);
        AggregationMethodFactoryStddev that = (AggregationMethodFactoryStddev) intoTableAgg;
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationInputType(aggregatedValueType, that.aggregatedValueType);
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
    }

    public AggregationAgent getAggregationStateAgent() {
        return null;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }

    private AggregationMethod makeStddevAggregator(boolean hasFilter) {
        if (!hasFilter) {
            return new AggregatorStddev();
        }
        return new AggregatorStddevFilter();
    }
}