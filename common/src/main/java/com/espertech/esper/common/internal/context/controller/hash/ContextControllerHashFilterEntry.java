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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFilterEntry;
import com.espertech.esper.common.internal.context.mgr.ContextManagerUtil;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackFilter;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;

import java.util.Collection;

public class ContextControllerHashFilterEntry implements FilterHandleCallback, ContextControllerFilterEntry {

    private final ContextControllerHashImpl callback;
    private final IntSeqKey controllerPath;
    private final ContextControllerDetailHashItem item;

    private final EPStatementHandleCallbackFilter filterHandle;
    private final FilterValueSetParam[][] filterValueSet;

    public ContextControllerHashFilterEntry(ContextControllerHashImpl callback, IntSeqKey controllerPath, ContextControllerDetailHashItem item, Object[] parentPartitionKeys) {
        this.callback = callback;
        this.controllerPath = controllerPath;
        this.item = item;

        AgentInstanceContext agentInstanceContext = callback.getAgentInstanceContextCreate();
        this.filterHandle = new EPStatementHandleCallbackFilter(agentInstanceContext.getEpStatementAgentInstanceHandle(), this);
        FilterValueSetParam[][] addendum = ContextManagerUtil.computeAddendumNonStmt(parentPartitionKeys, item.getFilterSpecActivatable(), callback.getRealization());
        this.filterValueSet = item.getFilterSpecActivatable().getValueSet(null, addendum, agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
        agentInstanceContext.getFilterService().add(item.getFilterSpecActivatable().getFilterForEventType(), filterValueSet, filterHandle);
        long filtersVersion = agentInstanceContext.getFilterService().getFiltersVersion();
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        callback.matchFound(item, theEvent, controllerPath);
    }

    public boolean isSubSelect() {
        return false;
    }

    public int getStatementId() {
        return callback.getAgentInstanceContextCreate().getStatementContext().getStatementId();
    }

    public void destroy() {
        AgentInstanceContext agentInstanceContext = callback.getAgentInstanceContextCreate();
        agentInstanceContext.getFilterService().remove(filterHandle, item.getFilterSpecActivatable().getFilterForEventType(), filterValueSet);
        long filtersVersion = agentInstanceContext.getFilterService().getFiltersVersion();
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    public EPStatementHandleCallbackFilter getFilterHandle() {
        return filterHandle;
    }
}
