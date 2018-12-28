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
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.support.SupportBean;

public class SupportAggMFEventsAsListHandler implements AggregationMultiFunctionHandler {
    private final static AggregationMultiFunctionStateKey AGGREGATION_STATE_KEY = new AggregationMultiFunctionStateKey() {
    };

    public EPType getReturnType() {
        return EPTypeHelper.collectionOfSingleValue(SupportBean.class);
    }

    public AggregationMultiFunctionStateKey getAggregationStateUniqueKey() {
        return AGGREGATION_STATE_KEY;
    }

    public AggregationMultiFunctionStateMode getStateMode() {
        return new AggregationMultiFunctionStateModeManaged().setInjectionStrategyAggregationStateFactory(new InjectionStrategyClassNewInstance(SupportAggMFEventsAsListStateFactory.class));
    }

    public AggregationMultiFunctionAccessorMode getAccessorMode() {
        return new AggregationMultiFunctionAccessorModeManaged().setInjectionStrategyAggregationAccessorFactory(new InjectionStrategyClassNewInstance(SupportAggMFEventsAsListAccessorFactory.class));
    }

    public AggregationMultiFunctionAgentMode getAgentMode() {
        return new AggregationMultiFunctionAgentModeManaged().setInjectionStrategyAggregationAgentFactory(new InjectionStrategyClassNewInstance(SupportAggMFEventsAsListAggregationAgentFactory.class));
    }

    public AggregationMultiFunctionAggregationMethodMode getAggregationMethodMode(AggregationMultiFunctionAggregationMethodContext ctx) {
        throw new UnsupportedOperationException("Table-column-read not implemented");
    }
}
