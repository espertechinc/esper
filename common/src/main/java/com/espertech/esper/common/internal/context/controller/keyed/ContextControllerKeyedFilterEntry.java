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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFilterEntry;
import com.espertech.esper.common.internal.context.mgr.ContextManagerUtil;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackFilter;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;

public abstract class ContextControllerKeyedFilterEntry implements FilterHandleCallback, ContextControllerFilterEntry {

    protected final ContextControllerKeyedImpl callback;
    protected final IntSeqKey controllerPath;
    protected final ContextControllerDetailKeyedItem item;
    protected final Object[] parentPartitionKeys;

    protected EPStatementHandleCallbackFilter filterHandle;
    protected FilterValueSetParam[][] filterValueSet;

    public ContextControllerKeyedFilterEntry(ContextControllerKeyedImpl callback, IntSeqKey controllerPath, ContextControllerDetailKeyedItem item, Object[] parentPartitionKeys) {
        this.callback = callback;
        this.controllerPath = controllerPath;
        this.item = item;
        this.parentPartitionKeys = parentPartitionKeys;
    }

    public abstract void destroy();

    protected void start(FilterSpecActivatable activatable) {
        if (filterHandle != null) {
            throw new IllegalStateException("Already started");
        }
        AgentInstanceContext agentInstanceContext = callback.getAgentInstanceContextCreate();
        this.filterHandle = new EPStatementHandleCallbackFilter(agentInstanceContext.getEpStatementAgentInstanceHandle(), this);
        FilterValueSetParam[][] addendum = ContextManagerUtil.computeAddendumNonStmt(parentPartitionKeys, activatable, callback.getRealization());
        this.filterValueSet = activatable.getValueSet(null, addendum, agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
        agentInstanceContext.getFilterService().add(activatable.getFilterForEventType(), filterValueSet, filterHandle);
        long filtersVersion = agentInstanceContext.getFilterService().getFiltersVersion();
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    protected void stop(FilterSpecActivatable activatable) {
        if (filterHandle == null) {
            return;
        }
        AgentInstanceContext agentInstanceContext = callback.getAgentInstanceContextCreate();
        agentInstanceContext.getFilterService().remove(filterHandle, activatable.getFilterForEventType(), filterValueSet);
        long filtersVersion = agentInstanceContext.getFilterService().getFiltersVersion();
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
        filterHandle = null;
        filterValueSet = null;
    }

    public boolean isSubSelect() {
        return false;
    }

    public int getStatementId() {
        return callback.getAgentInstanceContextCreate().getStatementContext().getStatementId();
    }

    public EPStatementHandleCallbackFilter getFilterHandle() {
        return filterHandle;
    }
}
