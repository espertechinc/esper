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

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.Viewable;

public class AgentInstance {

    private final StopCallback stopCallback;
    private final AgentInstanceContext agentInstanceContext;
    private final Viewable finalView;

    public AgentInstance(StopCallback stopCallback, AgentInstanceContext agentInstanceContext, Viewable finalView) {
        this.stopCallback = stopCallback;
        this.agentInstanceContext = agentInstanceContext;
        this.finalView = finalView;
    }

    public StopCallback getStopCallback() {
        return stopCallback;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public Viewable getFinalView() {
        return finalView;
    }
}
