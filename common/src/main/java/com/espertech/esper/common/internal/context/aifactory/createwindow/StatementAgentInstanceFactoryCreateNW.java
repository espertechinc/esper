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
package com.espertech.esper.common.internal.context.aifactory.createwindow;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.activator.ViewableActivationResult;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFilter;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryUtil;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowTailViewInstance;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewSimpleWProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWViewFactory;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.view.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;

public class StatementAgentInstanceFactoryCreateNW implements StatementAgentInstanceFactory, StatementReadyCallback {
    private static final Logger log = LoggerFactory.getLogger(StatementAgentInstanceFactoryCreateNW.class);

    private ViewableActivatorFilter activator;
    private String namedWindowName;
    private ViewFactory[] viewFactories;
    private ExprEvaluator insertFromFilter;
    private NamedWindow insertFromNamedWindow;
    private EventType asEventType;
    private ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider;

    public void setActivator(ViewableActivatorFilter activator) {
        this.activator = activator;
    }

    public void setNamedWindowName(String namedWindowName) {
        this.namedWindowName = namedWindowName;
    }

    public void setViewFactories(ViewFactory[] viewFactories) {
        this.viewFactories = viewFactories;
    }

    public void setInsertFromNamedWindow(NamedWindow insertFromNamedWindow) {
        this.insertFromNamedWindow = insertFromNamedWindow;
    }

    public void setInsertFromFilter(ExprEvaluator insertFromFilter) {
        this.insertFromFilter = insertFromFilter;
    }

    public void setAsEventType(EventType asEventType) {
        this.asEventType = asEventType;
    }

    public void setResultSetProcessorFactoryProvider(ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider) {
        this.resultSetProcessorFactoryProvider = resultSetProcessorFactoryProvider;
    }

    public EventType getStatementEventType() {
        return activator.getEventType();
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        NamedWindow namedWindow = statementContext.getNamedWindowManagementService().getNamedWindow(statementContext.getDeploymentId(), namedWindowName);
        namedWindow.setStatementContext(statementContext);
    }

    public void statementCreate(StatementContext statementContext) {
        // The filter lookupables for the as-type apply to this type, when used with contexts, as contexts generated filters for types
        if (statementContext.getContextRuntimeDescriptor() != null && asEventType != null) {
            NamedWindow namedWindow = statementContext.getNamedWindowManagementService().getNamedWindow(statementContext.getDeploymentId(), namedWindowName);
            statementContext.getFilterSharedLookupableRepository().applyLookupableFromType(asEventType, namedWindow.getRootView().getEventType(), statementContext.getStatementId());
        }
    }

    public void statementDestroy(StatementContext statementContext) {
        if (viewFactories[0] instanceof VirtualDWViewFactory) {
            ((VirtualDWViewFactory) viewFactories[0]).destroy();
        }
        statementContext.getNamedWindowManagementService().destroyNamedWindow(statementContext.getDeploymentId(), namedWindowName);
    }

    public StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        final List<AgentInstanceStopCallback> stopCallbacks = new ArrayList<>();

        //String windowName = statementSpec.getCreateWindowDesc().getWindowName();
        Viewable finalView;
        Viewable eventStreamParentViewable;
        Viewable topView;
        NamedWindowInstance namedWindowInstance;
        ViewableActivationResult viewableActivationResult;

