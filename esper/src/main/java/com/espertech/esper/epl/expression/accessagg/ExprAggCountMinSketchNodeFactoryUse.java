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
import com.espertech.esper.client.util.CountMinSketchTopK;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.approx.*;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;

public class ExprAggCountMinSketchNodeFactoryUse extends ExprAggCountMinSketchNodeFactoryBase {
    private final ExprEvaluator addOrFrequencyEvaluator;

    public ExprAggCountMinSketchNodeFactoryUse(ExprAggCountMinSketchNode parent, ExprEvaluator addOrFrequencyEvaluator) {
        super(parent);
        this.addOrFrequencyEvaluator = addOrFrequencyEvaluator;
    }

    public Class getResultType() {
        if (parent.getAggType() == CountMinSketchAggType.ADD) {
            return null;
        } else if (parent.getAggType() == CountMinSketchAggType.FREQ) {
            return Long.class;
        } else if (parent.getAggType() == CountMinSketchAggType.TOPK) {
            return CountMinSketchTopK[].class;
        } else {
            throw new UnsupportedOperationException("Unrecognized code " + parent.getAggType());
        }
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new UnsupportedOperationException();
    }

    public AggregationStateFactory getAggregationStateFactory(boolean isMatchRecognize) {
        throw new UnsupportedOperationException();
    }

    public AggregationAccessor getAccessor() {
        if (parent.getAggType() == CountMinSketchAggType.ADD) {
            // modifications handled by agent
            return CountMinSketchAggAccessorDefault.INSTANCE;
        } else if (parent.getAggType() == CountMinSketchAggType.FREQ) {
            return new CountMinSketchAggAccessorFrequency(addOrFrequencyEvaluator);
        } else if (parent.getAggType() == CountMinSketchAggType.TOPK) {
            return CountMinSketchAggAccessorTopk.INSTANCE;
        }
        throw new IllegalStateException("Aggregation accessor not available for this function '" + parent.getAggregationFunctionName() + "'");
    }

    public AggregationAgent getAggregationStateAgent() {
        if (parent.getAggType() == CountMinSketchAggType.ADD) {
            return new CountMinSketchAggAgentAdd(addOrFrequencyEvaluator);
        }
        throw new IllegalStateException("Aggregation agent not available for this function '" + parent.getAggregationFunctionName() + "'");
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        throw new IllegalStateException("Aggregation not compatible");
    }

    public ExprEvaluator getAddOrFrequencyEvaluator() {
        return addOrFrequencyEvaluator;
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return null;
    }
}
