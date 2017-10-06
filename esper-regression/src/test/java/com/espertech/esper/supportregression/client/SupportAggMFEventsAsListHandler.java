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

import com.espertech.esper.epl.agg.access.AggregationAccessorForge;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionAgentContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionHandler;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForge;
import com.espertech.esper.supportregression.bean.SupportBean;

public class SupportAggMFEventsAsListHandler implements PlugInAggregationMultiFunctionHandler {

    private final static AggregationStateKey AGGREGATION_STATE_KEY = new AggregationStateKey() {};

    public AggregationAccessorForge getAccessorForge() {
        return new SupportAggMFEventsAsListAccessorForge();
    }

    public EPType getReturnType() {
        return EPTypeHelper.collectionOfSingleValue(SupportBean.class);
    }

    public AggregationStateKey getAggregationStateUniqueKey() {
        return AGGREGATION_STATE_KEY;
    }

    public PlugInAggregationMultiFunctionStateForge getStateForge() {
        return new SupportAggMFEventsAsListStateForge();
    }

    public AggregationAgentForge getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext) {
        return new SupportAggMFEventsAsListAggregationAgentForge();
    }
}
