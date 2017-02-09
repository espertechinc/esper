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
import com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprCountEverNode;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;

public class AggregationMethodFactoryCountEver implements AggregationMethodFactory {
    protected final ExprCountEverNode parent;
    protected final boolean ignoreNulls;

    public AggregationMethodFactoryCountEver(ExprCountEverNode parent, boolean ignoreNulls) {
        this.parent = parent;
        this.ignoreNulls = ignoreNulls;
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public Class getResultType() {
        return long.class;
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
        return makeCountEverValueAggregator(parent.hasFilter(), ignoreNulls);
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationMethodFactoryUtil.validateAggregationType(this, intoTableAgg);
        AggregationMethodFactoryCountEver that = (AggregationMethodFactoryCountEver) intoTableAgg;
        if (that.ignoreNulls != ignoreNulls) {
            throw new ExprValidationException("The aggregation declares " +
                    (ignoreNulls ? "ignore-nulls" : "no-ignore-nulls") +
                    " and provided is " +
                    (that.ignoreNulls ? "ignore-nulls" : "no-ignore-nulls"));
        }
        AggregationMethodFactoryUtil.validateAggregationFilter(parent.hasFilter(), that.parent.hasFilter());
    }

    public AggregationAgent getAggregationStateAgent() {
        return null;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultEvaluator(parent.getPositionalParams(), join, typesPerStream);
    }

    private AggregationMethod makeCountEverValueAggregator(boolean hasFilter, boolean ignoreNulls) {
        if (!hasFilter) {
            if (ignoreNulls) {
                return new AggregatorCountEverNonNull();
            }
            return new AggregatorCountEver();
        } else {
            if (ignoreNulls) {
                return new AggregatorCountEverNonNullFilter();
            }
            return new AggregatorCountEverFilter();
        }
    }
}

