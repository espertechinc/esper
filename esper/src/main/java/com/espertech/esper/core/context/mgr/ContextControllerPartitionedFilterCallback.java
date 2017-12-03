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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.filter.*;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.filterspec.FilterValueSetParam;

import java.util.Collection;

public class ContextControllerPartitionedFilterCallback implements FilterHandleCallback {

    private final AgentInstanceContext agentInstanceContextCreateContext;
    private final EventPropertyGetter[] getters;
    private final ContextControllerPartitionedInstanceManageCallback callback;
    private final EPStatementHandleCallback filterHandle;
    private final FilterServiceEntry filterServiceEntry;
    private final String optionInitConditionName;

    public ContextControllerPartitionedFilterCallback(EPServicesContext servicesContext, AgentInstanceContext agentInstanceContextCreateContext, EventPropertyGetter[] getters, FilterSpecCompiled filterSpec, ContextControllerPartitionedInstanceManageCallback callback, ContextInternalFilterAddendum filterAddendum, String optionInitConditionName) {
        this.agentInstanceContextCreateContext = agentInstanceContextCreateContext;
        this.callback = callback;
        this.getters = getters;
        this.optionInitConditionName = optionInitConditionName;

        filterHandle = new EPStatementHandleCallback(agentInstanceContextCreateContext.getEpStatementAgentInstanceHandle(), this);

        FilterValueSetParam[][] addendum = filterAddendum != null ? filterAddendum.getFilterAddendum(filterSpec) : null;
        FilterValueSet filterValueSet = filterSpec.getValueSet(null, addendum, null, null, null);
        filterServiceEntry = servicesContext.getFilterService().add(filterValueSet, filterHandle);
        long filtersVersion = servicesContext.getFilterService().getFiltersVersion();
        agentInstanceContextCreateContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        Object key;
        if (getters.length > 1) {
            Object[] keys = new Object[getters.length];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = getters[i].get(theEvent);
            }
            key = new MultiKeyUntyped(keys);
        } else {
            key = getters[0].get(theEvent);
        }

        callback.createKey(key, theEvent, allStmtMatches, optionInitConditionName);
    }

    public boolean isSubSelect() {
        return false;
    }

    public int getStatementId() {
        return agentInstanceContextCreateContext.getStatementContext().getStatementId();
    }

    public void destroy(FilterService filterService) {
        filterService.remove(filterHandle, filterServiceEntry);
        long filtersVersion = agentInstanceContextCreateContext.getStatementContext().getFilterService().getFiltersVersion();
        agentInstanceContextCreateContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    public EPStatementHandleCallback getFilterHandle() {
        return filterHandle;
    }
}
