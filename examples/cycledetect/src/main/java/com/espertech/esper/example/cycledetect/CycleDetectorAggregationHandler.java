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

import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAccessorForge;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.plugin.*;

import java.util.Locale;

public class CycleDetectorAggregationHandler implements PlugInAggregationMultiFunctionHandler {

    private static final AggregationStateKey CYCLE_KEY = new AggregationStateKey() {
    };

    private final CycleDetectorAggregationFactory factory;
    private final PlugInAggregationMultiFunctionValidationContext validationContext;

    public CycleDetectorAggregationHandler(CycleDetectorAggregationFactory factory, PlugInAggregationMultiFunctionValidationContext validationContext) {
        this.factory = factory;
        this.validationContext = validationContext;
    }

    public AggregationStateKey getAggregationStateUniqueKey() {
        return CYCLE_KEY;   // Share the same provider
    }

    @Override
    public PlugInAggregationMultiFunctionCodegenType getCodegenType() {
        return PlugInAggregationMultiFunctionCodegenType.CODEGEN_NONE;
    }

    public PlugInAggregationMultiFunctionStateForge getStateForge() {
        return new PlugInAggregationMultiFunctionStateForge() {
            public PlugInAggregationMultiFunctionStateFactory getStateFactory() {
                return new CycleDetectorAggregationStateFactory(factory.getFromExpression().getExprEvaluator(), factory.getToExpression().getExprEvaluator());
            }
        };
    }

    public AggregationAccessorForge getAccessorForge() {
        return new AggregationAccessorForge() {
            public AggregationAccessor getAccessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
                if (validationContext.getFunctionName().toLowerCase(Locale.ENGLISH).equals(CycleDetectorConstant.CYCLEOUTPUT_NAME)) {
                    return new CycleDetectorAggregationAccessorOutput();
                }
                return new CycleDetectorAggregationAccessorDetect();
            }
        };
    }

    public EPType getReturnType() {
        if (validationContext.getFunctionName().toLowerCase(Locale.ENGLISH).equals(CycleDetectorConstant.CYCLEOUTPUT_NAME)) {
            return EPTypeHelper.collectionOfSingleValue(factory.getFromExpression().getEvaluationType());
        }
        return EPTypeHelper.singleValue(Boolean.class);
    }

    public AggregationAgentForge getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext) {
        return null;
    }
}
