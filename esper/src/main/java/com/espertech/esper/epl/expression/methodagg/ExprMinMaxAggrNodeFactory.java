/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;

public class ExprMinMaxAggrNodeFactory implements AggregationMethodFactory
{
    private final ExprMinMaxAggrNode parent;
    private final Class type;
    private final boolean hasDataWindows;

    public ExprMinMaxAggrNodeFactory(ExprMinMaxAggrNode parent, Class type, boolean hasDataWindows) {
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

    public Class getResultType()
    {
        return type;
    }

    public AggregationMethod make(MethodResolutionService methodResolutionService, int agentInstanceId, int groupId, int aggregationId) {
        AggregationMethod method = methodResolutionService.makeMinMaxAggregator(agentInstanceId, groupId, aggregationId, parent.getMinMaxTypeEnum(), type, hasDataWindows, parent.isHasFilter());
        if (!parent.isDistinct()) {
            return method;
        }
        return methodResolutionService.makeDistinctAggregator(agentInstanceId, groupId, aggregationId, method, type, parent.isHasFilter());
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationMethodFactoryUtil.validateAggregationType(this, intoTableAgg);
        ExprMinMaxAggrNodeFactory that = (ExprMinMaxAggrNodeFactory) intoTableAgg;
        AggregationMethodFactoryUtil.validateAggregationInputType(type, that.type);
        AggregationMethodFactoryUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
        if (parent.getMinMaxTypeEnum() != that.parent.getMinMaxTypeEnum()) {
            throw new ExprValidationException("The aggregation declares " +
                    parent.getMinMaxTypeEnum().getExpressionText() +
                    " and provided is " +
                    that.parent.getMinMaxTypeEnum().getExpressionText());
        }
        AggregationMethodFactoryUtil.validateAggregationUnbound(hasDataWindows, that.hasDataWindows);
    }

    public AggregationAgent getAggregationStateAgent() {
        return null;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }
}
