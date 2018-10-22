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
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;

/**
 * Service for isolating non-HA and HA filter operation.
 * Implementations map the operator to a filter handle.
 */
public interface DataFlowFilterServiceAdapter {
    void addFilterCallback(
            FilterHandleCallback filterHandleCallback,
            AgentInstanceContext agentInstanceContext,
            EventType eventType,
            FilterValueSetParam[][] params,
            int filterCallbackId);

    void removeFilterCallback(
            FilterHandleCallback filterHandleCallback,
            AgentInstanceContext agentInstanceContext,
            EventType eventType,
            FilterValueSetParam[][] params,
            int filterCallbackId);
}
