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
package com.espertech.esper.example.cycledetect;

import com.espertech.esper.common.client.hook.aggmultifunc.*;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.util.Locale;

public class CycleDetectorAggregationHandler implements AggregationMultiFunctionHandler {

    private static final AggregationMultiFunctionStateKey CYCLE_KEY = new AggregationMultiFunctionStateKey() {
    };

    private final CycleDetectorAggregationForge forge;
    private final AggregationMultiFunctionValidationContext validationContext;

    public CycleDetectorAggregationHandler(CycleDetectorAggregationForge forge, AggregationMultiFunctionValidationContext validationContext) {
        this.forge = forge;
        this.validationContext = validationContext;
    }

    public AggregationMultiFunctionStateKey getAggregationStateUniqueKey() {
        return CYCLE_KEY;   // Share the same provider
    }

    public AggregationMultiFunctionStateMode getStateMode() {
        AggregationMultiFunctionStateModeManaged managed = new AggregationMultiFunctionStateModeManaged();
        InjectionStrategyClassNewInstance injection = new InjectionStrategyClassNewInstance(CycleDetectorAggregationStateFactory.class);
        injection.addExpression("from", forge.getFromExpression());
        injection.addExpression("to", forge.getToExpression());
        managed.setInjectionStrategyAggregationStateFactory(injection);
        return managed;
    }

    public AggregationMultiFunctionAccessorMode getAccessorMode() {
        Class accessor;
        if (validationContext.getFunctionName().toLowerCase(Locale.ENGLISH).equals(CycleDetectorConstant.CYCLEOUTPUT_NAME)) {
            accessor = CycleDetectorAggregationAccessorOutputFactory.class;
        } else {
            accessor = CycleDetectorAggregationAccessorDetectFactory.class;
        }
        AggregationMultiFunctionAccessorModeManaged managed = new AggregationMultiFunctionAccessorModeManaged();
        InjectionStrategyClassNewInstance injection = new InjectionStrategyClassNewInstance(accessor);
        managed.setInjectionStrategyAggregationAccessorFactory(injection);
        return managed;
    }

    public EPType getReturnType() {
        if (validationContext.getFunctionName().toLowerCase(Locale.ENGLISH).equals(CycleDetectorConstant.CYCLEOUTPUT_NAME)) {
            return EPTypeHelper.collectionOfSingleValue(forge.getFromExpression().getForge().getEvaluationType());
        }
        return EPTypeHelper.singleValue(Boolean.class);
    }

    public AggregationMultiFunctionAgentMode getAgentMode() {
        throw new UnsupportedOperationException("Not supported with tables");
    }

    public AggregationMultiFunctionAggregationMethodMode getAggregationMethodMode(AggregationMultiFunctionAggregationMethodContext ctx) {
        throw new UnsupportedOperationException("Not supported with tables");
    }
}
