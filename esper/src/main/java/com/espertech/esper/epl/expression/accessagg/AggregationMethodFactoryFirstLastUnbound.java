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
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.access.AggregationStateType;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.factory.AggregationMethodFactoryUtil;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;

public class AggregationMethodFactoryFirstLastUnbound implements AggregationMethodFactory {
    protected final ExprAggMultiFunctionLinearAccessNode parent;
    private final EventType collectionEventType;
    private final Class resultType;
    private final int streamNum;

    public AggregationMethodFactoryFirstLastUnbound(ExprAggMultiFunctionLinearAccessNode parent, EventType collectionEventType, Class resultType, int streamNum) {
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

    public AggregationMethod make() {
        if (parent.getStateType() == AggregationStateType.FIRST) {
            return AggregationMethodFactoryUtil.makeFirstEver(false);
        } else if (parent.getStateType() == AggregationStateType.LAST) {
            return AggregationMethodFactoryUtil.makeLastEver(false);
        }
        throw new RuntimeException("Window aggregation function is not available");
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationType(this, intoTableAgg);
        AggregationMethodFactoryFirstLastUnbound that = (AggregationMethodFactoryFirstLastUnbound) intoTableAgg;
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateStreamNumZero(that.streamNum);
        if (collectionEventType != null) {
            com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateEventType(collectionEventType, that.collectionEventType);
        } else {
            com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationInputType(resultType, that.resultType);
        }
    }

    public AggregationAgent getAggregationStateAgent() {
        throw new UnsupportedOperationException();
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }
}