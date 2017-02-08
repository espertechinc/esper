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
package com.espertech.esper.regression.client;

import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateFactory;
import com.espertech.esper.epl.agg.access.AggregationState;

import java.util.ArrayList;
import java.util.List;

public class SupportAggMFFactorySingleEvent implements PlugInAggregationMultiFunctionStateFactory {

    private static List<PlugInAggregationMultiFunctionStateContext> stateContexts = new ArrayList<PlugInAggregationMultiFunctionStateContext>();

    public static void reset() {
        stateContexts.clear();
    }

    public static void clear() {
        stateContexts.clear();
    }

    public static List<PlugInAggregationMultiFunctionStateContext> getStateContexts() {
        return stateContexts;
    }

    public AggregationState makeAggregationState(PlugInAggregationMultiFunctionStateContext stateContext) {
        stateContexts.add(stateContext);;
        return new SupportAggMFStateSingleEvent();
    }
}