        try {
            // Register interest
            viewableActivationResult = activator.activate(agentInstanceContext, false, isRecoveringResilient);
            stopCallbacks.add(viewableActivationResult.getStopCallback());
            eventStreamParentViewable = viewableActivationResult.getViewable();

            // Obtain processor for this named window
            NamedWindow namedWindow = agentInstanceContext.getNamedWindowManagementService().getNamedWindow(agentInstanceContext.getDeploymentId(), namedWindowName);
            if (namedWindow == null) {
                throw new RuntimeException("Failed to obtain named window '" + namedWindowName + "'");
            }

            // Allocate processor instance
            namedWindowInstance = new NamedWindowInstance(namedWindow, agentInstanceContext);
            View rootView = namedWindowInstance.getRootViewInstance();

            // Materialize views
            AgentInstanceViewFactoryChainContext viewFactoryChainContext = new AgentInstanceViewFactoryChainContext(agentInstanceContext, true, null, null);
            ViewablePair viewables = ViewFactoryUtil.materialize(viewFactories, eventStreamParentViewable, viewFactoryChainContext, stopCallbacks);

            eventStreamParentViewable.setChild(rootView);
            rootView.setParent(eventStreamParentViewable);
            topView = viewables.getTop();
            rootView.setChild((View) topView);
            finalView = viewables.getLast();

            // If this is a virtual data window implementation, bind it to the context for easy lookup
            AgentInstanceStopCallback envStopCallback = null;
            if (finalView instanceof VirtualDWView) {
                final String objectName = "/virtualdw/" + namedWindowName;
                final VirtualDWView virtualDWView = (VirtualDWView) finalView;
                try {
                    agentInstanceContext.getRuntimeEnvContext().bind(objectName, virtualDWView.getVirtualDataWindow());
                } catch (NamingException e) {
                    throw new ViewProcessingException("Invalid name for adding to context:" + e.getMessage(), e);
                }
                envStopCallback = new AgentInstanceStopCallback() {
                    public void stop(AgentInstanceStopServices stopServices) {
                        try {
                            virtualDWView.destroy();
                            stopServices.getAgentInstanceContext().getRuntimeEnvContext().unbind(objectName);
                        } catch (NamingException e) {
                        }
                    }
                };
            }
            final AgentInstanceStopCallback environmentStopCallback = envStopCallback;

            // destroy the instance
            AgentInstanceStopCallback allInOneStopMethod = new AgentInstanceStopCallback() {
                public void stop(AgentInstanceStopServices services) {
                    NamedWindowInstance instance = namedWindow.getNamedWindowInstance(agentInstanceContext);
                    if (instance == null) {
                        log.warn("Named window processor by name '" + namedWindowName + "' has not been found");
                    } else {
                        instance.destroy();
                    }
                    if (environmentStopCallback != null) {
                        environmentStopCallback.stop(services);
                    }
                }
            };
            stopCallbacks.add(allInOneStopMethod);

            // Attach tail view
            NamedWindowTailViewInstance tailView = namedWindowInstance.getTailViewInstance();
            finalView.setChild(tailView);
            tailView.setParent(finalView);
            finalView = tailView;

            // Attach output view
            Pair<ResultSetProcessor, AggregationService> pair = StatementAgentInstanceFactoryUtil.startResultSetAndAggregation(resultSetProcessorFactoryProvider, agentInstanceContext, false, null);
            OutputProcessViewSimpleWProcessor out = new OutputProcessViewSimpleWProcessor(agentInstanceContext, pair.getFirst());
            finalView.setChild(out);
            out.setParent(finalView);
            finalView = out;

            // Handle insert case
            if (insertFromNamedWindow != null && !isRecoveringResilient) {
                handleInsertFrom(agentInstanceContext, namedWindowInstance);
            }

        } catch (RuntimeException ex) {
            AgentInstanceStopCallback stopCallback = AgentInstanceUtil.finalizeSafeStopCallbacks(stopCallbacks);
            AgentInstanceUtil.stopSafe(stopCallback, agentInstanceContext);
            throw new EPException(ex.getMessage(), ex);
        }

        AgentInstanceStopCallback stopCallback = AgentInstanceUtil.finalizeSafeStopCallbacks(stopCallbacks);
        return new StatementAgentInstanceFactoryCreateNWResult(
                finalView, stopCallback, agentInstanceContext, eventStreamParentViewable, topView, namedWindowInstance, viewableActivationResult);
    }

    public boolean[] getPriorFlagPerStream() {
        return null;
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }

    private void handleInsertFrom(AgentInstanceContext agentInstanceContext, NamedWindowInstance processorInstance) {
        NamedWindowInstance sourceWindowInstances = insertFromNamedWindow.getNamedWindowInstance(agentInstanceContext);
        List<EventBean> events = new ArrayList<EventBean>();
        if (insertFromFilter != null) {
            EventBean[] eventsPerStream = new EventBean[1];
            for (EventBean candidate : sourceWindowInstances.getTailViewInstance()) {
                eventsPerStream[0] = candidate;
                Boolean result = (Boolean) insertFromFilter.evaluate(eventsPerStream, true, agentInstanceContext);
                if ((result == null) || (!result)) {
                    continue;
                }
                events.add(candidate);
            }
        } else {
            for (EventBean eventBean : sourceWindowInstances.getTailViewInstance()) {
                events.add(eventBean);
            }
        }
        if (events.size() > 0) {
            EventType rootViewType = processorInstance.getRootViewInstance().getEventType();
            EventBean[] convertedEvents = EventTypeUtility.typeCast(events, rootViewType, agentInstanceContext.getEventBeanTypedEventFactory(), agentInstanceContext.getEventTypeAvroHandler());
            processorInstance.getRootViewInstance().update(convertedEvents, null);
        }
    }

    public AIRegistryRequirements getRegistryRequirements() {
        return AIRegistryRequirements.noRequirements();
    }

    public String getAsEventTypeName() {
        return asEventType == null ? null : asEventType.getName();
    }
}
