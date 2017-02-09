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
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;

import java.math.BigDecimal;
import java.math.BigInteger;

public class AggregationMethodFactorySum implements AggregationMethodFactory {
    protected final ExprSumNode parent;
    protected final Class resultType;
    protected final Class inputValueType;

    public AggregationMethodFactorySum(ExprSumNode parent, Class inputValueType) {
        this.parent = parent;
        this.inputValueType = inputValueType;
        this.resultType = getSumAggregatorType(inputValueType);
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
        return resultType;
    }

    public AggregationMethod make() {
        AggregationMethod method = makeSumAggregator(inputValueType, parent.isHasFilter());
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
        AggregationMethodFactorySum that = (AggregationMethodFactorySum) intoTableAgg;
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationInputType(inputValueType, that.inputValueType);
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
    }

    public AggregationAgent getAggregationStateAgent() {
        return null;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }

    private Class getSumAggregatorType(Class type) {
        if (type == BigInteger.class) {
            return BigInteger.class;
        }
        if (type == BigDecimal.class) {
            return BigDecimal.class;
        }
        if ((type == Long.class) || (type == long.class)) {
            return Long.class;
        }
        if ((type == Integer.class) || (type == int.class)) {
            return Integer.class;
        }
        if ((type == Double.class) || (type == double.class)) {
            return Double.class;
        }
        if ((type == Float.class) || (type == float.class)) {
            return Float.class;
        }
        return Integer.class;
    }

    private AggregationMethod makeSumAggregator(Class type, boolean hasFilter) {
        if (!hasFilter) {
            if (type == BigInteger.class) {
                return new AggregatorSumBigInteger();
            }
            if (type == BigDecimal.class) {
                return new AggregatorSumBigDecimal();
            }
            if ((type == Long.class) || (type == long.class)) {
                return new AggregatorSumLong();
            }
            if ((type == Integer.class) || (type == int.class)) {
                return new AggregatorSumInteger();
            }
            if ((type == Double.class) || (type == double.class)) {
                return new AggregatorSumDouble();
            }
            if ((type == Float.class) || (type == float.class)) {
                return new AggregatorSumFloat();
            }
            return new AggregatorSumNumInteger();
        } else {
            if (type == BigInteger.class) {
                return new AggregatorSumBigIntegerFilter();
            }
            if (type == BigDecimal.class) {
                return new AggregatorSumBigDecimalFilter();
            }
            if ((type == Long.class) || (type == long.class)) {
                return new AggregatorSumLongFilter();
            }
            if ((type == Integer.class) || (type == int.class)) {
                return new AggregatorSumIntegerFilter();
            }
            if ((type == Double.class) || (type == double.class)) {
                return new AggregatorSumDoubleFilter();
            }
            if ((type == Float.class) || (type == float.class)) {
                return new AggregatorSumFloatFilter();
            }
            return new AggregatorSumNumIntegerFilter();
        }
    }
}