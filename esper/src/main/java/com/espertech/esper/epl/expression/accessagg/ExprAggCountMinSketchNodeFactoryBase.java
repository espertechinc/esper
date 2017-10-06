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

import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;

public abstract class ExprAggCountMinSketchNodeFactoryBase implements AggregationMethodFactory {
    protected final ExprAggCountMinSketchNode parent;

    public ExprAggCountMinSketchNodeFactoryBase(ExprAggCountMinSketchNode parent) {
        this.parent = parent;
    }

    public boolean isAccessAggregation() {
        return true;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new UnsupportedOperationException();
    }

    public AggregationMethod make() {
        throw new UnsupportedOperationException();
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public ExprAggCountMinSketchNode getParent() {
        return parent;
    }
}
