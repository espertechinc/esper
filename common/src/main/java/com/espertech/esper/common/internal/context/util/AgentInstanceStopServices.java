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

import com.espertech.esper.common.client.type.EPTypeClass;

public class AgentInstanceStopServices {
    public final static EPTypeClass EPTYPE = new EPTypeClass(AgentInstanceStopServices.class);

    private final AgentInstanceContext agentInstanceContext;

    public AgentInstanceStopServices(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }
}
