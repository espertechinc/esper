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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.activator.ViewableActivationResult;
import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.start.EPStatementStartMethodCreateWindow;
import com.espertech.esper.core.start.EPStatementStartMethodHelperAssignExpr;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;
import com.espertech.esper.epl.named.NamedWindowTailViewInstance;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.view.OutputProcessViewFactory;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StatementAgentInstanceFactoryCreateWindow extends StatementAgentInstanceFactoryBase {
    private static final Logger log = LoggerFactory.getLogger(EPStatementStartMethodCreateWindow.class);

    protected final StatementContext statementContext;
    protected final StatementSpecCompiled statementSpec;
    protected final EPServicesContext services;
    protected final ViewableActivator activator;
    protected final ViewFactoryChain unmaterializedViewChain;
    protected final ResultSetProcessorFactoryDesc resultSetProcessorPrototype;
    protected final OutputProcessViewFactory outputProcessViewFactory;
    protected final boolean isRecoveringStatement;
    protected final ExprEvaluator createWindowInsertFilter;

    public StatementAgentInstanceFactoryCreateWindow(StatementContext statementContext, StatementSpecCompiled statementSpec, EPServicesContext services, ViewableActivator activator, ViewFactoryChain unmaterializedViewChain, ResultSetProcessorFactoryDesc resultSetProcessorPrototype, OutputProcessViewFactory outputProcessViewFactory, boolean recoveringStatement) {
        super(statementContext.getAnnotations());
        this.statementContext = statementContext;
        this.statementSpec = statementSpec;
        this.services = services;
        this.activator = activator;
        this.unmaterializedViewChain = unmaterializedViewChain;
        this.resultSetProcessorPrototype = resultSetProcessorPrototype;
        this.outputProcessViewFactory = outputProcessViewFactory;
        isRecoveringStatement = recoveringStatement;

        if (statementSpec.getCreateWindowDesc().getInsertFilter() != null) {
            createWindowInsertFilter = ExprNodeCompiler.allocateEvaluator(statementSpec.getCreateWindowDesc().getInsertFilter().getForge(), statementContext.getEngineImportService(), StatementAgentInstanceFactoryCreateWindow.class, false, statementContext.getStatementName());
        } else {
            createWindowInsertFilter = null;
        }
    }

    public StatementAgentInstanceFactoryCreateWindowResult newContextInternal(final AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        final List<StopCallback> stopCallbacks = new ArrayList<StopCallback>();

        String windowName = statementSpec.getCreateWindowDesc().getWindowName();
        Viewable finalView;
        Viewable eventStreamParentViewable;
        StatementAgentInstancePostLoad postLoad;
        Viewable topView;
        NamedWindowProcessorInstance processorInstance;
        ViewableActivationResult viewableActivationResult;

        try {
            // Register interest
            viewableActivationResult = activator.activate(agentInstanceContext, false, isRecoveringResilient);
            stopCallbacks.add(viewableActivationResult.getStopCallback());
            eventStreamParentViewable = viewableActivationResult.getViewable();

            // Obtain processor for this named window
            NamedWindowProcessor processor = services.getNamedWindowMgmtService().getProcessor(windowName);

            if (processor == null) {
                throw new RuntimeException("Failed to obtain named window processor for named window '" + windowName + "'");
            }

            // Allocate processor instance
            processorInstance = processor.addInstance(agentInstanceContext);
            View rootView = processorInstance.getRootViewInstance();
            eventStreamParentViewable.addView(rootView);

            // Materialize views
            AgentInstanceViewFactoryChainContext viewFactoryChainContext = new AgentInstanceViewFactoryChainContext(agentInstanceContext, true, null, null);
            ViewServiceCreateResult createResult = services.getViewService().createViews(rootView, unmaterializedViewChain.getViewFactoryChain(), viewFactoryChainContext, false);
            topView = createResult.getTopViewable();
            finalView = createResult.getFinalViewable();

            // add views to stop callback if applicable
            StatementAgentInstanceFactorySelect.addViewStopCallback(stopCallbacks, createResult.getNewViews());

            // If this is a virtual data window implementation, bind it to the context for easy lookup
            StopCallback envStopCallback = null;
            if (finalView instanceof VirtualDWView) {
                final String objectName = "/virtualdw/" + windowName;
                final VirtualDWView virtualDWView = (VirtualDWView) finalView;
                try {
                    services.getEngineEnvContext().bind(objectName, virtualDWView.getVirtualDataWindow());
                } catch (NamingException e) {
                    throw new ViewProcessingException("Invalid name for adding to context:" + e.getMessage(), e);
                }
                envStopCallback = new StopCallback() {
                    public void stop() {
                        try {
                            virtualDWView.destroy();
                            services.getEngineEnvContext().unbind(objectName);
                        } catch (NamingException e) {
                        }
                    }
                };
            }
            final StopCallback environmentStopCallback = envStopCallback;

            // Only if we are context-allocated: destroy the instance
            final String contextName = processor.getContextName();
            final int agentInstanceId = agentInstanceContext.getAgentInstanceId();
            StopCallback allInOneStopMethod = new StopCallback() {
                public void stop() {
                    String windowName = statementSpec.getCreateWindowDesc().getWindowName();
                    NamedWindowProcessor processor = services.getNamedWindowMgmtService().getProcessor(windowName);
                    if (processor == null) {
                        log.warn("Named window processor by name '" + windowName + "' has not been found");
                    } else {
                        NamedWindowProcessorInstance instance = processor.getProcessorInstanceAllowUnpartitioned(agentInstanceId);
                        if (instance != null) {
                            if (contextName != null) {
                                instance.destroy();
                            } else {
                                instance.stop();
                            }
                        }
                    }
                    if (environmentStopCallback != null) {
                        environmentStopCallback.stop();
                    }
                }
            };
            stopCallbacks.add(allInOneStopMethod);

            // Attach tail view
            NamedWindowTailViewInstance tailView = processorInstance.getTailViewInstance();
            finalView.addView(tailView);
            finalView = tailView;

            // obtain result set processor
            ResultSetProcessor resultSetProcessor = EPStatementStartMethodHelperAssignExpr.getAssignResultSetProcessor(agentInstanceContext, resultSetProcessorPrototype, false, null, false);

            // Attach output view
            View outputView = outputProcessViewFactory.makeView(resultSetProcessor, agentInstanceContext);
            finalView.addView(outputView);
            finalView = outputView;

            // obtain post load
            postLoad = processorInstance.getPostLoad();

            // Handle insert case
            if (statementSpec.getCreateWindowDesc().isInsert() && !isRecoveringStatement && !isRecoveringResilient) {
                String insertFromWindow = statementSpec.getCreateWindowDesc().getInsertFromWindow();
                NamedWindowProcessor namedWindowProcessor = services.getNamedWindowMgmtService().getProcessor(insertFromWindow);
                NamedWindowProcessorInstance sourceWindowInstances = namedWindowProcessor.getProcessorInstance(agentInstanceContext);
                List<EventBean> events = new ArrayList<EventBean>();
                if (createWindowInsertFilter != null) {
                    EventBean[] eventsPerStream = new EventBean[1];
                    for (Iterator<EventBean> it = sourceWindowInstances.getTailViewInstance().iterator(); it.hasNext(); ) {
                        EventBean candidate = it.next();
                        eventsPerStream[0] = candidate;
                        Boolean result = (Boolean) createWindowInsertFilter.evaluate(eventsPerStream, true, agentInstanceContext);
                        if ((result == null) || (!result)) {
                            continue;
                        }
                        events.add(candidate);
                    }
                } else {
                    for (Iterator<EventBean> it = sourceWindowInstances.getTailViewInstance().iterator(); it.hasNext(); ) {
                        events.add(it.next());
                    }
                }
                if (events.size() > 0) {
                    EventType rootViewType = rootView.getEventType();
                    EventBean[] convertedEvents = services.getEventAdapterService().typeCast(events, rootViewType);
                    rootView.update(convertedEvents, null);
                }
            }
        } catch (RuntimeException ex) {
            StopCallback stopCallback = StatementAgentInstanceUtil.getStopCallback(stopCallbacks, agentInstanceContext);
            StatementAgentInstanceUtil.stopSafe(stopCallback, statementContext);
            throw ex;
        }

        StatementAgentInstanceFactoryCreateWindowResult createWindowResult = new StatementAgentInstanceFactoryCreateWindowResult(finalView, null, agentInstanceContext, eventStreamParentViewable, postLoad, topView, processorInstance, viewableActivationResult);
        if (statementContext.getStatementExtensionServicesContext() != null) {
            statementContext.getStatementExtensionServicesContext().contributeStopCallback(createWindowResult, stopCallbacks);
        }

        log.debug(".start Statement start completed");
        StopCallback stopCallback = StatementAgentInstanceUtil.getStopCallback(stopCallbacks, agentInstanceContext);
        createWindowResult.setStopCallback(stopCallback);

        return createWindowResult;
    }

    public void assignExpressions(StatementAgentInstanceFactoryResult result) {
    }

    public void unassignExpressions() {
    }
}
