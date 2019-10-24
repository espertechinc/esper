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
package com.espertech.esper.common.internal.context.controller.condition;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.context.mgr.ContextManagerUtil;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceUtil;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackFilter;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;

import java.util.Collection;
import java.util.Map;

public class ContextControllerConditionFilter implements ContextControllerConditionNonHA {

    private final IntSeqKey conditionPath;
    private final Object[] partitionKeys;
    private final ContextConditionDescriptorFilter filter;
    private final ContextControllerConditionCallback callback;
    private final ContextController controller;

    private EPStatementHandleCallbackFilter filterHandle;
    private EventBean lastEvent;

    public ContextControllerConditionFilter(IntSeqKey conditionPath, Object[] partitionKeys, ContextConditionDescriptorFilter filter, ContextControllerConditionCallback callback, ContextController controller) {
        this.conditionPath = conditionPath;
        this.partitionKeys = partitionKeys;
        this.filter = filter;
        this.callback = callback;
        this.controller = controller;
    }

    public boolean activate(EventBean optionalTriggeringEvent, ContextControllerEndConditionMatchEventProvider endConditionMatchEventProvider, Map<String, Object> optionalTriggeringPattern) {
        AgentInstanceContext agentInstanceContext = controller.getRealization().getAgentInstanceContextCreate();

        FilterHandleCallback filterCallback = new FilterHandleCallback() {
            public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
                filterMatchFound(theEvent);
            }

            public boolean isSubSelect() {
                return false;
            }
        };

        FilterValueSetParam[][] addendum = ContextManagerUtil.computeAddendumNonStmt(partitionKeys, filter.getFilterSpecActivatable(), controller.getRealization());
        filterHandle = new EPStatementHandleCallbackFilter(agentInstanceContext.getEpStatementAgentInstanceHandle(), filterCallback);
        FilterValueSetParam[][] filterValueSet = filter.getFilterSpecActivatable().getValueSet(null, addendum, agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
        agentInstanceContext.getFilterService().add(filter.getFilterSpecActivatable().getFilterForEventType(), filterValueSet, filterHandle);
        long filtersVersion = agentInstanceContext.getFilterService().getFiltersVersion();
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);

        boolean match = false;
        if (optionalTriggeringEvent != null) {
            match = AgentInstanceUtil.evaluateFilterForStatement(optionalTriggeringEvent, agentInstanceContext, filterHandle);
        }
        return match;
    }

    public void deactivate() {
        if (filterHandle == null) {
            return;
        }
        AgentInstanceContext agentInstanceContext = controller.getRealization().getAgentInstanceContextCreate();
        FilterValueSetParam[][] addendum = ContextManagerUtil.computeAddendumNonStmt(partitionKeys, filter.getFilterSpecActivatable(), controller.getRealization());
        FilterValueSetParam[][] filterValueSet = filter.getFilterSpecActivatable().getValueSet(null, addendum, agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
        agentInstanceContext.getFilterService().remove(filterHandle, filter.getFilterSpecActivatable().getFilterForEventType(), filterValueSet);
        filterHandle = null;
        long filtersVersion = agentInstanceContext.getStatementContext().getFilterService().getFiltersVersion();
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    public boolean isImmediate() {
        return false;
    }

    public boolean isRunning() {
        return filterHandle != null;
    }

    public ContextConditionDescriptor getDescriptor() {
        return filter;
    }

    public Long getExpectedEndTime() {
        return null;
    }

    private void filterMatchFound(EventBean theEvent) {
        // For OR-type filters we de-duplicate here by keeping the last event instance
        if (filter.getFilterSpecActivatable().getParameters().length > 1) {
            if (theEvent == lastEvent) {
                return;
            }
            lastEvent = theEvent;
        }
        callback.rangeNotification(conditionPath, this, theEvent, null, null, null);
    }
}
