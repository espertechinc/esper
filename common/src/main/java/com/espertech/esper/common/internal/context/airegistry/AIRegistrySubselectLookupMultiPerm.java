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
import com.espertech.esper.common.internal.collection.ArrayWrap;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;

import java.util.Collection;

public class AIRegistrySubselectLookupMultiPerm implements AIRegistrySubselectLookup {

    private final LookupStrategyDesc strategyDesc;
    private final ArrayWrap<SubordTableLookupStrategy> strategies;
    private int count;

    public AIRegistrySubselectLookupMultiPerm(LookupStrategyDesc strategyDesc) {
        this.strategyDesc = strategyDesc;
        this.strategies = new ArrayWrap<>(SubordTableLookupStrategy.class, 10);
    }

    public void assignService(int num, SubordTableLookupStrategy subselectStrategy) {
        AIRegistryUtil.checkExpand(num, strategies);
        strategies.getArray()[num] = subselectStrategy;
        count++;
    }

    public void deassignService(int num) {
        strategies.getArray()[num] = null;
        count--;
    }

    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        return strategies.getArray()[context.getAgentInstanceId()].lookup(events, context);
    }

    public int getInstanceCount() {
        return count;
    }

    public String toQueryPlan() {
        return strategyDesc.getLookupStrategy().name();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return strategyDesc;
    }
}
