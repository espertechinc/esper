/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.service.resource;

import com.espertech.esper.core.context.mgr.ContextStatePathKey;
import com.espertech.esper.pattern.EvalRootState;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StatementResourceService {

    private StatementResourceHolder resourcesZero;
    private Map<Integer, StatementResourceHolder> resourcesNonZero;
    private Map<ContextStatePathKey, EvalRootState> contextStartEndpoints;
    private Map<ContextStatePathKey, EvalRootState> contextEndEndpoints;

    public StatementResourceService(boolean partitioned) {
        if (partitioned) {
            resourcesNonZero = new TreeMap<Integer, StatementResourceHolder>();
        }
    }

    public StatementResourceHolder getResourcesZero() {
        return resourcesZero;
    }

    public Map<Integer, StatementResourceHolder> getResourcesNonZero() {
        return resourcesNonZero;
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

    public StatementResourceHolder allocateNonPartitioned() {
        if (resourcesZero != null) {
            return resourcesZero;
        }
        resourcesZero = new StatementResourceHolder();
        return resourcesZero;
    }

    public StatementResourceHolder allocatePartitioned(int agentInstanceId) {
        StatementResourceHolder resources = resourcesNonZero.get(agentInstanceId);
        if (resources == null) {
            resources = new StatementResourceHolder();
            resourcesNonZero.put(agentInstanceId, resources);
        }
        return resources;
    }

    public StatementResourceHolder getPartitioned(int agentInstanceId) {
        return resourcesNonZero.get(agentInstanceId);
    }

    public StatementResourceHolder getUnpartitioned() {
        return resourcesZero;
    }

    public void deallocatePartitioned(int agentInstanceId) {
        resourcesNonZero.remove(agentInstanceId);
    }

    private void removeContextPattern(boolean startEndpoint, ContextStatePathKey path) {
        if (startEndpoint) {
            if (contextStartEndpoints != null) {
                contextStartEndpoints.remove(path);
            }
        }
        else {
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
        }
        else {
            if (contextEndEndpoints == null) {
                contextEndEndpoints = new HashMap<ContextStatePathKey, EvalRootState>();
            }
            contextEndEndpoints.put(path, rootState);
        }
    }
}
