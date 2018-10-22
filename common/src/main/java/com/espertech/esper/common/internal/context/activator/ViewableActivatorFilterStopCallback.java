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
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;

public class ViewableActivatorFilterStopCallback implements AgentInstanceStopCallback {

    private FilterHandle filterHandle;
    private FilterSpecActivatable filterSpecActivatable;

    public ViewableActivatorFilterStopCallback(FilterHandle filterHandle, FilterSpecActivatable filterSpecActivatable) {
        this.filterHandle = filterHandle;
        this.filterSpecActivatable = filterSpecActivatable;
    }

    public synchronized void stop(AgentInstanceStopServices services) {
        if (filterHandle != null) {
            FilterValueSetParam[][] addendum = null;
            AgentInstanceContext agentInstanceContext = services.getAgentInstanceContext();
            if (agentInstanceContext.getAgentInstanceFilterProxy() != null) {
                addendum = agentInstanceContext.getAgentInstanceFilterProxy().getAddendumFilters(filterSpecActivatable, agentInstanceContext);
            }
            FilterValueSetParam[][] filterValues = filterSpecActivatable.getValueSet(null, addendum, agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
            services.getAgentInstanceContext().getFilterService().remove(filterHandle, filterSpecActivatable.getFilterForEventType(), filterValues);
        }
        filterHandle = null;
    }
}
