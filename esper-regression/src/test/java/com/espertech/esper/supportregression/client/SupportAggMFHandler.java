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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.plugin.*;

import java.util.ArrayList;
import java.util.List;

public class SupportAggMFHandler implements PlugInAggregationMultiFunctionHandler {

    public static List<AggregationStateKey> providerKeys = new ArrayList<AggregationStateKey>();
    public static List<AggregationAccessor> accessors = new ArrayList<AggregationAccessor>();
    public static List<PlugInAggregationMultiFunctionStateFactory> providerFactories = new ArrayList<PlugInAggregationMultiFunctionStateFactory>();

    private final PlugInAggregationMultiFunctionValidationContext validationContext;

    public SupportAggMFHandler(PlugInAggregationMultiFunctionValidationContext validationContext) {
        this.validationContext = validationContext;
    }

    @Override
    public PlugInAggregationMultiFunctionCodegenType getCodegenType() {
        return PlugInAggregationMultiFunctionCodegenType.CODEGEN_ALL;
    }

    public static void reset() {
        providerKeys.clear();
        accessors.clear();
        providerFactories.clear();
    }

    public static List<AggregationStateKey> getProviderKeys() {
        return providerKeys;
    }

    public static List<AggregationAccessor> getAccessors() {
        return accessors;
    }

    public static List<PlugInAggregationMultiFunctionStateFactory> getProviderFactories() {
        return providerFactories;
    }

    public AggregationStateKey getAggregationStateUniqueKey() {
        // we share single-event stuff
        if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
            AggregationStateKey key = new SupportAggregationStateKey("A1");
            providerKeys.add(key);
            return key;
        }
        // never share anything else
        return new AggregationStateKey() {
        };
    }

    public PlugInAggregationMultiFunctionStateForge getStateForge() {
        return new PlugInAggregationMultiFunctionStateForge() {
            public PlugInAggregationMultiFunctionStateFactory getStateFactory() {
                // for single-event tracking factories for assertions
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFFactorySingleEvent factory = new SupportAggMFFactorySingleEvent();
                    providerFactories.add(factory);
                    return factory;
                }
                return SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).getStateFactory(validationContext);
            }

            public void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFFactorySingleEvent.rowMemberCodegen(context);
                }
                else {
                    SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).rowMemberCodegen(context);
                }
            }

            public void applyEnterCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFFactorySingleEvent.applyEnterCodegen(context);
                }
                else {
                    SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).applyEnterCodegen(validationContext, context);
                }
            }

            public void applyLeaveCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFFactorySingleEvent.applyLeaveCodegen(context);
                }
                else {
                    SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).applyLeaveCodegen(context);
                }
            }

            public void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFFactorySingleEvent.clearCodegen(context);
                }
                else {
                    SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).clearCodegen(context);
                }
            }
        };
    }

    public AggregationAccessorForge getAccessorForge() {
        return new AggregationAccessorForge() {
            public AggregationAccessor getAccessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
                // for single-event tracking accessAccessors for assertions
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFAccessorSingleEvent accessorEvent = new SupportAggMFAccessorSingleEvent();
                    accessors.add(accessorEvent);
                    return accessorEvent;
                }
                return SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).getAccessor();
            }

            public PlugInAggregationMultiFunctionCodegenType getPluginCodegenType() {
                return PlugInAggregationMultiFunctionCodegenType.CODEGEN_ALL;
            }

            public void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {
                // for single-event tracking accessAccessors for assertions
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFAccessorSingleEvent.getValueCodegen(context);
                }
                else {
                    SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).getValueCodegen(context);
                }
            }

            public void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {
                // for single-event tracking accessAccessors for assertions
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFAccessorSingleEvent.getEnumerableEventsCodegen(context);
                }
                else {
                    SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).getEnumerableEventsCodegen(context);
                }
            }

            public void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {
                // for single-event tracking accessAccessors for assertions
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFAccessorSingleEvent.getEnumerableEventCodegen(context);
                }
                else {
                    SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).getEnumerableEventCodegen(context);
                }
            }

            public void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {
                // for single-event tracking accessAccessors for assertions
                if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
                    SupportAggMFAccessorSingleEvent.getEnumerableScalarCodegen(context);
                }
                else {
                    SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).getEnumerableScalarCodegen(context);
                }
            }
        };
    }

    public EPType getReturnType() {
        return SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).
                getReturnType(validationContext.getEventTypes()[0], validationContext.getParameterExpressions());
    }

    public AggregationAgentForge getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext) {
        return null;
    }

    private static class SupportAggregationStateKey implements AggregationStateKey {
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
