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
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.epl.spec.ContextDetailConditionFilter;
import com.espertech.esper.filter.FilterHandleCallback;
import com.espertech.esper.filter.FilterServiceEntry;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.filterspec.FilterValueSetParam;
import com.espertech.esper.filterspec.MatchedEventMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ContextControllerConditionFilter implements ContextControllerCondition {

    private final EPServicesContext servicesContext;
    private final AgentInstanceContext agentInstanceContext;
    private final ContextDetailConditionFilter endpointFilterSpec;
    private final ContextControllerConditionCallback callback;
    private final ContextInternalFilterAddendum filterAddendum;

    private EPStatementHandleCallback filterHandle;
    private FilterServiceEntry filterServiceEntry;
    private EventBean lastEvent;

    public ContextControllerConditionFilter(EPServicesContext servicesContext, AgentInstanceContext agentInstanceContext, ContextDetailConditionFilter endpointFilterSpec, ContextControllerConditionCallback callback, ContextInternalFilterAddendum filterAddendum) {
        this.servicesContext = servicesContext;
        this.agentInstanceContext = agentInstanceContext;
        this.endpointFilterSpec = endpointFilterSpec;
        this.callback = callback;
        this.filterAddendum = filterAddendum;
    }

    public void activate(EventBean optionalTriggeringEvent, MatchedEventMap priorMatches, long timeOffset, boolean isRecoveringResilient) {
        FilterHandleCallback filterCallback = new FilterHandleCallback() {
            public int getStatementId() {
                return agentInstanceContext.getStatementContext().getStatementId();
            }

            public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
                filterMatchFound(theEvent);
            }

            public boolean isSubSelect() {
                return false;
            }
        };

        // determine addendum, if any
        FilterValueSetParam[][] addendum = null;
        if (filterAddendum != null) {
            addendum = filterAddendum.getFilterAddendum(endpointFilterSpec.getFilterSpecCompiled());
        }

        filterHandle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), filterCallback);
        FilterValueSet filterValueSet = endpointFilterSpec.getFilterSpecCompiled().getValueSet(null, addendum, agentInstanceContext, agentInstanceContext.getEngineImportService(), agentInstanceContext.getAnnotations());
        filterServiceEntry = servicesContext.getFilterService().add(filterValueSet, filterHandle);
        long filtersVersion = servicesContext.getFilterService().getFiltersVersion();
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);

        if (optionalTriggeringEvent != null) {
            boolean match = StatementAgentInstanceUtil.evaluateFilterForStatement(servicesContext, optionalTriggeringEvent, agentInstanceContext, filterHandle);

            if (match) {
                filterMatchFound(optionalTriggeringEvent);
            }
        }
    }

    private void filterMatchFound(EventBean theEvent) {
        // For OR-type filters we de-duplicate here by keeping the last event instance
        if (endpointFilterSpec.getFilterSpecCompiled().getParameters().length > 1) {
            if (theEvent == lastEvent) {
                return;
            }
            lastEvent = theEvent;
        }
        Map<String, Object> props = Collections.emptyMap();
        if (endpointFilterSpec.getOptionalFilterAsName() != null) {
            props = Collections.<String, Object>singletonMap(endpointFilterSpec.getOptionalFilterAsName(), theEvent);
        }
        callback.rangeNotification(props, this, theEvent, null, null, filterAddendum);
    }

    public void deactivate() {
        if (filterHandle != null) {
            servicesContext.getFilterService().remove(filterHandle, filterServiceEntry);
            filterHandle = null;
            filterServiceEntry = null;
            long filtersVersion = agentInstanceContext.getStatementContext().getFilterService().getFiltersVersion();
            agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
        }
    }

    public boolean isRunning() {
        return filterHandle != null;
    }

    public Long getExpectedEndTime() {
        return null;
    }

    public boolean isImmediate() {
        return false;
    }

    public ContextDetailConditionFilter getEndpointFilterSpec() {
        return endpointFilterSpec;
    }
}
