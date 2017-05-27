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

import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionAgentContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionHandler;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateFactory;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionValidationContext;

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

    public PlugInAggregationMultiFunctionStateFactory getStateFactory() {

        // for single-event tracking factories for assertions
        if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
            SupportAggMFFactorySingleEvent factory = new SupportAggMFFactorySingleEvent();
            providerFactories.add(factory);
            return factory;
        }
        return SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).getStateFactory(validationContext);
    }

    public AggregationAccessor getAccessor() {
        // for single-event tracking accessors for assertions
        if (SupportAggMFFunc.isSingleEvent(validationContext.getFunctionName())) {
            SupportAggMFAccessorSingleEvent accessorEvent = new SupportAggMFAccessorSingleEvent();
            accessors.add(accessorEvent);
            return accessorEvent;
        }
        return SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).getAccessor();
    }

    public EPType getReturnType() {
        return SupportAggMFFunc.fromFunctionName(validationContext.getFunctionName()).
                getReturnType(validationContext.getEventTypes()[0], validationContext.getParameterExpressions());
    }

    public AggregationAgent getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext) {
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
