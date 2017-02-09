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
package com.espertech.esper.plugin;

/**
 * Context for use with {@link PlugInAggregationMultiFunctionStateFactory} provides
 * contextual information at the time an aggregation state is allocated.
 */
public class PlugInAggregationMultiFunctionStateContext {
    private final int agentInstanceId;
    private final Object groupKey;

    /**
     * Ctor.
     *
     * @param agentInstanceId agent instance id
     * @param groupKey        group key, or null if there are no group-by criteria
     */
    public PlugInAggregationMultiFunctionStateContext(int agentInstanceId, Object groupKey) {
        this.agentInstanceId = agentInstanceId;
        this.groupKey = groupKey;
    }

    /**
     * Returns the agent instance id.
     *
     * @return context partition id
     */
    public int getAgentInstanceId() {
        return agentInstanceId;
    }

    /**
     * Returns the group key or null if no group-by criteria are defined
     *
     * @return group key
     */
    public Object getGroupKey() {
        return groupKey;
    }
}
