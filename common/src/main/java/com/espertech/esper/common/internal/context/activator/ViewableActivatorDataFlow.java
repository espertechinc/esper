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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.core.ViewableDefaultImpl;

public class ViewableActivatorDataFlow implements ViewableActivator {

    private EventType eventType;

    public ViewableActivatorDataFlow() {
    }

    public EventType getEventType() {
        return eventType;
    }

    public ViewableActivationResult activate(AgentInstanceContext agentInstanceContext, boolean isSubselect, boolean isRecoveringResilient) {
        Viewable viewable = new ViewableDefaultImpl(eventType);
        return new ViewableActivationResult(viewable, AgentInstanceStopCallback.INSTANCE_NO_ACTION, null, false, false, null, null);
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
