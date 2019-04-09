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
package com.espertech.esper.common.internal.context.aifactory.update;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactory;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperStart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatementAgentInstanceFactoryUpdate implements StatementAgentInstanceFactory, StatementReadyCallback {

    private InternalEventRouterDesc desc;
    private InternalRoutePreprocessView viewable;
    private Map<Integer, SubSelectFactory> subselects;

    public void setDesc(InternalEventRouterDesc desc) {
        this.desc = desc;
    }

    public void setSubselects(Map<Integer, SubSelectFactory> subselects) {
        this.subselects = subselects;
    }

    public EventType getStatementEventType() {
        return desc.getEventType();
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        viewable = new InternalRoutePreprocessView(desc.getEventType(), statementContext.getStatementResultService());
        desc.setAnnotations(statementContext.getAnnotations());
    }

    public void statementCreate(StatementContext statementContext) {
    }

    public void statementDestroy(StatementContext statementContext) {
    }

    public StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        List<AgentInstanceStopCallback> stopCallbacks = new ArrayList<>();
        stopCallbacks.add(new AgentInstanceStopCallback() {
            public void stop(AgentInstanceStopServices services) {
                agentInstanceContext.getInternalEventRouter().removePreprocessing(desc.getEventType(), desc);
            }
        });

        Map<Integer, SubSelectFactoryResult> subselectActivations = SubSelectHelperStart.startSubselects(subselects, agentInstanceContext, stopCallbacks, isRecoveringResilient);

        boolean hasSubselect = !subselectActivations.isEmpty();
        agentInstanceContext.getInternalEventRouter().addPreprocessing(desc, viewable, agentInstanceContext.getStatementContext(), hasSubselect);

        AgentInstanceStopCallback stopCallback = AgentInstanceUtil.finalizeSafeStopCallbacks(stopCallbacks);
        return new StatementAgentInstanceFactoryUpdateResult(viewable, stopCallback, agentInstanceContext, subselectActivations);
    }

    public AIRegistryRequirements getRegistryRequirements() {
        return AIRegistryRequirements.noRequirements();
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
