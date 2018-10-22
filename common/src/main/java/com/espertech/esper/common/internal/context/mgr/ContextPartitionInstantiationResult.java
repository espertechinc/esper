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
package com.espertech.esper.common.internal.context.mgr;

import com.espertech.esper.common.internal.context.util.AgentInstance;

import java.util.List;

public class ContextPartitionInstantiationResult {
    private final int subpathOrCPId;
    private final List<AgentInstance> agentInstances;

    public ContextPartitionInstantiationResult(int subpathOrCPId, List<AgentInstance> agentInstances) {
        this.subpathOrCPId = subpathOrCPId;
        this.agentInstances = agentInstances;
    }

    public int getSubpathOrCPId() {
        return subpathOrCPId;
    }

    public List<AgentInstance> getAgentInstances() {
        return agentInstances;
    }
}
