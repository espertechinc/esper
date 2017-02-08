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

import com.espertech.esper.client.context.ContextPartitionState;

import java.util.List;
import java.util.Map;

public class ContextControllerTreeAgentInstanceList {

    private long filterVersionAfterAllocation;
    private Object initPartitionKey;
    private Map<String, Object> initContextProperties;
    private List<AgentInstance> agentInstances;
    private ContextPartitionState state;

    public ContextControllerTreeAgentInstanceList(long filterVersionAfterAllocation, Object initPartitionKey, Map<String, Object> initContextProperties, List<AgentInstance> agentInstances, ContextPartitionState state) {
        this.filterVersionAfterAllocation = filterVersionAfterAllocation;
        this.initPartitionKey = initPartitionKey;
        this.initContextProperties = initContextProperties;
        this.agentInstances = agentInstances;
        this.state = state;
    }

    public long getFilterVersionAfterAllocation() {
        return filterVersionAfterAllocation;
    }

    public Object getInitPartitionKey() {
        return initPartitionKey;
    }

    public Map<String, Object> getInitContextProperties() {
        return initContextProperties;
    }

    public List<AgentInstance> getAgentInstances() {
        return agentInstances;
    }

    public ContextPartitionState getState() {
        return state;
    }

    public void setState(ContextPartitionState state) {
        this.state = state;
    }

    public void clearAgentInstances() {
        agentInstances.clear();
    }
}
