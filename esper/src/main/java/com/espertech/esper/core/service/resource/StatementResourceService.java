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
package com.espertech.esper.core.service.resource;

import com.espertech.esper.core.context.mgr.ContextStatePathKey;
import com.espertech.esper.pattern.EvalRootState;

import java.util.HashMap;
import java.util.Map;

public class StatementResourceService {

    private StatementResourceHolder resourcesUnpartitioned;
    private Map<Integer, StatementResourceHolder> resourcesPartitioned;
    private Map<ContextStatePathKey, EvalRootState> contextStartEndpoints;
    private Map<ContextStatePathKey, EvalRootState> contextEndEndpoints;

    public StatementResourceService(boolean partitioned) {
        if (partitioned) {
            resourcesPartitioned = new HashMap<Integer, StatementResourceHolder>();
        }
    }

    public StatementResourceHolder getResourcesUnpartitioned() {
        return resourcesUnpartitioned;
    }

    public Map<Integer, StatementResourceHolder> getResourcesPartitioned() {
        return resourcesPartitioned;
    }

    public Map<ContextStatePathKey, EvalRootState> getContextEndEndpoints() {
        return contextEndEndpoints;
    }

    public Map<ContextStatePathKey, EvalRootState> getContextStartEndpoints() {
        return contextStartEndpoints;
    }

    public void startContextPattern(EvalRootState patternStopCallback, boolean startEndpoint, ContextStatePathKey path) {
        this.addContextPattern(patternStopCallback, startEndpoint, path);
    }

    public void stopContextPattern(boolean startEndpoint, ContextStatePathKey path) {
        this.removeContextPattern(startEndpoint, path);
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

    private void removeContextPattern(boolean startEndpoint, ContextStatePathKey path) {
        if (startEndpoint) {
            if (contextStartEndpoints != null) {
                contextStartEndpoints.remove(path);
            }
        } else {
            if (contextEndEndpoints != null) {
                contextEndEndpoints.remove(path);
            }
        }
    }

    private void addContextPattern(EvalRootState rootState, boolean startEndpoint, ContextStatePathKey path) {
        if (startEndpoint) {
            if (contextStartEndpoints == null) {
                contextStartEndpoints = new HashMap<ContextStatePathKey, EvalRootState>();
            }
            contextStartEndpoints.put(path, rootState);
        } else {
            if (contextEndEndpoints == null) {
                contextEndEndpoints = new HashMap<ContextStatePathKey, EvalRootState>();
            }
            contextEndEndpoints.put(path, rootState);
        }
    }
}
