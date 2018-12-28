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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.hook.aggmultifunc.*;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategy;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.util.ArrayList;
import java.util.List;

public class SupportAggMFMultiRTHandler implements AggregationMultiFunctionHandler {
    private final AggregationMultiFunctionValidationContext validationContext;

    public static List<AggregationMultiFunctionStateKey> providerKeys = new ArrayList<AggregationMultiFunctionStateKey>();
    public static List<AggregationMultiFunctionStateMode> stateFactoryModes = new ArrayList<>();
    public static List<AggregationMultiFunctionAccessorMode> accessorModes = new ArrayList<>();

    public static void reset() {
        providerKeys.clear();
        stateFactoryModes.clear();
        accessorModes.clear();
    }

    public static List<AggregationMultiFunctionStateKey> getProviderKeys() {
        return providerKeys;
    }

    public static List<AggregationMultiFunctionStateMode> getStateFactoryModes() {
        return stateFactoryModes;
    }

    public static List<AggregationMultiFunctionAccessorMode> getAccessorModes() {
        return accessorModes;
    }

    public SupportAggMFMultiRTHandler(AggregationMultiFunctionValidationContext validationContext) {
        this.validationContext = validationContext;
    }

    public AggregationMultiFunctionStateKey getAggregationStateUniqueKey() {
        // we share single-event stuff
        String functionName = validationContext.getFunctionName();
        if (functionName.equals("se1") || functionName.equals("se2")) {
            AggregationMultiFunctionStateKey key = new SupportAggregationStateKey("A1");
            providerKeys.add(key);
            return key;
        }
        // never share anything else
        return new AggregationMultiFunctionStateKey() {
        };
    }

    public AggregationMultiFunctionStateMode getStateMode() {
        InjectionStrategy injectionStrategy;
        String functionName = validationContext.getFunctionName();
        if (functionName.equals("ss")) {
            injectionStrategy = new InjectionStrategyClassNewInstance(SupportAggMFMultiRTPlainScalarStateFactory.class)
                .addExpression("param", validationContext.getAllParameterExpressions()[0]);
        } else if (functionName.equals("sa") || functionName.equals("sc")) {
            injectionStrategy = new InjectionStrategyClassNewInstance(SupportAggMFMultiRTArrayCollScalarStateFactory.class)
                .addExpression("evaluator", validationContext.getAllParameterExpressions()[0])
                .addConstant("evaluationType", validationContext.getAllParameterExpressions()[0].getForge().getEvaluationType());
        } else if (functionName.equals("se1")) {
            injectionStrategy = new InjectionStrategyClassNewInstance(SupportAggMFMultiRTSingleEventStateFactory.class);
        } else if (functionName.equals("ee")) {
            injectionStrategy = new InjectionStrategyClassNewInstance(SupportAggMFMultiRTEnumerableEventsStateFactory.class);
        } else {
            throw new UnsupportedOperationException("Unknown function '" + functionName + "'");
        }
        AggregationMultiFunctionStateModeManaged mode = new AggregationMultiFunctionStateModeManaged().setInjectionStrategyAggregationStateFactory(injectionStrategy);
        stateFactoryModes.add(mode);
        return mode;
    }

    public AggregationMultiFunctionAccessorMode getAccessorMode() {
        String functionName = validationContext.getFunctionName();
        InjectionStrategy injectionStrategy;
        if (functionName.equals("ss")) {
            injectionStrategy = new InjectionStrategyClassNewInstance(SupportAggMFMultiRTPlainScalarAccessorFactory.class);
        } else if (functionName.equals("sa")) {
            injectionStrategy = new InjectionStrategyClassNewInstance(SupportAggMFMultiRTArrayScalarAccessorFactory.class);
        } else if (functionName.equals("sc")) {
            injectionStrategy = new InjectionStrategyClassNewInstance(SupportAggMFMultiRTCollScalarAccessorFactory.class);
        } else if (functionName.equals("se1") || functionName.equals("se2")) {
            injectionStrategy = new InjectionStrategyClassNewInstance(SupportAggMFMultiRTSingleEventAccessorFactory.class);
        } else if (functionName.equals("ee")) {
            injectionStrategy = new InjectionStrategyClassNewInstance(SupportAggMFMultiRTEnumerableEventsAccessorFactory.class);
        } else {
            throw new IllegalStateException("Unrecognized function name '" + functionName + "'");
        }
        AggregationMultiFunctionAccessorModeManaged mode = new AggregationMultiFunctionAccessorModeManaged().setInjectionStrategyAggregationAccessorFactory(injectionStrategy);
        accessorModes.add(mode);
        return mode;
    }

    public EPType getReturnType() {
        String functionName = validationContext.getFunctionName();
        if (functionName.equals("ss")) {
            return EPTypeHelper.singleValue(validationContext.getAllParameterExpressions()[0].getForge().getEvaluationType());
        } else if (functionName.equals("sa")) {
            return EPTypeHelper.array(validationContext.getAllParameterExpressions()[0].getForge().getEvaluationType());
        } else if (functionName.equals("sc")) {
            return EPTypeHelper.collectionOfSingleValue(validationContext.getAllParameterExpressions()[0].getForge().getEvaluationType());
        } else if (functionName.equals("se1") || functionName.equals("se2")) {
            return EPTypeHelper.singleEvent(validationContext.getEventTypes()[0]);
        } else if (functionName.equals("ee")) {
            return EPTypeHelper.collectionOfEvents(validationContext.getEventTypes()[0]);
        } else {
            throw new IllegalStateException("Unrecognized function name '" + functionName + "'");
        }
    }

    public AggregationMultiFunctionAgentMode getAgentMode() {
        throw new UnsupportedOperationException("This implementation does not support tables");
    }

    public AggregationMultiFunctionAggregationMethodMode getAggregationMethodMode(AggregationMultiFunctionAggregationMethodContext ctx) {
        return null; // not implemented
    }

    private static class SupportAggregationStateKey implements AggregationMultiFunctionStateKey {
        private final String id;

        private SupportAggregationStateKey(String id) {
            this.id = id;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SupportAggregationStateKey that = (SupportAggregationStateKey) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;

            return true;
        }

        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }
}
