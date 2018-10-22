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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

public final class ZeroDepthStreamNoIterateWAudit extends ZeroDepthStreamNoIterate {
    private final AgentInstanceContext agentInstanceContext;
    private final String filterSpecText;
    private final int streamNumber;
    private final boolean subselect;
    private final int subselectNumber;

    public ZeroDepthStreamNoIterateWAudit(EventType eventType, AgentInstanceContext agentInstanceContext, FilterSpecActivatable filterSpec, int streamNumber, boolean subselect, int subselectNumber) {
        super(eventType);
        this.agentInstanceContext = agentInstanceContext;
        this.filterSpecText = filterSpec.getFilterText();
        this.streamNumber = streamNumber;
        this.subselect = subselect;
        this.subselectNumber = subselectNumber;
    }

    @Override
    public void insert(EventBean theEvent) {
        agentInstanceContext.getAuditProvider().stream(theEvent, agentInstanceContext, filterSpecText);
        agentInstanceContext.getInstrumentationProvider().qFilterActivationStream(theEvent.getEventType().getName(), streamNumber, agentInstanceContext, subselect, subselectNumber);
        super.insert(theEvent);
        agentInstanceContext.getInstrumentationProvider().aFilterActivationStream(agentInstanceContext, subselect, subselectNumber);
    }

    @Override
    public void insert(EventBean[] events) {
        agentInstanceContext.getAuditProvider().stream(events, null, agentInstanceContext, filterSpecText);
        super.insert(events);
    }
}


