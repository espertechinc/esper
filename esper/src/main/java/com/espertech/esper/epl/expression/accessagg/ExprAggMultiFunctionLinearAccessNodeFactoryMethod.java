/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.access.AggregationStateType;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;

public class ExprAggMultiFunctionLinearAccessNodeFactoryMethod implements AggregationMethodFactory
{
    private final ExprAggMultiFunctionLinearAccessNode parent;
    private final EventType collectionEventType;
    private final Class resultType;
    private final int streamNum;

    public ExprAggMultiFunctionLinearAccessNodeFactoryMethod(ExprAggMultiFunctionLinearAccessNode parent, EventType collectionEventType, Class resultType, int streamNum) {
        this.parent = parent;
        this.collectionEventType = collectionEventType;
        this.resultType = resultType;
        this.streamNum = streamNum;
    }

    public Class getResultType() {
        return resultType;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new UnsupportedOperationException();
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public AggregationStateFactory getAggregationStateFactory(boolean isMatchRecognize) {
        throw new UnsupportedOperationException();
    }

    public AggregationAccessor getAccessor() {
        throw new UnsupportedOperationException();
    }

    public AggregationMethod make(MethodResolutionService methodResolutionService, int agentInstanceId, int groupId, int aggregationId) {
        if (parent.getStateType() == AggregationStateType.FIRST) {
            return methodResolutionService.makeFirstEverValueAggregator(agentInstanceId, groupId, aggregationId, resultType, false);
        }
        else if (parent.getStateType() == AggregationStateType.LAST) {
            return methodResolutionService.makeLastEverValueAggregator(agentInstanceId, groupId, aggregationId, resultType, false);
        }
        throw new RuntimeException("Window aggregation function is not available");
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationMethodFactoryUtil.validateAggregationType(this, intoTableAgg);
        ExprAggMultiFunctionLinearAccessNodeFactoryMethod that = (ExprAggMultiFunctionLinearAccessNodeFactoryMethod) intoTableAgg;
        AggregationMethodFactoryUtil.validateStreamNumZero(that.streamNum);
        if (collectionEventType != null) {
            AggregationMethodFactoryUtil.validateEventType(collectionEventType, that.collectionEventType);
        }
        else {
            AggregationMethodFactoryUtil.validateAggregationInputType(resultType, that.resultType);
        }
    }

    public AggregationAgent getAggregationStateAgent() {
        throw new UnsupportedOperationException();
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }
}