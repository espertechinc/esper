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

import com.espertech.esper.codegen.core.CodegenTypedParam;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.plugin.*;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.NAME_AGENTINSTANCECONTEXT;

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
        stateContexts.add(stateContext);

        return new SupportAggMFStateSingleEvent();
    }

    public static void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {
        SupportAggMFStateSingleEvent.rowMemberCodegen(context);
    }

    public static void applyEnterCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        SupportAggMFStateSingleEvent.applyEnterCodegen(context);
    }

    public static void applyLeaveCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        SupportAggMFStateSingleEvent.applyLeaveCodegen(context);
    }

    public static void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {
        SupportAggMFStateSingleEvent.clearCodegen(context);
    }
}
