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
import com.espertech.esper.epl.spec.ContextDetailPartitionItem;
import com.espertech.esper.filter.*;

import java.util.Collection;

public class ContextControllerPartitionedFilterCallback implements FilterHandleCallback {

    private final AgentInstanceContext agentInstanceContextCreateContext;
    private final EventPropertyGetter[] getters;
    private final ContextControllerPartitionedInstanceCreateCallback callback;
    private final EPStatementHandleCallback filterHandle;
    private final FilterServiceEntry filterServiceEntry;

    public ContextControllerPartitionedFilterCallback(EPServicesContext servicesContext, AgentInstanceContext agentInstanceContextCreateContext, ContextDetailPartitionItem partitionItem, ContextControllerPartitionedInstanceCreateCallback callback, ContextInternalFilterAddendum filterAddendum) {
        this.agentInstanceContextCreateContext = agentInstanceContextCreateContext;
        this.callback = callback;

        filterHandle = new EPStatementHandleCallback(agentInstanceContextCreateContext.getEpStatementAgentInstanceHandle(), this);

        getters = new EventPropertyGetter[partitionItem.getPropertyNames().size()];
        for (int i = 0; i < partitionItem.getPropertyNames().size(); i++) {
            String propertyName = partitionItem.getPropertyNames().get(i);
            EventPropertyGetter getter = partitionItem.getFilterSpecCompiled().getFilterForEventType().getGetter(propertyName);
            getters[i] = getter;
        }

        FilterValueSetParam[][] addendum = filterAddendum != null ? filterAddendum.getFilterAddendum(partitionItem.getFilterSpecCompiled()) : null;
        FilterValueSet filterValueSet = partitionItem.getFilterSpecCompiled().getValueSet(null, null, addendum);
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

        callback.create(key, theEvent);
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
