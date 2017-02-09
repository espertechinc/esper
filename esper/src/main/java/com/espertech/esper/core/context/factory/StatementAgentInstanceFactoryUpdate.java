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
package com.espertech.esper.core.context.factory;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.InternalEventRouterDesc;
import com.espertech.esper.core.service.InternalRoutePreprocessView;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.start.EPStatementStartMethodHelperSubselect;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.spec.UpdateDesc;
import com.espertech.esper.util.StopCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatementAgentInstanceFactoryUpdate extends StatementAgentInstanceFactoryBase {

    private static final Logger log = LoggerFactory.getLogger(StatementAgentInstanceFactoryUpdate.class);

    private final StatementContext statementContext;
    private final EPServicesContext services;
    private final EventType streamEventType;
    private final UpdateDesc desc;
    private final InternalRoutePreprocessView onExprView;
    private final InternalEventRouterDesc routerDesc;
    private final SubSelectStrategyCollection subSelectStrategyCollection;

    public StatementAgentInstanceFactoryUpdate(StatementContext statementContext, EPServicesContext services, EventType streamEventType, UpdateDesc desc, InternalRoutePreprocessView onExprView, InternalEventRouterDesc routerDesc, SubSelectStrategyCollection subSelectStrategyCollection) {
        super(statementContext.getAnnotations());
        this.statementContext = statementContext;
        this.services = services;
        this.streamEventType = streamEventType;
        this.desc = desc;
        this.onExprView = onExprView;
        this.subSelectStrategyCollection = subSelectStrategyCollection;
        this.routerDesc = routerDesc;
    }

    public StatementAgentInstanceFactoryUpdateResult newContextInternal(final AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        final List<StopCallback> stopCallbacks = new ArrayList<StopCallback>();

        Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies;

        try {
            stopCallbacks.add(new StopCallback() {
                public void stop() {
                    services.getInternalEventRouter().removePreprocessing(streamEventType, desc);
                }
            });

            services.getInternalEventRouter().addPreprocessing(routerDesc, onExprView, agentInstanceContext.getAgentInstanceLock(), !subSelectStrategyCollection.getSubqueries().isEmpty());

            // start subselects
            subselectStrategies = EPStatementStartMethodHelperSubselect.startSubselects(services, subSelectStrategyCollection, agentInstanceContext, stopCallbacks, isRecoveringResilient);
        } catch (RuntimeException ex) {
            StopCallback stopCallback = StatementAgentInstanceUtil.getStopCallback(stopCallbacks, agentInstanceContext);
            StatementAgentInstanceUtil.stopSafe(stopCallback, statementContext);
            throw ex;
        }

        StatementAgentInstanceFactoryUpdateResult result = new StatementAgentInstanceFactoryUpdateResult(onExprView, null, agentInstanceContext, subselectStrategies);
        if (statementContext.getStatementExtensionServicesContext() != null) {
            statementContext.getStatementExtensionServicesContext().contributeStopCallback(result, stopCallbacks);
        }

        StopCallback stopCallback = StatementAgentInstanceUtil.getStopCallback(stopCallbacks, agentInstanceContext);
        result.setStopCallback(stopCallback);

        return result;
    }

    public void assignExpressions(StatementAgentInstanceFactoryResult result) {
    }

    public void unassignExpressions() {
    }
}
