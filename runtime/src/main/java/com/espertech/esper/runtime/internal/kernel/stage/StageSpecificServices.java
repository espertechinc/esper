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
package com.espertech.esper.runtime.internal.kernel.stage;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.util.InternalEventRouteDest;
import com.espertech.esper.common.internal.context.util.InternalEventRouter;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.event.core.EventTypeResolvingBeanFactory;
import com.espertech.esper.common.internal.metrics.stmtmetrics.MetricReportingService;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.util.ManagedReadWriteLock;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleService;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterServiceSPI;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesEvaluation;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesPath;
import com.espertech.esper.runtime.internal.kernel.thread.ThreadingService;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceSPI;

public class StageSpecificServices implements EPServicesEvaluation, EPServicesPath {
    private final DeploymentLifecycleService deploymentLifecycleService;
    private final ManagedReadWriteLock eventProcessingRWLock;
    private final FilterServiceSPI filterService;
    private final InternalEventRouter internalEventRouter;
    private final MetricReportingService metricReportingService;
    private final SchedulingServiceSPI schedulingService;
    private final StageRuntimeServices stageRuntimeServices;
    private final ThreadingService threadingService;

    private final PathRegistry<String, NamedWindowMetaData> namedWindowPathRegistry = new PathRegistry<>(PathRegistryObjectType.NAMEDWINDOW);
    private final PathRegistry<String, ContextMetaData> contextPathRegistry = new PathRegistry<>(PathRegistryObjectType.CONTEXT);
    private final PathRegistry<String, EventType> eventTypesPathRegistry = new PathRegistry<>(PathRegistryObjectType.EVENTTYPE);
    private final PathRegistry<String, TableMetaData> tablesPathRegistry = new PathRegistry<>(PathRegistryObjectType.TABLE);
    private final PathRegistry<String, VariableMetaData> variablesPathRegistry = new PathRegistry<>(PathRegistryObjectType.VARIABLE);
    private final PathRegistry<String, ExpressionDeclItem> exprDeclaredPathRegistry = new PathRegistry<>(PathRegistryObjectType.EXPRDECL);
    private final PathRegistry<NameAndParamNum, ExpressionScriptProvided> scriptPathRegistry = new PathRegistry<>(PathRegistryObjectType.SCRIPT);
    private InternalEventRouteDest internalEventRouteDest;

    public StageSpecificServices(DeploymentLifecycleService deploymentLifecycleService, ManagedReadWriteLock eventProcessingRWLock, FilterServiceSPI filterService, InternalEventRouter internalEventRouter, MetricReportingService metricReportingService, SchedulingServiceSPI schedulingService, StageRuntimeServices stageRuntimeServices, ThreadingService threadingService) {
        this.deploymentLifecycleService = deploymentLifecycleService;
        this.eventProcessingRWLock = eventProcessingRWLock;
        this.filterService = filterService;
        this.internalEventRouter = internalEventRouter;
        this.metricReportingService = metricReportingService;
        this.schedulingService = schedulingService;
        this.stageRuntimeServices = stageRuntimeServices;
        this.threadingService = threadingService;
    }

    public void initialize(EPStageEventServiceSPI eventService) {
        this.internalEventRouteDest = eventService;
        this.metricReportingService.setContext(filterService, schedulingService, eventService);
    }

    public ManagedReadWriteLock getEventProcessingRWLock() {
        return eventProcessingRWLock;
    }

    public FilterServiceSPI getFilterService() {
        return filterService;
    }

    public DeploymentLifecycleService getDeploymentLifecycleService() {
        return deploymentLifecycleService;
    }

    public MetricReportingService getMetricReportingService() {
        return metricReportingService;
    }

    public SchedulingServiceSPI getSchedulingService() {
        return schedulingService;
    }

    public VariableManagementService getVariableManagementService() {
        return stageRuntimeServices.getVariableManagementService();
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return stageRuntimeServices.getExceptionHandlingService();
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return stageRuntimeServices.getTableExprEvaluatorContext();
    }

    public PathRegistry<String, NamedWindowMetaData> getNamedWindowPathRegistry() {
        return namedWindowPathRegistry;
    }

    public InternalEventRouteDest getInternalEventRouteDest() {
        return internalEventRouteDest;
    }

    public PathRegistry<String, ContextMetaData> getContextPathRegistry() {
        return contextPathRegistry;
    }

    public EventTypeResolvingBeanFactory getEventTypeResolvingBeanFactory() {
        return stageRuntimeServices.getEventTypeResolvingBeanFactory();
    }

    public ThreadingService getThreadingService() {
        return threadingService;
    }

    public PathRegistry<String, ExpressionDeclItem> getExprDeclaredPathRegistry() {
        return exprDeclaredPathRegistry;
    }

    public PathRegistry<String, EventType> getEventTypePathRegistry() {
        return eventTypesPathRegistry;
    }

    public PathRegistry<NameAndParamNum, ExpressionScriptProvided> getScriptPathRegistry() {
        return scriptPathRegistry;
    }

    public PathRegistry<String, TableMetaData> getTablePathRegistry() {
        return tablesPathRegistry;
    }

    public PathRegistry<String, VariableMetaData> getVariablePathRegistry() {
        return variablesPathRegistry;
    }

    public InternalEventRouter getInternalEventRouter() {
        return internalEventRouter;
    }

    public void destroy() {
        filterService.destroy();
        schedulingService.destroy();
        threadingService.destroy();
        metricReportingService.destroy();
    }
}
