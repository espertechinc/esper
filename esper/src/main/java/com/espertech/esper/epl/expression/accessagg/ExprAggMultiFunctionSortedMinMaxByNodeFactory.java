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
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;

public class ExprAggMultiFunctionSortedMinMaxByNodeFactory implements AggregationMethodFactory {
    private final ExprAggMultiFunctionSortedMinMaxByNode parent;
    private final AggregationAccessor accessor;
    private final Class accessorResultType;
    private final EventType containedEventType;

    private final AggregationStateKey optionalStateKey;
    private final SortedAggregationStateFactoryFactory optionalStateFactory;
    private final AggregationAgent optionalAgent;

    public ExprAggMultiFunctionSortedMinMaxByNodeFactory(ExprAggMultiFunctionSortedMinMaxByNode parent, AggregationAccessor accessor, Class accessorResultType, EventType containedEventType, AggregationStateKey optionalStateKey, SortedAggregationStateFactoryFactory optionalStateFactory, AggregationAgent optionalAgent) {
        this.parent = parent;
        this.accessor = accessor;
        this.accessorResultType = accessorResultType;
        this.containedEventType = containedEventType;
        this.optionalStateKey = optionalStateKey;
        this.optionalStateFactory = optionalStateFactory;
        this.optionalAgent = optionalAgent;
    }

    public boolean isAccessAggregation() {
        return true;
    }

    public AggregationMethod make() {
        throw new UnsupportedOperationException();
    }

    public Class getResultType() {
        return accessorResultType;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        return optionalStateKey;
    }

    public AggregationStateFactory getAggregationStateFactory(boolean isMatchRecognize) {
        if (isMatchRecognize || optionalStateFactory == null) {
            return null;
        }
        return optionalStateFactory.makeFactory();
    }

    public AggregationAccessor getAccessor() {
        return accessor;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationMethodFactoryUtil.validateAggregationType(this, intoTableAgg);
        ExprAggMultiFunctionSortedMinMaxByNodeFactory other = (ExprAggMultiFunctionSortedMinMaxByNodeFactory) intoTableAgg;
        AggregationMethodFactoryUtil.validateEventType(this.containedEventType, other.getContainedEventType());
        AggregationMethodFactoryUtil.validateAggFuncName(parent.getAggregationFunctionName(), other.getParent().getAggregationFunctionName());
    }

    public AggregationAgent getAggregationStateAgent() {
        return optionalAgent;
    }

    public EventType getContainedEventType() {
        return containedEventType;
    }

    public ExprAggMultiFunctionSortedMinMaxByNode getParent() {
        return parent;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return null;
    }
}