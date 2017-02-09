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
import com.espertech.esper.epl.expression.methodagg.ExprAvgNode;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class AggregationMethodFactoryAvg implements AggregationMethodFactory {
    protected final ExprAvgNode parent;
    protected final Class childType;
    protected final Class resultType;
    protected final MathContext optionalMathContext;

    public AggregationMethodFactoryAvg(ExprAvgNode parent, Class childType, MathContext optionalMathContext) {
        this.parent = parent;
        this.childType = childType;
        this.resultType = getAvgAggregatorType(childType);
        this.optionalMathContext = optionalMathContext;
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public Class getResultType() {
        return resultType;
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
        AggregationMethod method = makeAvgAggregator(childType, parent.isHasFilter(), optionalMathContext);
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
        AggregationMethodFactoryAvg that = (AggregationMethodFactoryAvg) intoTableAgg;
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationInputType(childType, that.childType);
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
    }

    public AggregationAgent getAggregationStateAgent() {
        return null;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }

    private Class getAvgAggregatorType(Class type) {
        if ((type == BigDecimal.class) || (type == BigInteger.class)) {
            return BigDecimal.class;
        }
        return Double.class;
    }

    private AggregationMethod makeAvgAggregator(Class type, boolean hasFilter, MathContext optionalMathContext) {
        if (hasFilter) {
            if ((type == BigDecimal.class) || (type == BigInteger.class)) {
                return new AggregatorAvgBigDecimalFilter(optionalMathContext);
            }
            return new AggregatorAvgFilter();
        }
        if ((type == BigDecimal.class) || (type == BigInteger.class)) {
            return new AggregatorAvgBigDecimal(optionalMathContext);
        }
        return new AggregatorAvg();
    }
}