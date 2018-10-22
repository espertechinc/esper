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
package com.espertech.esper.common.internal.context.airegistry;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AIRegistrySubselectLookupMap implements AIRegistrySubselectLookup {

    private final LookupStrategyDesc strategyDesc;
    private final Map<Integer, SubordTableLookupStrategy> services;

    public AIRegistrySubselectLookupMap(LookupStrategyDesc strategyDesc) {
        this.strategyDesc = strategyDesc;
        this.services = new HashMap<>();
    }

    public void assignService(int num, SubordTableLookupStrategy subselectStrategy) {
        services.put(num, subselectStrategy);
    }

    public void deassignService(int num) {
        services.remove(num);
    }

    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        return services.get(context.getAgentInstanceId()).lookup(events, context);
    }

    public int getInstanceCount() {
        return services.size();
    }

    public String toQueryPlan() {
        return strategyDesc.getLookupStrategy().name();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return strategyDesc;
    }
}
