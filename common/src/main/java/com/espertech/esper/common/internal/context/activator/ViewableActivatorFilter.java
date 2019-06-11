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
package com.espertech.esper.common.internal.context.activator;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackFilter;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Collection;

public class ViewableActivatorFilter implements ViewableActivator {

    protected FilterSpecActivatable filterSpec;
    protected boolean canIterate;
    protected Integer streamNumFromClause;
    protected boolean isSubSelect;
    protected int subselectNumber;

    public void setFilterSpec(FilterSpecActivatable filterSpec) {
        this.filterSpec = filterSpec;
    }

    public void setCanIterate(boolean canIterate) {
        this.canIterate = canIterate;
    }

    public void setStreamNumFromClause(Integer streamNumFromClause) {
        this.streamNumFromClause = streamNumFromClause;
    }

    public void setSubSelect(boolean subSelect) {
        isSubSelect = subSelect;
    }

    public void setSubselectNumber(int subselectNumber) {
        this.subselectNumber = subselectNumber;
    }

    public EventType getEventType() {
        return filterSpec.getResultEventType();
    }

    public ViewableActivationResult activate(final AgentInstanceContext agentInstanceContext, boolean isSubselect, boolean isRecoveringResilient) {

        FilterValueSetParam[][] addendum = null;
        if (agentInstanceContext.getAgentInstanceFilterProxy() != null) {
            addendum = agentInstanceContext.getAgentInstanceFilterProxy().getAddendumFilters(filterSpec, agentInstanceContext);
        }
        FilterValueSetParam[][] filterValues = filterSpec.getValueSet(null, addendum, agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());

        EventStream theStream;
        if (!agentInstanceContext.getAuditProvider().activated() && !agentInstanceContext.getInstrumentationProvider().activated()) {
            theStream = canIterate ? new ZeroDepthStreamIterable(filterSpec.getResultEventType()) : new ZeroDepthStreamNoIterate(filterSpec.getResultEventType());
        } else {
            int streamNum = streamNumFromClause == null ? -1 : streamNumFromClause;
            theStream = canIterate ? new ZeroDepthStreamIterableWAudit(filterSpec.getResultEventType(), agentInstanceContext, filterSpec, streamNum, isSubselect, subselectNumber) : new ZeroDepthStreamNoIterateWAudit(filterSpec.getResultEventType(), agentInstanceContext, filterSpec, streamNum, isSubselect, subselectNumber);
        }

        FilterHandleCallback filterCallback;
        if (filterSpec.getOptionalPropertyEvaluator() == null) {
            filterCallback = new FilterHandleCallback() {
                public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
                    theStream.insert(theEvent);
                }

                public boolean isSubSelect() {
                    return isSubSelect;
                }
            };
        } else {
            filterCallback = new FilterHandleCallback() {
                public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
                    EventBean[] result = filterSpec.getOptionalPropertyEvaluator().getProperty(theEvent, agentInstanceContext);
                    if (result == null) {
                        return;
                    }
                    theStream.insert(result);
                }

                public boolean isSubSelect() {
                    return isSubSelect;
                }
            };
        }

        EPStatementHandleCallbackFilter filterHandle = new EPStatementHandleCallbackFilter(agentInstanceContext.getEpStatementAgentInstanceHandle(), filterCallback);
        agentInstanceContext.getStatementContext().getStatementContextRuntimeServices().getFilterService().add(filterSpec.getFilterForEventType(), filterValues, filterHandle);
        ViewableActivatorFilterStopCallback stopCallback = new ViewableActivatorFilterStopCallback(filterHandle, filterSpec);
        return new ViewableActivationResult(theStream, stopCallback, null, false, false, null, null);
    }
}
