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
package com.espertech.esper.common.internal.view.firstevent;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.view.core.*;

public class FirstEventViewFactory implements DataWindowViewFactory {
    protected EventType eventType;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new FirstEventView(this, agentInstanceViewFactoryContext.getAgentInstanceContext());
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getViewName() {
        return ViewEnum.FIRST_EVENT.getName();
    }
}
