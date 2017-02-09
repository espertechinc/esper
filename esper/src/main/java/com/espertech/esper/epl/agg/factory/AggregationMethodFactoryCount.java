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
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.core.ExprWildcard;
import com.espertech.esper.epl.expression.methodagg.ExprCountNode;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;

public class AggregationMethodFactoryCount implements AggregationMethodFactory {
    protected final ExprCountNode parent;
    protected final boolean ignoreNulls;
    protected final Class countedValueType;

    public AggregationMethodFactoryCount(ExprCountNode parent, boolean ignoreNulls, Class countedValueType) {
        this.parent = parent;
        this.ignoreNulls = ignoreNulls;
        this.countedValueType = countedValueType;
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public Class getResultType() {
        return Long.class;
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
        AggregationMethod method = makeCountAggregator(ignoreNulls, parent.isHasFilter());
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
        AggregationMethodFactoryCount that = (AggregationMethodFactoryCount) intoTableAgg;
        com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
        if (parent.isDistinct()) {
            com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil.validateAggregationInputType(countedValueType, that.countedValueType);
        }
        if (ignoreNulls != that.ignoreNulls) {
            throw new ExprValidationException("The aggregation declares" +
                    (ignoreNulls ? "" : " no") +
                    " ignore nulls and provided is" +
                    (that.ignoreNulls ? "" : " no") +
                    " ignore nulls");
        }
    }

    public AggregationAgent getAggregationStateAgent() {
        return null;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return getMethodAggregationEvaluatorCountBy(parent.getPositionalParams(), join, typesPerStream);
    }

    public static ExprEvaluator getMethodAggregationEvaluatorCountBy(ExprNode[] childNodes, boolean join, EventType[] typesPerStream)
            throws ExprValidationException {
        if (childNodes[0] instanceof ExprWildcard && childNodes.length == 2) {
            return ExprMethodAggUtil.getDefaultEvaluator(new ExprNode[]{childNodes[1]}, join, typesPerStream);
        }
        if (childNodes[0] instanceof ExprWildcard && childNodes.length == 1) {
            return ExprMethodAggUtil.getDefaultEvaluator(new ExprNode[0], join, typesPerStream);
        }
        return ExprMethodAggUtil.getDefaultEvaluator(childNodes, join, typesPerStream);
    }

    private final AggregationMethod makeCountAggregator(boolean isIgnoreNull, boolean hasFilter) {
        if (!hasFilter) {
            if (isIgnoreNull) {
                return new AggregatorCountNonNull();
            }
            return new AggregatorCount();
        } else {
            if (isIgnoreNull) {
                return new AggregatorCountNonNullFilter();
            }
            return new AggregatorCountFilter();
        }
    }
}