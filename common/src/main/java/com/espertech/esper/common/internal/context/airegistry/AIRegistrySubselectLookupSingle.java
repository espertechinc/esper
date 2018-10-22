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

public class AIRegistrySubselectLookupSingle implements AIRegistrySubselectLookup {

    private final LookupStrategyDesc strategyDesc;
    private SubordTableLookupStrategy service;

    public AIRegistrySubselectLookupSingle(LookupStrategyDesc strategyDesc) {
        this.strategyDesc = strategyDesc;
    }

    public void assignService(int num, SubordTableLookupStrategy subselectStrategy) {
        service = subselectStrategy;
    }

    public void deassignService(int num) {
        service = null;
    }

    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        return service.lookup(events, context);
    }

    public int getInstanceCount() {
        return service == null ? 0 : 1;
    }

    public String toQueryPlan() {
        return strategyDesc.getLookupStrategy().name();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return strategyDesc;
    }
}
