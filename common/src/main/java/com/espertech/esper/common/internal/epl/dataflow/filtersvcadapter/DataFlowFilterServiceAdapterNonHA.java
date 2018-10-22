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
package com.espertech.esper.common.internal.epl.dataflow.filtersvcadapter;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackFilter;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;

import java.util.HashMap;
import java.util.Map;

public class DataFlowFilterServiceAdapterNonHA implements DataFlowFilterServiceAdapter {

    public final static DataFlowFilterServiceAdapterNonHA INSTANCE = new DataFlowFilterServiceAdapterNonHA();

    private final Map<FilterHandleCallback, EPStatementHandleCallbackFilter> handlesPerOp = new HashMap<>();

    private DataFlowFilterServiceAdapterNonHA() {
    }

    public void addFilterCallback(FilterHandleCallback filterHandleCallback, AgentInstanceContext agentInstanceContext, EventType eventType, FilterValueSetParam[][] params, int filterCallbackId) {
        EPStatementHandleCallbackFilter handle = new EPStatementHandleCallbackFilter(agentInstanceContext.getEpStatementAgentInstanceHandle(), filterHandleCallback);
        agentInstanceContext.getFilterService().add(eventType, params, handle);
        handlesPerOp.put(filterHandleCallback, handle);
    }

    public void removeFilterCallback(FilterHandleCallback filterHandleCallback, AgentInstanceContext agentInstanceContext, EventType eventType, FilterValueSetParam[][] params, int filterCallbackId) {
        EPStatementHandleCallbackFilter handle = handlesPerOp.remove(filterHandleCallback);
        if (handle == null) {
            return;
        }
        agentInstanceContext.getFilterService().remove(handle, eventType, params);
    }
}
