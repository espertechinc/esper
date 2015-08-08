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

import com.espertech.esper.core.context.factory.*;
import com.espertech.esper.core.context.mgr.ContextStatePathKey;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.pattern.EvalRootState;
import com.espertech.esper.view.Viewable;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StatementResourceService {

    private StatementResourceHolder resourcesZero;
    private Map<Integer, StatementResourceHolder> resourcesNonZero;
    private Map<ContextStatePathKey, EvalRootState> contextStartEndpoints;
    private Map<ContextStatePathKey, EvalRootState> contextEndEndpoints;

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

    public void startContextPartition(StatementAgentInstanceFactoryResult startResult, int agentInstanceId) {

        EPStatementAgentInstanceHandle handle = startResult.getAgentInstanceContext().getEpStatementAgentInstanceHandle();
        StatementResourceHolder recoveryResources = null;

        if (startResult instanceof StatementAgentInstanceFactorySelectResult) {
            StatementAgentInstanceFactorySelectResult selectResult = (StatementAgentInstanceFactorySelectResult) startResult;
            recoveryResources = new StatementResourceHolder(
                    handle,
                    selectResult.getTopViews(),
                    selectResult.getEventStreamViewables(),
                    selectResult.getPatternRoots(),
                    selectResult.getOptionalAggegationService(),
                    selectResult.getSubselectStrategies(),
                    selectResult.getOptionalPostLoadJoin());
        }

        if (startResult instanceof StatementAgentInstanceFactoryCreateWindowResult) {
            StatementAgentInstanceFactoryCreateWindowResult createResult = (StatementAgentInstanceFactoryCreateWindowResult) startResult;
            recoveryResources = new StatementResourceHolder(handle,new Viewable[] {createResult.getTopView()}, null,
                    null, null, null, createResult.getPostLoad());
        }

        if (startResult instanceof StatementAgentInstanceFactoryCreateTableResult) {
            StatementAgentInstanceFactoryCreateTableResult createResult = (StatementAgentInstanceFactoryCreateTableResult) startResult;
            recoveryResources = new StatementResourceHolder(handle,new Viewable[] {createResult.getFinalView()}, null,
                    null, createResult.getOptionalAggegationService(), null, null);
        }

        if (startResult instanceof StatementAgentInstanceFactoryOnTriggerResult) {
            StatementAgentInstanceFactoryOnTriggerResult onTriggerResult = (StatementAgentInstanceFactoryOnTriggerResult) startResult;
            recoveryResources = new StatementResourceHolder(handle, null, null,
                    new EvalRootState[] {onTriggerResult.getOptPatternRoot()},
                    onTriggerResult.getOptionalAggegationService(), onTriggerResult.getSubselectStrategies(), null);
        }

        if (recoveryResources != null) {
            this.addRecoveryResources(agentInstanceId, recoveryResources);
        }
    }

    public void endContextPartition(int agentInstanceId) {
        this.removeRecoveryResources(agentInstanceId);
    }

    private void addRecoveryResources(int agentInstanceId, StatementResourceHolder recoveryResources) {
        if (agentInstanceId == 0) {
            resourcesZero = recoveryResources;
        }
        else {
            if (resourcesNonZero == null) {
                resourcesNonZero = new TreeMap<Integer, StatementResourceHolder>();
            }
            resourcesNonZero.put(agentInstanceId, recoveryResources);
        }
    }

    private void removeRecoveryResources(int agentInstanceId) {
        if (agentInstanceId == 0) {
            resourcesZero = null;
        }
        else {
            if (resourcesNonZero != null) {
                resourcesNonZero.remove(agentInstanceId);
            }
        }
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
