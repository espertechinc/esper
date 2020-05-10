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

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceMgmtCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceTransferServices;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;

public class ViewableActivatorFilterMgmtCallback implements AgentInstanceMgmtCallback {

    private FilterHandle filterHandle;
    private FilterSpecActivatable filterSpecActivatable;

    public ViewableActivatorFilterMgmtCallback(FilterHandle filterHandle, FilterSpecActivatable filterSpecActivatable) {
        this.filterHandle = filterHandle;
        this.filterSpecActivatable = filterSpecActivatable;
    }

    public synchronized void stop(AgentInstanceStopServices services) {
        if (filterHandle != null) {
            FilterValueSetParam[][] filterValues = computeFilterValues(services.getAgentInstanceContext());
            if (filterValues != null) {
                services.getAgentInstanceContext().getFilterService().remove(filterHandle, filterSpecActivatable.getFilterForEventType(), filterValues);
            }
        }
        filterHandle = null;
    }


    public void transfer(AgentInstanceTransferServices services) {
        if (filterHandle == null) {
            return;
        }
        FilterValueSetParam[][] filterValues = computeFilterValues(services.getAgentInstanceContext());
        if (filterValues != null) {
            services.getAgentInstanceContext().getFilterService().remove(filterHandle, filterSpecActivatable.getFilterForEventType(), filterValues);
            services.getTargetFilterService().add(filterSpecActivatable.getFilterForEventType(), filterValues, filterHandle);
        }
    }

    private FilterValueSetParam[][] computeFilterValues(AgentInstanceContext agentInstanceContext) {
        FilterValueSetParam[][] addendum = null;
        if (agentInstanceContext.getAgentInstanceFilterProxy() != null) {
            addendum = agentInstanceContext.getAgentInstanceFilterProxy().getAddendumFilters(filterSpecActivatable, agentInstanceContext);
        }
        return filterSpecActivatable.getValueSet(null, addendum, agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
    }
}
