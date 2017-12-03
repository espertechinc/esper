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
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.epl.spec.ContextDetailHashItem;
import com.espertech.esper.filter.*;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.filterspec.FilterValueSetParam;

import java.util.Collection;

public class ContextControllerHashedFilterCallback implements FilterHandleCallback {

    private final AgentInstanceContext agentInstanceContextCreateContext;
    private final EventPropertyGetter getter;
    private final ContextControllerHashedInstanceCallback callback;
    private final EPStatementHandleCallback filterHandle;
    private final FilterServiceEntry filterServiceEntry;

    public ContextControllerHashedFilterCallback(EPServicesContext servicesContext, AgentInstanceContext agentInstanceContextCreateContext, ContextDetailHashItem hashItem, ContextControllerHashedInstanceCallback callback, ContextInternalFilterAddendum filterAddendum) {
        this.agentInstanceContextCreateContext = agentInstanceContextCreateContext;
        this.callback = callback;
        this.getter = hashItem.getLookupable().getGetter();

        filterHandle = new EPStatementHandleCallback(agentInstanceContextCreateContext.getEpStatementAgentInstanceHandle(), this);

        FilterValueSetParam[][] addendum = filterAddendum != null ? filterAddendum.getFilterAddendum(hashItem.getFilterSpecCompiled()) : null;
        FilterValueSet filterValueSet = hashItem.getFilterSpecCompiled().getValueSet(null, addendum, null, null, null);
        filterServiceEntry = servicesContext.getFilterService().add(filterValueSet, filterHandle);
        long filtersVersion = servicesContext.getFilterService().getFiltersVersion();
        agentInstanceContextCreateContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        int value = (Integer) getter.get(theEvent);
        callback.create(value, theEvent);
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
