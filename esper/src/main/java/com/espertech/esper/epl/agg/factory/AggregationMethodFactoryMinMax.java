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
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;
import com.espertech.esper.epl.expression.methodagg.ExprMinMaxAggrNode;
import com.espertech.esper.type.MinMaxTypeEnum;

public class AggregationMethodFactoryMinMax implements AggregationMethodFactory {
    protected final ExprMinMaxAggrNode parent;
    protected final Class type;
    protected final boolean hasDataWindows;

    public AggregationMethodFactoryMinMax(ExprMinMaxAggrNode parent, Class type, boolean hasDataWindows) {
        this.parent = parent;
        this.type = type;
        this.hasDataWindows = hasDataWindows;
    }

    public boolean isAccessAggregation() {
        return false;
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

    public Class getResultType() {
        return type;
    }

    public AggregationMethod make() {
        AggregationMethod method = makeMinMaxAggregator(parent.getMinMaxTypeEnum(), type, hasDataWindows, parent.isHasFilter());
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
        AggregationMethodFactoryMinMax that = (AggregationMethodFactoryMinMax) intoTableAgg;
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationInputType(type, that.type);
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
        if (parent.getMinMaxTypeEnum() != that.parent.getMinMaxTypeEnum()) {
            throw new ExprValidationException("The aggregation declares " +
                    parent.getMinMaxTypeEnum().getExpressionText() +
                    " and provided is " +
                    that.parent.getMinMaxTypeEnum().getExpressionText());
        }
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationUnbound(hasDataWindows, that.hasDataWindows);
    }

    public AggregationAgent getAggregationStateAgent() {
        return null;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }

    private AggregationMethod makeMinMaxAggregator(MinMaxTypeEnum minMaxTypeEnum, Class targetType, boolean isHasDataWindows, boolean hasFilter) {
        if (!hasFilter) {
            if (!isHasDataWindows) {
                return new AggregatorMinMaxEver(minMaxTypeEnum);
            }
            return new AggregatorMinMax(minMaxTypeEnum);
        } else {
            if (!isHasDataWindows) {
                return new AggregatorMinMaxEverFilter(minMaxTypeEnum);
            }
            return new AggregatorMinMaxFilter(minMaxTypeEnum);
        }
    }
}
