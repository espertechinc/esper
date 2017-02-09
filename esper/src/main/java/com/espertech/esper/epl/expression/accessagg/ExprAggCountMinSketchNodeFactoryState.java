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
import com.espertech.esper.epl.agg.factory.AggregationStateFactoryCountMinSketch;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.approx.CountMinSketchAggAccessorDefault;
import com.espertech.esper.epl.approx.CountMinSketchAggType;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Arrays;

public class ExprAggCountMinSketchNodeFactoryState extends ExprAggCountMinSketchNodeFactoryBase {
    private final AggregationStateFactoryCountMinSketch stateFactory;

    public ExprAggCountMinSketchNodeFactoryState(AggregationStateFactoryCountMinSketch stateFactory) {
        super(stateFactory.getParent());
        this.stateFactory = stateFactory;
    }

    public Class getResultType() {
        return null;
    }

    public AggregationAccessor getAccessor() {
        return CountMinSketchAggAccessorDefault.INSTANCE;
    }

    public AggregationStateFactory getAggregationStateFactory(boolean isMatchRecognize) {
        // For match-recognize we don't allow
        if (isMatchRecognize) {
            throw new IllegalStateException("Count-min-sketch is not supported for match-recognize");
        }
        return stateFactory;
    }

    public AggregationAgent getAggregationStateAgent() {
        throw new UnsupportedOperationException();
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        ExprAggCountMinSketchNodeFactoryUse use = (ExprAggCountMinSketchNodeFactoryUse) intoTableAgg;
        CountMinSketchAggType aggType = use.getParent().getAggType();
        if (aggType == CountMinSketchAggType.FREQ || aggType == CountMinSketchAggType.ADD) {
            Class clazz = use.getAddOrFrequencyEvaluator().getType();
            boolean foundMatch = false;
            for (Class allowed : stateFactory.getSpecification().getAgent().getAcceptableValueTypes()) {
                if (JavaClassHelper.isSubclassOrImplementsInterface(clazz, allowed)) {
                    foundMatch = true;
                }
            }
            if (!foundMatch) {
                throw new ExprValidationException("Mismatching parameter return type, expected any of " + Arrays.toString(stateFactory.getSpecification().getAgent().getAcceptableValueTypes()) + " but received " + JavaClassHelper.getClassNameFullyQualPretty(clazz));
            }
        }
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return null;
    }
}
