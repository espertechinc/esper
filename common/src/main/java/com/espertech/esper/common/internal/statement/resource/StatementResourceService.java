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
package com.espertech.esper.common.internal.statement.resource;

import java.util.HashMap;
import java.util.Map;

public class StatementResourceService {

    private StatementResourceHolder resourcesUnpartitioned;
    private Map<Integer, StatementResourceHolder> resourcesPartitioned;

    public StatementResourceService(boolean partitioned) {
        if (partitioned) {
            resourcesPartitioned = new HashMap<>();
        }
    }

    public StatementResourceHolder getResourcesUnpartitioned() {
        return resourcesUnpartitioned;
    }

    public Map<Integer, StatementResourceHolder> getResourcesPartitioned() {
        return resourcesPartitioned;
    }

    public StatementResourceHolder getPartitioned(int agentInstanceId) {
        return resourcesPartitioned.get(agentInstanceId);
    }

    public void setUnpartitioned(StatementResourceHolder statementResourceHolder) {
        resourcesUnpartitioned = statementResourceHolder;
    }

    public void setPartitioned(int agentInstanceId, StatementResourceHolder statementResourceHolder) {
        resourcesPartitioned.put(agentInstanceId, statementResourceHolder);
    }

    public StatementResourceHolder getUnpartitioned() {
        return resourcesUnpartitioned;
    }

    public StatementResourceHolder deallocatePartitioned(int agentInstanceId) {
        return resourcesPartitioned.remove(agentInstanceId);
    }

    public StatementResourceHolder deallocateUnpartitioned() {
        StatementResourceHolder unpartitioned = resourcesUnpartitioned;
        resourcesUnpartitioned = null;
        return unpartitioned;
    }
}
