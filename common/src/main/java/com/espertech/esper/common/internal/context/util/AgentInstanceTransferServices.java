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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.internal.filtersvc.FilterService;
import com.espertech.esper.common.internal.schedule.SchedulingService;

public class AgentInstanceTransferServices {
    private final AgentInstanceContext agentInstanceContext;
    private final FilterService targetFilterService;
    private final SchedulingService targetSchedulingService;
    private final InternalEventRouter targetInternalEventRouter;

    public AgentInstanceTransferServices(AgentInstanceContext agentInstanceContext, FilterService targetFilterService, SchedulingService targetSchedulingService, InternalEventRouter targetInternalEventRouter) {
        this.agentInstanceContext = agentInstanceContext;
        this.targetFilterService = targetFilterService;
        this.targetSchedulingService = targetSchedulingService;
        this.targetInternalEventRouter = targetInternalEventRouter;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public FilterService getTargetFilterService() {
        return targetFilterService;
    }

    public SchedulingService getTargetSchedulingService() {
        return targetSchedulingService;
    }

    public InternalEventRouter getTargetInternalEventRouter() {
        return targetInternalEventRouter;
    }
}
